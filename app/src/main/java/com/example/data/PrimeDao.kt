package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrimeDao {
    @Query("SELECT * FROM settings_table WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<PrimeSettingsEntity?>

    @Query("SELECT * FROM settings_table WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): PrimeSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: PrimeSettingsEntity)

    // Chat Users
    @Query("SELECT * FROM chat_users_table ORDER BY isBot ASC, displayName ASC")
    fun getChatUsersFlow(): Flow<List<ChatUserEntity>>

    @Query("SELECT * FROM chat_users_table WHERE id = :userId LIMIT 1")
    suspend fun getChatUserDirect(userId: String): ChatUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChatUser(user: ChatUserEntity)

    @Query("DELETE FROM chat_users_table WHERE id = :userId")
    suspend fun deleteChatUser(userId: String)

    // Messages
    @Query("SELECT * FROM messages_table WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages_table WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatDirect(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages_table ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages_table SET isDeletedLocally = 1 WHERE id = :messageId")
    suspend fun markMessageDeletedLocally(messageId: Long)

    @Query("UPDATE messages_table SET text = :text, isInterceptedDeleted = 1 WHERE id = :messageId")
    suspend fun markMessageInterceptedDeleted(messageId: Long, text: String)

    @Query("UPDATE messages_table SET text = :newText, isTranslated = 0, translatedText = NULL WHERE id = :messageId")
    suspend fun updateMessageText(messageId: Long, newText: String)

    @Query("DELETE FROM messages_table WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM messages_table WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)

    @Query("DELETE FROM messages_table")
    suspend fun clearAllMessages()

    @Query("UPDATE messages_table SET isTranslated = 1, translatedText = :translatedText WHERE id = :messageId")
    suspend fun updateMessageTranslation(messageId: Long, translatedText: String)

    // Live Proxies
    @Query("SELECT * FROM proxy_profiles_table")
    fun getProxyProfilesFlow(): Flow<List<ProxyProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxyProfile(proxy: ProxyProfileEntity)

    @Query("DELETE FROM proxy_profiles_table WHERE id = :proxyId")
    suspend fun deleteProxy(proxyId: Int)

    // MiniApps
    @Query("SELECT * FROM miniapps_table")
    fun getMiniAppsFlow(): Flow<List<MiniAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMiniApp(miniApp: MiniAppEntity)

    @Query("DELETE FROM miniapps_table WHERE id = :miniAppId")
    suspend fun deleteMiniApp(miniAppId: String)

    // Plugins
    @Query("SELECT * FROM plugins_table")
    fun getPluginsFlow(): Flow<List<PluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: PluginEntity)

    @Query("UPDATE plugins_table SET isEnabled = :enabled WHERE id = :pluginId")
    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean)

    @Query("DELETE FROM plugins_table WHERE id = :pluginId")
    suspend fun deletePlugin(pluginId: String)
}
