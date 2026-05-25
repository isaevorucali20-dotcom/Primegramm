package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prime_settings")
data class PrimeSettings(
    @PrimaryKey val id: Int = 1,
    val themeName: String = "Elegant Dark", // Elegant Dark, Midnight Cherry, AMOLED Gold, Mint Ghost, Sapphire Prime, Classic Telegram
    val translatorEnabled: Boolean = true,
    val translationTargetLanguage: String = "Русский", // Русский, English, Español, Deutsch
    val cloudSyncEnabled: Boolean = true,
    val lastCloudSyncTime: Long = System.currentTimeMillis() - 120000, // 2 mins ago
    val encryptionLevel: String = "AES-256", // AES-256, ChaCha20, RSA-4096
    val passcodeLockEnabled: Boolean = false,
    val securitySelfDestructTimer: Int = 0, // Minutes
    val hardwareAccelerationEnabled: Boolean = true,
    
    // Ghost Mode & Privacy Settings
    val ghostModeEnabled: Boolean = true, // Ghost Mode (fully invisible)
    val hideOnlineStatusUniversal: Boolean = true,
    val anonymousStoriesViewer: Boolean = true,
    val hideTypingStatus: Boolean = true,
    val hideReadStatus: Boolean = false,
    
    // Recovery & Vault
    val saveDeletedMessages: Boolean = true,
    val saveSelfDestructingMedia: Boolean = true,
    
    // Proxy & Location
    val proxyEnabled: Boolean = false,
    val currentProxyId: Int = 1,
    val spoofedIpLocation: String = "Zurich, Switzerland", // Zurich, Tokyo, New York, Moscow, London, Off
    val spoofedIpAddress: String = "194.209.14.88",
    
    // Set API configuration
    val setApiJson: String = "{}",

    // Profile Customization
    val userUniqueId: String = "",
    val userDisplayName: String = "Пользователь Primegram",
    val userUsername: String = "@user_prime",
    val userBio: String = "Приватный аккаунт в Primegram ⚡",
    val userAvatarGradientStart: Int = 0xFF9C27B0.toInt(),
    val userAvatarGradientEnd: Int = 0xFF3F51B5.toInt(),
    val userNeonGlowColor: String = "Neon Purple", // Off, Neon Purple, Neon Cyan, Neon Pink, Neon Gold, Neon Green
    val userSpentStars: Int = 0,
    val userRating: Int = 100
)

@Entity(tableName = "deleted_messages")
data class DeletedMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val senderAvatarColor: Int, // hex color value
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deletedTimestamp: Long = System.currentTimeMillis() + 15000,
    val originalChatId: Int = 1
)

@Entity(tableName = "self_destruct_media")
data class SelfDestructMedia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val fileType: String, // "image" or "video"
    val durationSeconds: Int = 5,
    val fileSizeKb: Int = 1420,
    val timestamp: Long = System.currentTimeMillis(),
    val visualPlaceholderRes: String = "ic_locked",
    val title: String
)

@Entity(tableName = "proxy_servers")
data class ProxyServer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val host: String,
    val port: Int,
    val type: String, // MTProto, SOCKS5
    val secret: String? = null,
    val pingMs: Int = 45,
    val isCustom: Boolean = false
)

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val isInstalled: Boolean = false,
    val sizeMb: Double = 1.2,
    val scriptLanguage: String = "javascript", // "python" or "javascript"
    val scriptCode: String = ""
)

@Entity(tableName = "analytics_log")
data class AnalyticsLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "network", "app_usage", "translation"
    val timestamp: Long = System.currentTimeMillis(),
    val label: String,
    val value: Float
)

@Entity(tableName = "chat_users")
data class ChatUser(
    @PrimaryKey val id: String, // User numeric ID or username
    val displayName: String,
    val username: String, // e.g. "@username"
    val avatarColor: Int = 0xFF42A5F5.toInt(),
    val isBot: Boolean = false,
    val botToken: String? = null,
    val botScript: String? = null, // Rules like "hello->Hi there!;ping->pong"
    val isLocallyCreated: Boolean = true,
    val bio: String = "Пользователь Primegram ⚡",
    val spentStars: Int = 0,
    val neonGlowColor: String = "Off"
)

@Entity(tableName = "local_messages")
data class LocalMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatUserId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean,
    val isDeleted: Boolean = false,
    val isOneTimeMedia: Boolean = false,
    val mediaPlaceholder: String = "",
    val isVideoType: Boolean = false,
    val isTranslated: Boolean = false
)

@Entity(tableName = "mini_apps")
data class MiniAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val url: String,
    val iconName: String = "web",
    val addedByUser: Boolean = false
)

