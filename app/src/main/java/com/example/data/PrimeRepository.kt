package com.example.data

import kotlinx.coroutines.flow.Flow

class PrimeRepository(private val dao: PrimeDao) {
    val settings: Flow<PrimeSettingsEntity?> = dao.getSettingsFlow()
    val chatUsers: Flow<List<ChatUserEntity>> = dao.getChatUsersFlow()
    val allMessages: Flow<List<MessageEntity>> = dao.getAllMessagesFlow()
    val proxies: Flow<List<ProxyProfileEntity>> = dao.getProxyProfilesFlow()
    val miniApps: Flow<List<MiniAppEntity>> = dao.getMiniAppsFlow()
    val plugins: Flow<List<PluginEntity>> = dao.getPluginsFlow()

    suspend fun getSettingsDirect(): PrimeSettingsEntity? = dao.getSettingsDirect()
    suspend fun updateSettings(settings: PrimeSettingsEntity) = dao.insertOrUpdateSettings(settings)

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = dao.getMessagesForChatFlow(chatId)

    suspend fun getChatUserDirect(userId: String) = dao.getChatUserDirect(userId)
    suspend fun insertOrUpdateChatUser(user: ChatUserEntity) = dao.insertOrUpdateChatUser(user)
    suspend fun deleteChatUser(userId: String) = dao.deleteChatUser(userId)

    suspend fun insertMessage(message: MessageEntity) = dao.insertMessage(message)
    suspend fun markMessageDeletedLocally(messageId: Long) = dao.markMessageDeletedLocally(messageId)
    suspend fun markMessageInterceptedDeleted(messageId: Long, text: String) = dao.markMessageInterceptedDeleted(messageId, text)
    suspend fun clearChatMessages(chatId: String) = dao.clearChatMessages(chatId)
    suspend fun clearAllMessages() = dao.clearAllMessages()
    suspend fun updateMessageTranslation(messageId: Long, translatedText: String) = dao.updateMessageTranslation(messageId, translatedText)

    suspend fun insertProxyProfile(proxy: ProxyProfileEntity) = dao.insertProxyProfile(proxy)
    suspend fun deleteProxy(proxyId: Int) = dao.deleteProxy(proxyId)

    suspend fun insertMiniApp(miniApp: MiniAppEntity) = dao.insertMiniApp(miniApp)
    suspend fun deleteMiniApp(miniAppId: String) = dao.deleteMiniApp(miniAppId)

    suspend fun insertPlugin(plugin: PluginEntity) = dao.insertPlugin(plugin)
    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean) = dao.setPluginEnabled(pluginId, enabled)
}
