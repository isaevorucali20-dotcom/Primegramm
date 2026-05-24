package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import com.example.data.DeletedMessage
import com.example.data.PluginEntity
import com.example.data.PrimeSettings
import com.example.data.ProxyServer
import com.example.data.SelfDestructMedia
import com.example.data.AnalyticsLog
import com.example.ui.MockChatMessage
import com.example.ui.PrimeViewModel
import com.example.ui.theme.PrimegramTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimegramDashboard(
    viewModel: PrimeViewModel,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settings.collectAsState()
    val activeSettings = settingsState ?: PrimeSettings()

    // Screen dynamic state
    var currentScreen by remember { mutableStateOf("chats") } // chats or pg_settings

    // Theme integration using Settings state
    PrimegramTheme(themeName = activeSettings.themeName) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            AnimatedVisibility(
                visible = currentScreen == "chats",
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut()
            ) {
                ChatsScreen(
                    viewModel = viewModel,
                    activeSettings = activeSettings,
                    onOpenPGSettings = { currentScreen = "pg_settings" }
                )
            }

            AnimatedVisibility(
                visible = currentScreen == "pg_settings",
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                PrimegramSettingsScreen(
                    viewModel = viewModel,
                    activeSettings = activeSettings,
                    onBack = { currentScreen = "chats" }
                )
            }

            // Toast Alert Overlay
            val toastMsg by viewModel.toastMessage.collectAsState()
            toastMsg?.let { msg ->
                ToastNotification(message = msg, onDismiss = { viewModel.clearToast() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings,
    onOpenPGSettings: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeChatId by viewModel.activeChatId.collectAsState()
    val activeMessagesMap by viewModel.activeChatMessages.collectAsState()
    val draft by viewModel.chatDraft.collectAsState()

    val currentMessages = activeMessagesMap[activeChatId] ?: emptyList()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenuContent(
                activeSettings = activeSettings,
                activeChatId = activeChatId,
                onOpenPGSettings = {
                    scope.launch { drawerState.close() }
                    onOpenPGSettings()
                },
                onSelectChat = { id ->
                    scope.launch { drawerState.close() }
                    viewModel.selectChat(id)
                },
                viewModel = viewModel
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Главное меню")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = viewModel.getChatPartnerName(activeChatId),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeSettings.ghostModeEnabled) Color(0xFF78909C) else Color(0xFF4CAF50)
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activeSettings.ghostModeEnabled) "Режим призрака (скрыт)" else "в сети",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.simulateDeletedMessageTrigger() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Симулировать удаленное сообщение",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.simulateOneTimeMediaTrigger() }) {
                            Icon(
                                Icons.Default.OfflineBolt,
                                contentDescription = "Симулировать одноразовое медиа",
                                tint = MaterialTheme.colorScheme.secondary
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
                // Diagonal background grid to look highly technical and like Cherrygram/Primegram Telegram client
                BackgroundGrid()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    reverseLayout = false,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🔒 Сквозное шифрование Primegram: ${activeSettings.encryptionLevel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (activeSettings.ghostModeEnabled) {
                                        Text(
                                            text = "👻 Активен Режим призрака. Собеседники не видят, что вы читаете чаты.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(currentMessages) { message ->
                        ChatMessageItem(message = message, activeSettings = activeSettings)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerMenuContent(
    activeSettings: PrimeSettings,
    activeChatId: Int,
    onOpenPGSettings: () -> Unit,
    onSelectChat: (Int) -> Unit,
    viewModel: PrimeViewModel
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .width(290.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                RoundedCornerShape(0.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Profile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
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
                        // Custom avatar with star
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Prime User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // Premium icon inside main menu as requested list item 10!
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Премиум",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).testTag("premium_menu_indicator")
                                )
                            }
                            Text(
                                text = "@primegram_god",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status details
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Режим невидимки:", style = MaterialTheme.typography.bodySmall)
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
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            // Sidebar Actions List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                item {
                    Text(
                        text = "ЧАТЫ ДЛЯ ТЕСТИРОВАНИЯ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    DrawerChatItem(
                        title = "Арслан Cherrygram",
                        subtitle = "Привет, зацени фичи...",
                        isActive = activeChatId == 1,
                        onClick = { onSelectChat(1) }
                    )
                }

                item {
                    DrawerChatItem(
                        title = "Разработчик Primegram",
                        subtitle = "Тестируем обфускацию...",
                        isActive = activeChatId == 2,
                        onClick = { onSelectChat(2) }
                    )
                }

                item {
                    DrawerChatItem(
                        title = "Мама",
                        subtitle = "Сынок, ты покушал? ❤️",
                        isActive = activeChatId == 3,
                        onClick = { onSelectChat(3) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "КЛИЕНТСКИЕ НАСТРОЙКИ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }

                item {
                    // Primegram Settings button inside main menu as requested item 10!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            .clickable { onOpenPGSettings() }
                            .padding(14.dp)
                            .testTag("prime_settings_entry"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Настройки Primegram",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Облачные моды и приватность",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Footer info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Праймграм v2.4 (Cherry Client Mod)\nСборка со сверхскоростью ⚡",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DrawerChatItem(
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: MockChatMessage,
    activeSettings: PrimeSettings
) {
    val alignment = if (message.isMe) Alignment.End else Alignment.Start
    val containerBg = if (message.isMe) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

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
                    if (message.isDeletedInterpreted) Color.Red.copy(alpha = 0.5f) else Color.Transparent,
                    RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                if (!message.isMe) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (message.isDeletedInterpreted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GppBad,
                            contentDescription = "Deleted Message Blocked",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Удалено собеседником (Перехвачено):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = TextDecoration.LineThrough
                    )
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isTranslated) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = "Translated",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
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
        modifier = Modifier.fillMaxWidth().testTag("chat_input_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        "Написать сообщение...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("chat_message_input"),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("chat_send_btn")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun CherrygramCoreBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("cherrygram_core_banner")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Ядро Cherrygram v1.4.2",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Upstream База & Исходный код",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Мы создаем наш клиент на исходном коде Cherrygram. Все скрытые функции сообщений, обход блокировок, переводчик и режимы конфиденциальности интегрированы напрямую из официального репозитория.",
                fontSize = 11.sp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GitHub репозиторий:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    "arsLan4k1390/Cherrygram",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimegramSettingsScreen(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("privacy") } // privacy, recovery, proxy, customization, plugins, stats

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад к чатам")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Настройки Primegram",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal scrolling setting tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabItem(
                    title = "Призрак👻",
                    isSelected = selectedTab == "privacy",
                    onClick = { selectedTab = "privacy" }
                )
                TabItem(
                    title = "Удаленное🗑️",
                    isSelected = selectedTab == "recovery",
                    onClick = { selectedTab = "recovery" }
                )
                TabItem(
                    title = "Прокси🔌",
                    isSelected = selectedTab == "proxy",
                    onClick = { selectedTab = "proxy" }
                )
                TabItem(
                    title = "Темы🎨",
                    isSelected = selectedTab == "customization",
                    onClick = { selectedTab = "customization" }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabItem(
                    title = "Моды & Плагины🔌",
                    isSelected = selectedTab == "plugins",
                    onClick = { selectedTab = "plugins" }
                )
                TabItem(
                    title = "Скорость & Аналитика⚡",
                    isSelected = selectedTab == "stats",
                    onClick = { selectedTab = "stats" }
                )
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                CherrygramCoreBanner()
                Spacer(modifier = Modifier.height(8.dp))
                
                when (selectedTab) {
                    "privacy" -> PrivacyTabContent(viewModel = viewModel, activeSettings = activeSettings)
                    "recovery" -> RecoveryTabContent(viewModel = viewModel, activeSettings = activeSettings)
                    "proxy" -> ProxyTabContent(viewModel = viewModel, activeSettings = activeSettings)
                    "customization" -> CustomizationTabContent(viewModel = viewModel, activeSettings = activeSettings)
                    "plugins" -> PluginsTabContent(viewModel = viewModel, activeSettings = activeSettings)
                    "stats" -> StatsTabContent(viewModel = viewModel, activeSettings = activeSettings)
                }
            }
        }
    }
}

@Composable
fun TabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val indicatorColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

// 1. PRIVACY TABS
@Composable
fun PrivacyTabContent(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "👻 РЕЖИМ ПРИЗРАКА (Stealth)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Абсолютная невидимость. Скрывает статус 'в сети', отчеты о прочтении, статус печатания во всех чатах.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = activeSettings.ghostModeEnabled,
                            onCheckedChange = { viewModel.toggleGhostMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "ДЕТАЛИЗИРОВАННЫЕ НАСТРОЙКИ АНОНИМНОСТИ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            PrivacyToggleItem(
                title = "Скрывать онлайн",
                desc = "Ваша активность в сети скрыта глобально.",
                checked = activeSettings.hideOnlineStatusUniversal,
                onCheckedChange = { viewModel.toggleSetting("hideOnlineStatusUniversal", it) }
            )
        }

        item {
            PrivacyToggleItem(
                title = "Анонимный просмотр историй",
                desc = "Авторы историй не увидят, что вы просмотрели кружки.",
                checked = activeSettings.anonymousStoriesViewer,
                onCheckedChange = { viewModel.toggleSetting("anonymousStoriesViewer", it) }
            )
        }

        item {
            PrivacyToggleItem(
                title = "Скрывать статус печатания",
                desc = "Собеседник не увидит плашку 'печатает...' при наборе сообщений.",
                checked = activeSettings.hideTypingStatus,
                onCheckedChange = { viewModel.toggleSetting("hideTypingStatus", it) }
            )
        }

        item {
            PrivacyToggleItem(
                title = "Скрывать статус прочтения",
                desc = "Отправляйте отметки о прочтении только при ручном открытии.",
                checked = activeSettings.hideReadStatus,
                onCheckedChange = { viewModel.toggleSetting("hideReadStatus", it) }
            )
        }
    }
}

@Composable
fun PrivacyToggleItem(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// 2. RECOVERY TAB CONTENT
@Composable
fun RecoveryTabContent(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings
) {
    val messages by viewModel.deletedMessages.collectAsState()
    val mediaFiles by viewModel.selfDestructMedia.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🗑️ Перехват удаленных сообщений", fontWeight = FontWeight.Bold)
                            Text("Сохраняет удаленные куски диалогов во всех чатах во внутренний журнал.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = activeSettings.saveDeletedMessages,
                            onCheckedChange = { viewModel.toggleSetting("saveDeletedMessages", it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🖼️ Спасение одноразовых медиа", fontWeight = FontWeight.Bold)
                            Text("Сохраняет фото и видео 'посмотреть один раз' до того, как они сгорают.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = activeSettings.saveSelfDestructingMedia,
                            onCheckedChange = { viewModel.toggleSetting("saveSelfDestructingMedia", it) }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ЖУРНАЛ УДАЛЕННЫХ СООБЩЕНИЙ (${messages.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (messages.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearDeletedMessagesHistory() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Очистить", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (messages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Удаленных сообщений пока нет. Нажмите 🗑️ сверху в чатах для симуляции!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        } else {
            items(messages) { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(msg.senderAvatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(msg.senderName.take(1), color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg.senderName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.deletedTimestamp)),
                                style = MaterialTheme.typography.bodySmall, color = Color.Red
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(msg.messageText, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "СЕЙФ ОДНОРАЗОВЫХ ФАЙЛОВ (${mediaFiles.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (mediaFiles.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearMediaVault() }) {
                        Text("Очистить сейф", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (mediaFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Сейф пуст. Нажмите значок молнии ⚡ в чатах для перехвата одноразовых фото/видео!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        } else {
            items(mediaFiles) { media ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (media.fileType == "video") Icons.Outlined.Videocam else Icons.Outlined.Photo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(media.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("От: ${media.senderName} • ${media.fileSizeKb} KB", fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { viewModel.showToast("💾 Файл сохранен в локальную галерею за пределами кэша!") },
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Экспорт", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// 3. PROXY TAB CONTENT
@Composable
fun ProxyTabContent(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings
) {
    val proxies by viewModel.proxyServers.collectAsState()
    var proxyTitle by remember { mutableStateOf("") }
    var proxyHost by remember { mutableStateOf("") }
    var proxyPort by remember { mutableStateOf("1080") }
    var proxySecret by remember { mutableStateOf("") }
    var proxyType by remember { mutableStateOf("SOCKS5") } // SOCKS5 or MTProto

    var activeDialog by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔗 Встроенный прокси-сервер", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Направляет трафик через кастомные SOCKS5/MTProto цепочки для конфиденциальности.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = activeSettings.proxyEnabled,
                            onCheckedChange = { viewModel.toggleSetting("proxyEnabled", it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Подмена IP-адреса: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = activeSettings.spoofedIpLocation,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Location switchers
        item {
            Text("ВЫБОР ПОДМЕННОГО ГЕО-IP:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Zurich, Switzerland", "Tokyo, Japan", "New York, USA", "Off").forEach { loc ->
                    val isLocActive = activeSettings.spoofedIpLocation == loc
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLocActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.updateSpoofedLocation(loc) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loc.split(",").first(),
                            fontSize = 11.sp,
                            color = if (isLocActive) Color.Black else MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "КОНФИГУРАЦИЯ СЕРВЕРОВ ПРОКСИ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { activeDialog = !activeDialog }) {
                    Icon(if (activeDialog) Icons.Default.Close else Icons.Default.Add, contentDescription = "Добавить прокси", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (activeDialog) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Добавить прокси", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { proxyType = "SOCKS5" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (proxyType == "SOCKS5") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                            ) { Text("SOCKS5") }
                            Button(
                                onClick = { proxyType = "MTProto" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (proxyType == "MTProto") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                            ) { Text("MTProto") }
                        }

                        OutlinedTextField(
                            value = proxyTitle,
                            onValueChange = { proxyTitle = it },
                            label = { Text("Название (например, Мой приватный)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = proxyHost,
                                onValueChange = { proxyHost = it },
                                label = { Text("Хост/IP") },
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = proxyPort,
                                onValueChange = { proxyPort = it },
                                label = { Text("Порт") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (proxyType == "MTProto") {
                            OutlinedTextField(
                                value = proxySecret,
                                onValueChange = { proxySecret = it },
                                label = { Text("Секретный ключ (16-ричный)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                if (proxyTitle.isNotBlank() && proxyHost.isNotBlank()) {
                                    viewModel.addProxyServer(
                                        title = proxyTitle,
                                        host = proxyHost,
                                        port = proxyPort.toIntOrNull() ?: 1080,
                                        type = proxyType,
                                        secret = if (proxyType == "MTProto") proxySecret else null
                                    )
                                    proxyTitle = ""
                                    proxyHost = ""
                                    activeDialog = false
                                } else {
                                    viewModel.showToast("Пожалуйста, заполните поля хоста и названия")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сохранить прокси")
                        }
                    }
                }
            }
        }

        items(proxies) { proxy ->
            val isActive = activeSettings.currentProxyId == proxy.id && activeSettings.proxyEnabled
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (proxy.type == "MTProto") Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(proxy.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${proxy.host}:${proxy.port} • ${proxy.type}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${proxy.pingMs} ms",
                        color = if (proxy.pingMs < 60) Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (proxy.isCustom) {
                        IconButton(onClick = { viewModel.deleteProxy(proxy.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "delete", tint = Color.Red.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// 4. CUSTOMIZATION TAB CONTENT
@Composable
fun CustomizationTabContent(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings
) {
    val themes = listOf("Elegant Dark", "Midnight Cherry", "AMOLED Gold", "Mint Ghost", "Sapphire Prime", "Classic Telegram")
    val languages = listOf("Русский", "English", "Español", "Deutsch")

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("КАСТОМИЗАЦИЯ ИНТЕРФЕЙСА (Темы)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        items(themes) { theme ->
            val isCurrent = activeSettings.themeName == theme
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.updateTheme(theme) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(2.dp, if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (theme) {
                                    "Elegant Dark" -> Color(0xFFD0BCFF)
                                    "AMOLED Gold" -> Color(0xFFFFD700)
                                    "Mint Ghost" -> Color(0xFF00FFCC)
                                    "Sapphire Prime" -> Color(0xFF1E88E5)
                                    "Classic Telegram" -> Color(0xFF0088CC)
                                    else -> Color(0xFFE53935)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(theme, fontWeight = FontWeight.Bold)
                        Text(
                            when (theme) {
                                "Elegant Dark" -> "Элегантный темный дизайн с лавандовыми акцентными тонами"
                                "AMOLED Gold" -> "Идеально черный для OLED-экранов с золотыми деталями"
                                "Mint Ghost" -> "Информационный стиль с неоново-мятной прицельностью"
                                "Sapphire Prime" -> "Премиальный кобальтово-синий оттенок"
                                "Classic Telegram" -> "Родной визуальный стиль мессенджера"
                                else -> "Сигнальный темно-вишневый дизайн Cherrygram"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    if (isCurrent) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(6.dp))
            Text("ВСТРОЕННЫЙ МГНОВЕННЫЙ ПЕРЕВОДЧИК", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Включить переводчик", fontWeight = FontWeight.Bold)
                            Text("Интегрирует плавающий перевод по нажатию над каждым пузырем чата.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = activeSettings.translatorEnabled,
                            onCheckedChange = { viewModel.toggleSetting("translatorEnabled", it) }
                        )
                    }
                }
            }
        }

        item {
            Text("ЦЕЛЕВОЙ ЯЗЫК НА КОТОРЫЙ ПЕРЕВОДИТЬ:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languages.forEach { lang ->
                    val isLangActive = activeSettings.translationTargetLanguage == lang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLangActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.updateTranslationTargetLanguage(lang) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang,
                            fontSize = 12.sp,
                            color = if (isLangActive) Color.Black else MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 5. PLUGINS TAB CONTENT
@Composable
fun PluginsTabContent(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings
) {
    val plugins by viewModel.plugins.collectAsState()
    val downloadsProgress by viewModel.pluginDownloadStatus.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Сторонние Плагины Sandbox", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Плагины позволяют расширять API клиента, менять стили звонков или автоматизировать отправку.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        items(plugins) { plugin ->
            val progress = downloadsProgress[plugin.id]
            val isDownloading = progress != null

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(plugin.name, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("v${plugin.version}", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("Разработчик: ${plugin.author} • ${plugin.sizeMb} MB", fontSize = 11.sp, color = Color.Gray)
                        }

                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { progress ?: 0f },
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Button(
                                onClick = { viewModel.togglePluginInstall(plugin.id, plugin.isInstalled) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (plugin.isInstalled) Color.Gray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (plugin.isInstalled) MaterialTheme.colorScheme.onBackground else Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (plugin.isInstalled) Icons.Default.DownloadDone else Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (plugin.isInstalled) "Выкл" else "Установить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(plugin.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress ?: 0f },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// 6. STATS & ANALYTICS TAB CONTENT
@Composable
fun StatsTabContent(
    viewModel: PrimeViewModel,
    activeSettings: PrimeSettings
) {
    val logs by viewModel.analyticsLogs.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isCleaning by viewModel.isCleaningCache.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🛡️ ГЛУБОКАЯ АНАЛИТИКА ТРАФИКА И ОПТИМИЗАЦИЯ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Встроенные алгоритмы шифруют сетевые туннели и фильтруют спам для максимальной безопасности на любых нагрузках.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Simulating speed graphics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ГРАФИК СВЕРХСКОРОСТИ И НАГРУЗКИ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Simple custom drawing graph illustrating peak rates
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        val strokeColor = MaterialTheme.colorScheme.primary

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val points = listOf(
                                Offset(0f, h * 0.9f),
                                Offset(w * 0.15f, h * 0.8f),
                                Offset(w * 0.3f, h * 0.3f), // Peaks!
                                Offset(w * 0.45f, h * 0.5f),
                                Offset(w * 0.6f, h * 0.1f), // Max stress load
                                Offset(w * 0.75f, h * 0.2f),
                                Offset(w * 0.9f, h * 0.4f),
                                Offset(w, h * 0.15f)
                            )

                            // Draw baseline grid
                            drawLine(Color.Gray.copy(alpha = 0.1f), Offset(0f, h*0.5f), Offset(w, h*0.5f), strokeWidth = 1f)
                            drawLine(Color.Gray.copy(alpha = 0.1f), Offset(0f, h*0.1f), Offset(w, h*0.1f), strokeWidth = 1f)

                            // Drar line paths
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = strokeColor,
                                    start = points[i],
                                    end = points[i+1],
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Скорость соед: 100 Gbit/S", fontSize = 11.sp, color = Color.Gray)
                        Text("Нагрузка CPU: Оптимизировано ⚡", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("ИНСТРУМЕНТЫ ОПТИМИЗАЦИИ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.triggerCloudSync() },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Облачная синхр.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.triggerCacheClean() },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    if (isCleaning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Очистить кэш", fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔒 Уровень шифрования: ", fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(activeSettings.encryptionLevel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("AES-256", "ChaCha20", "RSA-4096").forEach { crypt ->
                            val isActive = activeSettings.encryptionLevel == crypt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                    .border(1.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .clickable { viewModel.updateEncryptionLevel(crypt) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(crypt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isActive) Color.Black else MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("ИСТОРИЯ ОПТИМИЗАЦИОННЫХ ПОТОКОВ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        if (logs.isEmpty()) {
            item {
                Text("Действий пока нет. Пожалуйста, измените настройки выше.", fontSize = 11.sp, color = Color.Gray)
            }
        } else {
            items(logs) { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(log.label, fontSize = 13.sp)
                    }
                    Text(
                        String.format(Locale.getDefault(), "%.1f", log.value),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Visual layout grid lines backgrounds
@Composable
fun BackgroundGrid() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val interval = 60.dp.toPx()
        
        var x = 0f
        while (x < w) {
            drawLine(
                Color.LightGray.copy(alpha = 0.03f),
                Offset(x, 0f),
                Offset(x, h),
                strokeWidth = 1f
            )
            x += interval
        }

        var y = 0f
        while (y < h) {
            drawLine(
                Color.LightGray.copy(alpha = 0.03f),
                Offset(0f, y),
                Offset(w, y),
                strokeWidth = 1f
            )
            y += interval
        }
    }
}

// Dynamic Animated Toast Alert Components
@Composable
fun ToastNotification(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Праймграм Уведомление",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
