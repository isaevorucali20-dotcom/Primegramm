package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrimeDao {

    // Settings
    @Query("SELECT * FROM prime_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<PrimeSettings?>

    @Query("SELECT * FROM prime_settings WHERE id = 1")
    suspend fun getSettingsDirect(): PrimeSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: PrimeSettings)

    @Update
    suspend fun updateSettings(settings: PrimeSettings)

    // Deleted Messages
    @Query("SELECT * FROM deleted_messages ORDER BY timestamp DESC")
    fun getDeletedMessagesFlow(): Flow<List<DeletedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedMessage(msg: DeletedMessage)

    @Query("DELETE FROM deleted_messages")
    suspend fun clearDeletedMessages()

    @Query("DELETE FROM deleted_messages WHERE id = :id")
    suspend fun deleteDeletedMessage(id: Int)

    // Self Destruct Media
    @Query("SELECT * FROM self_destruct_media ORDER BY timestamp DESC")
    fun getSelfDestructMediaFlow(): Flow<List<SelfDestructMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelfDestructMedia(media: SelfDestructMedia)

    @Query("DELETE FROM self_destruct_media")
    suspend fun clearSelfDestructMedia()

    // Proxy Servers
    @Query("SELECT * FROM proxy_servers ORDER BY isCustom DESC, id ASC")
    fun getProxyServersFlow(): Flow<List<ProxyServer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxyServer(proxy: ProxyServer)

    @Query("DELETE FROM proxy_servers WHERE id = :id")
    suspend fun deleteProxyServerById(id: Int)

    // Plugins
    @Query("SELECT * FROM plugins ORDER BY name ASC")
    fun getPluginsFlow(): Flow<List<PluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: PluginEntity)

    @Update
    suspend fun updatePlugin(plugin: PluginEntity)

    // Analytics Logs
    @Query("SELECT * FROM analytics_log ORDER BY timestamp DESC LIMIT 100")
    fun getAnalyticsLogsFlow(): Flow<List<AnalyticsLog>>

    @Query("SELECT * FROM analytics_log WHERE category = :category ORDER BY timestamp DESC")
    fun getAnalyticsLogsByCategory(category: String): Flow<List<AnalyticsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsLog(log: AnalyticsLog)
    
    @Query("DELETE FROM analytics_log")
    suspend fun clearAnalyticsLogs()

    // Chat Users
    @Query("SELECT * FROM chat_users ORDER BY displayName ASC")
    fun getChatUsersFlow(): Flow<List<ChatUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatUser(user: ChatUser)

    @Query("DELETE FROM chat_users WHERE id = :id")
    suspend fun deleteChatUserById(id: String)

    // Local Messages
    @Query("SELECT * FROM local_messages WHERE chatUserId = :chatUserId ORDER BY timestamp ASC")
    fun getLocalMessagesForChatFlow(chatUserId: String): Flow<List<LocalMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalMessage(msg: LocalMessage)

    @Update
    suspend fun updateLocalMessage(msg: LocalMessage)

    @Query("UPDATE local_messages SET isDeleted = 1 WHERE id = :id")
    suspend fun markLocalMessageDeleted(id: Int)

    @Query("DELETE FROM local_messages WHERE chatUserId = :chatUserId")
    suspend fun clearChatHistory(chatUserId: String)

    // Mini Apps
    @Query("SELECT * FROM mini_apps ORDER BY name ASC")
    fun getMiniAppsFlow(): Flow<List<MiniAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMiniApp(app: MiniAppEntity)

    @Query("DELETE FROM mini_apps WHERE id = :id")
    suspend fun deleteMiniAppById(id: String)
}
