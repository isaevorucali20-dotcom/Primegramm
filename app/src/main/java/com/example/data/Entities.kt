package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings_table")
data class PrimeSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val userUniqueId: String = "",
    val displayName: String = "Пользователь Prime",
    val username: String = "@member_prime",
    val bio: String = "Цифровой призрак в криптографическом туннеле.",
    val selectedTheme: String = "Dark Cosmic Slate",
    val targetTranslationLang: String = "Russian",
    val proxyEnabled: Boolean = false,
    val ghostModeEnabled: Boolean = false,
    val antiRecallEnabled: Boolean = true,
    val mediaSaverEnabled: Boolean = true,
    val spoofedIpAddress: String = "192.168.1.1",
    val locationSpoof: String = "Off",
    val activeEncryptionLevel: String = "AES-256-GCM",
    val starsBalance: Int = 12500,
    val torTunnelEnabled: Boolean = false,
    val torOnionAddress: String = "",
    val p2pDhtEnabled: Boolean = false,
    val p2pPeerId: String = ""
)

@Entity(tableName = "chat_users_table")
data class ChatUserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String?,
    val bio: String?,
    val isBot: Boolean = false,
    val avatarColor: Long = 0xFF2196F3,
    val neonGlowColor: String? = null,
    val spentStars: Int = 0,
    val botToken: String? = null,
    val botScript: String? = null
)

@Entity(tableName = "messages_table")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOneTimeMedia: Boolean = false,
    val mediaSavedReplicaPath: String? = null,
    val isDeletedLocally: Boolean = false,
    val isInterceptedDeleted: Boolean = false,
    val sizeBytes: Long = 0,
    val isTranslated: Boolean = false,
    val translatedText: String? = null
)

@Entity(tableName = "proxy_profiles_table")
data class ProxyProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val host: String,
    val port: Int,
    val secret: String?,
    val isCurrentlyActive: Boolean = false
)

@Entity(tableName = "miniapps_table")
data class MiniAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val url: String,
    val iconEmoji: String = "🤖"
)

@Entity(tableName = "plugins_table")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val isEnabled: Boolean,
    val type: String,
    val description: String
)
