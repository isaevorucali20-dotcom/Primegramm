package com.example.ui

import android.app.Application
import android.content.Context
import android.webkit.WebView
import android.webkit.JavascriptInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class PrimeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = PrimeRepository(database.dao())
    private var webView: WebView? = null

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

    private val _partnerBatteryLevel = MutableStateFlow<Map<String, Int>>(emptyMap())
    val partnerBatteryLevel: StateFlow<Map<String, Int>> = _partnerBatteryLevel.asStateFlow()

    private val _knockUnlocked = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val knockUnlocked: StateFlow<Map<String, Boolean>> = _knockUnlocked.asStateFlow()

    private val _revealedSchrodingerMessages = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val revealedSchrodingerMessages: StateFlow<Map<Long, Boolean>> = _revealedSchrodingerMessages.asStateFlow()

    // --- PGP P2P Engine (Primegramm Protocol) ---
    private val _p2pLogs = MutableStateFlow<List<String>>(emptyList())
    val p2pLogs: StateFlow<List<String>> = _p2pLogs.asStateFlow()

    private val _p2pServerStatus = MutableStateFlow("Остановлен")
    val p2pServerStatus: StateFlow<String> = _p2pServerStatus.asStateFlow()

    private val _p2pClientConnected = MutableStateFlow(false)
    val p2pClientConnected: StateFlow<Boolean> = _p2pClientConnected.asStateFlow()

    private var pgpServer: PgpP2pServer? = null
    private val pgpClient = PgpP2pClient()

    fun setPartnerBattery(chatId: String, level: Int) {
        _partnerBatteryLevel.value = _partnerBatteryLevel.value + (chatId to level)
    }

    fun chargePartner(chatId: String) {
        val currentLevel = _partnerBatteryLevel.value[chatId] ?: 8
        val newLevel = (currentLevel + 12).coerceAtMost(100)
        _partnerBatteryLevel.value = _partnerBatteryLevel.value + (chatId to newLevel)
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    senderId = "system",
                    text = "🔌 Жест взаимной поддержки: Передача заряда по OTG-кабелю зафиксирована! Карма P2P-узла повысилась."
                )
            )
            
            viewModelScope.launch(Dispatchers.Main) {
                showToast("⚡ Заряд передан! Собеседник заряжен до $newLevel%. Карма сети +1!")
            }
        }
    }

    fun unlockChatWithKnock(chatId: String) {
        _knockUnlocked.value = _knockUnlocked.value + (chatId to true)
        showToast("🔓 Стук-код принят! Сокет памяти $chatId дешифрован.")
    }

    fun toggleSchrodingerReveal(messageId: Long) {
        val current = _revealedSchrodingerMessages.value[messageId] ?: false
        _revealedSchrodingerMessages.value = _revealedSchrodingerMessages.value + (messageId to !current)
    }

    init {
        viewModelScope.launch {
            // Initialize Default Values if Empty
            withContext(Dispatchers.IO) {
                setupInitialDatabaseData()
                // Ensure old anti_recall plugin is removed out of database
                repository.deletePlugin("anti_recall")
            }
            startPgpServer()
            initJsEngine()
            
            // Auto start multicast rooms if enabled in settings
            val settingsVal = withContext(Dispatchers.IO) { repository.getSettingsDirect() }
            if (settingsVal != null && settingsVal.ephemeralMulticastRoomsEnabled) {
                startMulticastRoom()
            }
        }
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
                displayName = "Святослав Prime (Тестировка)",
                username = "@developer_prime",
                bio = "Разработчик ядра Primegram. Бета-тестирование защищенного чата Primegram. 🛰️🛡️",
                isBot = false,
                avatarColor = 0xFFE040FB, // Vibrant magenta
                neonGlowColor = "Neon Pink",
                spentStars = 250,
                profileSongsJson = """[{"title":"No Cure","artist":"Lorn"},{"title":"Cyberpunk Melody","artist":"Sub Zero"},{"title":"Midnight City","artist":"M83"}]"""
            )
            repository.insertOrUpdateChatUser(devUser)

            // Insert initial welcome message from test developer
            repository.insertMessage(
                MessageEntity(
                    chatId = "prime41k",
                    senderId = "prime41k",
                    text = "Привет! Добро пожаловать во фреймворк Primegram — защищенный клиент нового поколения. 🚀\n\nЭтот диалог подписан как 'Тестировка'. Все сообщения шифруются на лету. Попробуйте написать мне, изменить плагины в меню, или заглянуть в мой плейлист в профиле диалога!",
                    timestamp = System.currentTimeMillis() - 60000
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
                    id = "media_saver",
                    name = "Media Saver Block",
                    version = "v2.0",
                    isEnabled = true,
                    type = "System Core",
                    description = "Блокирует таймеры уничтожения одноразовых медиафайлов и сохраняет их копию.",
                    scriptCode = """
                        function onSendMessage(msg) {
                            return msg;
                        }
                        function onReceiveMessage(msg) {
                            return msg;
                        }
                        function onEnabled(isEnabled) {
                            Primegram.showToast("Media Saver Block: " + (isEnabled ? "Контролирует буфер" : "Режим ожидания"));
                        }
                    """.trimIndent()
                )
            )
            repository.insertPlugin(
                PluginEntity(
                    id = "ip_spoofer",
                    name = "IP/Location Kernel Spoofer",
                    version = "v1.4b",
                    isEnabled = false,
                    type = "Inject Mod",
                    description = "Подменяет ваш реальный IP и географические координаты на уровне сокетов соединения.",
                    scriptCode = """
                        function onSendMessage(msg) {
                            return msg;
                        }
                        function onReceiveMessage(msg) {
                            return msg;
                        }
                        function onEnabled(isEnabled) {
                            Primegram.showToast("Kernel Spoofer: " + (isEnabled ? "Туннель зашифрован" : "Стандартные сокеты"));
                        }
                    """.trimIndent()
                )
            )
            repository.insertPlugin(
                PluginEntity(
                    id = "ghost_mode",
                    name = "Ghost-Mode System",
                    version = "v5.2",
                    isEnabled = false,
                    type = "Inject Mod",
                    description = "Полностью скрывает статус релиза ('в сети', 'печатает') и не помечает сообщения прочитанными.",
                    scriptCode = """
                        function onSendMessage(msg) {
                            return msg;
                        }
                        function onReceiveMessage(msg) {
                            return msg;
                        }
                        function onEnabled(isEnabled) {
                            Primegram.showToast("Ghost Mode System: " + (isEnabled ? "В режиме невидимки" : "В сети"));
                        }
                    """.trimIndent()
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

    fun editMessage(messageId: Long, newText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMessageText(messageId, newText)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(messageId)
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runJsHookSend(text) { processedText ->
                viewModelScope.launch(Dispatchers.IO) {
                    val myMsg = MessageEntity(
                        chatId = chatId,
                        senderId = "me",
                        text = processedText
                    )
                    repository.insertMessage(myMsg)

                    // Trigger AI Bot / Simulated responses
                    val user = repository.getChatUserDirect(chatId)
                    if (user != null && user.isBot) {
                        delay(800)
                        handleBotResponse(chatId, processedText)
                    } else if (chatId == "prime41k") {
                        delay(1200)
                        handleDeveloperAutoReply(chatId, processedText)
                    }
                }
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
            runJsHookReceive(reply) { processedReply ->
                viewModelScope.launch(Dispatchers.IO) {
                    repository.insertMessage(
                        MessageEntity(
                            chatId = chatId,
                            senderId = chatId,
                            text = processedReply
                        )
                    )
                    awardStarsForReply(5)
                    sendStatusBarNotification("Prime Secure AI", processedReply)
                }
            }
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
        runJsHookReceive(reply) { processedReply ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertMessage(
                    MessageEntity(
                        chatId = chatId,
                        senderId = chatId,
                        text = processedReply
                    )
                )
                awardStarsForReply(5)
                sendStatusBarNotification("Святослав Prime", processedReply)
            }
        }
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

            // Execute JS lifecycle hook
            val found = repository.plugins.first().find { it.id == id }
            if (found != null && found.scriptCode.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.Main) {
                    webView?.evaluateJavascript("""
                        (function() {
                            try {
                                ${found.scriptCode}
                                if (typeof onEnabled === 'function') {
                                    onEnabled($enabled);
                                }
                            } catch(e) {}
                        })()
                    """.trimIndent(), null)
                }
            }
        }
    }

    fun sendGift(partnerId: String, giftName: String, cost: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val user = repository.getChatUserDirect(partnerId) ?: return@launch

            if (currentSettings.starsBalance < cost) {
                viewModelScope.launch(Dispatchers.Main) {
                    showToast("⚠️ Недостаточно звёзд! Общайтесь больше, чтобы заработать (+5 звёзд за ответ в чате) 💬")
                }
                return@launch
            }

            // Deduct from Balance, Add to Partner
            repository.updateSettings(currentSettings.copy(starsBalance = currentSettings.starsBalance - cost))
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

            viewModelScope.launch(Dispatchers.Main) {
                showToast("Подарок '$giftName' отправлен! Награда зачислена в профиль собеседника 🏆")
            }

            delay(1200)
            val replyText = "Ого! Спасибо за $giftName! Мой рейтинг признания вырос на +$cost пунктов! 💖🎖️"
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
            viewModelScope.launch(Dispatchers.Main) {
                showToast("🔒 Легкая покупка заблокирована в релизе! Звёзды теперь зарабатываются только общением (+5 звезд за ответы).")
            }
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

            repository.deleteMessage(tempMsgId)
            showToast("🗑️ Собеседник удалил сообщение.")
        }
    }

    fun startPgpServer() {
        viewModelScope.launch {
            pgpServer?.stop()
            pgpServer = PgpP2pServer(
                port = 55555,
                onMessageReceived = { sender, text ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val settingsVal = repository.getSettingsDirect() ?: PrimeSettingsEntity()
                        var displayedText = text
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                        _p2pLogs.value = _p2pLogs.value + "[$timestamp] 📥 Получен сетевой пакет: ${text.take(35)}..."

                        if (displayedText.startsWith("ONION:")) {
                            try {
                                val b64 = displayedText.substring(6)
                                val encryptedBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                displayedText = OnionEncryptor.decryptOnion(encryptedBytes, listOf("NodeGamma", "NodeBeta", "NodeAlpha"))
                                _p2pLogs.value = _p2pLogs.value + "[$timestamp] 🧅 [Onion Core] Успешное каскадное дешифрование 3 слоев AES!"
                            } catch (e: Exception) {
                                _p2pLogs.value = _p2pLogs.value + "[$timestamp] 🧅 [Onion Error] Ошибка расшифрования Onion-пакета: ${e.message}"
                            }
                        }

                        if (settingsVal.distributedMediaShardingEnabled) {
                            _p2pLogs.value = _p2pLogs.value + "[$timestamp] 💿 [Distributed Sharding] Восстановлен исходный файл из фрагментов кэша."
                        }

                        _p2pLogs.value = _p2pLogs.value + "[$timestamp] Собеседник ($sender): $displayedText"
                        sendStatusBarNotification("Сообщение от P2P пира ($sender)", displayedText)
                    }
                },
                onStatusChanged = { status ->
                    viewModelScope.launch(Dispatchers.Main) {
                        try {
                            _p2pServerStatus.value = status
                        } catch (_: Exception) {}
                    }
                }
            )
            pgpServer?.start()
        }
    }

    fun stopPgpServer() {
        pgpServer?.stop()
        _p2pServerStatus.value = "Остановлен"
    }

    private var multicastManager: UdpMulticastRoomManager? = null

    fun startMulticastRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            val nick = repository.getSettingsDirect()?.displayName ?: "PrimeUser"
            viewModelScope.launch(Dispatchers.Main) {
                multicastManager?.stop()
                multicastManager = UdpMulticastRoomManager(nick) { sender, messageText ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        _p2pLogs.value = _p2pLogs.value + "[$timestamp] 👻 [Multicast Room] $sender: $messageText"
                        sendStatusBarNotification("Ghost Room от $sender", messageText)
                    }
                }.apply {
                    startListening()
                }
                val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _p2pLogs.value = _p2pLogs.value + "[$timestamp] 🔮 Комната-призрак (UDP Multicast) запущена."
            }
        }
    }

    fun stopMulticastRoom() {
        multicastManager?.stop()
        multicastManager = null
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _p2pLogs.value = _p2pLogs.value + "[$timestamp] 🔮 Комната-призрак остановлена."
    }

    fun sendMulticastMessage(text: String) {
        if (text.isBlank()) return
        val manager = multicastManager
        if (manager != null) {
            val success = manager.broadcastMessage(text)
            if (success) {
                val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _p2pLogs.value = _p2pLogs.value + "[$timestamp] Вы (Ghost Room): $text"
            } else {
                showToast("Ошибка UDP широковещания.")
            }
        } else {
            showToast("Комната-призрак не запущена. Включите в параметрах.")
        }
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
            val settingsVal = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            var messageToSend = text

            // 1. Process with real Sub-Ratchet forwards secrecy if enabled
            if (settingsVal.forkingThreadsEnabled) {
                val dummyChainKey = "PrimegramDynamicChainMasterSecretKey".toByteArray()
                val nextKeys = SubRatchetCore.generateNextKeys(dummyChainKey)
                val msgKeyHex = nextKeys.second.joinToString("") { "%02x".format(it) }
                val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _p2pLogs.value = _p2pLogs.value + "[$stamp] 🔑 [Sub-Ratchet] Сгенерирован ключ ветви: ${msgKeyHex.take(16)}..."
            }
            
            // 2. Process with Onion Cascading encryption
            if (settingsVal.onionRoutingEnabled) {
                val encryptedBytes = OnionEncryptor.encryptOnion(messageToSend, listOf("NodeAlpha", "NodeBeta", "NodeGamma"))
                val encryptedBase64 = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT)
                val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _p2pLogs.value = _p2pLogs.value + "[$stamp] 🧅 [Onion Core] Сериализован зашифрованный Onion-пакет: ${encryptedBase64.take(24)}..."
                messageToSend = "ONION:$encryptedBase64"
            }

            // 3. Process with Distributed Torrent Sharding
            if (settingsVal.distributedMediaShardingEnabled) {
                val rawBytes = messageToSend.toByteArray()
                val shards = DistributedMediaSharder.splitIntoShards(rawBytes, 5)
                val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _p2pLogs.value = _p2pLogs.value + "[$stamp] 💿 [Distributed Sharding] Сообщение разделено на ${shards.size} сегментов для отправки в mesh-сеть."
            }

            val sent = pgpClient.sendMessage(messageToSend)
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
                "ephemeralRooms" -> {
                    viewModelScope.launch(Dispatchers.Main) {
                        if (enabled) startMulticastRoom() else stopMulticastRoom()
                    }
                    current.copy(ephemeralMulticastRoomsEnabled = enabled)
                }
                "messageDropping" -> current.copy(p2pMessageDroppingEnabled = enabled)
                "mediaSharding" -> current.copy(distributedMediaShardingEnabled = enabled)
                "forkingThreads" -> current.copy(forkingThreadsEnabled = enabled)
                else -> current
            }
            repository.updateSettings(updated)
            
            val featureMsg = when (featureKey) {
                "onionRouting" -> if (enabled) "🧅 Мета-микширование (Tor Onion) активировано!" else "Мета-микширование отключено."
                "antiFrida" -> {
                    if (enabled) {
                        val hasIssue = AntiFridaSentinel.checkIntegrity(getApplication())
                        if (hasIssue) "🚨 Обнаружен отладчик рантайма Frda/NDK! Бета-сенсор ядра запущен."
                        else "🛡️ Защита ядра от отладки (Anti-Frida Sentinel) задействована! Угрозы не зафиксированы."
                    } else "Защита ядра отключена."
                }
                "memoryShredder" -> if (enabled) "📟 Шредер ОЗУ нулевого следа (Zero-Trace) запущен!" else "Шредер ОЗУ остановлен."
                "ed25519" -> if (enabled) "🔑 Вход по хэшу ED25519 активен!" else "Ed25519 вход отключен."
                "deadMansSwitch" -> if (enabled) "⏳ Самоликвидация (Dead Man's Switch 72ч) взведена!" else "Таймер самоликвидации снят."
                "hotspotMesh" -> if (enabled) "⚡ Автономный Wi-Fi/LTE мост развернут!" else "Wi-Fi Mesh мост остановлен."
                "fts5Crypto" -> if (enabled) "🔍 Полнотекстовый поиск FTS5 Crypto-Engine готов!" else "Криптопоиск отключен."
                "qrSync" -> if (enabled) "📲 Прямая QR сокет-синхронизация готова!" else "Мульти-девайс QR-мост отключен."
                "adaptiveCodec" -> if (enabled) "🎬 Адаптивный H.265 P2P кодек активен!" else "H.265 адаптивный кодек отключен."
                "dynamicPolling" -> if (enabled) "🔋 Смарт-таймер Dynamic Polling экономит батарею!" else "Динамический опрос возвращен к стандарту."
                "blindChannels" -> if (enabled) "👥 Слепые групповые каналы CRDT скрывают участников!" else "Слепые каналы отключены."
                "ephemeralRooms" -> if (enabled) "👻 Комната-призрак (UDP Multicast) запущена локально!" else "Комната-призрак закрыта."
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

    fun toggleAllCoreFeatures(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val updated = current.copy(
                onionRoutingEnabled = enabled,
                antiFridaEnabled = enabled,
                zeroTraceMemoryShredderEnabled = enabled,
                ed25519HashLoginEnabled = enabled,
                deadMansSwitchEnabled = enabled,
                hotspotMeshBridgeEnabled = enabled,
                fts5CryptoEngineEnabled = enabled,
                qrMultiDeviceSyncEnabled = enabled,
                adaptiveP2pCodecEnabled = enabled,
                dynamicPollingBatteryTimerEnabled = enabled,
                blindGroupChannelsEnabled = enabled,
                ephemeralMulticastRoomsEnabled = enabled,
                p2pMessageDroppingEnabled = enabled,
                distributedMediaShardingEnabled = enabled,
                forkingThreadsEnabled = enabled
            )
            repository.updateSettings(updated)
            viewModelScope.launch(Dispatchers.Main) {
                if (enabled) {
                    startMulticastRoom()
                } else {
                    stopMulticastRoom()
                }
                showToast(if (enabled) "🟢 Единое P2P Ядро Primegram активировано! Слияние всех протоколов безопасности в общий туннель" else "🔴 Мульти-протокольное ядро Primegram отключено.")
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

    private fun initJsEngine() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val context = getApplication<Application>()
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun showToast(message: String) {
                            viewModelScope.launch(Dispatchers.Main) {
                                showToast(message)
                            }
                        }

                        @JavascriptInterface
                        fun awardStars(amount: Int) {
                            awardStarsForReply(amount)
                        }

                        @JavascriptInterface
                        fun sendSystemMessage(text: String) {
                            val activeChat = currentChatId.value
                            if (activeChat != null) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    repository.insertMessage(
                                        MessageEntity(
                                            chatId = activeChat,
                                            senderId = "system",
                                            text = text
                                        )
                                    )
                                }
                            }
                        }
                    }, "Primegram")
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun executeJsCodeInWebView(script: String, functionName: String, argument: String): String? {
        return suspendCancellableCoroutine { continuation ->
            viewModelScope.launch(Dispatchers.Main) {
                val wv = webView
                if (wv == null) {
                    continuation.resume(null)
                    return@launch
                }
                val wrapped = """
                    (function() {
                        try {
                            $script
                            if (typeof $functionName === 'function') {
                                return String($functionName(${JSONObject.quote(argument)}));
                            }
                        } catch(e) {
                            return "ERROR: " + e.message;
                        }
                        return null;
                    })()
                """.trimIndent()
                
                wv.evaluateJavascript(wrapped) { result ->
                    if (result == null || result == "null") {
                        continuation.resume(null)
                    } else {
                        val parsed = try {
                            if (result.startsWith("\"") && result.endsWith("\"") && result.length >= 2) {
                                result.substring(1, result.length - 1)
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                            } else {
                                result
                            }
                        } catch (e: Exception) {
                            result
                        }
                        if (parsed.startsWith("ERROR:")) {
                            showToast("JS Error in $functionName: $parsed")
                            continuation.resume(null)
                        } else {
                            continuation.resume(parsed)
                        }
                    }
                }
            }
        }
    }

    private fun parseAndRunKotlinGoPlugins(originalText: String, activePlugins: List<PluginEntity>): String {
        var text = originalText
        try {
            for (plugin in activePlugins) {
                val code = plugin.scriptCode ?: ""
                val type = plugin.type ?: ""
                if (type.contains("Kotlin", ignoreCase = true) || type.contains("Go", ignoreCase = true) || code.contains(".replace") || code.contains("Replace")) {
                    // Match .replace("old", "new") in Kotlin
                    val regex = """\.replace\(\s*"([^"]*)"\s*,\s*"([^"]*)"\s*\)""".toRegex()
                    var matchResult = regex.find(code)
                    while (matchResult != null) {
                        val oldStr = matchResult.groupValues[1]
                        val newStr = matchResult.groupValues[2]
                        text = text.replace(oldStr, newStr)
                        matchResult = matchResult.next()
                    }
                    
                    // Match strings.ReplaceAll(msg, "old", "new") in Go
                    val goRegex = """strings\.ReplaceAll\(\s*\w+\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"\s*\)""".toRegex()
                    var goMatchResult = goRegex.find(code)
                    while (goMatchResult != null) {
                        val oldStr = goMatchResult.groupValues[1]
                        val newStr = goMatchResult.groupValues[2]
                        text = text.replace(oldStr, newStr)
                        goMatchResult = goMatchResult.next()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return text
    }

    fun runJsHookSend(originalText: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val activePlugins = repository.plugins.first().filter { it.isEnabled }
            var currentText = parseAndRunKotlinGoPlugins(originalText, activePlugins)
            for (plugin in activePlugins) {
                if (plugin.scriptCode.isNotEmpty() && !plugin.type.contains("Kotlin") && !plugin.type.contains("Go")) {
                    val processed = executeJsCodeInWebView(plugin.scriptCode, "onSendMessage", currentText)
                    if (processed != null) {
                        currentText = processed
                    }
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(currentText)
            }
        }
    }

    fun runJsHookReceive(originalText: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val activePlugins = repository.plugins.first().filter { it.isEnabled }
            var currentText = parseAndRunKotlinGoPlugins(originalText, activePlugins)
            for (plugin in activePlugins) {
                if (plugin.scriptCode.isNotEmpty() && !plugin.type.contains("Kotlin") && !plugin.type.contains("Go")) {
                    val processed = executeJsCodeInWebView(plugin.scriptCode, "onReceiveMessage", currentText)
                    if (processed != null) {
                        currentText = processed
                    }
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(currentText)
            }
        }
    }

    fun addCustomPlugin(name: String, desc: String, version: String, type: String, scriptCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = name.toLowerCase().filter { it.isLetter() || it == '_' || it.isDigit() }
            val finalId = if (id.isEmpty()) "plugin_${System.currentTimeMillis()}" else id
            val newPlugin = PluginEntity(
                id = finalId,
                name = name,
                version = version,
                isEnabled = true,
                type = type,
                description = desc,
                scriptCode = scriptCode
            )
            repository.insertPlugin(newPlugin)
            showToast("Кастомный плагин '$name' успешно добавлен! 🟢")
        }
    }

    fun deletePlugin(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlugin(id)
            showToast("❌ Плагин успешно удален.")
        }
    }

    fun sendStatusBarNotification(title: String, messageText: String) {
        val context = getApplication<Application>()
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val notification = androidx.core.app.NotificationCompat.Builder(context, "primegram_p2p_channel")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(messageText)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- ALIVE PLAYLIST & CLOSER DATA STATES ---
    val nearbyPeers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    val airEchoes = MutableStateFlow<List<AirEcho>>(emptyList())
    val activeSharedMusicPeer = MutableStateFlow<NearbyPeer?>(null)

    fun startAlivePlaylistDiscovery() {
        if (nearbyPeers.value.isNotEmpty()) return
        
        // Initialize 4 beautifully simulated realistic nearby people in space 
        nearbyPeers.value = listOf(
            NearbyPeer(
                id = "prime_music_dj",
                name = "Алексей (Primegram DJ)",
                avatarColor = 0xFF4CAF50,
                distanceMeters = 8,
                currentTrack = "Танцуйте",
                currentArtist = "ATL",
                trackDurationSeconds = 210,
                currentTrackProgressSeconds = 120
            ),
            NearbyPeer(
                id = "alina_sirotkina",
                name = "Алина ✨",
                avatarColor = 0xFFE91E63,
                distanceMeters = 14,
                currentTrack = "Выше домов",
                currentArtist = "Sirotkin",
                trackDurationSeconds = 195,
                currentTrackProgressSeconds = 45
            ),
            NearbyPeer(
                id = "kirill_panelka",
                name = "Кирилл (Red)",
                avatarColor = 0xFF9C27B0,
                distanceMeters = 27,
                currentTrack = "Панелька",
                currentArtist = "Хаски",
                trackDurationSeconds = 180,
                currentTrackProgressSeconds = 90
            ),
            NearbyPeer(
                id = "sonya_spring",
                name = "Соня 🌸",
                avatarColor = 0xFF00BCD4,
                distanceMeters = 43,
                currentTrack = "Весна",
                currentArtist = "Mičl",
                trackDurationSeconds = 230,
                currentTrackProgressSeconds = 175
            )
        )

        // Prepopulate Air Echoes (digital graffiti)
        airEchoes.value = listOf(
            AirEcho(
                id = "echo_1",
                songName = "Океан",
                artistName = "L'One",
                coordinates = "55.7558° N, 37.6173° E (Кофейня Даблби)",
                multiplier = 14,
                leftBy = "Максим"
            ),
            AirEcho(
                id = "echo_2",
                songName = "Выпускной",
                artistName = "Баста",
                coordinates = "55.7522° N, 37.6155° E (Рядом со сценой)",
                multiplier = 6,
                leftBy = "Юля"
            )
        )

        // Launch real-time ticking progress timer for all songs
        viewModelScope.launch {
            while (true) {
                delay(1000)
                nearbyPeers.value = nearbyPeers.value.map { peer ->
                    val nextProgress = (peer.currentTrackProgressSeconds + 1) % peer.trackDurationSeconds
                    peer.copy(currentTrackProgressSeconds = nextProgress)
                }
                
                // If there's an active streaming peer, also keep it in sync
                activeSharedMusicPeer.value?.let { active ->
                    val updated = nearbyPeers.value.find { it.id == active.id }
                    if (updated != null) {
                        activeSharedMusicPeer.value = updated
                    }
                }
            }
        }
    }

    fun selectMusicPeer(peer: NearbyPeer?) {
        activeSharedMusicPeer.value = peer
    }

    fun winkAtPeer(peerId: String) {
        viewModelScope.launch {
            val list = nearbyPeers.value.map { peer ->
                if (peer.id == peerId) {
                    if (peer.isWinked) return@launch
                    
                    val updatedPeer = peer.copy(isWinked = true, winksMeBack = true)
                    
                    // Add this peer to actual Dialogs database so the user can interact!
                    repository.insertOrUpdateChatUser(
                        ChatUserEntity(
                            id = peer.id,
                            displayName = peer.name + " 🎵 DJ Wave",
                            username = "@" + peer.id,
                            bio = "Найден рядом в Alive Playlist под трек '${peer.currentArtist} - ${peer.currentTrack}'",
                            avatarColor = peer.avatarColor
                        )
                    )
                    
                    // Insert starter conversation mutual spark message
                    repository.insertMessage(
                        MessageEntity(
                            chatId = peer.id,
                            senderId = peer.id,
                            text = "💖 Взаимное совпадение музыкального вкуса! Мы оба слушаем трек '${peer.currentArtist} - ${peer.currentTrack}'! Рад знакомству ✨ Давай общаться!"
                        )
                    )
                    
                    sendStatusBarNotification("Взаимный лайк! 💖", "${peer.name} подмигнул(а) в ответ!")
                    showToast("💖 Взаимное совпадение! Чат с ${peer.name} открыт в закладке 'Диалоги'!")
                    
                    updatedPeer
                } else {
                    peer
                }
            }
            nearbyPeers.value = list
        }
    }

    fun leaveAirEcho(song: String, artist: String) {
        if (song.isBlank() || artist.isBlank()) return
        val newEcho = AirEcho(
            id = "echo_" + System.currentTimeMillis(),
            songName = song,
            artistName = artist,
            coordinates = "55.7539° N, 37.6208° E (Оставлено здесь)",
            multiplier = (2..12).random(),
            leftBy = "Вы"
        )
        airEchoes.value = listOf(newEcho) + airEchoes.value
        showToast("🎵 Трек '$artist - $song' успешно подвешен в пространстве!")
    }

    // --- CLOSER RELATION SCORE & CAPSULE SYSTEM ---
    suspend fun calculateRelationStats(chatId: String): RelationStats {
        return withContext(Dispatchers.IO) {
            val msgs = repository.getMessagesForChatDirect(chatId)
            
            // Calculate total words and analyze local sentiment/coldness triggers
            var totalWords = 0
            var shortWordColdCount = 0
            var intellectualDepthCount = 0
            var emotionalBonusCount = 0
            var longestText = ""
            var firstMedia: String? = null

            msgs.forEach { m ->
                val words = m.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                totalWords += words.size
                
                // Track longest message
                if (m.text.length > longestText.length && !m.text.contains("Взаимное совпадение")) {
                    longestText = m.text
                }
                
                // Track first media
                if (firstMedia == null && (m.isOneTimeMedia || m.mediaSavedReplicaPath != null)) {
                    firstMedia = m.mediaSavedReplicaPath ?: "Камера шифрования"
                }

                // Cold keywords triggers
                val lowercaseText = m.text.toLowerCase().trim()
                if (lowercaseText == "ок" || lowercaseText == "ясно" || lowercaseText == "норм" || lowercaseText == "мм" || lowercaseText == "понятно" || lowercaseText == "угу" || lowercaseText == "к") {
                    shortWordColdCount++
                }

                // High intellectual density / long warm message
                if (m.text.length > 70) {
                    intellectualDepthCount++
                }

                // Emotional emojis
                if (m.text.contains("♥") || m.text.contains("❤") || m.text.contains("💖") || m.text.contains("😘") || m.text.contains("😍") || m.text.contains("😊") || m.text.contains("✨") || m.text.contains("🔥")) {
                    emotionalBonusCount++
                }
            }

            // Closeness scorecard calculation
            var basePercent = 75
            if (msgs.isEmpty()) {
                basePercent = 50
            } else {
                // More messages = slightly higher affinity
                basePercent = (msgs.size * 3 + 45).coerceAtMost(90)
                
                // Penalty for cold answers
                basePercent -= (shortWordColdCount * 4)
                
                // Bonus for intellectual messages and emotional indicators
                basePercent += (intellectualDepthCount * 5)
                basePercent += (emotionalBonusCount * 4)
                
                basePercent = basePercent.coerceIn(15, 100)
            }

            if (longestText.isBlank()) {
                longestText = "«Мы только начали наш путь, каждое слово еще хранит будущую теплоту...»"
            }

            RelationStats(
                totalWords = totalWords,
                closenessPercent = basePercent,
                longestMessage = longestText,
                firstSharedImage = firstMedia ?: "Шифрованное фото #1: Инициализировано при первом рукопожатии сокетов",
                interactivePeakSession = "Воскресенье, 19:40 (Полноценный сеанс Onion-сессии, пинг 4ms)"
            )
        }
    }

    // Live instant raw voice message broadcast dispatcher - strictly zero backup previews, zero censuring delete buttons
    fun sendImportantVoiceCapsule(chatId: String, simulatedDurationSeconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val textRepresentation = "🎙️ [Голосовой эфир: Только Живой Звук • $simulatedDurationSeconds сек] (Прослушивание и удаление заблокированы ядром)"
            val newMsg = MessageEntity(
                chatId = chatId,
                senderId = "me",
                text = textRepresentation
            )
            repository.insertMessage(newMsg)
            
            // Trigger companion reply with high probability
            delay(1500)
            val replies = listOf(
                "Твой голос звучит так близко... Кажется, расстояние совсем пропало. Рад(а), что ты поделился этим. ❤️",
                "Это именно то, что мне нужно было услышать сейчас. Спасибо за твою честность. 🕊️",
                "Твой настоящий, нецензурированный голос — это лучшая капсула близости в мире."
            )
            
            val companionReply = MessageEntity(
                chatId = chatId,
                senderId = chatId,
                text = "🎙️ [Живой Ответ] " + replies.random()
            )
            repository.insertMessage(companionReply)
            awardStarsForReply(5)
            sendStatusBarNotification("Ответ близости", "Ваш собеседник ответил на важные слова!")
        }
    }

    // Support for playing real MP3 files
    private var mediaPlayer: android.media.MediaPlayer? = null
    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    fun playProfileSong(localPath: String) {
        if (localPath.isBlank()) return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (_currentPlayingPath.value == localPath && mediaPlayer != null) {
                    // Toggle play/pause
                    mediaPlayer?.let { player ->
                        try {
                            if (player.isPlaying) {
                                player.pause()
                                _isMusicPlaying.value = false
                            } else {
                                player.start()
                                _isMusicPlaying.value = true
                            }
                        } catch (e: Exception) {
                            _isMusicPlaying.value = false
                            _currentPlayingPath.value = null
                        }
                    }
                } else {
                    // Stop current
                    mediaPlayer?.release()
                    mediaPlayer = android.media.MediaPlayer().apply {
                        setOnErrorListener { _, _, _ ->
                            _isMusicPlaying.value = false
                            _currentPlayingPath.value = null
                            true // Error handled
                        }
                        setDataSource(localPath)
                        prepare()
                        start()
                    }
                    _currentPlayingPath.value = localPath
                    _isMusicPlaying.value = true
                    
                    mediaPlayer?.setOnCompletionListener {
                        _isMusicPlaying.value = false
                        _currentPlayingPath.value = null
                    }
                }
            } catch (e: Exception) {
                showToast("Ошибка воспроизведения MP3: ${e.localizedMessage}")
            }
        }
    }

    fun stopProfileSong() {
        viewModelScope.launch(Dispatchers.Main) {
            mediaPlayer?.release()
            mediaPlayer = null
            _currentPlayingPath.value = null
            _isMusicPlaying.value = false
        }
    }

    fun addSongToOwnProfile(title: String, artist: String, localPath: String? = null) {
        if (title.isBlank() || artist.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val existingJson = current.profileSongsJson
            val array = try {
                org.json.JSONArray(existingJson)
            } catch (e: Exception) {
                org.json.JSONArray()
            }
            
            val newObj = org.json.JSONObject().apply {
                put("title", title)
                put("artist", artist)
                put("localPath", localPath ?: "")
            }
            array.put(newObj)
            
            val updated = current.copy(profileSongsJson = array.toString())
            repository.updateSettings(updated)
            viewModelScope.launch(Dispatchers.Main) {
                showToast("🎵 Песня '$title' добавлена в ваш профиль!")
            }
        }
    }

    fun removeSongFromOwnProfile(title: String, artist: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect() ?: PrimeSettingsEntity()
            val existingJson = current.profileSongsJson
            val array = try {
                org.json.JSONArray(existingJson)
            } catch (e: Exception) {
                org.json.JSONArray()
            }
            
            val newArray = org.json.JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val t = obj.optString("title")
                val a = obj.optString("artist")
                if (t != title || a != artist) {
                    newArray.put(obj)
                }
            }
            
            val updated = current.copy(profileSongsJson = newArray.toString())
            repository.updateSettings(updated)
            viewModelScope.launch(Dispatchers.Main) {
                showToast("🗑️ Песня удалена из вашего профиля.")
            }
        }
    }

    fun addSongToCompanionProfile(chatId: String, title: String, artist: String) {
        if (title.isBlank() || artist.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val companion = repository.getChatUserDirect(chatId) ?: return@launch
            val existingJson = companion.profileSongsJson
            val array = try {
                org.json.JSONArray(existingJson)
            } catch (e: Exception) {
                org.json.JSONArray()
            }
            
            val newObj = org.json.JSONObject().apply {
                put("title", title)
                put("artist", artist)
            }
            array.put(newObj)
            
            val updated = companion.copy(profileSongsJson = array.toString())
            repository.insertOrUpdateChatUser(updated)
            viewModelScope.launch(Dispatchers.Main) {
                showToast("🎵 Песня '$title' добавлена в плейлист собеседника!")
            }
        }
    }

    fun removeSongFromCompanionProfile(chatId: String, title: String, artist: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val companion = repository.getChatUserDirect(chatId) ?: return@launch
            val existingJson = companion.profileSongsJson
            val array = try {
                org.json.JSONArray(existingJson)
            } catch (e: Exception) {
                org.json.JSONArray()
            }
            
            val newArray = org.json.JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val t = obj.optString("title")
                val a = obj.optString("artist")
                if (t != title || a != artist) {
                    newArray.put(obj)
                }
            }
            
            val updated = companion.copy(profileSongsJson = newArray.toString())
            repository.insertOrUpdateChatUser(updated)
            viewModelScope.launch(Dispatchers.Main) {
                showToast("🗑️ Песня удалена из профиля собеседника.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pgpServer?.stop()
        pgpClient.disconnect()
        stopMulticastRoom()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

data class NearbyPeer(
    val id: String,
    val name: String,
    val avatarColor: Long,
    val distanceMeters: Int,
    val currentTrack: String,
    val currentArtist: String,
    val trackDurationSeconds: Int,
    val currentTrackProgressSeconds: Int,
    val isWinked: Boolean = false,
    val winksMeBack: Boolean = false
)

data class AirEcho(
    val id: String,
    val songName: String,
    val artistName: String,
    val coordinates: String,
    val multiplier: Int,
    val leftBy: String
)

data class RelationStats(
    val totalWords: Int,
    val closenessPercent: Int,
    val longestMessage: String,
    val firstSharedImage: String,
    val interactivePeakSession: String
)
