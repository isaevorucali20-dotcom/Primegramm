package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.PrimeViewModel
import com.example.ui.theme.PrimegramTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PrimegramDashboard(
    viewModel: PrimeViewModel,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settings.collectAsState()
    val activeSettings = settingsState ?: PrimeSettings()
    val context = LocalContext.current
    
    var currentScreen by remember { mutableStateOf("chats") }

    PrimegramTheme(themeName = activeSettings.themeName) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenNavigation"
            ) { screen ->
                when (screen) {
                    "chats" -> {
                        ChatsScreen(
                            viewModel = viewModel,
                            activeSettings = activeSettings,
                            onOpenSettings = { currentScreen = "settings" }
                        )
                    }
                    "settings" -> {
                        PrimegramSettingsScreen(
                            viewModel = viewModel,
                            activeSettings = activeSettings,
                            onBack = { currentScreen = "chats" },
                            onExitApp = { (context as? android.app.Activity)?.finish() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings,
    onOpenSettings: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val activeChatId by viewModel.activeChatId.collectAsState()
    val chatUsers by viewModel.chatUsers.collectAsState()
    val currentMessages by viewModel.activeChatMessages.collectAsState()
    val draft by viewModel.chatDraft.collectAsState()
    val typingState by viewModel.typingState.collectAsState()
    
    // Dialog overlays
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showMiniAppsDialog by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }
    var activeMiniAppUrl by remember { mutableStateOf<String?>(null) }
    var activeMiniAppName by remember { mutableStateOf("") }
    
    // Parse title_override and online_count overrides from Set API
    val (appTitle, onlineLabel) = remember(activeSettings.setApiJson, activeSettings.ghostModeEnabled) {
        var title = "Cherrygram Stealth"
        var online = if (activeSettings.ghostModeEnabled) "Режим призрака активен 👻" else "в сети"
        
        try {
            val json = activeSettings.setApiJson
            if (json.contains("title_override")) {
                val extractedTitle = json.substringAfter("title_override\"")
                    .substringAfter(":").substringAfter("\"").substringBefore("\"")
                if (extractedTitle.isNotBlank()) title = extractedTitle
            }
            if (json.contains("online_count")) {
                val extractedOnline = json.substringAfter("online_count\"")
                    .substringAfter(":").substringAfter("\"").substringBefore("\"")
                if (extractedOnline.isNotBlank()) online = extractedOnline
            }
        } catch (e: Exception) {
            // dynamic fallback
        }
        Pair(title, online)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenuContent(
                activeSettings = activeSettings,
                activeChatId = activeChatId,
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                onSelectChat = { id ->
                    scope.launch { drawerState.close() }
                    viewModel.selectChat(id)
                },
                onOpenAddUser = {
                    scope.launch { drawerState.close() }
                    showAddUserDialog = true
                },
                onOpenMiniApps = {
                    scope.launch { drawerState.close() }
                    showMiniAppsDialog = true
                },
                viewModel = viewModel
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Главное меню")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = viewModel.getChatPartnerName(activeChatId),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val activeTyping = typingState[activeChatId]
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeTyping != null) MaterialTheme.colorScheme.primary 
                                            else if (activeSettings.ghostModeEnabled) Color(0xFF78909C) 
                                            else Color(0xFF4CAF50)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeTyping ?: onlineLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeTyping != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontWeight = if (activeTyping != null) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showVaultDialog = true },
                            modifier = Modifier.testTag("vault_view_button")
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "Сейф перехватов",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { scope.launch { viewModel.simulateDeletedMessageTrigger() } },
                            modifier = Modifier.testTag("sim_delete_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Симулировать удаление",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = { scope.launch { viewModel.simulateOneTimeMediaTrigger() } },
                            modifier = Modifier.testTag("sim_media_button")
                        ) {
                            Icon(
                                Icons.Default.OfflineBolt,
                                contentDescription = "Симулировать фото-призрак",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                ChatInputBar(
                    draft = draft,
                    onDraftChange = { viewModel.updateDraft(it) },
                    onSend = { viewModel.sendDraftMessage() }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Creative Matrix Background Lines
                BackgroundGrid()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔒 Сквозное шифрование: ${activeSettings.encryptionLevel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Все сообщения полностью автономны и зашифрованы.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (currentMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "История сообщений чиста. Начните диалог или введите бот-команды!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(currentMessages, key = { it.id }) { message ->
                            ChatMessageItem(message = message, activeSettings = activeSettings, viewModel = viewModel)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Overlays
    if (showAddUserDialog) {
        AddChatUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { id, name, username, isBot, botToken, botScript ->
                viewModel.addChatUser(id, name, username, isBot, botToken, botScript)
                showAddUserDialog = false
            }
        )
    }

    if (showMiniAppsDialog) {
        MiniAppHubDialog(
            viewModel = viewModel,
            onDismiss = { showMiniAppsDialog = false },
            onLaunchApp = { url, name ->
                activeMiniAppUrl = url
                activeMiniAppName = name
            }
        )
    }

    if (activeMiniAppUrl != null) {
        MiniAppWebViewDialog(
            url = activeMiniAppUrl!!,
            appName = activeMiniAppName,
            onDismiss = { activeMiniAppUrl = null }
        )
    }

    if (showVaultDialog) {
        VaultViewDialog(
            viewModel = viewModel,
            onDismiss = { showVaultDialog = false }
        )
    }
}

@Composable
fun DrawerMenuContent(
    activeSettings: PrimeSettings,
    activeChatId: String,
    onOpenSettings: () -> Unit,
    onSelectChat: (String) -> Unit,
    onOpenAddUser: () -> Unit,
    onOpenMiniApps: () -> Unit,
    viewModel: PrimeViewModel
) {
    val chatUsers by viewModel.chatUsers.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .width(290.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                RoundedCornerShape(0.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Profile Header Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isPremiumActive = viewModel.isPluginInstalled("plugin_premium_status")
                                Text(
                                    text = if (isPremiumActive) "Cherry Stealth ⭐" else "Cherry Stealth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPremiumActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Верифицирован",
                                    tint = if (isPremiumActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "@stealth_user",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Режим Невидимки", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (activeSettings.ghostModeEnabled) "АКТИВЕН 👻" else "ВЫКЛ 👁️",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSettings.ghostModeEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            // Sidebar Controls & Connections
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ИНСТРУМЕНТЫ И УПРАВЛЕНИЕ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                        label = { Text("Создать собеседника / Бота") },
                        selected = false,
                        onClick = onOpenAddUser,
                        modifier = Modifier.height(48.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Extension, contentDescription = null) },
                        label = { Text("Мини-приложения (API)") },
                        selected = false,
                        onClick = onOpenMiniApps,
                        modifier = Modifier.height(48.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Stealth Настройки") },
                        selected = false,
                        onClick = onOpenSettings,
                        modifier = Modifier.height(48.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ШИФРОВАННЫЕ КАНАЛЫ СВЯЗИ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }

                if (chatUsers.isEmpty()) {
                    item {
                        Text(
                            text = "Нет активных чатов.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    items(chatUsers, key = { it.id }) { user ->
                        val isSelected = activeChatId == user.id
                        val itemBg = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(itemBg)
                                .clickable { onSelectChat(user.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar indicator
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(user.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (user.isBot) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF9C27B0))
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (user.isBot) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "bot",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier
                                                .background(Color(0xFF9C27B0), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: LocalMessage,
    activeSettings: PrimeSettings,
    viewModel: PrimeViewModel
) {
    val alignment = if (message.isMe) Alignment.End else Alignment.Start
    val containerBg = if (message.isMe) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val isAntiRecallActive = viewModel.isPluginInstalled("plugin_anti_recall")
    val isMediaSaverActive = viewModel.isPluginInstalled("plugin_self_destruct_saver")
    var showMediaDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (message.isMe) 14.dp else 2.dp,
                        bottomEnd = if (message.isMe) 2.dp else 14.dp
                    )
                )
                .background(containerBg)
                .border(
                    1.dp,
                    if (message.isDeleted && isAntiRecallActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else if (message.isDeleted) Color.Red.copy(alpha = 0.5f)
                    else Color.Transparent,
                    RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                if (!message.isMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (message.isDeleted) {
                    if (isAntiRecallActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🛡️ Предотвращено Anti-Recall Pro:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GppBad,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Сообщение удалено собеседником",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🚫 Содержимое стерто (Включите Anti-Recall Pro)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (message.isTranslated) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🌐 Translated live into: ${activeSettings.translationTargetLanguage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (message.isOneTimeMedia) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showMediaDialog = true }
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (message.isVideoType) Icons.Default.PlayCircle else Icons.Default.Image,
                                contentDescription = null,
                                tint = if (isMediaSaverActive) MaterialTheme.colorScheme.primary else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.mediaPlaceholder ?: "file.jpg",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isMediaSaverActive) "📥 Сейф: перехвачено (нажмите)" else "🔒 Удалено сервером (нажмите)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.sp,
                                    color = if (isMediaSaverActive) MaterialTheme.colorScheme.primary else Color.Red.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = "Прочитано",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

    if (showMediaDialog) {
        AlertDialog(
            onDismissRequest = { showMediaDialog = false },
            confirmButton = {
                TextButton(onClick = { showMediaDialog = false }) {
                    Text("Закрыть")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isMediaSaverActive) Icons.Default.Security else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isMediaSaverActive) MaterialTheme.colorScheme.primary else Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMediaSaverActive) "Дешифровано в Сейф" else "Файл заблокирован",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    if (isMediaSaverActive) {
                        Text(
                            text = "🛡️ Плагин [Media Saver Block] заблокировал команду уничтожения:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (message.isVideoType) Icons.Default.PlayCircle else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message.mediaPlaceholder ?: "image.jpg",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Размер: ${(100..2500).random()} KB | Формат: ${if (message.isVideoType) "MP4 Видео" else "JPEG Изображение"}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Встроенный дешифратор Cherrygram сохранил локальную копию в Секретном Сейфе. Отправитель уверен, что файл стерт.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "⏳ Файл уничтожен сервером призрака.",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Собеседник отправил этот медиафайл в одноразовом режиме (Self-destructing). Для автоматического обхода защиты и удержания файлов включите плагин 'Media Saver Block' во вкладке Плагинов!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        )
    }

        if (!message.isMe && message.chatUserId == "assistant_bot") {
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val botCommands = listOf("hello", "ping", "игра", "погода", "info")
                botCommands.forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable {
                                viewModel.updateDraft(cmd)
                                viewModel.sendDraftMessage()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = cmd,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Напишите сообщение...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text")
                    .heightIn(max = 120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = draft.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (draft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("send_msg_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = if (draft.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// Background Decoration Matrix Grid lines
@Composable
fun BackgroundGrid() {
    val gridColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = 40.dp.toPx()

        var x = 0f
        while (x < width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += step
        }

        var y = 0f
        while (y < height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += step
        }
    }
}

// dialog overlay implementations
@Composable
fun AddChatUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, String?, String?) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isBot by remember { mutableStateOf(false) }
    var botToken by remember { mutableStateOf("") }
    var botScript by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("➕ Создать Канал / Бота") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it.replace(" ", "") },
                    label = { Text("ID численный или имя") },
                    placeholder = { Text("e.g. 104509") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Отображаемое имя") },
                    placeholder = { Text("e.g. Арслан Новые фичи") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Юзернейм (@)") },
                    placeholder = { Text("e.g. @arslan_dev") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isBot = !isBot }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = isBot, onCheckedChange = { isBot = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Является Python ботом")
                }

                if (isBot) {
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Токен бота (Bot API)") },
                        placeholder = { Text("1234:ABCDEF_token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = botScript,
                        onValueChange = { botScript = it },
                        label = { Text("Хуки скрипта Python (разделитель ';')") },
                        placeholder = { Text("hello->Приветствую!; ping->Pong!; помощь->Команды...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userId.isNotBlank() && displayName.isNotBlank()) {
                        val finalUsername = if (username.startsWith("@")) username else "@$username"
                        onConfirm(
                            userId,
                            displayName,
                            finalUsername,
                            isBot,
                            if (isBot) botToken else null,
                            if (isBot) botScript else null
                        )
                    }
                },
                enabled = userId.isNotBlank() && displayName.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun MiniAppHubDialog(
    viewModel: PrimeViewModel,
    onLaunchApp: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val miniApps by viewModel.miniApps.collectAsState()
    
    // Add custom app states
    var showAddApp by remember { mutableStateOf(false) }
    var newAppId by remember { mutableStateOf("") }
    var newAppName by remember { mutableStateOf("") }
    var newAppDesc by remember { mutableStateOf("") }
    var newAppUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🌐 Mini Apps API Hub")
                IconButton(onClick = { showAddApp = !showAddApp }) {
                    Icon(if (showAddApp) Icons.Default.Close else Icons.Default.AddCircle, contentDescription = null)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showAddApp) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("➕ Добавить Новое Мини-Приложение", style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = newAppId,
                                onValueChange = { newAppId = it.replace(" ", "") },
                                label = { Text("ID приложения") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newAppName,
                                onValueChange = { newAppName = it },
                                label = { Text("Название") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newAppDesc,
                                onValueChange = { newAppDesc = it },
                                label = { Text("Описание") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newAppUrl,
                                onValueChange = { newAppUrl = it },
                                label = { Text("Web URL (https://)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newAppId.isNotBlank() && newAppName.isNotBlank() && newAppUrl.isNotBlank()) {
                                        viewModel.addMiniApp(newAppId, newAppName, newAppDesc, newAppUrl, "travel_explore")
                                        showAddApp = false
                                        newAppId = ""
                                        newAppName = ""
                                        newAppDesc = ""
                                        newAppUrl = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                enabled = newAppId.isNotBlank() && newAppName.isNotBlank() && newAppUrl.isNotBlank()
                            ) {
                                Text("Сохранить")
                            }
                        }
                    }
                }

                Text(
                    "Ниже представлены интегрированные веб-приложения на основе API. Нажмите для запуска в безопасном контейнере:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(290.dp)
                ) {
                    items(miniApps) { app ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLaunchApp(app.url, app.name)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(app.description, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color.Gray)
                                    Text(app.url, style = MaterialTheme.typography.bodySmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                if (app.addedByUser) {
                                    IconButton(onClick = { viewModel.removeMiniApp(app.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppWebViewDialog(
    url: String,
    appName: String,
    onDismiss: () -> Unit
) {
    var isWebViewSupported by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Назад")
                            }
                        },
                        title = {
                            Column {
                                Text(appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Безопасный контейнер Mini App API", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    if (isWebViewSupported) {
                        AndroidView(
                            factory = { context ->
                                try {
                                    WebView(context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = WebViewClient()
                                        loadUrl(url)
                                    }
                                } catch (e: Exception) {
                                    isWebViewSupported = false
                                    e.printStackTrace()
                                    // Fallback to a plain textview to satisfy factory requirement
                                    android.widget.TextView(context).apply {
                                        text = "Запуск мини-приложения..."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Не удалось запустить WebView",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ваш эмулятор или устройство не имеет установленного\n'Android System WebView'. Вы можете открыть ссылку напрямую в вашем веб-браузере:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Button(
                                onClick = { 
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            ) {
                                Text("Открыть в браузере")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultViewDialog(
    viewModel: PrimeViewModel,
    onDismiss: () -> Unit
) {
    val deletedList by viewModel.deletedMessages.collectAsState()
    val mediaVaultList by viewModel.selfDestructMedia.collectAsState()

    var activeTab by remember { mutableStateOf("recall") } // "recall", "media"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🛡️ Сейф Перехватов") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = if (activeTab == "recall") 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == "recall",
                        onClick = { activeTab = "recall" },
                        text = { Text("Anti-Recall") }
                    )
                    Tab(
                        selected = activeTab == "media",
                        onClick = { activeTab = "media" },
                        text = { Text("MediaSaver") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    if (activeTab == "recall") {
                        if (deletedList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Удаленных сообщений не обнаружено.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        } else {
                            items(deletedList) { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(msg.senderAvatarColor))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(msg.senderName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(msg.messageText, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Время перехвата: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(msg.deletedTimestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 8.sp,
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (mediaVaultList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Сейф одноразовых фото/видео пуст.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        } else {
                            items(mediaVaultList) { med ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (med.fileType == "video") Icons.Default.Videocam else Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(med.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text("Отправитель: " + med.senderName, style = MaterialTheme.typography.bodySmall, fontSize = 9.sp)
                                            Text("Объем: ${med.fileSizeKb} KB | Время: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(med.timestamp)), style = MaterialTheme.typography.bodySmall, fontSize = 8.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        if (activeTab == "recall") viewModel.clearDeletedMessagesHistory() else viewModel.clearMediaVault()
                    }
                ) {
                    Text("Очистить раздел", color = Color.Red.copy(alpha = 0.8f))
                }
                TextButton(onClick = onDismiss) { Text("ОК") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimegramSettingsScreen(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings,
    onBack: () -> Unit,
    onExitApp: () -> Unit
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isCleaningCache by viewModel.isCleaningCache.collectAsState()
    val plugins by viewModel.plugins.collectAsState()
    val downloadStatus by viewModel.pluginDownloadStatus.collectAsState()
    
    var showAddPluginDialog by remember { mutableStateOf(false) }
    var showPluginGuide by remember { mutableStateOf(false) }
    var setApiInput by remember(activeSettings.setApiJson) { mutableStateOf(activeSettings.setApiJson) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = { Text("🛡️ Stealth Конфигуратор", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onExitApp) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Выйти из приложения", tint = Color.Red.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CENTRALIZED SECURITY CONTROLS CARD
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Уровни приватности Cherrygram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        SettingToggleRow(
                            title = "Режим Призрака (Ghost mode)",
                            subtitle = "Скрывает статус 'в сети' и блокирует пометки о прочтении.",
                            checked = activeSettings.ghostModeEnabled,
                            onCheckedChange = { viewModel.toggleGhostMode(it) }
                        )

                        SettingToggleRow(
                            title = "Сейф перехвата Anti-Recall",
                            subtitle = "Автоматически сохранять удаленные собеседником сообщения.",
                            checked = activeSettings.saveDeletedMessages,
                            onCheckedChange = { viewModel.toggleSetting("saveDeletedMessages", it) }
                        )

                        SettingToggleRow(
                            title = "Защита Сейфа MediaSaver",
                            subtitle = "Игнорировать таймер самоуничтожения одноразовых медиа.",
                            checked = activeSettings.saveSelfDestructingMedia,
                            onCheckedChange = { viewModel.toggleSetting("saveSelfDestructingMedia", it) }
                        )
                    }
                }
            }

            // SET API CENTRAL OVERRIDES
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Инструмент Set API (Свои метаданные)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            "Движок Set API позволяет переопределить переменные интерфейса. Отредактируйте JSON-конфиг ниже для мгновенных изменений в приложении:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = setApiInput,
                            onValueChange = { setApiInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("{\"title_override\": \"Cherry Stealth\", \"online_count\": \"99 чатов онлайн\"}") },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = { viewModel.updateSetApiConfig(setApiInput) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Применить Set API")
                        }
                    }
                }
            }

            // NETWORK & GEOLOCATION IP SPOOFING
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Локация & Подмена IP адреса", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        SettingToggleRow(
                            title = "Обфускация соединений (Proxy)",
                            subtitle = "Маршрутизировать все запросы через встроенные или кастомные MTProto / SOCKS5 шлюзы.",
                            checked = activeSettings.proxyEnabled,
                            onCheckedChange = { viewModel.toggleSetting("proxyEnabled", it) }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Выберите подменяемую точку геолокации IP:", style = MaterialTheme.typography.labelMedium)
                        
                        val locationsList = listOf("Zurich, Switzerland", "Tokyo, Japan", "New York, USA", "Moscow, Russia", "Off")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            locationsList.forEach { loc ->
                                FilterChip(
                                    selected = activeSettings.spoofedIpLocation == loc,
                                    onClick = { viewModel.updateSpoofedLocation(loc) },
                                    label = { Text(loc) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Текущий IP: ${activeSettings.spoofedIpAddress} (Инжектирован в заголовок сокета MTProto)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // PLUGINS ENGINE FRAMEWORK
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Фреймворк Плагинов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            IconButton(onClick = { showPluginGuide = !showPluginGuide }) {
                                Icon(
                                    imageVector = if (showPluginGuide) Icons.Default.Info else Icons.Default.Help,
                                    contentDescription = "Инструкция по плагинам",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (showPluginGuide) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💡 Руководство разработчика плагинов",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Cherrygram Scripting Engine поддерживает локальную инжекцию легких плагинов, написанных на JavaScript (ES6+) или Python.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Доступные хуки и сигнатуры запуска:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("• on_message_receive(msg): Срабатывает при приеме. Возвращает объект сообщения.", fontSize = 9.sp)
                                        Text("• on_message_send(msg): Изменяет исходящий текст перед шифрованием.", fontSize = 9.sp)
                                        Text("• bypass_dpi_routing(packet): Сниффинг и обход пакетов TCP/DPI.", fontSize = 9.sp)
                                    }
                                }
                                Text(
                                    text = "Шаблон кода плагина:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "// JS: Перехватчик ключевых слов\nfunction on_message_receive(msg) {\n  if (msg.text.includes('пароль')) {\n    msg.text = '[ЗАШИФРОВАНО 🔒]';\n  }\n  return msg;\n}",
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        plugins.forEach { plugin ->
                            val progress = downloadStatus[plugin.id]
                            val isDownloading = progress != null

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(plugin.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("v${plugin.version}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 9.sp)
                                        }
                                        Text(plugin.description, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Автор: ${plugin.author} | Вес: ${plugin.sizeMb} MB", style = MaterialTheme.typography.bodySmall, fontSize = 8.sp, color = Color.Gray)
                                        if (plugin.scriptCode.isNotBlank()) {
                                            val langIcon = if (plugin.scriptLanguage == "python") "🐍 Python" else "📜 JavaScript"
                                            Text(
                                                text = "Кастомный скрипт ($langIcon):\n${plugin.scriptCode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 9.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Button(
                                        onClick = { viewModel.togglePluginInstall(plugin.id, plugin.isInstalled) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (plugin.isInstalled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                        modifier = Modifier.height(34.dp),
                                        enabled = !isDownloading
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White)
                                        } else {
                                            Text(if (plugin.isInstalled) "Выкл" else "Вкл", fontSize = 11.sp)
                                        }
                                    }
                                }
                                if (isDownloading && progress != null) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddPluginDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Инсталлировать сторонний плагин", fontSize = 12.sp)
                        }
                    }
                }
            }

            // AUTO TRANSLATE SETTINGS
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Автоматический перевод чатов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        SettingToggleRow(
                            title = "Двухсторонний перевод",
                            subtitle = "Переводить входящие и исходящие сообщения.",
                            checked = activeSettings.translatorEnabled,
                            onCheckedChange = { viewModel.toggleSetting("translatorEnabled", it) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Выбор целевого языка автоперевода:", style = MaterialTheme.typography.labelMedium)
                        val langList = listOf("Русский", "English", "Deutsch", "Español", "Türkçe")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            langList.forEach { lang ->
                                FilterChip(
                                    selected = activeSettings.translationTargetLanguage == lang,
                                    onClick = { viewModel.updateTranslationTargetLanguage(lang) },
                                    label = { Text(lang) }
                                )
                            }
                        }
                    }
                }
            }

            // DESIGN THEMING STYLING
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Внешний вид и Оформление", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        val themeOptions = listOf("Cosmic Blue Theme", "Ghost Slate Theme", "Emerald Stealth Theme", "Classic Telegram Theme")
                        themeOptions.forEach { themeName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateTheme(themeName) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(themeName, style = MaterialTheme.typography.bodyMedium)
                                RadioButton(
                                    selected = activeSettings.themeName == themeName,
                                    onClick = { viewModel.updateTheme(themeName) }
                                )
                            }
                        }
                    }
                }
            }

            // UTILITIES AND CLOUD SYNC CARD
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Служебные утилиты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Синхронизация...")
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                Text("Резервное копирование в Cloud (" + (if (activeSettings.lastCloudSyncTime > 0) format.format(Date(activeSettings.lastCloudSyncTime)) else "ни разу") + ")")
                            }
                        }

                        Button(
                            onClick = { viewModel.triggerCacheClean() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCleaningCache,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            if (isCleaningCache) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Очистка кэша...")
                            } else {
                                Icon(Icons.Default.ClearAll, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Очистить кэш клиента (345.5 MB)")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPluginDialog) {
        AddPluginDialog(
            onDismiss = { showAddPluginDialog = false },
            onConfirm = { id, name, desc, author, ver, size, scriptLang, scriptCode ->
                viewModel.addCustomPlugin(id, name, desc, author, ver, size, scriptLang, scriptCode)
                showAddPluginDialog = false
            }
        )
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AddPluginDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Double, String, String) -> Unit
) {
    var pId by remember { mutableStateOf("custom_crypto_filter") }
    var pName by remember { mutableStateOf("Шифратор Текста PRO") }
    var pDesc by remember { mutableStateOf("Перехватывает сообщения и на лету шифрует конфиденциальные данные.") }
    var pAuthor by remember { mutableStateOf("PrimeAnon") }
    var pVer by remember { mutableStateOf("1.0.0") }
    var pSize by remember { mutableStateOf("0.8") }
    var pScriptLanguage by remember { mutableStateOf("javascript") }
    var pScriptCode by remember { 
        mutableStateOf("// JS: Обработчик события\nfunction on_message_receive(msg) {\n  if (msg.text.includes('секрет')) {\n    msg.text = '⚠️ [ДАННЫЕ ЗАШИФРОВАНЫ КЛИЕНТОМ]';\n  }\n  return msg;\n}") 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔌 Установка кастомного плагина") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = pId,
                    onValueChange = { pId = it },
                    label = { Text("Идентификатор плагина (ID)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pName,
                    onValueChange = { pName = it },
                    label = { Text("Название плагина") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pDesc,
                    onValueChange = { pDesc = it },
                    label = { Text("Описание функционала") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pAuthor,
                        onValueChange = { pAuthor = it },
                        label = { Text("Автор") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pVer,
                        onValueChange = { pVer = it },
                        label = { Text("Версия") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = pSize,
                    onValueChange = { pSize = it },
                    label = { Text("Размер (MB)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("Выбор языка скрипта:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("javascript", "python").forEach { lang ->
                        val isSelected = pScriptLanguage == lang
                        Button(
                            onClick = { 
                                pScriptLanguage = lang
                                if (pScriptCode.isBlank() || pScriptCode.startsWith("//") || pScriptCode.startsWith("#")) {
                                    pScriptCode = if (lang == "javascript") {
                                        "// JS: Обработчик события\nfunction on_message_receive(msg) {\n  return msg;\n}"
                                    } else {
                                        "# Python: Обработчик события\ndef on_message_receive(msg):\n    return msg"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (lang == "javascript") "📜 JavaScript" else "🐍 Python")
                        }
                    }
                }
                
                OutlinedTextField(
                    value = pScriptCode,
                    onValueChange = { pScriptCode = it },
                    label = { Text("Исходный код плагина") },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = { Text("Напишите листинг кода...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pId.isNotBlank() && pName.isNotBlank()) {
                        val sizeVal = pSize.toDoubleOrNull() ?: 1.0
                        onConfirm(pId, pName, pDesc, pAuthor, pVer, sizeVal, pScriptLanguage, pScriptCode)
                    }
                }
            ) {
                Text("Установить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
