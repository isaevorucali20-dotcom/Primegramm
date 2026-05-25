package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DeletedMessage
import com.example.data.PluginEntity
import com.example.data.PrimeRepository
import com.example.data.PrimeSettings
import com.example.data.ProxyServer
import com.example.data.SelfDestructMedia
import com.example.data.AnalyticsLog
import com.example.data.ChatUser
import com.example.data.LocalMessage
import com.example.data.MiniAppEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PrimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PrimeRepository
    
    val settings: StateFlow<PrimeSettings?>
    val deletedMessages: StateFlow<List<DeletedMessage>>
    val selfDestructMedia: StateFlow<List<SelfDestructMedia>>
    val proxyServers: StateFlow<List<ProxyServer>>
    val plugins: StateFlow<List<PluginEntity>>
    val analyticsLogs: StateFlow<List<AnalyticsLog>>
    
    // Live database entities
    val chatUsers: StateFlow<List<ChatUser>>
    val miniApps: StateFlow<List<MiniAppEntity>>

    // UI Transient States
    private val _typingState = MutableStateFlow<Map<String, String>>(emptyMap()) // chatId to status description
    val typingState: StateFlow<Map<String, String>> = _typingState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isCleaningCache = MutableStateFlow(false)
    val isCleaningCache: StateFlow<Boolean> = _isCleaningCache.asStateFlow()

    private val _pluginDownloadStatus = MutableStateFlow<Map<String, Float>>(emptyMap()) // pluginId to progress (0..1)
    val pluginDownloadStatus: StateFlow<Map<String, Float>> = _pluginDownloadStatus.asStateFlow()

    private val _activeChatId = MutableStateFlow("prime41k") // Default active chat: prime41k dev
    val activeChatId: StateFlow<String> = _activeChatId.asStateFlow()

    val activeChatMessages: StateFlow<List<LocalMessage>>

    private val _chatDraft = MutableStateFlow("")
    val chatDraft: StateFlow<String> = _chatDraft.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PrimeRepository(database.primeDao())

        settings = repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        deletedMessages = repository.deletedMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        selfDestructMedia = repository.selfDestructMedia.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        proxyServers = repository.proxyServers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        plugins = repository.plugins.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        analyticsLogs = repository.analyticsLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        chatUsers = repository.chatUsers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        miniApps = repository.miniApps.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeChatMessages = _activeChatId
            .flatMapLatest { id -> repository.getLocalMessagesForChat(id) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed data on startup
        viewModelScope.launch {
            repository.ensureSeeded()

            // Check and set random user unique ID if not set
            try {
                val s = repository.getSettings()
                if (s.userUniqueId.isEmpty()) {
                    val randId = (100000..999999).random().toString()
                    repository.updateSettings(s.copy(userUniqueId = randId))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Start proactive background conversation loop for active groups and chats to make them feel 100% alive & realistic
            launch {
                delay(8000)
                while (true) {
                    val activeId = _activeChatId.value
                    val rand = (1..100).random()
                    if (rand < 55) {
                        // Random user deletes a message mock-event to trigger "Anti-Recall" banner dynamically in front of user
                        if (activeId == "prime41k") {
                            simulateDeletedMessageTrigger()
                        }
                    } else if (rand < 75) {
                        // Send one-time self destruct media saved log
                        if (activeId == "prime41k") {
                            simulateOneTimeMediaTrigger()
                        }
                    }
                    delay(30000) // cycle every 30 seconds
                }
            }
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            delay(3000)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun selectChat(id: String) {
        _activeChatId.value = id
        _chatDraft.value = ""
    }

    fun updateDraft(text: String) {
        _chatDraft.value = text
    }

    // Live Database Message Submission
    fun isPluginInstalled(pluginId: String): Boolean {
        return plugins.value.any { it.id == pluginId && it.isInstalled }
    }

    private suspend fun generateGeminiResponse(chatId: String, userPrompt: String): String? {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            return null // Fallback to smart local responder
        }

        return withContext(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val systemInstruction = when (chatId) {
                    "1" -> "Ты — Арслан (@arslan_ton), близкий друг пользователя и крутой разработчик защищенных модов Cherrygram/Telegram. Тебе 24 года. Ты обожаешь TON, крипту, прокси и кибербезопасность. Общаешься на русском разговорном языке с программистским сленгом. Твои ответы должны быть очень краткими (1-3 коротких предложения), живыми и реалистичными, без лишней вежливости, как реальный друг в мессенджере. Используй уместно сленг: бро, рил, софт, TON, база, тема, крипта."
                    "2" -> "Ты — Ведущий Разработчик Primegram (@prime_developer). Ты создатель ядра этого мессенджера и движка плагинов. Серьезен, умен, вежлив, пишешь лаконично и технически грамотно. Обсуждай с пользователем архитектуру его Cherrygram Stealth, его предложения по доработке плагинов (Anti-Recall, Media Saver, Auto-Responder) или Set API хуки. Отвечай кратко и емко."
                    "3" -> "Ты — любящая и теплая русская мама пользователя. Ты общаешься очень заботливо, волнуешься за него, спрашиваешь вежливые домашние вещи: покушал ли сыночек, как дела, не устал ли, не дует ли в окно. Используй ласковые слова, смайлики (❤️, 😘, 😊) и пиши просто, жизненно, как типичная мама в мессенджере."
                    "assistant_bot" -> "Ты — усовершенствованный Stealth Ассистент-Бот. Отвечаешь профессионально о криптографии, шифровании AES-256, протоколе MTProto, настройках прокси и анонимности."
                    else -> "Ты — собеседник пользователя в приватном чате Cherrygram. Общайся дружелюбно, естественно и кратко."
                }

                val currentMessagesList = activeChatMessages.value.takeLast(10)
                val contentsJson = org.json.JSONArray()

                // Insert dialog context
                currentMessagesList.forEach { msg ->
                    val role = if (msg.isMe) "user" else "model"
                    contentsJson.put(org.json.JSONObject().apply {
                        put("role", role)
                        put("parts", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("text", msg.text)
                            })
                        })
                    })
                }

                // Add current prompt if not already in list
                if (currentMessagesList.none { it.text == userPrompt }) {
                    contentsJson.put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("parts", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                }

                val requestJson = org.json.JSONObject().apply {
                    put("contents", contentsJson)
                    put("systemInstruction", org.json.JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                    put("generationConfig", org.json.JSONObject().apply {
                        put("temperature", 0.7)
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = okhttp3.RequestBody.create(mediaType, requestJson.toString())

                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val resStr = response.body?.string() ?: return@withContext null
                    val retJson = org.json.JSONObject(resStr)
                    val candidates = retJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        if (content != null) {
                            val parts = content.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                return@withContext parts.getJSONObject(0).optString("text", null)
                            }
                        }
                    }
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun sendDraftMessage() {
        val draft = _chatDraft.value
        if (draft.isBlank()) return

        val currentChatId = _activeChatId.value
        
        viewModelScope.launch {
            val userMsg = LocalMessage(
                chatUserId = currentChatId,
                senderName = "Me",
                text = draft,
                isMe = true
            )
            repository.insertLocalMessage(userMsg)
            _chatDraft.value = ""

            repository.addAnalyticsLog("app_usage", "Отправлено сообщ.", 1.0f)
            
            val currentUsers = chatUsers.value
            val targetUser = currentUsers.find { it.id == currentChatId }
            val partner = getChatPartnerName(currentChatId)

            // Auto-Responder Trigger: If auto_rely is active, the bot typing is triggered, let's start typing
            setTypingState(currentChatId, "$partner печатает...")
            delay(1200)
            clearTypingState(currentChatId)

            // Try to get response from Gemini
            val geminiResponse = generateGeminiResponse(currentChatId, draft)
            val finalReplyText: String
            val isTranslated: Boolean

            if (geminiResponse != null) {
                finalReplyText = geminiResponse.trim()
                isTranslated = isPluginInstalled("plugin_auto_translate")
            } else {
                // Highly realistic local smart response fallbacks in case of no key / offline
                isTranslated = isPluginInstalled("plugin_auto_translate")
                finalReplyText = when {
                    targetUser != null && targetUser.isBot -> {
                        // Custom system bots
                        var reply = "Бот-Ассистент: Извините, я вас не понял. Отправьте команду 'помощь' для списка функций."
                        val script = targetUser.botScript ?: ""
                        val rules = script.split(";")
                        for (rule in rules) {
                            val parts = rule.split("->")
                            if (parts.size == 2) {
                                val command = parts[0].trim().lowercase()
                                val response = parts[1].trim()
                                if (draft.trim().lowercase().contains(command)) {
                                    reply = response
                                    break
                                }
                            }
                        }
                        reply
                    }
                    currentChatId == "prime41k" -> {
                        when {
                            draft.contains("привет", ignoreCase = true) || draft.contains("ку", ignoreCase = true) || draft.contains("здравствуй", ignoreCase = true) -> 
                                "Привет! Рад тебя видеть в Primegram. Как тебе сборка? Полностью переписал ядро на Kotlin Coroutines 🚀"
                            draft.contains("дела", ignoreCase = true) || draft.contains("как ты", ignoreCase = true) -> 
                                "Отлично, оптимизировал автопереводы и систему плагинов. Сейчас тестирую Monet Dynamic рендеринг палитр."
                            draft.contains("плагин", ignoreCase = true) || draft.contains("plugin", ignoreCase = true) -> 
                                "Движок плагинов Primegram полностью открыт! Ты можешь скопировать код плагина и встроить его во вкладке плагинов."
                            draft.contains("звезд", ignoreCase = true) || draft.contains("рейтинг", ignoreCase = true) || draft.contains("подар", ignoreCase = true) -> 
                                "Да, система подарков Telegram-style начисляет звёздные рейтинги. Рядом с твоим именем сразу зажжется щит, кружок или треугольник!"
                            else -> 
                                "База! Primegram v3.0 работает полностью без лишней шелухи. Чистая приватность и скорость."
                        }
                    }
                    else -> {
                        val nameStr = partner
                        when {
                            draft.contains("привет", ignoreCase = true) || draft.contains("ку", ignoreCase = true) -> 
                                "Привет! Мой уникальный Primegram ID: $currentChatId. Как круто, что ты нашел меня по поиску!"
                            draft.contains("дела", ignoreCase = true) -> 
                                "Всё супер! Общаюсь через зашифрованные сокеты Primegram. Сквозное AES-256 работает на ура 🛡️"
                            draft.contains("подар", ignoreCase = true) || draft.contains("звезд", ignoreCase = true) -> 
                                "Ого, спасибо за интерес к подаркам! Подари мне какой-нибудь стикер или сувенир, чтобы поднять мой рейтинг звезд!"
                            else -> 
                                "Крутая тема! Кстати, ты настроил неоновое свечение в кастомизации своего профиля? Выглядит нереально футуристично ✨"
                        }
                    }
                }
            }

            // Insert response to DB
            repository.insertLocalMessage(
                LocalMessage(
                    chatUserId = currentChatId,
                    senderName = partner,
                    text = finalReplyText,
                    isMe = false,
                    isTranslated = isTranslated
                )
            )

            if (isTranslated) {
                repository.addAnalyticsLog("translation", "Выполнено автопереводов", 1.0f)
            }

            // Automatic custom responder plugin trigger! (Auto-Reply)
            if (isPluginInstalled("plugin_auto_reply")) {
                delay(1200)
                val autoResponderMsg = "🤖 [Auto-Responder Pro]: Ваше сообщение получено. Мой аккаунт находится в приватном статусе инкогнито."
                repository.insertLocalMessage(
                    LocalMessage(
                        chatUserId = currentChatId,
                        senderName = "Me",
                        text = autoResponderMsg,
                        isMe = true
                    )
                )
                repository.addAnalyticsLog("app_usage", "Сработал автоответчик", 1.0f)
            }
        }
    }

    private suspend fun runLocalBotScript(bot: ChatUser, userText: String) {
        delay(800)
        val script = bot.botScript ?: ""
        var botReply = "Извините, я вас не понял. Отправьте команду 'помощь' для списка функций."
        
        // Simple python-like logic parser
        val rules = script.split(";")
        for (rule in rules) {
            val parts = rule.split("->")
            if (parts.size == 2) {
                val command = parts[0].trim().lowercase()
                val response = parts[1].trim()
                if (userText.trim().lowercase().contains(command)) {
                    botReply = response
                    break
                }
            }
        }

        val botMsg = LocalMessage(
            chatUserId = bot.id,
            senderName = bot.displayName,
            text = botReply,
            isMe = false
        )
        repository.insertLocalMessage(botMsg)
        repository.addAnalyticsLog("app_usage", "Бот API: ответ от ${bot.username}", 1.0f)
    }

    private suspend fun simulateLocalTranslatedResponse(chatUserId: String, triggerText: String) {
        val partner = getChatPartnerName(chatUserId)
        val responseText = when {
            triggerText.contains("привет", ignoreCase = true) || triggerText.contains("hi", ignoreCase = true) -> 
                "[Встроенный переводчик с English]: Hey there! This is an auto-translated response."
            triggerText.contains("тест", ignoreCase = true) -> 
                "[Встроенный переводчик]: Translation Service is fully operational (Ping: 10ms)"
            else -> 
                "[Встроенный переводчик]: Detected text. Translating into: ${settings.value?.translationTargetLanguage ?: "Русский"}"
        }

        val autoReply = LocalMessage(
            chatUserId = chatUserId,
            senderName = partner,
            text = responseText,
            isMe = false,
            isTranslated = true
        )
        repository.insertLocalMessage(autoReply)
        repository.addAnalyticsLog("translation", "Выполнено переводов", 1.0f)
    }

    // Visual Set API Overrides Editor
    fun updateSetApiConfig(json: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            val updated = current.copy(setApiJson = json)
            repository.updateSettings(updated)
            showToast("⚙️ Set API: Конфигурация успешно применена локально!")
        }
    }

    // Dynamic Live User Profile Registration
    fun addChatUser(
        id: String, 
        displayName: String, 
        username: String, 
        isBot: Boolean = false, 
        botToken: String? = null, 
        botScript: String? = null,
        bio: String = "Пользователь Primegram ⚡",
        spentStars: Int = 0,
        neonGlowColor: String = "Off"
    ) {
        viewModelScope.launch {
            val user = ChatUser(
                id = id,
                displayName = displayName,
                username = username,
                avatarColor = listOf(0xFFEF5350.toInt(), 0xFF26A69A.toInt(), 0xFFFFCA28.toInt(), 0xFF5C6BC0.toInt(), 0xFFAB47BC.toInt()).random(),
                isBot = isBot,
                botToken = botToken,
                botScript = botScript,
                bio = bio,
                spentStars = spentStars,
                neonGlowColor = neonGlowColor
            )
            repository.insertChatUser(user)
            // Seed a welcome message
            val welcomeText = if (isBot) "Привет! Я бот $displayName. Спасибо за интеграцию!" else "Привет! Рад общению в защищенном Primegram."
            repository.insertLocalMessage(
                LocalMessage(
                    chatUserId = id,
                    senderName = displayName,
                    text = welcomeText,
                    isMe = false
                )
            )
            showToast("Собеседник $displayName добавлен по ID: $id!")
        }
    }

    fun purchaseStars(amount: Int) {
        viewModelScope.launch {
            val s = repository.getSettings()
            val updated = s.copy(
                userSpentStars = s.userSpentStars + amount,
                userRating = s.userRating + amount / 10
            )
            repository.updateSettings(updated)
            showToast("Приобретено $amount звёзд в Primegram! 🌟")
        }
    }

    fun updateUserProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarStart: Int,
        avatarEnd: Int,
        neonGlow: String,
        uniqueId: String? = null
    ) {
        viewModelScope.launch {
            val s = repository.getSettings()
            val updated = s.copy(
                userDisplayName = displayName,
                userUsername = username,
                userBio = bio,
                userAvatarGradientStart = avatarStart,
                userAvatarGradientEnd = avatarEnd,
                userNeonGlowColor = neonGlow,
                userUniqueId = uniqueId ?: s.userUniqueId
            )
            repository.updateSettings(updated)
            showToast("Профиль успешно обновлен! ✨")
        }
    }

    fun sendGift(chatUserId: String, giftName: String, starCost: Int) {
        viewModelScope.launch {
            val s = repository.getSettings()
            if (s.userSpentStars < starCost) {
                // Auto buy gaps
                val gap = starCost - s.userSpentStars
                val roundedGap = ((gap / 1000) + 1) * 1000
                val updatedSpent = s.userSpentStars + roundedGap
                repository.updateSettings(s.copy(userSpentStars = updatedSpent))
                showToast("Автоматически приобретено +$roundedGap звёзд для подарка! 🌟")
            }
            
            // Re-fetch and pay
            val freshSettings = repository.getSettings()
            val newMeSpent = freshSettings.userSpentStars + starCost
            repository.updateSettings(freshSettings.copy(userSpentStars = newMeSpent))
            
            // Add message
            repository.insertLocalMessage(
                LocalMessage(
                    chatUserId = chatUserId,
                    senderName = "Me",
                    text = "🎁 Отправил подарок: '$giftName' стоимостью $starCost звёзд! 🌟",
                    isMe = true
                )
            )
            
            val targetUser = chatUsers.value.find { it.id == chatUserId }
            if (targetUser != null) {
                val updatedTarget = targetUser.copy(spentStars = targetUser.spentStars + starCost)
                repository.insertChatUser(updatedTarget)
                
                showToast("Подарок '$giftName' успешно отправлен! 🏆")
                
                delay(1200)
                setTypingState(chatUserId, "${targetUser.displayName} отвечает...")
                delay(1000)
                clearTypingState(chatUserId)
                
                val thanksMessage = "🎁 Спасибо огромное за потрясающий подарок '$giftName'! Мой звёздный рейтинг теперь вырос! ✨"
                repository.insertLocalMessage(
                    LocalMessage(
                        chatUserId = chatUserId,
                        senderName = targetUser.displayName,
                        text = thanksMessage,
                        isMe = false
                    )
                )
            }
        }
    }

    fun removeChatUser(id: String) {
        viewModelScope.launch {
            repository.deleteChatUser(id)
            repository.clearChatHistory(id)
            showToast("Чат удален.")
            if (_activeChatId.value == id) {
                _activeChatId.value = "1"
            }
        }
    }

    // Dynamic Live Mini App Registration
    fun addMiniApp(id: String, name: String, description: String, url: String, iconName: String) {
        viewModelScope.launch {
            val app = MiniAppEntity(id, name, description, url, iconName, addedByUser = true)
            repository.insertMiniApp(app)
            showToast("Мини-апп $name успешно добавлен в панель инструментов!")
        }
    }

    fun removeMiniApp(id: String) {
        viewModelScope.launch {
            repository.deleteMiniApp(id)
            showToast("Мини-апп удален.")
        }
    }

    // SIMULATED MOD FEATURES INTERCEPTIONS
    suspend fun simulateDeletedMessageTrigger() {
        val currentChatId = _activeChatId.value
        val partner = getChatPartnerName(currentChatId)
        val indexColor = getChatPartnerColor(currentChatId)

        val phrase = listOf(
            "Слушай, а где исходники Черриграма лежали?",
            "Алекс, удали мою фотку с сервера плиз!",
            "Ой, случайно отправил тебе пароль от кошелька: secret_seed_phrase_2026",
            "Завтра в 14:00 встреча. Не говори никому, стираю сообщение."
        ).random()

        // Step 1: Insert normal message
        val msgId = (10000..99999).random()
        val incomingMsg = LocalMessage(
            id = msgId,
            chatUserId = currentChatId,
            senderName = partner,
            text = phrase,
            isMe = false
        )
        repository.insertLocalMessage(incomingMsg)

        showToast("$partner печатает...")
        delay(3500)

        // Step 2: Intercept deleted message in Room DB
        val isAntiRecallActive = isPluginInstalled("plugin_anti_recall")
        if (isAntiRecallActive) {
            val deletedModel = incomingMsg.copy(isDeleted = true, text = phrase)
            repository.insertLocalMessage(deletedModel)

            // Save to actual deleted logs
            val dbMsg = DeletedMessage(
                senderName = partner,
                senderAvatarColor = indexColor,
                messageText = phrase,
                timestamp = System.currentTimeMillis() - 3500,
                deletedTimestamp = System.currentTimeMillis(),
                originalChatId = if (currentChatId.all { it.isDigit() }) currentChatId.toIntOrNull() ?: 1 else 1
            )
            repository.insertDeletedMessage(dbMsg)
            repository.addAnalyticsLog("security", "Логи удел. сообщений", 1.0f)
            
            showToast("📥 Anti-Recall Pro: $partner удалил сообщение, но оно перехвачено и сохранено!")
        } else {
            val deletedModel = incomingMsg.copy(isDeleted = true, text = "🚫 Сообщение удалено собеседником")
            repository.insertLocalMessage(deletedModel)
            showToast("⚠️ Собеседник $partner удалил сообщение. Включите плагин 'Anti-Recall Pro' для перехвата!")
        }
    }

    suspend fun simulateOneTimeMediaTrigger() {
        val currentChatId = _activeChatId.value
        val partner = getChatPartnerName(currentChatId)
        
        val isVideo = listOf(true, false).random()
        val fileTypeName = if (isVideo) "одноразовое видео" else "одноразовое фото"
        val fileExtension = if (isVideo) "mp4" else "jpg"
        val mockTitle = if (isVideo) "Секретное видео_${System.currentTimeMillis() % 1000}.$fileExtension" else "Фото-призрак_${System.currentTimeMillis() % 1000}.$fileExtension"
        
        val isMediaSaverActive = isPluginInstalled("plugin_self_destruct_saver")
        val mediaText = if (isMediaSaverActive) "🖼 [$fileTypeName, нажмите для просмотра]" else "🖼 [Одноразовое медиа, истекает через 10 сек]"

        val msgId = (10000..99999).random()
        val mediaMsg = LocalMessage(
            id = msgId,
            chatUserId = currentChatId,
            senderName = partner,
            text = mediaText,
            isMe = false,
            isOneTimeMedia = true,
            mediaPlaceholder = mockTitle,
            isVideoType = isVideo
        )
        repository.insertLocalMessage(mediaMsg)

        if (isMediaSaverActive) {
            delay(2000)
            // Primegram intercepts and downloads it to DB!
            val mediaObj = SelfDestructMedia(
                senderName = partner,
                fileType = if (isVideo) "video" else "image",
                durationSeconds = if (isVideo) (5..20).random() else 0,
                fileSizeKb = (300..5000).random(),
                timestamp = System.currentTimeMillis(),
                visualPlaceholderRes = if (isVideo) "video_preview" else "photo_preview",
                title = mockTitle
            )
            repository.insertSelfDestructMedia(mediaObj)
            repository.addAnalyticsLog("security", "Сохранено 1-time файлов", 1.0f)

            showToast("📥 Media Saver: Копия одноразового файла от $partner перехвачена и сохранена!")
        } else {
            // Auto destruction logic right before their eyes!
            delay(10000)
            val expiredMsg = mediaMsg.copy(
                text = "🔒 Ссылка уничтожена (Включите Media Saver Block)",
                isOneTimeMedia = false,
                mediaPlaceholder = "Файл стерт безвозвратно"
            )
            repository.insertLocalMessage(expiredMsg)
            showToast("⚠️ Одноразовый файл от $partner удален сервером. Включите плагин 'Media Saver Block'!")
        }
    }

    // UPDATE AND CONTROL ACTIONS
    fun toggleGhostMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = getSettingsDirect()
            val updated = current.copy(
                ghostModeEnabled = enabled,
                hideOnlineStatusUniversal = enabled,
                hideTypingStatus = enabled,
                anonymousStoriesViewer = enabled
            )
            repository.updateSettings(updated)
            repository.addAnalyticsLog("security", "Режим призрака", if (enabled) 1.0f else 0.0f)
            showToast(if (enabled) "👻 Режим Призрака активирован! Вы полностью невидимы." else "Режим Призрака деактивирован.")
        }
    }

    fun toggleSetting(key: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = getSettingsDirect()
            val updated = when (key) {
                "translatorEnabled" -> current.copy(translatorEnabled = enabled)
                "cloudSyncEnabled" -> current.copy(cloudSyncEnabled = enabled)
                "passcodeLockEnabled" -> current.copy(passcodeLockEnabled = enabled)
                "saveDeletedMessages" -> current.copy(saveDeletedMessages = enabled)
                "saveSelfDestructingMedia" -> current.copy(saveSelfDestructingMedia = enabled)
                "proxyEnabled" -> current.copy(proxyEnabled = enabled).also {
                    showToast(if (enabled) "🔗 Прокси-сервер подключен!" else "Прокси отключен.")
                }
                "hideOnlineStatusUniversal" -> current.copy(hideOnlineStatusUniversal = enabled)
                "anonymousStoriesViewer" -> current.copy(anonymousStoriesViewer = enabled)
                "hideTypingStatus" -> current.copy(hideTypingStatus = enabled)
                "hideReadStatus" -> current.copy(hideReadStatus = enabled)
                "hardwareAccelerationEnabled" -> current.copy(hardwareAccelerationEnabled = enabled)
                else -> current
            }
            repository.updateSettings(updated)
        }
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch {
            val current = getSettingsDirect()
            val updated = current.copy(themeName = themeName)
            repository.updateSettings(updated)
            showToast("🎨 Тема изменена на: $themeName")
        }
    }

    fun updateTranslationTargetLanguage(lang: String) {
        viewModelScope.launch {
            val current = getSettingsDirect()
            val updated = current.copy(translationTargetLanguage = lang)
            repository.updateSettings(updated)
            showToast("🌐 Язык перевода: $lang")
        }
    }

    fun updateSpoofedLocation(loc: String) {
        viewModelScope.launch {
            val current = getSettingsDirect()
            val ipMap = mapOf(
                "Zurich, Switzerland" to "194.209.14.88",
                "Tokyo, Japan" to "210.140.10.35",
                "New York, USA" to "64.233.161.99",
                "Moscow, Russia" to "87.250.250.242",
                "London, UK" to "185.12.14.24",
                "Off" to "Real User IP"
            )
            val updated = current.copy(
                spoofedIpLocation = loc,
                spoofedIpAddress = ipMap[loc] ?: "Real User IP"
            )
            repository.updateSettings(updated)
            showToast(if (loc == "Off") "Подмена IP отключена" else "📍 IP изменен на: ${updated.spoofedIpAddress} ($loc)")
        }
    }

    fun updateEncryptionLevel(level: String) {
        viewModelScope.launch {
            val current = getSettingsDirect()
            val updated = current.copy(encryptionLevel = level)
            repository.updateSettings(updated)
            repository.addAnalyticsLog("security", "Ротация ключей $level", 256f)
            showToast("🔒 Алгоритм шифрования обновлен: $level. Сгенерированы новые сессионные ключи.")
        }
    }

    fun addProxyServer(title: String, host: String, port: Int, type: String, secret: String?) {
        viewModelScope.launch {
            repository.addCustomProxy(title, host, port, type, secret)
            showToast("✅ Прокси $title успешно добавлен!")
        }
    }

    fun deleteProxy(id: Int) {
        viewModelScope.launch {
            repository.deleteProxy(id)
            showToast("❌ Прокси удален.")
        }
    }

    fun togglePluginInstall(pluginId: String, currentInstalled: Boolean) {
        viewModelScope.launch {
            if (currentInstalled) {
                repository.installPlugin(pluginId, false)
                showToast("🔌 Плагин отключен")
            } else {
                _pluginDownloadStatus.value = _pluginDownloadStatus.value.toMutableMap().apply { put(pluginId, 0.05f) }
                for (progress in 1..10) {
                    delay(150)
                    _pluginDownloadStatus.value = _pluginDownloadStatus.value.toMutableMap().apply { 
                        put(pluginId, progress * 0.1f) 
                    }
                }
                delay(100)
                _pluginDownloadStatus.value = _pluginDownloadStatus.value.toMutableMap().apply { remove(pluginId) }
                repository.installPlugin(pluginId, true)
                showToast("🔌 Плагин загружен и запущен во фреймворке!")
            }
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            repository.addAnalyticsLog("security", "Облачный бэкап настроек", 1.0f)
            delay(1500)
            val current = getSettingsDirect()
            val updated = current.copy(lastCloudSyncTime = System.currentTimeMillis())
            repository.updateSettings(updated)
            _isSyncing.value = false
            showToast("☁️ Настройки синхронизированы с облаком Primegram Secure Cloud!")
        }
    }

    fun triggerCacheClean() {
        viewModelScope.launch {
            if (_isCleaningCache.value) return@launch
            _isCleaningCache.value = true
            delay(1200)
            repository.clearCache()
            _isCleaningCache.value = false
            showToast("🧹 Кэш очищен! Освобождено 345.5 MB")
        }
    }

    fun clearDeletedMessagesHistory() {
        viewModelScope.launch {
            repository.clearDeletedMessages()
            showToast("🧹 История удаленных сообщений очищена.")
        }
    }

    fun clearMediaVault() {
        viewModelScope.launch {
            repository.clearSelfDestructMedia()
            showToast("🧹 Сейф одноразовых медиа очищен.")
        }
    }

    private suspend fun getSettingsDirect(): PrimeSettings {
        return repository.getSettings()
    }

    private suspend fun simulateGroupMessage(groupId: String) {
        val randMemberAndPhrase = when (groupId) {
            "ton_hackers" -> {
                val member = listOf("TON_Miner_99", "Durov_Fans", "Hacker_TON").random()
                val phrase = listOf(
                    "Кто пробовал выкачать TON SDK во встроенный мини-апп? Ссылки работают?",
                    "Демка игры Gamee в Mini App просто пушка, без лагов и без костылей.",
                    "В новом билде Cherrygram плагин Premium Star Decorator работает отлично!",
                    "Разрабы Черриграма перевели сетевой сокет на рутины, пинг теперь 35мс.",
                    "Сейф перехвата Anti-Recall вчера спас удаленный пост админа, лол!"
                ).random()
                Pair(member, phrase)
            }
            "stealth_leaks" -> {
                val member = listOf("Admin_Stealth", "LeakBot", "BypassGroup").random()
                val phrase = listOf(
                    "📡 СКАНИРОВАНИЕ... Системы обхода ТСПУ зафиксировали ротацию портов. Подключаем резервы.",
                    "Внимание: плагин 'Anti-Recall Pro' перехватывает медиафайлы даже после клика.",
                    "Встроенный 'Silent Typing Injector' теперь маскирует статус набора текста под запись аудио.",
                    "Все настройки и бэкапы зашифрованы локально по стандарту AES-256."
                ).random()
                Pair(member, phrase)
            }
            else -> null
        }

        if (randMemberAndPhrase != null) {
            setTypingState(groupId, "${randMemberAndPhrase.first} печатает...")
            delay(2000)
            clearTypingState(groupId)

            val msg = LocalMessage(
                chatUserId = groupId,
                senderName = randMemberAndPhrase.first,
                text = randMemberAndPhrase.second,
                isMe = false
            )
            repository.insertLocalMessage(msg)
            repository.addAnalyticsLog("network", "Групповой месседж от ${randMemberAndPhrase.first}", 1.0f)
        }
    }

    fun addCustomPlugin(id: String, name: String, description: String, author: String, version: String, sizeMb: Double, scriptLang: String = "javascript", scriptCode: String = "") {
        viewModelScope.launch {
            val updatedPlugin = com.example.data.PluginEntity(
                id = id,
                name = name,
                description = description,
                author = author,
                version = version,
                isInstalled = true,
                sizeMb = sizeMb,
                scriptLanguage = scriptLang,
                scriptCode = scriptCode
            )
            repository.insertPlugin(updatedPlugin)
            repository.addAnalyticsLog("app_usage", "Добавлен плагин: $name", sizeMb.toFloat())
            showToast("🔌 Плагин '$name' успешно внедрен в ядро!")
        }
    }

    fun setTypingState(chatId: String, text: String) {
        _typingState.value = _typingState.value.toMutableMap().apply {
            put(chatId, text)
        }
    }

    fun clearTypingState(chatId: String) {
        _typingState.value = _typingState.value.toMutableMap().apply {
            remove(chatId)
        }
    }

    fun getChatPartnerName(chatId: String): String {
        val user = chatUsers.value.find { it.id == chatId }
        if (user != null) return user.displayName
        return when (chatId) {
            "1" -> "Арслан Cherrygram"
            "2" -> "Разработчик Primegram"
            "3" -> "Мама"
            else -> "Пользователь $chatId"
        }
    }

    fun getChatPartnerColor(chatId: String): Int {
        val user = chatUsers.value.find { it.id == chatId }
        if (user != null) return user.avatarColor
        return when (chatId) {
            "1" -> 0xFFEF5350.toInt()
            "2" -> 0xFF26A69A.toInt()
            "3" -> 0xFFFFCA28.toInt()
            else -> 0xFF42A5F5.toInt()
        }
    }
}
