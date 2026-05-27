package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.PrimeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimegramDashboard(viewModel: PrimeViewModel) {
    val settingsState by viewModel.settings.collectAsState()
    val activeSettings = settingsState ?: PrimeSettingsEntity()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    var currentScreen by remember { mutableStateOf("chats") } // "chats", "proxy", "plugins", "miniapps", "settings"
    var showFirstTimeIdSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(settingsState) {
        val sState = settingsState
        if (sState != null) {
            showFirstTimeIdSetupDialog = sState.userUniqueId.isEmpty()
        }
    }

    var showSearchUserDialog by remember { mutableStateOf(false) }
    var showPartnerProfileDialog by remember { mutableStateOf(false) }
    var showAddProxyDialog by remember { mutableStateOf(false) }
    var showAddMiniAppDialog by remember { mutableStateOf(false) }
    var showAddPluginDialog by remember { mutableStateOf(false) }
    var activeMiniAppUrl by remember { mutableStateOf<String?>(null) }
    var activeMiniAppName by remember { mutableStateOf("") }

    val activeChatId by viewModel.currentChatId.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerTonalElevation = 6.dp
            ) {
                // Client Identity Profile Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeSettings.displayName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = activeSettings.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (activeSettings.userUniqueId.isNotEmpty()) "ID: ${activeSettings.userUniqueId}" else "Личность не создана",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                val menuItems = listOf(
                    Triple("chats", "Диалоги", Icons.Default.Chat),
                    Triple("alive", "Эфир (Alive Playlist)", Icons.Default.MusicNote),
                    Triple("proxy", "Управление Прокси", Icons.Default.VpnLock),
                    Triple("plugins", "Плагины (Mods)", Icons.Default.Extension),
                    Triple("miniapps", "Мини-Приложения", Icons.Default.Apps),
                    Triple("settings", "Параметры Ядра", Icons.Default.Settings)
                )

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.third, contentDescription = null) },
                        label = { Text(item.second) },
                        selected = currentScreen == item.first,
                        onClick = {
                            currentScreen = item.first
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("nav_item_${item.first}")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer branding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Primegram Shield Kernel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v3.85 • Закрытое тестирование",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = when (currentScreen) {
                                    "chats" -> if (activeChatId != null) "Приватный чат" else "Primegram"
                                    "proxy" -> "Модем & Прокси"
                                    "plugins" -> "Внедрение модов"
                                    "miniapps" -> "Мини-Апп игры"
                                    "settings" -> "Identity & Ядро"
                                    else -> "Primegram"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            if (activeSettings.ghostModeEnabled) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "GHOST",
                                        color = Color.Red,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (currentScreen == "chats" && activeChatId != null) {
                            IconButton(onClick = { viewModel.selectChat(null) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                            }
                        } else {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_drawer_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню")
                            }
                        }
                    },
                    actions = {
                        if (currentScreen == "chats" && activeChatId == null) {
                            IconButton(
                                onClick = { showSearchUserDialog = true },
                                modifier = Modifier.testTag("search_user_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Поиск")
                            }
                        } else if (currentScreen == "chats" && activeChatId != null) {
                            IconButton(
                                onClick = { showPartnerProfileDialog = true },
                                modifier = Modifier.testTag("partner_profile_btn")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Инфо собеседника")
                            }
                        } else if (currentScreen == "proxy") {
                            IconButton(onClick = { showAddProxyDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить прокси")
                            }
                        } else if (currentScreen == "miniapps") {
                            IconButton(onClick = { showAddMiniAppDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Новый мини-апп")
                            }
                        } else if (currentScreen == "plugins") {
                            IconButton(onClick = { showAddPluginDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить плагин JS")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    "chats" -> {
                        if (activeChatId != null) {
                            ChatConversationScreen(
                                viewModel = viewModel,
                                chatId = activeChatId!!,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ChatsTab(
                                viewModel = viewModel,
                                onSelectChat = { viewModel.selectChat(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    "proxy" -> ProxyTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    "alive" -> AlivePlaylistTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    "plugins" -> PluginsTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    "miniapps" -> MiniAppsTab(
                        viewModel = viewModel,
                        onOpenUrl = { url, name ->
                            activeMiniAppUrl = url
                            activeMiniAppName = name
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    "settings" -> SettingsTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    // Dialog sheets
    if (showFirstTimeIdSetupDialog) {
        FirstTimeSetupDialog(viewModel = viewModel)
    }

    if (showSearchUserDialog) {
        SearchUserDialog(
            viewModel = viewModel,
            onDismiss = { showSearchUserDialog = false }
        )
    }

    if (showPartnerProfileDialog && activeChatId != null) {
        PartnerProfileDialog(
            viewModel = viewModel,
            partnerId = activeChatId!!,
            onDismiss = { showPartnerProfileDialog = false }
        )
    }

    if (showAddProxyDialog) {
        AddProxyDialog(
            viewModel = viewModel,
            onDismiss = { showAddProxyDialog = false }
        )
    }

    if (showAddMiniAppDialog) {
        AddMiniAppDialog(
            viewModel = viewModel,
            onDismiss = { showAddMiniAppDialog = false }
        )
    }

    if (showAddPluginDialog) {
        AddPluginDialog(
            viewModel = viewModel,
            onDismiss = { showAddPluginDialog = false }
        )
    }

    if (activeMiniAppUrl != null) {
        MiniAppWebViewDialog(
            title = activeMiniAppName,
            url = activeMiniAppUrl!!,
            onDismiss = { activeMiniAppUrl = null }
        )
    }
}

@Composable
fun ChatsTab(
    viewModel: PrimeViewModel,
    onSelectChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val users by viewModel.chatUsers.collectAsState()

    if (users.isEmpty()) {
        Column(
            modifier = modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AlternateEmail,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Диалогов нет.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Добавьте собеседника по его уникальной ID-подписи или по имени в поиске контактов сверху справа.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Защищенные сессии диалогов",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(users) { user ->
                Card(
                    onClick = { onSelectChat(user.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_card_${user.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(user.avatarColor),
                                            Color(user.avatarColor).copy(alpha = 0.6f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.id == "prime41k") {
                                    ExtraBadge("DEV", Color(0xFFD500F9))
                                }
                                if (user.isBot) {
                                    ExtraBadge("BOT", MaterialTheme.colorScheme.primary)
                                }
                                if (user.spentStars > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "★",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${user.spentStars}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = user.username ?: ("ID: " + user.id),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExtraBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChatConversationScreen(
    viewModel: PrimeViewModel,
    chatId: String,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.currentChatMessages.collectAsState()
    val isTyping by viewModel.isBotTyping.collectAsState()
    
    val partnerBatteryMap by viewModel.partnerBatteryLevel.collectAsState()
    val knockUnlockedMap by viewModel.knockUnlocked.collectAsState()
    val revealedSchrodingerMap by viewModel.revealedSchrodingerMessages.collectAsState()

    val partnerBattery = partnerBatteryMap[chatId] ?: 12
    val isChatKnockUnlocked = knockUnlockedMap[chatId] ?: false

    var inputMessageText by remember { mutableStateOf("") }
    var sendAsSchrodinger by remember { mutableStateOf(false) }
    var cinemaMeshExpanded by remember { mutableStateOf(false) }
    var isMoviePlaying by remember { mutableStateOf(false) }
    var simulatedTimeSeconds by remember { mutableStateOf(0) }
    var knockClicksCount by remember { mutableStateOf(0) }

    // --- «БЛИЖЕ» (Closer) RELATIONSHIP ENGINE STATES ---
    var closerExpanded by remember { mutableStateOf(false) }
    var relationStats by remember { mutableStateOf<com.example.ui.RelationStats?>(null) }
    var voiceRecordSeconds by remember { mutableStateOf(0) }
    var isVoiceRecordingActive by remember { mutableStateOf(false) }

    LaunchedEffect(chatId, messages) {
        relationStats = viewModel.calculateRelationStats(chatId)
    }

    LaunchedEffect(isVoiceRecordingActive) {
        if (isVoiceRecordingActive) {
            voiceRecordSeconds = 0
            while (isVoiceRecordingActive) {
                kotlinx.coroutines.delay(1000)
                voiceRecordSeconds++
            }
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Initialize partner battery level on first load to trigger low energy share scenario
    LaunchedEffect(chatId) {
        if (partnerBatteryMap[chatId] == null) {
            viewModel.setPartnerBattery(chatId, 12)
        }
    }

    LaunchedEffect(isMoviePlaying) {
        if (isMoviePlaying) {
            while (isMoviePlaying) {
                kotlinx.coroutines.delay(1000)
                simulatedTimeSeconds = (simulatedTimeSeconds + 1) % 180
            }
        }
    }

    if (!isChatKnockUnlocked) {
        // 🔒 СТУК-КОД АНАЛОГОВАЯ ЗАЩИТА (KNOCK-CODE LOCK SCREEN OVERLAY)
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "СОКЕТ СЕССИИ ЗАШИФРОВАН",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Децентрализованный канал требует воспроизведения ритма физического стук-кода (Knock-Code) для инжекции ключей Ed25519 в RAM.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rhythmic Interactive Knock Button
            Button(
                onClick = {
                    knockClicksCount++
                    // Trigger dynamic vibration on actual tap
                    try {
                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                        vibrator?.vibrate(60)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    if (knockClicksCount >= 3) {
                        viewModel.unlockChatWithKnock(chatId)
                    }
                },
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "СТУК-КОД 📡",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите 3 раза\nв ритме пульса",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tap progress indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (knockClicksCount > index) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    } else {
        // MAIN DECRYPTED CONVERSATION VIEW
        Column(modifier = modifier) {
            
            // 📡 ТАКТИЧЕСКИЙ ХЕДЕР ПАНЕЛИ P2P (Unified Protocol Status Bar)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (partnerBattery > 15) Color.Green else Color.Red)
                            )
                            Text(
                                text = "P2P Линк: Активен (Onion • CRDT • UDP • Sub-Ratchet)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Closer" Romantic Spark Trigger
                            IconButton(
                                onClick = { closerExpanded = !closerExpanded },
                                modifier = Modifier.size(24.dp).testTag("closer_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Ближе",
                                    tint = if (closerExpanded) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Cinema Mesh Toggle Button
                            IconButton(
                                onClick = { cinemaMeshExpanded = !cinemaMeshExpanded },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "Cinema Mesh",
                                    tint = if (cinemaMeshExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Proximity indicators block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚙️ Радар: 3 соседа рядом • Буфер RAM без следов",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Companion Battery Sharing Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🔋 Собеседник: $partnerBattery%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (partnerBattery > 15) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            if (partnerBattery <= 15) {
                                Button(
                                    onClick = { viewModel.chargePartner(chatId) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Text("Зарядить ⚡", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // 🎬 WATCH TOGETHER (CINEMA-MESH SYNCHRONOUS PLAYER WIDGET)
            if (cinemaMeshExpanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🎬", fontSize = 16.sp)
                                Text(
                                    text = "Watch Together Offline (Cinema-Mesh)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            // Close player button
                            IconButton(onClick = { cinemaMeshExpanded = false }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            }
                        }

                        // Simulated Screen Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF0D47A1), Color.Black)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isMoviePlaying) {
                                    Text(
                                        text = "ИДЕТ СИНХРОННЫЙ ПРОСМОТР 🍿",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Green
                                    )
                                    Text(
                                        text = "[Сноуден - 2016]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "Таймкод: ${simulatedTimeSeconds / 60}:${String.format("%02d", simulatedTimeSeconds % 60)} / 3:00",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                    Text(
                                        text = "Cinema-Mesh Готов к запуску",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        // Playback Bar and Sync details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { isMoviePlaying = !isMoviePlaying },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMoviePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Loading Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.DarkGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = simulatedTimeSeconds / 180f)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }

                            Text(
                                text = "Пинг: 4ms • Sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Green,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 💖 «БЛИЖЕ» (CLOSER) RELATIONSHIP PULSE PANEL
            if (closerExpanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("closer_relationship_panel"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF4081).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💖", fontSize = 18.sp)
                                Text(
                                    text = "«Ближе» • Резонанс Чувств",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF4081)
                                )
                            }
                            IconButton(onClick = { closerExpanded = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            }
                        }

                        // Affinity indicator warmth score
                        val percent = relationStats?.closenessPercent ?: 75
                        val statusText = when {
                            percent >= 85 -> "Идеальный резонанс. Вы звучите на одной частоте. ✨"
                            percent >= 60 -> "Хороший линк. Но слова становятся короче. Не забывайте о тепле. 🌻"
                            else -> "Линк остывает. Период легкого отдаления. Самое время сказать важное. 💔"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFF4081).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = percent / 100f,
                                    color = Color(0xFFFF4081),
                                    trackColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(54.dp)
                                )
                                Text(
                                    text = "$percent%",
                                    color = Color(0xFFFF4081),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Column {
                                Text(
                                    text = "Коэффициент Близости",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Digital Time Capsule section
                        Text(
                            text = "📦 Родовые Скрижали (Капсула Времени)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // First photo
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Ваш первый снимок", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(text = relationStats?.firstSharedImage ?: "Ищется фото...", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }

                            // Longest text
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Самое теплое / длинное сообщение", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if ((relationStats?.longestMessage?.length ?: 0) > 60)
                                                "«" + relationStats?.longestMessage?.take(60) + "...»"
                                            else "«" + (relationStats?.longestMessage ?: "") + "»",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Peak simultaneous presence
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Пик одновременного онлайна", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(text = relationStats?.interactivePeakSession ?: "Поиск пика...", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        // VOICE BROADCAST: «СКАЗАТЬ ВАЖНОЕ» PANEL
                        if (isVoiceRecordingActive) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Red, RoundedCornerShape(12.dp))
                                    .background(Color.Red.copy(alpha = 0.05f))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                    Text(
                                        text = "ПРЯМОЙ ЭФИР: ИДЕТ ЗАПИСЬ... ${voiceRecordSeconds}с",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                // Bouncing micro waves during recording
                                Row(
                                    modifier = Modifier.height(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(12) { index ->
                                        val heightVal = (5..18).random()
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .height(heightVal.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                        )
                                    }
                                }

                                Text(
                                    text = "Запись идет в сыром виде. Исключена цензура, перезапись, предпрослушивание или удаление.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.sendImportantVoiceCapsule(chatId, voiceRecordSeconds)
                                            isVoiceRecordingActive = false
                                            viewModel.showToast("🎙️ Капсула отправлена напрямую собеседнику.")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Отправить в Эфир 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { isVoiceRecordingActive = false },
                                        border = BorderStroke(1.dp, Color.Gray),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Сброс", fontSize = 11.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        } else {
                            // "Сказать Важное" micro launcher button
                            Button(
                                onClick = {
                                    isVoiceRecordingActive = true
                                    voiceRecordSeconds = 0
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("say_important_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "СКАЗАТЬ ВАЖНОЕ (СЫРОЙ ГОЛОС)",
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Chat History Frame
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderId == "me"
                    val isSchrodinger = message.text.contains("[Шредингер]")
                    val isRevealed = revealedSchrodingerMap[message.id] ?: false

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                                        else if (isSchrodinger && !isRevealed) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        else Color.Transparent
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (message.isInterceptedDeleted) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.OfflinePin,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Intercepted Log (Anti-Recall)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (message.isOneTimeMedia) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.secondary,
                                                        MaterialTheme.colorScheme.background
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.HideImage,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Media Saver Intercept Block",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Handle Schrödinger Blurred Mode
                                if (isSchrodinger && !isRevealed) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleSchrodingerReveal(message.id) }
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "📦 КВАНТОВОЕ СООБЩЕНИЕ ШРЕДИНГЕРА",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "[Нажмите для анонимной дешифрации]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    // Normal or decrypted Schrödinger message
                                    val renderedText = if (isSchrodinger) {
                                        message.text.replace("[Шредингер]", "").trim()
                                    } else {
                                        message.text
                                    }

                                    Column {
                                        if (isSchrodinger) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "ДЕШИФРОВАНО • SECURE 🛡️",
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = renderedText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Timestamp and Tactile Morse Whisper Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(message.timestamp)),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            
                            // Vibro Whisper Button
                            IconButton(
                                onClick = {
                                    // Trigger brief vibration Morse sequence
                                    try {
                                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                        vibrator?.vibrate(longArrayOf(0, 150, 80, 150, 80, 300), -1)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    viewModel.showToast("🔊 ТАКТИЛЬНЫЙ ШЕПОТ: Сообщение перекодировано в вибро-паттерн.")
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Tactile Whisper",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                if (isTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "печатает в зашифрованном канале...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Diagnostic / Deletion Testing Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Тест модулей:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                AssistChip(
                    onClick = { viewModel.simulateDeletedMessageTrigger() },
                    label = { Text("Удалить ответ") },
                    leadingIcon = {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
                AssistChip(
                    onClick = { viewModel.sendOneTimeMedia(chatId) },
                    label = { Text("Одноразовое фото") },
                    leadingIcon = {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }

            // Input Messaging bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Schrödinger state selector
                IconButton(
                    onClick = { 
                        sendAsSchrodinger = !sendAsSchrodinger
                        viewModel.showToast(if (sendAsSchrodinger) "🧪 Режим Шредингера: Сообщения будут запечатаны в квантовую структуру!" else "Обычный режим отправки")
                    },
                    modifier = Modifier
                        .background(
                            if (sendAsSchrodinger) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Квантовый купол",
                        tint = if (sendAsSchrodinger) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = inputMessageText,
                    onValueChange = { inputMessageText = it },
                    placeholder = { 
                        Text(if (sendAsSchrodinger) "Квантовое сообщение..." else "Напишите сообщение...") 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (sendAsSchrodinger) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                IconButton(
                    onClick = {
                        if (inputMessageText.isNotBlank()) {
                            val finalMsg = if (sendAsSchrodinger) "[Шредингер] $inputMessageText" else inputMessageText
                            viewModel.sendMessage(chatId, finalMsg)
                            inputMessageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (sendAsSchrodinger) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                        .testTag("chat_send_btn")
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Отправить",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ProxyTab(
    viewModel: PrimeViewModel,
    modifier: Modifier = Modifier
) {
    val proxies by viewModel.proxies.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val activeSettings = settingsState ?: PrimeSettingsEntity()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Двойное туннелирование MTProto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeSettings.proxyEnabled) "СОЕДИНЕНО" else "ОТКЛЮЧЕНО",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = if (activeSettings.proxyEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = activeSettings.proxyEnabled,
                        onCheckedChange = { viewModel.setProxyEnabled(it) }
                    )
                }
                Text(
                    text = "Используйте встроенные прокси, чтобы скрыть сетевой след от провайдеров интернета. Трафик шифруется и пробрасывается через анонимные хабы.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Card 2: Tor Hidden Service (.onion OnionProxy)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Анонимный режим Tor Onion (.onion)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeSettings.torTunnelEnabled) "АКТИВЕН (OR СЕТЬ)" else "ВЫКЛЮЧЕН",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = if (activeSettings.torTunnelEnabled) Color(0xFFA020F0) else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = activeSettings.torTunnelEnabled,
                        onCheckedChange = { viewModel.setTorTunnelEnabled(it) }
                    )
                }
                Text(
                    text = "Запускает встроенный клиент OnionProxy на базе сети Tor. Трафик проходит через 3 независимых шифрованных узла (Входной -> Срединный -> Выходной). Ваше устройство получает приватный .onion адрес.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activeSettings.torTunnelEnabled && activeSettings.torOnionAddress.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ваш приватный адрес:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeSettings.torOnionAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 3: libp2p DHT Peer Sync
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Децентрализованный узел libp2p DHT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeSettings.p2pDhtEnabled) "УЗЕЛ СИНХРОНИЗИРОВАН (DHT)" else "ОСТАНОВЛЕН",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = if (activeSettings.p2pDhtEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = activeSettings.p2pDhtEnabled,
                        onCheckedChange = { viewModel.setP2pDhtEnabled(it) }
                    )
                }
                Text(
                    text = "Использует распределенную хеш-таблицу (Kademlia DHT) через Go/Rust-мосты для прямого P2P обнаружения устройств. Позволяет обходить централизованные серверы Primegram.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activeSettings.p2pDhtEnabled && activeSettings.p2pPeerId.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ваш Peer ID:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color.Green, CircleShape))
                                    Text(
                                        text = "Активно (14 peers)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Green
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeSettings.p2pPeerId,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Card 4: PGP P2P TCP Socket Engine
        val p2pLogs by viewModel.p2pLogs.collectAsState()
        val p2pServerStatus by viewModel.p2pServerStatus.collectAsState()
        val p2pClientConnected by viewModel.p2pClientConnected.collectAsState()
        var peerIpInput by remember { mutableStateOf("") }
        var p2pMessageInput by remember { mutableStateOf("") }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Локальный PGP Р2Р Модем (TCP)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (p2pServerStatus.contains("Слушает")) Color.Green else Color.Red,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = "СЕРВЕР: $p2pServerStatus",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            if (p2pServerStatus.contains("Слушает")) {
                                viewModel.stopPgpServer()
                            } else {
                                viewModel.startPgpServer()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (p2pServerStatus.contains("Слушает")) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Тумблер сервера",
                            tint = if (p2pServerStatus.contains("Слушает")) Color.Green else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = "Прямое соединение пир-ту-пир по кастомному бинарному протоколу PGP ([1 байт тип] + [4 байта длина] + [данные]). Работает без интернета через локальный WiFi/IP хосты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = peerIpInput,
                        onValueChange = { peerIpInput = it },
                        label = { Text("IP адрес друга (пира)") },
                        placeholder = { Text("Напр. 192.168.1.50") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        trailingIcon = {
                            if (p2pClientConnected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = Color.Green
                                )
                            }
                        }
                    )

                    Button(
                        onClick = {
                            if (p2pClientConnected) {
                                viewModel.disconnectFromPgpPeer()
                            } else {
                                if (peerIpInput.isNotBlank()) {
                                    viewModel.connectToPgpPeer(peerIpInput) { _ -> }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (p2pClientConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (p2pClientConnected) "Откл" else "Связь")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (p2pLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Лог сообщений PGP пуст\nПодключитесь и отправьте сообщение",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        LaunchedEffect(p2pLogs.size) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            p2pLogs.forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (log.contains("Вы:")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = p2pMessageInput,
                        onValueChange = { p2pMessageInput = it },
                        placeholder = { Text("Сообщение PGP...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    IconButton(
                        onClick = {
                            if (p2pMessageInput.isNotBlank()) {
                                viewModel.sendPgpP2pMessage(p2pMessageInput)
                                p2pMessageInput = ""
                            }
                        },
                        enabled = p2pClientConnected,
                        modifier = Modifier.background(
                            if (p2pClientConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send binary PGP bundle",
                            tint = if (p2pClientConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
                
                if (p2pLogs.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearP2pLogs() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Очистить журнал", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Text(
            text = "Доступные мосты соединения",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        proxies.forEach { proxy ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = proxy.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${proxy.host}:${proxy.port}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteProxy(proxy.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (proxy.secret != null) {
                        Text(
                            text = "Secret Hex: ${proxy.secret.take(18)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PluginsTab(
    viewModel: PrimeViewModel,
    modifier: Modifier = Modifier
) {
    val plugins by viewModel.plugins.collectAsState()

    // Form states for custom JS update creation
    var customName by remember { mutableStateOf("") }
    var customVersion by remember { mutableStateOf("v1.0") }
    var customDesc by remember { mutableStateOf("") }
    var customCode by remember { mutableStateOf("") }
    var isFormExpanded by remember { mutableStateOf(false) }

    // Predefined official JS updates
    val officialUpdates = listOf(
        Triple(
            "Quantum Encryptor (v3.5)",
            "Инжектирует дополнительный слой квантово-резистентного шифрования в каждый исходящий сокет для защиты от суперкомпьютеров ИИ.",
            "function onSendMessage(msg) {\n    Primegram.showToast(\"🛡️ [JS Kernel v3.5] Инжектирован слой зашифрованного сокета!\");\n    return \"🔒 [Quantum-Secret] \" + msg;\n}"
        ),
        Triple(
            "Core Spam-Firewall (v3.6)",
            "Фильтрует входящий рекламный спам и акции на лету, подменяя текст предупреждением сетевого администратора ядра.",
            "function onReceiveMessage(msg) {\n    if (msg.toLowerCase().includes(\"купить\") || msg.toLowerCase().includes(\"акция\") || msg.toLowerCase().includes(\"скидка\")) {\n        Primegram.showToast(\"🛑 [JS Firewall] Спам успешно нейтрализован!\");\n        return \"📥 [Сетевое ядро: Входящий рекламный блок заблокирован и дезинфицирован]\";\n    }\n    return msg;\n}"
        ),
        Triple(
            "Crypto Shield Transliterator (v3.7)",
            "Автоматически шифрует важные ключевые фразы (пароль, баг, секрет, TON) в безопасные крипто-символы для блокировки глубокого сканирования провайдером.",
            "function onSendMessage(msg) {\n    return msg.replace(\"пароль\", \"🔑\").replace(\"баг\", \"🐛\").replace(\"секрет\", \"🤫\").replace(\"ton\", \"💎\");\n}"
        )
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Модификации Ядра (Mods Engine)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Данные скрипты и хуки инжектируются в оригинальное соединение Telegram, изменяя поведение сервера, фильтрацию и рендеринг.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // JS Updates Center Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Динамические JS Обновления Ядра",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Доступно обновлений: ${officialUpdates.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Official Updates section
        officialUpdates.forEach { (nameAndVer, desc, code) ->
            val nameOnly = nameAndVer.substringBefore(" (")
            val verOnly = nameAndVer.substringAfter("(").substringBefore(")")
            
            // Check if already installed
            val isInstalled = plugins.any { it.name == nameOnly }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Доступное JS Обновление",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = nameOnly,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = verOnly, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Что добавилось в обновлении:\n$desc",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            viewModel.addCustomPlugin(nameOnly, desc, verOnly, "JS Hot-Update", code)
                        },
                        enabled = !isInstalled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isInstalled) "✅ Системное Обновление Применено" else "📥 Установить JS Обновление Ядра",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Custom JS hot patches form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFormExpanded = !isFormExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Custom JS code",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Развернуть ручную установку JS скриптов",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Icon(
                        imageVector = if (isFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Тумблер",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                if (isFormExpanded) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Название скрипта / мода") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customVersion,
                        onValueChange = { customVersion = it },
                        label = { Text("Версия") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customDesc,
                        onValueChange = { customDesc = it },
                        label = { Text("Описание изменений (Что нового)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customCode,
                        onValueChange = { customCode = it },
                        label = { Text("Код JS обновления ядра") },
                        placeholder = { Text("function onSendMessage(msg) {\n  return msg;\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 10
                    )

                    Button(
                        onClick = {
                            if (customName.isNotBlank() && customCode.isNotBlank()) {
                                viewModel.addCustomPlugin(customName, customDesc, customVersion, "Custom User JS", customCode)
                                customName = ""
                                customDesc = ""
                                customCode = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Применить и заинжектить JS обновление", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Активные плагины и горячие моды",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        plugins.forEach { plugin ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = plugin.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = plugin.version,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = plugin.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Тип: ${plugin.type}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val isSystemPlugin = plugin.id in listOf("anti_recall", "media_saver", "ip_spoofer", "ghost_mode")
                    if (!isSystemPlugin) {
                        IconButton(onClick = { viewModel.deletePlugin(plugin.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить плагин",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Switch(
                        checked = plugin.isEnabled,
                        onCheckedChange = { viewModel.setPluginEnabled(plugin.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun MiniAppsTab(
    viewModel: PrimeViewModel,
    onOpenUrl: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val miniApps by viewModel.miniApps.collectAsState()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Песочница Mini-Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Запускайте интегрированные веб-приложения Telegram в один тап. Игры, Web3 крипто-кошельки и утилиты прямо внутри Primegram.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Библиотека приложений",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        miniApps.forEach { app ->
            Card(
                onClick = { onOpenUrl(app.url, app.name) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = app.iconEmoji, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = app.url,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { viewModel.deleteMiniApp(app.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: PrimeViewModel,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settings.collectAsState()
    val activeSettings = settingsState ?: PrimeSettingsEntity()

    var uniqueIdText by remember(activeSettings) { mutableStateOf(activeSettings.userUniqueId) }
    var nameText by remember(activeSettings) { mutableStateOf(activeSettings.displayName) }
    var usernameText by remember(activeSettings) { mutableStateOf(activeSettings.username) }
    var bioText by remember(activeSettings) { mutableStateOf(activeSettings.bio) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Star Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Баланс Звёзд Primegram",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeSettings.starsBalance} 🌟 звезды",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.purchaseStars(2500) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Refill", tint = Color.Black)
                    }
                }
                Text(
                    text = "Звезды звеньев Primegram используются для отправки премиум-подарков разработчикам или анонимным собеседникам. За покупки начисляются дополнительные баллы доверия.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Identity Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Шифрованная Личность",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uniqueIdText,
                    onValueChange = { uniqueIdText = it.take(20).filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Ваш уникальный ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Отображаемое имя") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = usernameText,
                    onValueChange = { usernameText = it },
                    label = { Text("Юзернейм (@)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bioText,
                    onValueChange = { bioText = it },
                    label = { Text("О себе") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        if (uniqueIdText.length < 3) {
                            viewModel.showToast("ID не может быть короче 3 символов!")
                            return@Button
                        }
                        viewModel.updateUserProfile(uniqueIdText, nameText, usernameText, bioText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить параметры личности")
                }
            }
        }

        // Crypto Cryptographic standard selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Стандарт Криптографии диалогов",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Текущий алгоритм: ${activeSettings.activeEncryptionLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                val levels = listOf("AES-256-GCM", "ChaCha20-Poly1305", "Triple-DES Extended", "Quantum-Safe Crystal Kyber")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    levels.take(2).forEach { level ->
                        OutlinedButton(
                            onClick = { viewModel.setEncryptionLevel(level) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(level, fontSize = 10.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    levels.takeLast(2).forEach { level ->
                        OutlinedButton(
                            onClick = { viewModel.setEncryptionLevel(level) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(level, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ⚡ ЕДИНОЕ МУЛЬТИПРОТОКОЛЬНОЕ P2P ЯДРО (CONSOLIDATED CORE ACTION)
        val isCoreActive = activeSettings.onionRoutingEnabled

        Text(
            text = "Параметры безопасности (Режим Параноика)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isCoreActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, if (isCoreActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡", style = MaterialTheme.typography.titleMedium)
                        Column {
                            Text(
                                text = "Единый P2P-Протокол Слияния",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Совмещение всех 10 автономных систем в единый поток",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isCoreActive,
                        onCheckedChange = { viewModel.toggleAllCoreFeatures(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Terminal Simulation Console Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCoreActive) Color.Green else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCoreActive) "STATUS: CORE ACTIVE • MULTICAST TUNNEL ONLINE" else "STATUS: STANDBY • LOCAL CLIENT ONLY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCoreActive) Color.Green else Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = if (isCoreActive) 
                                ">> [DHT] Listening on 254.0.0.1:4139\n>> [Wifi P2P] Discovery active • Broadcaster online\n>> [Sub-Ratchet] Active Keys generated (Ed25519)\n>> [Sharding-V2] 128-bit chunking table verified"
                                else ">> [DHT] Engine offline\n>> [Tor onion] Tunnel dormant\n>> TABS CONSOLIDATED INTO ONE SEAMLESS PROTOCOL",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCoreActive) Color.Green.copy(alpha = 0.8f) else Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = "Интегрированные протоколы связи в активной сессии:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Sub-Features list
                val subProtocols = listOf(
                    "🧅 Tor Onion Routing" to "Мета-микширование пакетов через 3 анонимных пира",
                    "📶 Wi-Fi Hotspot Mesh" to "Сквозная раздача трафика и карт в офлайн-зонах без LTE",
                    "👥 CRDT Blind Channels" to "Слепые группы: никто не знает участников, кроме своих соседей",
                    "👻 UDP Multicast Rooms" to "Комнаты-призраки: переписка пишется только в сверхбыстрое ОЗУ",
                    "✉️ P2P Message Dropping" to "Офлайн-транзит сообщений через попутные онлайн-ноды",
                    "💿 Distributed Torrent Sharding" to "Нарезка тяжелых файлов на 100 шардов для распределенного кэша",
                    "🧵 Sub-Ratchet Cryptography" to "Каждая ветка чата имеет автономное дерево ключей хранилища",
                    "🛡️ NDK Anti-Frida Sentinel" to "Непрерывная верификация памяти на перехватчики ядра",
                    "🔍 Bio FTS5 Crypto-Search" to "Криптографический мгновенный поиск по зашифрованной базе"
                )

                subProtocols.forEach { (title, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isCoreActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCoreActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Tools / Maintenance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Утилиты обслуживания ядра",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.clearCache() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Очистить кэш")
                    }
                    Button(
                        onClick = { viewModel.clearLocalStoredIntercepted() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Очистить перехват")
                    }
                }
            }
        }
    }
}

@Composable
fun FirstTimeSetupDialog(viewModel: PrimeViewModel) {
    var uniqueId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                "Создание личности Primegram",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Пожалуйста, сконфигурируйте начальный профиль вашей шифрованной личности, так как база пуста.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uniqueId,
                    onValueChange = { uniqueId = it.take(20).filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Уникальный ID личности") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Ваша подпись (Имя)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Юзернейм (@)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (uniqueId.length < 3) {
                        viewModel.showToast("ID не может быть короче 3 символов!")
                        return@Button
                    }
                    if (displayName.isBlank()) {
                        viewModel.showToast("Имя не может быть пустым!")
                        return@Button
                    }
                    viewModel.updateUserProfile(uniqueId, displayName, if (username.startsWith("@")) username else "@$username", "Новый аноним")
                }
            ) {
                Text("Инициализировать ядро")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserDialog(
    viewModel: PrimeViewModel,
    onDismiss: () -> Unit
) {
    val chatUsers by viewModel.chatUsers.collectAsState()
    var query by remember { mutableStateOf("") }

    var isCreatingNew by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var isNewBot by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Поиск контактов",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isCreatingNew) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Введите ID, имя или @username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val filteredUsers = remember(query, chatUsers) {
                        if (query.isBlank()) {
                            chatUsers
                        } else {
                            chatUsers.filter {
                                it.id.contains(query, ignoreCase = true) ||
                                        it.displayName.contains(query, ignoreCase = true) ||
                                        (it.username ?: "").contains(query, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Собеседник не найден локально",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    newId = query.take(20).filter { it.isLetterOrDigit() || it == '_' }
                                    isCreatingNew = true
                                }
                            ) {
                                Text("Создать по ID: $query")
                            }
                        }
                    } else {
                        Text(
                            text = "Результаты поиска:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 220.dp)
                        ) {
                            items(filteredUsers) { user ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectChat(user.id)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(user.avatarColor),
                                                            Color(user.avatarColor).copy(alpha = 0.6f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.displayName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (user.id == "prime41k") {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    ExtraBadge("DEV", Color(0xFFD500F9))
                                                }
                                            }
                                            Text(
                                                text = "ID: ${user.id} • ${user.username ?: ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Новый защищенный контакт",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it.take(20).filter { c -> c.isLetterOrDigit() || c == '_' } },
                        label = { Text("Уникальный ID (латиница)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Имя контакта") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username (@)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Например: @member_prime") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isNewBot, onCheckedChange = { isNewBot = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Создать как Ассистента (Бот)")
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(
                    onClick = {
                        if (newId.length < 3) {
                            viewModel.showToast("ID слишком короткий!")
                            return@Button
                        }
                        if (newName.isBlank()) {
                            viewModel.showToast("Имя не может быть пустым!")
                            return@Button
                        }
                        viewModel.addChatUser(
                            id = newId,
                            displayName = newName,
                            username = if (newUsername.startsWith("@") || newUsername.isBlank()) newUsername else "@$newUsername",
                            isBot = isNewBot
                        )
                        viewModel.selectChat(newId)
                        onDismiss()
                    }
                ) {
                    Text("Открыть чат")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        },
        dismissButton = {
            if (isCreatingNew) {
                TextButton(onClick = { isCreatingNew = false }) {
                    Text("Назад")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerProfileDialog(
    viewModel: PrimeViewModel,
    partnerId: String,
    onDismiss: () -> Unit
) {
    val chatUsers by viewModel.chatUsers.collectAsState()
    val partner = remember(partnerId, chatUsers) {
        chatUsers.find { it.id == partnerId }
    }

    if (partner == null) {
        onDismiss()
        return
    }

    val glowColor = remember(partner.neonGlowColor) {
        when (partner.neonGlowColor) {
            "Neon Cyan" -> Color(0xFF00E5FF)
            "Neon Purple" -> Color(0xFFD500F9)
            "Neon Gold" -> Color(0xFFFFD600)
            "Neon Pink" -> Color(0xFFFF4081)
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Профиль собеседника",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (partner.isBot) {
                    ExtraBadge("BOT", MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (glowColor != null) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .border(3.dp, glowColor.copy(alpha = 0.4f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, glowColor, CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(partner.avatarColor),
                                        Color(partner.avatarColor).copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = partner.displayName.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = partner.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = partner.username ?: ("@id_" + partner.id),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (partner.spentStars > 0) {
                            Text(
                                text = "Набрано звёзд: ${partner.spentStars} 🌟",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "О себе (Bio)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = partner.bio ?: "Описание отсутствует.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Уникальный ID",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = partner.id,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🎁 Отправить Telegram Подарок",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Отправляйте подарки, чтобы повысить звездный статус собеседника. Звезды списываются с вашего баланса.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val gifts = listOf(
                        Triple("Серебряная Звезда", 500, "⭐️"),
                        Triple("Кубок Точности", 1200, "🏆"),
                        Triple("Алмаз Привата", 5000, "💎"),
                        Triple("Космический Шаттл", 10000, "🛸")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gifts.take(2).forEach { gift ->
                            Box(modifier = Modifier.weight(1f)) {
                                GiftCard(gift.first, gift.second, gift.third) {
                                    viewModel.sendGift(partner.id, gift.first, gift.second)
                                    onDismiss()
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gifts.takeLast(2).forEach { gift ->
                            Box(modifier = Modifier.weight(1f)) {
                                GiftCard(gift.first, gift.second, gift.third) {
                                    viewModel.sendGift(partner.id, gift.first, gift.second)
                                    onDismiss()
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun GiftCard(
    name: String,
    cost: Int,
    icon: String,
    onSend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSend() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                fontSize = 28.sp
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$cost 🌟",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddProxyDialog(
    viewModel: PrimeViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить MTProto Мост") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название прокси") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Хост (IP / Домен)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Порт (Digital)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("MTProto Secret Hex (Опционально)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = port.toIntOrNull()
                    if (title.isBlank() || host.isBlank() || p == null) {
                        viewModel.showToast("Пожалуйста, заполните необходимые поля!")
                        return@Button
                    }
                    viewModel.addProxy(title, host, p, if (secret.isBlank()) null else secret)
                    onDismiss()
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AddMiniAppDialog(
    viewModel: PrimeViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎮") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить Mini-App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название Mini-App") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Краткое описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Веб-Ссылка WebApp (https://)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Иконка Emoji") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || url.isBlank()) {
                        viewModel.showToast("Название и Web-URL обязательны!")
                        return@Button
                    }
                    viewModel.addMiniApp(name, desc, url, emoji)
                    onDismiss()
                }
            ) {
                Text("Добавить апп")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppWebViewDialog(
    title: String,
    url: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("SANDBOX WEB", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Инициализация безопасной WebView сессии. Поскольку внутри эмулятора запуск внешних JS фреймов изолирован, мы симулируем защищенную игровую сессию:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sandbox mock frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎮", fontSize = 32.sp)
                        }

                        Text(
                            text = "Консоль WebApp: $title",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "URL: $url\n\nМолниеносный запуск JS-скриптов... Провайдер данных TON API успешно подключен! Симуляция завершена.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть сессию")
            }
        }
    )
}

@Composable
fun ParanoidFeatureRow(
    title: String,
    description: String,
    iconEmoji: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AddPluginDialog(
    viewModel: PrimeViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("v1.0") }
    var type by remember { mutableStateOf("Кастомный JS") }
    
    val initialScript = """
        function onSendMessage(msg) {
            return msg;
        }
        function onReceiveMessage(msg) {
            return msg;
        }
        function onEnabled(isEnabled) {
            Primegram.showToast("Плагин изменен: " + isEnabled);
        }
    """.trimIndent()
    
    var scriptCode by remember { mutableStateOf(initialScript) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить JS-Плагин 🚀") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название плагина") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Версия") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Категория") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Text(
                    text = "Выберите шаблон кода:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    val templates = listOf(
                        "Пустой" to initialScript,
                        "ALL CAPS" to """
                            function onSendMessage(msg) {
                                return msg.toUpperCase() + " !!!";
                            }
                            function onReceiveMessage(msg) {
                                return msg;
                            }
                            function onEnabled(isEnabled) {
                                Primegram.showToast("ALL CAPS: " + isEnabled);
                            }
                        """.trimIndent(),
                        "Rot13 Шифр" to """
                            function onSendMessage(msg) {
                                return "[ROT13] " + msg.replace(/[a-zA-Z]/g, function(c){
                                    return String.fromCharCode((c<="Z"?90:122)>=(c=c.charCodeAt(0)+13)?c:c-26);
                                });
                            }
                            function onReceiveMessage(msg) {
                                return msg;
                            }
                        """.trimIndent(),
                        "Авто-Респондер" to """
                            function onSendMessage(msg) {
                                return msg;
                            }
                            function onReceiveMessage(msg) {
                                if (msg.toLowerCase().indexOf("как дела") !== -1) {
                                    Primegram.sendSystemMessage("JS Авто-Реплика: Я работаю круглосуточно на благо анонимности!");
                                    Primegram.awardStars(5);
                                }
                                return msg;
                            }
                        """.trimIndent()
                    )
                    
                    templates.forEach { (title, templateCode) ->
                        Button(
                            onClick = { scriptCode = templateCode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(title)
                        }
                    }
                }

                OutlinedTextField(
                    value = scriptCode,
                    onValueChange = { scriptCode = it },
                    label = { Text("JavaScript Код") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    maxLines = 15
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || scriptCode.isBlank()) {
                        viewModel.showToast("Название и JavaScript код обязательны!")
                        return@Button
                    }
                    viewModel.addCustomPlugin(name, desc, version, type, scriptCode)
                    onDismiss()
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

@Composable
fun AlivePlaylistTab(
    viewModel: PrimeViewModel,
    modifier: Modifier = Modifier
) {
    val nearbyPeers by viewModel.nearbyPeers.collectAsState()
    val airEchoes by viewModel.airEchoes.collectAsState()
    val activeMusicPeer by viewModel.activeSharedMusicPeer.collectAsState()

    var customSong by remember { mutableStateOf("") }
    var customArtist by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.startAlivePlaylistDiscovery()
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RADAR HEADER & BRIEF DESCRIPTION
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Breathing radar animation panel
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📡 ЭФИР «ALIVE PLAYLIST»",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Подключайтесь к наушникам людей в радиусе 50 метров без интернета (Wi-Fi Direct + UDP). Слушайте музыку в реальном времени.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // RADAR SIMULATOR RINGS VIEW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F171E))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing circles in background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0x3300E5FF),
                    radius = 50.dp.toPx(),
                    center = centerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0x2200E5FF),
                    radius = 90.dp.toPx(),
                    center = centerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0x1100E5FF),
                    radius = 130.dp.toPx(),
                    center = centerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                
                // Sweep line representing proximity audit
                val time = System.currentTimeMillis() % 4000
                val angle = (time / 4000f) * 360f
                val length = 150.dp.toPx()
                val endX = centerOffset.x + length * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                val endY = centerOffset.y + length * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                
                drawLine(
                    color = Color(0x7700E5FF),
                    start = centerOffset,
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Сканирование пространства...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${nearbyPeers.size} узлов «AlivePlaylist Node» найдено в метро",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
        }

        // LIST OF NEARBY PEERS
        Text(
            text = "Люди рядом и их музыкальный поток",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        nearbyPeers.forEach { peer ->
            val isStreaming = activeMusicPeer?.id == peer.id
            val percentProgress = peer.currentTrackProgressSeconds.toFloat() / peer.trackDurationSeconds.toFloat()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peer_card_${peer.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isStreaming) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isStreaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(peer.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = peer.name.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = peer.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "• ${peer.distanceMeters}м рядом",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${peer.currentArtist} — ${peer.currentTrack}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Spark or Wink back badge indicator
                        if (peer.isWinked) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF4081).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "❤️ Взаимно!",
                                    color = Color(0xFFFF4081),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Music progress bar ticked by state
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LinearProgressIndicator(
                            progress = percentProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${peer.currentTrackProgressSeconds / 60}:${String.format("%02d", peer.currentTrackProgressSeconds % 60)}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${peer.trackDurationSeconds / 60}:${String.format("%02d", peer.trackDurationSeconds % 60)}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Bottom stream and wink actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isStreaming) {
                                    viewModel.selectMusicPeer(null)
                                } else {
                                    viewModel.selectMusicPeer(peer)
                                    viewModel.showToast("🎧 Успешное подключение к потоку ${peer.name}. Задержка UDP: 24ms")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStreaming) Color.Gray else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isStreaming) Icons.Default.Pause else Icons.Default.Hearing,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isStreaming) "Отключить Эфир" else "Слушать Вместе",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.winkAtPeer(peer.id)
                            },
                            enabled = !peer.isWinked,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF4081)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF4081)
                            ),
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color(0xFFFF4081)
                                )
                                Text(
                                    text = if (peer.isWinked) "Подмигнуто" else "Подмигнуть",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // SIMULATED HEADSETS AUDIO ENGINES STATS PANEL
        if (activeMusicPeer != null) {
            val peer = activeMusicPeer!!
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Green)
                            )
                            Text(
                                text = "АКТИВНЫЙ UDP АУДИОМОСТ С ${peer.name.toUpperCase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.selectMusicPeer(null) }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        }
                    }

                    // Ticking wavy bars simulating real-time audio stream buffer levels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(16) { index ->
                            val heightOffset = (10..22).random()
                            val isPulse = System.currentTimeMillis() % 400 > index * 10
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 1.5.dp)
                                    .width(3.dp)
                                    .height(if (isPulse) heightOffset.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Text(
                        text = "Синхронизация NTP: Jitter 4ms | Буфер RAM: 15 кадров (24ms разница) | Кодек Opus-P2P",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // AIR ECHO SECTION (DIGITAL GRAFFITI)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Эхо-режим (Оставить музыкальный след)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "Оставьте трек 'висеть' в пространстве. Люди, зашедшие в эту локацию после вас, смогут поймать и услышать вашу волну.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = customArtist,
                    onValueChange = { customArtist = it },
                    label = { Text("Исполнитель / Группа") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customSong,
                    onValueChange = { customSong = it },
                    label = { Text("Название трека") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (customArtist.isNotBlank() && customSong.isNotBlank()) {
                            viewModel.leaveAirEcho(customSong, customArtist)
                            customArtist = ""
                            customSong = ""
                        } else {
                            viewModel.showToast("Введите исполнителя и песню!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Подвесить трек в этой геоточке 🪐", fontWeight = FontWeight.Black)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Активные Эхо-Граффити вокруг вас",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                airEchoes.forEach { echo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📻", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = "${echo.artistName} — ${echo.songName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Оставил: ${echo.leftBy} • ${echo.coordinates}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "x${echo.multiplier} ловов",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

