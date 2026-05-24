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
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isCleaningCache = MutableStateFlow(false)
    val isCleaningCache: StateFlow<Boolean> = _isCleaningCache.asStateFlow()

    private val _pluginDownloadStatus = MutableStateFlow<Map<String, Float>>(emptyMap()) // pluginId to progress (0..1)
    val pluginDownloadStatus: StateFlow<Map<String, Float>> = _pluginDownloadStatus.asStateFlow()

    private val _activeChatId = MutableStateFlow("1") // "1": Arslan, "2": Dev, "3": Mom, "assistant_bot": Stealth Bot
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
            
            // Check if selected contact is local BOT, or simulation
            val currentUsers = chatUsers.value
            val targetUser = currentUsers.find { it.id == currentChatId }

            if (targetUser != null && targetUser.isBot) {
                runLocalBotScript(targetUser, draft)
            } else {
                val currentSettings = settings.value ?: return@launch
                if (currentSettings.translatorEnabled) {
                    delay(1200)
                    simulateLocalTranslatedResponse(currentChatId, draft)
                }
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
    fun addChatUser(id: String, displayName: String, username: String, isBot: Boolean = false, botToken: String? = null, botScript: String? = null) {
        viewModelScope.launch {
            val user = ChatUser(
                id = id,
                displayName = displayName,
                username = username,
                avatarColor = listOf(0xFFEF5350.toInt(), 0xFF26A69A.toInt(), 0xFFFFCA28.toInt(), 0xFF5C6BC0.toInt(), 0xFFAB47BC.toInt()).random(),
                isBot = isBot,
                botToken = botToken,
                botScript = botScript
            )
            repository.insertChatUser(user)
            // Seed a welcome message
            val welcomeText = if (isBot) "Привет! Я бот $displayName. Спасибо за интеграцию!" else "Привет! Рад общению в защищенном Cherrygram."
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
    fun simulateDeletedMessageTrigger() {
        viewModelScope.launch {
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
            val deletedModel = incomingMsg.copy(isDeleted = true)
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
            
            showToast("⚠️ Anti-Recall: $partner удалил сообщение! Перехвачено и сохранено.")
        }
    }

    fun simulateOneTimeMediaTrigger() {
        viewModelScope.launch {
            val currentChatId = _activeChatId.value
            val partner = getChatPartnerName(currentChatId)
            
            val isVideo = listOf(true, false).random()
            val fileTypeName = if (isVideo) "одноразовое видео" else "одноразовое фото"
            val fileExtension = if (isVideo) "mp4" else "jpg"
            val mockTitle = if (isVideo) "Секретное видео_${System.currentTimeMillis() % 1000}.$fileExtension" else "Фото-призрак_${System.currentTimeMillis() % 1000}.$fileExtension"
            
            val mediaMsg = LocalMessage(
                chatUserId = currentChatId,
                senderName = partner,
                text = "🖼 [$fileTypeName, нажмите для просмотра]",
                isMe = false,
                isOneTimeMedia = true,
                mediaPlaceholder = mockTitle,
                isVideoType = isVideo
            )
            repository.insertLocalMessage(mediaMsg)

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

            showToast("📥 MediaSaver: $partner прислал одноразовое медиа. Копия сохранена в Секретный Сейф!")
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
