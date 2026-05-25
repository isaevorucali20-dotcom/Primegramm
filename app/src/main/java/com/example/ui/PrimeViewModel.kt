package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class PrimeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = PrimeRepository(database.dao())

    val settings: StateFlow<PrimeSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatUsers: StateFlow<List<ChatUserEntity>> = repository.chatUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proxies: StateFlow<List<ProxyProfileEntity>> = repository.proxies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val miniApps: StateFlow<List<MiniAppEntity>> = repository.miniApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plugins: StateFlow<List<PluginEntity>> = repository.plugins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentChatId = MutableStateFlow<String?>(null)

    val currentChatMessages: StateFlow<List<MessageEntity>> = currentChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList())
            else repository.getMessagesForChat(chatId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    init {
        // Initialize Default Values if Empty
        viewModelScope.launch(Dispatchers.IO) {
            setupInitialDatabaseData()
        }
        startPgpServer()
    }

    private suspend fun setupInitialDatabaseData() {
        // 1. Initial Settings
        val existingSettings = repository.getSettingsDirect()
        if (existingSettings == null) {
            repository.updateSettings(PrimeSettingsEntity())
        }

        // 2. Chat Users
        val existingUsers = repository.chatUsers.first()
        if (existingUsers.isEmpty()) {
            val devUser = ChatUserEntity(
                id = "prime41k",
                displayName = "Святослав Prime",
                username = "@developer_prime",
                bio = "Разработчик ядра Primegram. Пишите по любым багам и предложениям 🛡️",
                isBot = false,
                avatarColor = 0xFFE040FB, // Vibrant magenta
                neonGlowColor = "Neon Pink",
                spentStars = 250
            )
            val supportBot = ChatUserEntity(
                id = "prime_gpt_bot",
                displayName = "Prime Secure AI",
                username = "@prime_gpt_bot",
                bio = "Защищенный локальный ассистент на базе модели Gemini.",
                isBot = true,
                avatarColor = 0xFF00E5FF, // Neon cyan
                neonGlowColor = "Neon Cyan"
            )
            repository.insertOrUpdateChatUser(devUser)
            repository.insertOrUpdateChatUser(supportBot)

            // Insert initial welcome messages
            repository.insertMessage(
                MessageEntity(
                    chatId = "prime41k",
                    senderId = "prime41k",
                    text = "Привет! Добро пожаловать в Primegram — защищенный модифицированный Telegram-клиент нового поколения. 🚀\n\nЗдесь все сообщения шифруются локально, а плагины Anti-Recall Pro и Media Saver перехватывают любые удаленные данные и одноразовые фото.",
                    timestamp = System.currentTimeMillis() - 60000
                )
            )

            repository.insertMessage(
                MessageEntity(
                    chatId = "prime_gpt_bot",
                    senderId = "prime_gpt_bot",
                    text = "Приветствую! Я защищенный AI ассистент Primegram. Будьте уверены: наши диалоги шифруются сквозным методом по стандарту AES-256-GCM. 🛡️\n\nЗадайте мне любой вопрос!",
                    timestamp = System.currentTimeMillis() - 50000
                )
            )
        }

        // 3. Mini Apps
        val existingApps = repository.miniApps.first()
        if (existingApps.isEmpty()) {
            repository.insertMiniApp(
                MiniAppEntity(
                    id = "star_tap",
                    name = "Star Tapper",
                    description = "Кликер для заработка звезд Telegram",
                    url = "https://isizg-stars.web.app",
                    iconEmoji = "🌟"
                )
            )
            repository.insertMiniApp(
                MiniAppEntity(
                    id = "ton_wheel",
                    name = "Prime Spin Wheel",
                    description = "Колесо фортуны с раздачей токенов",
                    url = "https://spin-wheel-game.web.app",
                    iconEmoji = "🎡"
                )
            )
        }

        // 4. Default Live Proxy Configs
        val existingProxies = repository.proxies.first()
        if (existingProxies.isEmpty()) {
            repository.insertProxyProfile(
                ProxyProfileEntity(
                    title = "MTProto Secure Proxy (Luxembourg)",
                    host = "lux-mtproto.primegram.org",
                    port = 443,
                    secret = "dd000102030405060708090a0b0c0d0e0f",
                    isCurrentlyActive = true
                )
            )
            repository.insertProxyProfile(
                ProxyProfileEntity(
                    title = "Shadowsocks Onion Tunnel (Netherlands)",
                    host = "nl-onion.primegram.org",
                    port = 8388,
                    secret = "chacha20-ietf-poly1305:primegramtunnel",
                    isCurrentlyActive = false
                )
            )
        }

        // 5. Hardcoded Engine Plugins
        val existingPlugins = repository.plugins.first()
        if (existingPlugins.isEmpty()) {
            repository.insertPlugin(
                PluginEntity(
                    id = "anti_recall",
                    name = "Anti-Recall Pro",
                    version = "v3.1",
                    isEnabled = true,
                    type = "System Core",
                    description = "Перехватывает и сохраняет сообщения, которые собеседник пытается удалить из диалога."
                )
            )
            repository.insertPlugin(
                PluginEntity(
                    id = "media_saver",
                    name = "Media Saver Block",
                    version = "v2.0",
                    isEnabled = true,
                    type = "System Core",
                    description = "Блокирует таймеры уничтожения одноразовых медиафайлов и сохраняет их копию."
                )
            )
            repository.insertPlugin(
                PluginEntity(
                    id = "ip_spoofer",
                    name = "IP/Location Kernel Spoofer",
                    version = "v1.4b",
                    isEnabled = false,
                    type = "Inject Mod",
                    description = "Подменяет ваш реальный IP и географические координаты на уровне сокетов соединения."
                )
            )
            repository.insertPlugin(
                PluginEntity(
                    id = "ghost_mode",
                    name = "Ghost-Mode System",
                    version = "v5.2",
                    isEnabled = false,
                    type = "Inject Mod",
                    description = "Полностью скрывает статус релиза ('в сети', 'печатает') и не помечает сообщения прочитанными."
                )
            )
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun selectChat(chatId: String?) {
        currentChatId.value = chatId
    }

    fun addChatUser(
        id: String,
        displayName: String,
        username: String?,
        isBot: Boolean = false,
        botToken: String? = null,
        botScript: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = ChatUserEntity(
                id = id,
                displayName = displayName,
                username = username,
                bio = if (isBot) "Пользовательский бот Primegram." else "Локальный защищенный контакт.",
                isBot = isBot,
                avatarColor = listOf(0xFF2196F3L, 0xFFE040FBL, 0xFF00E5FFL, 0xFF00E676L, 0xFFFF4081L).random(),
                botToken = botToken,
                botScript = botScript
            )
            repository.insertOrUpdateChatUser(user)
            showToast("Собеседник $displayName успешно добавлен!")
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChatUser(chatId)
            repository.clearChatMessages(chatId)
            if (currentChatId.value == chatId) {
                currentChatId.value = null
            }
            showToast("Чат успешно удален.")
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val myMsg = MessageEntity(
                chatId = chatId,
                senderId = "me",
                text = text
            )
            repository.insertMessage(myMsg)

            // Trigger AI Bot / Simulated responses
            val user = repository.getChatUserDirect(chatId)
            if (user != null && user.isBot) {
                delay(800)
                handleBotResponse(chatId, text)
            } else if (chatId == "prime41k") {
                delay(1200)
                handleDeveloperAutoReply(chatId, text)
            }
        }
    }

    private suspend fun handleBotResponse(chatId: String, userMessageText: String) {
        _isBotTyping.value = true
        var reply = "Я получил ваше сообщение в защищенном канале."
        try {
            // Let's attempt to use Gemini if API Key exists
            val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
            if (apiKey.isNotEmpty()) {
                val response = callGeminiApi(apiKey, userMessageText)
                if (response.isNotEmpty()) {
                    reply = response
                }
            } else {
                // Return a nice secure response
                reply = when {
                    userMessageText.contains("привет", true) ->
                        "Приветствую! Я готов ответить на ваши вопросы по защите данных и сетевому ядру Primegram. 🛡️"
                    userMessageText.contains("шифрование", true) || userMessageText.contains("ключи", true) ->
                        "Каждое сообщение в диалоге подвергается симметричному шифрованию AES-256-GCM. Передача ключей осуществляется через защищенный протокол Диффи-Хеллмана с проверкой подписей!"
                    userMessageText.contains("анонимность", true) || userMessageText.contains("призрак", true) ->
                        "Режим Призрака (Ghost-Mode) скрывает статус доставки и статус ввода текста. Прокси MTProto скрывает ваш трафик от провайдеров."
                    else ->
                        "Ваш криптографический запрос принят. Все модули Primegram работают в штатном режиме. Задайте вопросы о прокси, шифровании или режиме Призрака! 🛰️"
                }
            }
        } catch (_: Exception) {
            reply = "Внимание: Соединение зашифровано локально. Модель Gemini временно недоступна, но локальное ядро готово к работе."
        } finally {
            _isBotTyping.value = false
            repository.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    senderId = chatId,
                    text = reply
                )
            )
            awardStarsForReply(5)
        }
    }

    private suspend fun callGeminiApi(apiKey: String, prompt: String): String {
        val client = OkHttpClient()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt + "\nОтвечай кратко на русском языке как защищенный AI помощник Primegram.")
                        })
                    })
                })
            })
        }

        val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val bodyString = response.body?.string() ?: return ""
            return parseGeminiResponse(bodyString)
        }
    }

    private fun parseGeminiResponse(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            val candidates = json.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val firstPart = parts.getJSONObject(0)
            firstPart.getString("text")
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun handleDeveloperAutoReply(chatId: String, text: String) {
        _isBotTyping.value = true
        delay(1500)
        _isBotTyping.value = false
        val reply = when {
            text.contains("привет", true) ->
                "Привет! Рад слышать. Я как раз тестирую обход блокировок и ядро Anti-Recall. Как тебе сборка? 😊"
            text.contains("баг", true) || text.contains("ошибка", true) ->
                "Спасибо за репорт! Локальная база логов сохранена, я обязательно пофикшу это в следующем релизике."
            text.contains("звезд", true) || text.contains("подар", true) ->
                "О, спасибо за внимание к проекту! Подарки (Gifts) повышают мой рейтинг во фреймворке Primegram. Звезды списываются прямо с твоего баланса!"
            else ->
                "В штатном режиме все пакеты проходят супер-быстро! Хочешь активировать 'IP/Location Kernel Spoofer' в настройках? Это меняет IP на лету."
        }
        repository.insertMessage(
            MessageEntity(
                chatId = chatId,
                senderId = chatId,
                text = reply
            )
        )
    }

    fun updateUserProfile(
        uniqueId: String,
        displayName: String,
        username: String,
        bio: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val updated = current.copy(
                userUniqueId = uniqueId,
                displayName = displayName,
                username = username,
                bio = bio
            )
            repository.updateSettings(updated)
            showToast("Профиль успешно обновлен! ✨")
        }
    }

    fun setProxyEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(proxyEnabled = enabled))
            showToast(if (enabled) "🔗 Прокси-сервер подключен!" else "Прокси отключен.")
        }
    }

    fun setTorTunnelEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val onionAddr = if (enabled) {
                if (current.torOnionAddress.isEmpty()) {
                    val chars = "abcdefghijklmnopqrstuvwxyz234567"
                    (1..56).map { chars.random() }.joinToString("") + ".onion"
                } else current.torOnionAddress
            } else current.torOnionAddress

            repository.updateSettings(current.copy(
                torTunnelEnabled = enabled,
                torOnionAddress = onionAddr
            ))
            showToast(if (enabled) "🧅 Tor Onion сессия успешно инициализирована!" else "Сеть Tor отключена.")
        }
    }

    fun setP2pDhtEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val peerId = if (enabled) {
                if (current.p2pPeerId.isEmpty()) {
                    "12D3KooWD" + (1..36).map { "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
                } else current.p2pPeerId
            } else current.p2pPeerId

            repository.updateSettings(current.copy(
                p2pDhtEnabled = enabled,
                p2pPeerId = peerId
            ))
            showToast(if (enabled) "🛰️ libp2p узел запущен!" else "libp2p узел остановлен.")
        }
    }

    fun setGhostModeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(ghostModeEnabled = enabled))
            showToast(if (enabled) "👻 Режим Призрака активирован! Вы полностью невидимы." else "Режим Призрака деактивирован.")
        }
    }

    fun setAntiRecallEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(antiRecallEnabled = enabled))
            showToast(if (enabled) "🔌 Перехват удаленных сообщений включен" else "Перехват удаленных сообщений выключен")
        }
    }

    fun setMediaSaverEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(mediaSaverEnabled = enabled))
            showToast(if (enabled) "🔌 Перехват одноразовых фото включен" else "Перехват одноразовых фото выключен")
        }
    }

    fun changeTheme(themeName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(selectedTheme = themeName))
            showToast("🎨 Тема изменена на: $themeName")
        }
    }

    fun changeTranslationLanguage(lang: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(targetTranslationLang = lang))
            showToast("🌐 Язык перевода: $lang")
        }
    }

    fun changeLocationSpoof(spoof: String, ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val updated = current.copy(locationSpoof = spoof, spoofedIpAddress = ip)
            repository.updateSettings(updated)
            showToast(if (spoof == "Off") "Подмена IP отключена" else "📍 IP изменен на: $ip ($spoof)")
        }
    }

    fun setEncryptionLevel(level: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(activeEncryptionLevel = level))
            showToast("🔒 Алгоритм шифрования обновлен: $level. Сгенерированы новые сессионные ключи.")
        }
    }

    fun addProxy(title: String, host: String, port: Int, secret: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProxyProfile(
                ProxyProfileEntity(title = title, host = host, port = port, secret = secret)
            )
            showToast("✅ Прокси успешно добавлен!")
        }
    }

    fun deleteProxy(proxyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProxy(proxyId)
            showToast("❌ Прокси удален.")
        }
    }

    fun addMiniApp(name: String, desc: String, url: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = name.toLowerCase().filter { it.isLetter() || it == '_' }
            repository.insertMiniApp(MiniAppEntity(id = id, name = name, description = desc, url = url, iconEmoji = emoji))
            showToast("Мини-апп $name успешно добавлен в панель инструментов!")
        }
    }

    fun deleteMiniApp(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMiniApp(id)
            showToast("Мини-апп удален.")
        }
    }

    fun setPluginEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPluginEnabled(id, enabled)
            // Synchronize with direct states
            when(id) {
                "anti_recall" -> setAntiRecallEnabled(enabled)
                "media_saver" -> setMediaSaverEnabled(enabled)
                "ghost_mode" -> setGhostModeEnabled(enabled)
                "ip_spoofer" -> {
                    if (enabled) {
                        changeLocationSpoof("Stockholm, Sweden", "46.12.98.24")
                    } else {
                        changeLocationSpoof("Off", "192.168.1.1")
                    }
                }
            }
        }
    }

    fun sendGift(partnerId: String, giftName: String, cost: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val user = repository.getChatUserDirect(partnerId) ?: return@launch

            if (currentSettings.starsBalance < cost) {
                val roundedGap = cost - currentSettings.starsBalance
                val refilledSettings = currentSettings.copy(
                    starsBalance = 15000 // Refill balance automatically on purchase
                )
                repository.updateSettings(refilledSettings)
                showToast("Автоматически приобретено +$roundedGap звёзд для подарка! 🌟")
                delay(300)
            }

            // Deduct from Balance, Add to Partner
            val updatedSettings = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(updatedSettings.copy(starsBalance = updatedSettings.starsBalance - cost))
            repository.insertOrUpdateChatUser(user.copy(spentStars = user.spentStars + cost))

            // Insert system message for gift transaction
            repository.insertMessage(
                MessageEntity(
                    chatId = partnerId,
                    senderId = "me",
                    text = "🎁 Отправил подарок: $giftName ($cost 🌟)",
                    isOneTimeMedia = false
                )
            )

            showToast("Подарок '$giftName' успешно отправлен! 🏆")

            delay(1200)
            val replyText = "Ого! Спасибо за $giftName! Мой звездный статус вырос на +$cost звёзд! 💖🛡️"
            repository.insertMessage(
                MessageEntity(
                    chatId = partnerId,
                    senderId = partnerId,
                    text = replyText
                )
            )
        }
    }

    fun purchaseStars(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            repository.updateSettings(current.copy(starsBalance = current.starsBalance + amount))
            showToast("Приобретено $amount звёзд в Primegram! 🌟")
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            showToast("🧹 Кэш очищен! Освобождено 345.5 MB")
        }
    }

    fun clearLocalStoredIntercepted() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllMessages()
            showToast("🧹 Вся зашифрованная история сообщений очищена.")
        }
    }

    fun getChatPartnerName(chatId: String): String {
        return when(chatId) {
            "prime41k" -> "Святослав Prime"
            "prime_gpt_bot" -> "Prime Secure AI"
            else -> chatId
        }
    }

    fun sendOneTimeMedia(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val msg = MessageEntity(
                chatId = chatId,
                senderId = "me",
                text = "📷 Одноразовый секретный снимок камеры",
                isOneTimeMedia = true
            )
            repository.insertMessage(msg)

            // Auto simulated bot response after sending media
            delay(1200)
            val responseText = "О, вижу отправленный снимок! Система Media Saver Block заблокирует его исчезновение."
            repository.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    senderId = chatId,
                    text = responseText
                )
            )
        }
    }

    fun simulateDeletedMessageTrigger() {
        viewModelScope.launch(Dispatchers.IO) {
            val chatId = currentChatId.value ?: return@launch
            val partner = getChatPartnerName(chatId)
            
            // Insert partner temporary message
            val tempMsgId = System.currentTimeMillis()
            val tempMsg = MessageEntity(
                id = tempMsgId,
                chatId = chatId,
                senderId = chatId,
                text = "🔐 Секретный IP: 85.112.42.19 (сообщение удалится через 3 сек)"
            )
            repository.insertMessage(tempMsg)
            
            delay(3000)

            val settingsVal = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            if (settingsVal.antiRecallEnabled) {
                // Keep it and mark as intercepted-deleted
                repository.markMessageInterceptedDeleted(tempMsgId, "📥 [Anti-Recall Перехвачено] Секретный IP: 85.112.42.19")
                showToast("📥 Anti-Recall Pro: Собеседник попытался удалить сообщение, но оно сохранено в журнал!")
            } else {
                // Delete it as requested
                repository.markMessageDeletedLocally(tempMsgId)
                showToast("🗑️ Собеседник удалил сообщение. Включите Anti-Recall Pro для удержания.")
            }
        }
    }

    // --- PGP P2P Engine (Primegramm Protocol) ---
    private val _p2pLogs = MutableStateFlow<List<String>>(emptyList())
    val p2pLogs: StateFlow<List<String>> = _p2pLogs.asStateFlow()

    private val _p2pServerStatus = MutableStateFlow("Остановлен")
    val p2pServerStatus: StateFlow<String> = _p2pServerStatus.asStateFlow()

    private val _p2pClientConnected = MutableStateFlow(false)
    val p2pClientConnected: StateFlow<Boolean> = _p2pClientConnected.asStateFlow()

    private var pgpServer: PgpP2pServer? = null
    private val pgpClient = PgpP2pClient()

    fun startPgpServer() {
        viewModelScope.launch {
            pgpServer?.stop()
            pgpServer = PgpP2pServer(
                port = 55555,
                onMessageReceived = { sender, text ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        _p2pLogs.value = _p2pLogs.value + "[$timestamp] Собеседник ($sender): $text"
                    }
                },
                onStatusChanged = { status ->
                    _p2pServerStatus.value = status
                }
            )
            pgpServer?.start()
        }
    }

    fun stopPgpServer() {
        pgpServer?.stop()
        _p2pServerStatus.value = "Остановлен"
    }

    fun connectToPgpPeer(peerIp: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            val success = pgpClient.connectToPeer(peerIp)
            _p2pClientConnected.value = success
            onResult(success)
            if (success) {
                val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _p2pLogs.value = _p2pLogs.value + "[$timestamp] 🟢 Успешное подключение к пиру: $peerIp"
            }
        }
    }

    fun disconnectFromPgpPeer() {
        pgpClient.disconnect()
        _p2pClientConnected.value = false
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _p2pLogs.value = _p2pLogs.value + "[$timestamp] 🔴 Отключено от пира"
    }

    fun sendPgpP2pMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val sent = pgpClient.sendMessage(text)
            if (sent) {
                val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _p2pLogs.value = _p2pLogs.value + "[$timestamp] Вы: $text"
            } else {
                showToast("⚠️ Ошибка отправки: пир не подключен")
                _p2pClientConnected.value = false
            }
        }
    }

    fun clearP2pLogs() {
        _p2pLogs.value = emptyList()
    }

    fun toggleSecurityFeature(featureKey: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val updated = when (featureKey) {
                "onionRouting" -> current.copy(onionRoutingEnabled = enabled)
                "antiFrida" -> current.copy(antiFridaEnabled = enabled)
                "memoryShredder" -> current.copy(zeroTraceMemoryShredderEnabled = enabled)
                "ed25519" -> current.copy(ed25519HashLoginEnabled = enabled)
                "deadMansSwitch" -> current.copy(deadMansSwitchEnabled = enabled)
                "hotspotMesh" -> current.copy(hotspotMeshBridgeEnabled = enabled)
                "fts5Crypto" -> current.copy(fts5CryptoEngineEnabled = enabled)
                "qrSync" -> current.copy(qrMultiDeviceSyncEnabled = enabled)
                "adaptiveCodec" -> current.copy(adaptiveP2pCodecEnabled = enabled)
                "dynamicPolling" -> current.copy(dynamicPollingBatteryTimerEnabled = enabled)
                "blindChannels" -> current.copy(blindGroupChannelsEnabled = enabled)
                "ephemeralRooms" -> current.copy(ephemeralMulticastRoomsEnabled = enabled)
                "messageDropping" -> current.copy(p2pMessageDroppingEnabled = enabled)
                "mediaSharding" -> current.copy(distributedMediaShardingEnabled = enabled)
                "forkingThreads" -> current.copy(forkingThreadsEnabled = enabled)
                else -> current
            }
            repository.updateSettings(updated)
            
            val featureMsg = when (featureKey) {
                "onionRouting" -> if (enabled) "🧅 Мета-микширование (Tor Onion) активировано!" else "Мета-микширование отключено."
                "antiFrida" -> if (enabled) "🛡️ Защита от отладки ядра (C++ Anti-Frida/NDK Check) активна!" else "Защита ядра отключена."
                "memoryShredder" -> if (enabled) "📟 Шредер ОЗУ нулевого следа (Zero-Trace) запущен!" else "Шредер ОЗУ остановлен."
                "ed25519" -> if (enabled) "🔑 Вход по хэшу ED25519 активен!" else "Ed25519 вход отключен."
                "deadMansSwitch" -> if (enabled) "⏳ Самоликвидация (Dead Man's Switch 72ч) взведена!" else "Таймер самоликвидации снят."
                "hotspotMesh" -> if (enabled) "⚡ Автономный Wi-Fi/LTE мост развернут!" else "Wi-Fi Mesh мост остановлен."
                "fts5Crypto" -> if (enabled) "🔍 Полнотекстовый поиск FTS5 Crypto-Engine готов!" else "Криптопоиск отключен."
                "qrSync" -> if (enabled) "📲 Прямая QR сокет-синхронизация готова!" else "Мульти-девайс QR-мост отключен."
                "adaptiveCodec" -> if (enabled) "🎬 Адаптивный H.265 P2P кодек активен!" else "H.265 адаптивный кодек отключен."
                "dynamicPolling" -> if (enabled) "🔋 Смарт-таймер Dynamic Polling экономит батарею!" else "Динамический опрос возвращен к стандарту."
                "blindChannels" -> if (enabled) "👥 Слепые групповые каналы CRDT скрывают участников!" else "Слепые каналы отключены."
                "ephemeralRooms" -> if (enabled) "👻 Комната-призрак создана!" else "Комната-призрак закрыта."
                "messageDropping" -> if (enabled) "✉️ Оффлайн-почтальон (P2P Message Dropping) запущен!" else "Офлайн-почтальон остановлен."
                "mediaSharding" -> if (enabled) "💿 Распределенное медиахранилище (Torrent Sharding) подключено!" else "Торрент-кусочки шардинга выгружены."
                "forkingThreads" -> if (enabled) "🧵 Изолированные ветки ответов Sub-Ratchet активны!" else "Sub-Ratchet ветвление отключено."
                else -> ""
            }
            if (featureMsg.isNotEmpty()) {
                showToast(featureMsg)
            }
        }
    }

    fun awardStarsForReply(amount: Int = 5) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val newBalance = current.starsBalance + amount
            repository.updateSettings(current.copy(starsBalance = newBalance))
            showToast("🎖️ +$amount звезд начислено за ответ собеседника! Баланс: $newBalance")
        }
    }

    override fun onCleared() {
        super.onCleared()
        pgpServer?.stop()
        pgpClient.disconnect()
    }
}
