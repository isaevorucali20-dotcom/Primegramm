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
    val chatUsers: Flow<List<ChatUser>> = primeDao.getChatUsersFlow()
    val miniApps: Flow<List<MiniAppEntity>> = primeDao.getMiniAppsFlow()

    fun getLocalMessagesForChat(chatUserId: String): Flow<List<LocalMessage>> {
        return primeDao.getLocalMessagesForChatFlow(chatUserId)
    }

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
                PluginEntity("plugin_anti_recall", "Anti-Recall Pro", "Разблокирует чтение сообщений, которые собеседник сразу удалил у себя и у вас.", "PrimeDev Team", "1.4", isInstalled = true, sizeMb = 0.8),
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
                    senderName = "Prine41k dev",
                    senderAvatarColor = 0xFF26A69A.toInt(),
                    messageText = "Я случайно отправил отладочный токен плагина: sec_tkn_prime41k_9041. Пожалуйста, сотри.",
                    timestamp = System.currentTimeMillis() - 7200000,
                    deletedTimestamp = System.currentTimeMillis() - 7180000,
                    originalChatId = 41
                )
            )
            for (msg in deletedSeed) {
                primeDao.insertDeletedMessage(msg)
            }

            // Seed Self-Destructing Media
            val mediaSeed = listOf(
                SelfDestructMedia(
                    senderName = "Prine41k dev",
                    fileType = "image",
                    durationSeconds = 3,
                    fileSizeKb = 820,
                    timestamp = System.currentTimeMillis() - 3600000,
                    visualPlaceholderRes = "photo_preview_1",
                    title = "Схема шифрования_протокола_MTProto.jpg"
                )
            )
            for (med in mediaSeed) {
                primeDao.insertSelfDestructMedia(med)
            }

            // Seed Analytics Logs
            val initialLogs = listOf(
                AnalyticsLog(category = "network", label = "Оптимизация трафика", value = 424.0f),
                AnalyticsLog(category = "network", label = "Запросы прокси", value = 1250.0f),
                AnalyticsLog(category = "app_usage", label = "Время в Primegram (мин)", value = 180.0f),
                AnalyticsLog(category = "translation", label = "Автопереводы", value = 48.0f),
                AnalyticsLog(category = "security", label = "Угрозы заблокированы", value = 7.0f)
            )
            for (log in initialLogs) {
                primeDao.insertAnalyticsLog(log)
            }

            // Seed Chat Users (Only 2 chats: AI Bot and Prine41k dev)
            val defaultUsers = listOf(
                ChatUser("prime41k", "Prine41k dev", "@prime41k_dev", 0xFF26A69A.toInt(), isBot = false, isLocallyCreated = false, bio = "Создатель ядра Primegram & оригинального stealth-движка плагинов ⚙️", spentStars = 15400, neonGlowColor = "Neon Gold"),
                ChatUser("assistant_bot", "Stealth Ассистент-Бот", "@prime_ai_bot", 0xFF9C27B0.toInt(), isBot = true, botToken = "bot2026_stealth", botScript = "hello->Здравствуйте! Я защищенный ИИ ассистент Primegram.;ping->Pong!;помощь->Команды: hello, ping, помощь, игра, погода;info->Primegram v3.0 (Stealth Edition);игра->🎲 Вы бросили кости! Выпало: " + (1..6).random() + " и " + (1..6).random() + "!;погода->⛅ Отличная погода для кибербезопасности: +21°C", isLocallyCreated = false, bio = "Ваш ИИ-помощник Primegram. Задайте любой технический вопрос 🧠", spentStars = 0, neonGlowColor = "Neon Cyan")
            )
            for (user in defaultUsers) {
                primeDao.insertChatUser(user)
            }

            // Seed Local Messages
            val defaultMessages = listOf(
                LocalMessage(chatUserId = "prime41k", senderName = "Prine41k dev", text = "Привет! Добро пожаловать в мессенджер Primegram. 🚀", timestamp = System.currentTimeMillis() - 600000, isMe = false),
                LocalMessage(chatUserId = "prime41k", senderName = "Me", text = "Привет! Сборка выглядит супер. Как работает ядро плагинов?", timestamp = System.currentTimeMillis() - 480000, isMe = true),
                LocalMessage(chatUserId = "prime41k", senderName = "Prine41k dev", text = "Оно интегрировано на низком уровне. Попробуй включить любой плагин во вкладке Плагинов!", timestamp = System.currentTimeMillis() - 400000, isMe = false),
                
                LocalMessage(chatUserId = "assistant_bot", senderName = "Stealth Ассистент-Бот", text = "Привет! Я твой встроенный Stealth ИИ Ассистент на базе API. Общаюсь на любые темы, связанные с шифрованием, криптой, TON или разработкой!", timestamp = System.currentTimeMillis() - 10000, isMe = false)
            )
            for (msg in defaultMessages) {
                primeDao.insertLocalMessage(msg)
            }

            // Seed Mini Apps
            val defaultMiniApps = listOf(
                MiniAppEntity("bot_picker", "BotFather Manager", "Кастомный менеджер ботов и токенов Primegram", "https://telegram.org/js/telegram-web-app.js", "smart_toy", false),
                MiniAppEntity("tg_games", "Retro Space Game", "Полноценная HTML5 игра Gamee для тренировки реакции", "https://tgbots.io/html5-game-demo", "sports_esports", false),
                MiniAppEntity("cherry_docs", "Primegram Guide", "Официальное руководство по сборке клиента и плагинов Primegram", "https://github.com/arsLan4k1390/Cherrygram/blob/master/README.md", "menu_book", false),
                MiniAppEntity("ton_viewer", "TON Space explorer", "Интегрированный HTML-кошелек и обозреватель адресов блокчейна", "https://tonscan.org/", "account_balance_wallet", false)
            )
            for (app in defaultMiniApps) {
                primeDao.insertMiniApp(app)
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

    suspend fun insertPlugin(plugin: PluginEntity) {
        primeDao.insertPlugin(plugin)
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

    suspend fun insertChatUser(user: ChatUser) {
        primeDao.insertChatUser(user)
    }

    suspend fun deleteChatUser(id: String) {
        primeDao.deleteChatUserById(id)
    }

    suspend fun insertLocalMessage(msg: LocalMessage) {
        primeDao.insertLocalMessage(msg)
    }

    suspend fun markLocalMessageDeleted(id: Int) {
        primeDao.markLocalMessageDeleted(id)
    }

    suspend fun clearChatHistory(chatUserId: String) {
        primeDao.clearChatHistory(chatUserId)
    }

    suspend fun insertMiniApp(app: MiniAppEntity) {
        primeDao.insertMiniApp(app)
    }

    suspend fun deleteMiniApp(id: String) {
        primeDao.deleteMiniAppById(id)
    }
}
