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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PrimeRepository
    
    val settings: StateFlow<PrimeSettings?>
    val deletedMessages: StateFlow<List<DeletedMessage>>
    val selfDestructMedia: StateFlow<List<SelfDestructMedia>>
    val proxyServers: StateFlow<List<ProxyServer>>
    val plugins: StateFlow<List<PluginEntity>>
    val analyticsLogs: StateFlow<List<AnalyticsLog>>

    // UI Transient States
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isCleaningCache = MutableStateFlow(false)
    val isCleaningCache: StateFlow<Boolean> = _isCleaningCache.asStateFlow()

    private val _pluginDownloadStatus = MutableStateFlow<Map<String, Float>>(emptyMap()) // pluginId to progress (0..1)
    val pluginDownloadStatus: StateFlow<Map<String, Float>> = _pluginDownloadStatus.asStateFlow()

    private val _activeChatId = MutableStateFlow(1) // 1: Arslan, 2: Dev, 3: Mom
    val activeChatId: StateFlow<Int> = _activeChatId.asStateFlow()

    private val _chatDraft = MutableStateFlow("")
    val chatDraft: StateFlow<String> = _chatDraft.asStateFlow()

    // Mock active chat messages list to showcase deleted messages in action
    private val _activeChatMessages = MutableStateFlow<Map<Int, List<MockChatMessage>>>(emptyMap())
    val activeChatMessages: StateFlow<Map<Int, List<MockChatMessage>>> = _activeChatMessages.asStateFlow()

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

        // Seed data on startup
        viewModelScope.launch {
            repository.ensureSeeded()
            initMockChats()
        }
    }

    private fun initMockChats() {
        val chats = mapOf(
            1 to listOf(
                MockChatMessage(101, "Арслан Cherrygram", "Привет, зацени фичи Праймграм! 🚀", System.currentTimeMillis() - 600000, isMe = false),
                MockChatMessage(102, "Me", "Ого, привет! А режим невидимки (призрака) работает?", System.currentTimeMillis() - 480000, isMe = true),
                MockChatMessage(103, "Арслан Cherrygram", "Да, в призраке сообщения читаются без пометки, а онлайн скрыт совсем.", System.currentTimeMillis() - 400000, isMe = false)
            ),
            2 to listOf(
                MockChatMessage(201, "Разработчик Primegram", "Привет! Тестируем повышенную безопасность и IP обфускацию.", System.currentTimeMillis() - 3600000, isMe = false),
                MockChatMessage(202, "Me", "Отлично, у меня пинг прокси 38мс!", System.currentTimeMillis() - 3400000, isMe = true)
            ),
            3 to listOf(
                MockChatMessage(301, "Мама", "Сынок, ты покушал? ❤️ На даче рассада взошла отлично.", System.currentTimeMillis() - 17200000, isMe = false),
                MockChatMessage(302, "Me", "Выглядит круто. Да, поел!", System.currentTimeMillis() - 17100000, isMe = true)
            )
        )
        _activeChatMessages.value = chats
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

    fun selectChat(id: Int) {
        _activeChatId.value = id
        _chatDraft.value = ""
    }

    fun updateDraft(text: String) {
        _chatDraft.value = text
    }

    fun sendDraftMessage() {
        val draft = _chatDraft.value
        if (draft.isBlank()) return

        val chatId = _activeChatId.value
        val chatList = _activeChatMessages.value[chatId]?.toMutableList() ?: mutableListOf()
        val userMsgId = (500..100000).random()
        
        val newMsg = MockChatMessage(
            id = userMsgId,
            sender = "Me",
            text = draft,
            timestamp = System.currentTimeMillis(),
            isMe = true
        )
        chatList.add(newMsg)
        
        val updatedMap = _activeChatMessages.value.toMutableMap()
        updatedMap[chatId] = chatList
        _activeChatMessages.value = updatedMap
        _chatDraft.value = ""

        viewModelScope.launch {
            repository.addAnalyticsLog("app_usage", "Отправлено сообщ.", 1.0f)
            
            // Trigger auto reply translated check
            val currentSettings = settings.value ?: return@launch
            if (currentSettings.translatorEnabled) {
                delay(1200)
                simulateAutomaticTranslatedResponse(chatId, draft)
            }
        }
    }

    private suspend fun simulateAutomaticTranslatedResponse(chatId: Int, triggerText: String) {
        val chatList = _activeChatMessages.value[chatId]?.toMutableList() ?: return
        val responseText = when {
            triggerText.contains("привет", ignoreCase = true) || triggerText.contains("hi", ignoreCase = true) -> 
                "[Встроенный переводчик с English]: Hey there! This is an auto-translated response."
            triggerText.contains("тест", ignoreCase = true) -> 
                "[Встроенный переводчик]: Translation Service is fully operational (Ping: 12ms)"
            else -> 
                "[Встроенный переводчик]: Detected text. Translating into: ${settings.value?.translationTargetLanguage ?: "Русский"}"
        }

        val autoReply = MockChatMessage(
            id = (500..100000).random(),
            sender = getChatPartnerName(chatId),
            text = responseText,
            timestamp = System.currentTimeMillis(),
            isMe = false,
            isTranslated = true
        )
        chatList.add(autoReply)
        val updatedMap = _activeChatMessages.value.toMutableMap()
        updatedMap[chatId] = chatList
        _activeChatMessages.value = updatedMap

        repository.addAnalyticsLog("translation", "Выполнено переводов", 1.0f)
    }

    // SIMULATED MOD FEATURES FOR REVENUE TESTING (СИМУЛЯЦИЯ ФУНКЦИЙ)
    fun simulateDeletedMessageTrigger() {
        viewModelScope.launch {
            val currentChatId = _activeChatId.value
            val partner = getChatPartnerName(currentChatId)
            val indexColor = getChatPartnerColor(currentChatId)

            val chatList = _activeChatMessages.value[currentChatId]?.toMutableList() ?: mutableListOf()
            
            // Step 1: Partner sends normal message
            val phrase = listOf(
                "Слушай, а где исходники Черриграма лежали?",
                "Алекс, удали мою фотку с сервера плиз!",
                "Ой, случайно отправил тебе пароль от кошелька: secret_seed_phrase_2026",
                "Завтра в 14:00 встреча. Не говори никому, стираю сообщение."
            ).random()

            val msgId = (10000..99999).random()
            val incomingMsg = MockChatMessage(
                id = msgId,
                sender = partner,
                text = phrase,
                timestamp = System.currentTimeMillis(),
                isMe = false
            )
            chatList.add(incomingMsg)
            _activeChatMessages.value = _activeChatMessages.value.toMutableMap().apply { put(currentChatId, chatList) }

            showToast("$partner печатает...")
            delay(3500)

            // Step 2: Partner deletes message (it disappears from regular client but Primegram intercepts it!)
            val updatedList = _activeChatMessages.value[currentChatId]?.toMutableList() ?: mutableListOf()
            val msgIndex = updatedList.indexOfFirst { it.id == msgId }
            if (msgIndex != -1) {
                // In Primegram, we show it with an red [УДАЛЕНО] strike-through / label!
                val deletedMarkMsg = updatedList[msgIndex].copy(isDeletedInterpreted = true)
                updatedList[msgIndex] = deletedMarkMsg
                _activeChatMessages.value = _activeChatMessages.value.toMutableMap().apply { put(currentChatId, updatedList) }
            }

            // Save to actual database
            val dbMsg = DeletedMessage(
                senderName = partner,
                senderAvatarColor = indexColor,
                messageText = phrase,
                timestamp = System.currentTimeMillis() - 3500,
                deletedTimestamp = System.currentTimeMillis(),
                originalChatId = currentChatId
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
            
            val chatList = _activeChatMessages.value[currentChatId]?.toMutableList() ?: mutableListOf()

            val msgId = (10000..99999).random()
            val mediaMsg = MockChatMessage(
                id = msgId,
                sender = partner,
                text = "🖼 [$fileTypeName, нажмите для просмотра]",
                timestamp = System.currentTimeMillis(),
                isMe = false,
                isOneTimeMedia = true,
                mediaPlaceholder = mockTitle,
                isVideoType = isVideo
            )
            chatList.add(mediaMsg)
            _activeChatMessages.value = _activeChatMessages.value.toMutableMap().apply { put(currentChatId, chatList) }

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
                // Instantly uninstall
                repository.installPlugin(pluginId, false)
                showToast("🔌 Плагин отключен")
            } else {
                // Simulate download
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
            
            // Increment progress logs
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

    // Helpers
    fun getChatPartnerName(chatId: Int): String {
        return when (chatId) {
            1 -> "Арслан Cherrygram"
            2 -> "Разработчик Primegram"
            else -> "Мама"
        }
    }

    private fun getChatPartnerColor(chatId: Int): Int {
        return when (chatId) {
            1 -> 0xFFEF5350.toInt()
            2 -> 0xFF26A69A.toInt()
            else -> 0xFFFFCA28.toInt()
        }
    }
}

data class MockChatMessage(
    val id: Int,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isMe: Boolean,
    val isDeletedInterpreted: Boolean = false, // Intercepted deleted message
    val isTranslated: Boolean = false,
    val isOneTimeMedia: Boolean = false,
    val mediaPlaceholder: String = "",
    val isVideoType: Boolean = false
)
