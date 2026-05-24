package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class PrimeRepository(private val primeDao: PrimeDao) {

    val settings: Flow<PrimeSettings?> = primeDao.getSettingsFlow()
    val deletedMessages: Flow<List<DeletedMessage>> = primeDao.getDeletedMessagesFlow()
    val selfDestructMedia: Flow<List<SelfDestructMedia>> = primeDao.getSelfDestructMediaFlow()
    val proxyServers: Flow<List<ProxyServer>> = primeDao.getProxyServersFlow()
    val plugins: Flow<List<PluginEntity>> = primeDao.getPluginsFlow()
    val analyticsLogs: Flow<List<AnalyticsLog>> = primeDao.getAnalyticsLogsFlow()

    suspend fun getSettings(): PrimeSettings {
        return primeDao.getSettingsDirect() ?: createDefaultSettings()
    }

    suspend fun createDefaultSettings(): PrimeSettings {
        val defaultSettings = PrimeSettings()
        primeDao.insertSettings(defaultSettings)
        return defaultSettings
    }

    suspend fun updateSettings(newSettings: PrimeSettings) {
        primeDao.insertSettings(newSettings)
        // Add log entry for setting update
        addAnalyticsLog("app_usage", "Изм. настроек", System.currentTimeMillis().toFloat() % 100)
    }

    suspend fun addAnalyticsLog(category: String, label: String, value: Float) {
        primeDao.insertAnalyticsLog(
            AnalyticsLog(
                category = category,
                label = label,
                value = value,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun ensureSeeded() {
        val existingSettings = primeDao.getSettingsDirect()
        if (existingSettings == null) {
            // Seed Settings
            createDefaultSettings()

            // Seed Proxies
            val defaultProxies = listOf(
                ProxyServer(title = "Zurich Premium Proxy (SOCKS5)", host = "ch.mtproto.primegram.org", port = 1080, type = "SOCKS5", pingMs = 38, isCustom = false),
                ProxyServer(title = "Dubai MTProto (High Speed)", host = "ae.mtproto.primegram.org", port = 443, type = "MTProto", secret = "ee0102030405060708090a0b0c0d0e0f10", pingMs = 52, isCustom = false),
                ProxyServer(title = "Tokyo Cloud Server (SOCKS5)", host = "jp.mtproto.primegram.org", port = 8080, type = "SOCKS5", pingMs = 125, isCustom = false),
                ProxyServer(title = "New York Edge (MTProto)", host = "us.mtproto.primegram.org", port = 443, type = "MTProto", secret = "dd74656c656772616d", pingMs = 94, isCustom = false)
            )
            for (proxy in defaultProxies) {
                primeDao.insertProxyServer(proxy)
            }

            // Seed Plugins
            val defaultPlugins = listOf(
                PluginEntity("plugin_anti_recall", "Anti-Recall Pro", "Разблокирует чтение сообщений, которые собеседник сразу удалил у себя и у вас.", "CherryMod Team", "1.4", isInstalled = true, sizeMb = 0.8),
                PluginEntity("plugin_auto_translate", "Chat Translator Pro", "Мгновенно переводит входящие и исходящие сообщения в реальном времени.", "Primegram Labs", "2.1", isInstalled = true, sizeMb = 1.4),
                PluginEntity("plugin_self_destruct_saver", "Media Saver Block", "Полностью блокирует команду уничтожения одноразовых фото и видео, сохраняя их локально.", "StealthLabs", "3.0", isInstalled = true, sizeMb = 0.5),
                PluginEntity("plugin_auto_reply", "Auto-Responder Bot", "Кастомный автоответчик для вашего аккаунта по расписанию и ключевым фразам.", "Developer4k", "1.1", isInstalled = false, sizeMb = 2.2),
                PluginEntity("plugin_typing_hider", "Silent Typing Injector", "Полностью блокирует отправку уведомления 'печатает...' в любые чаты при наборе текста.", "Ghost Dev", "1.0", isInstalled = false, sizeMb = 0.4),
                PluginEntity("plugin_premium_status", "Premium Star Decorator", "Придает визуальный статус премиум-аккаунта во всех меню для вас офлайн.", "VisualMods", "1.5", isInstalled = true, sizeMb = 0.3)
            )
            for (plugin in defaultPlugins) {
                primeDao.insertPlugin(plugin)
            }

            // Seed Deleted Messages
            val deletedSeed = listOf(
                DeletedMessage(
                    senderName = "Арслан Cherrygram",
                    senderAvatarColor = 0xFFEF5350.toInt(),
                    messageText = "Привет! Я слил закрытый бета-код для обхода блокировок призрака. Ссылка: github.com/arsLan4k1390/cherry-stealth-bypass-alpha",
                    timestamp = System.currentTimeMillis() - 7200000,
                    deletedTimestamp = System.currentTimeMillis() - 7180000,
                    originalChatId = 1
                ),
                DeletedMessage(
                    senderName = "Разработчик Primegram",
                    senderAvatarColor = 0xFF26A69A.toInt(),
                    messageText = "Завтра выкатываем прокси SOCKS5 с авторотацией IP. Затестишь пинг?",
                    timestamp = System.currentTimeMillis() - 14400000,
                    deletedTimestamp = System.currentTimeMillis() - 14380000,
                    originalChatId = 2
                ),
                DeletedMessage(
                    senderName = "Мама",
                    senderAvatarColor = 0xFFFFCA28.toInt(),
                    messageText = "Удалила это сообщение, потому что перепутала чат! Это было для тети Лены про рассаду помидор 🍅",
                    timestamp = System.currentTimeMillis() - 28800000,
                    deletedTimestamp = System.currentTimeMillis() - 28750000,
                    originalChatId = 3
                )
            )
            for (msg in deletedSeed) {
                primeDao.insertDeletedMessage(msg)
            }

            // Seed Self-Destructing Media
            val mediaSeed = listOf(
                SelfDestructMedia(
                    senderName = "Арслан Cherrygram",
                    fileType = "image",
                    durationSeconds = 3,
                    fileSizeKb = 820,
                    timestamp = System.currentTimeMillis() - 3600000,
                    visualPlaceholderRes = "photo_preview_1",
                    title = "Снимок экрана настроек призрака.jpg"
                ),
                SelfDestructMedia(
                    senderName = "Alice Private",
                    fileType = "video",
                    durationSeconds = 12,
                    fileSizeKb = 4200,
                    timestamp = System.currentTimeMillis() - 18000000,
                    visualPlaceholderRes = "video_preview_2",
                    title = "Секретное видео.mp4"
                )
            )
            for (med in mediaSeed) {
                primeDao.insertSelfDestructMedia(med)
            }

            // Seed Analytics Logs
            val initialLogs = listOf(
                AnalyticsLog(category = "network", label = "Оптимизация трафика", value = 424.0f),
                AnalyticsLog(category = "network", label = "Запросы прокси", value = 1250.0f),
                AnalyticsLog(category = "app_usage", label = "Время в призраке (мин)", value = 180.0f),
                AnalyticsLog(category = "translation", label = "Автопереводы", value = 48.0f),
                AnalyticsLog(category = "security", label = "Угрозы заблокированы", value = 7.0f)
            )
            for (log in initialLogs) {
                primeDao.insertAnalyticsLog(log)
            }
        }
    }

    suspend fun insertDeletedMessage(msg: DeletedMessage) {
        primeDao.insertDeletedMessage(msg)
    }

    suspend fun clearDeletedMessages() {
        primeDao.clearDeletedMessages()
    }

    suspend fun insertSelfDestructMedia(media: SelfDestructMedia) {
        primeDao.insertSelfDestructMedia(media)
    }

    suspend fun clearSelfDestructMedia() {
        primeDao.clearSelfDestructMedia()
    }

    suspend fun addCustomProxy(title: String, host: String, port: Int, type: String, secret: String? = null) {
        primeDao.insertProxyServer(
            ProxyServer(
                title = title,
                host = host,
                port = port,
                type = type,
                secret = secret,
                pingMs = (30..150).random(),
                isCustom = true
            )
        )
        addAnalyticsLog("network", "Добавлен прокси", 1.0f)
    }

    suspend fun deleteProxy(id: Int) {
        primeDao.deleteProxyServerById(id)
    }

    suspend fun installPlugin(id: String, install: Boolean) {
        primeDao.getPluginsFlow().firstOrNull()?.find { it.id == id }?.let { plugin ->
            val updated = plugin.copy(isInstalled = install)
            primeDao.updatePlugin(updated)
            
            val stateName = if (install) "Установка плагина" else "Удаление плагина"
            addAnalyticsLog("app_usage", "$stateName: ${plugin.name}", plugin.sizeMb.toFloat())
        }
    }

    suspend fun clearCache() {
        addAnalyticsLog("network", "Очистка кэша (MB)", 345.5f)
    }
}
