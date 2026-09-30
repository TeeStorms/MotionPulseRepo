package com.example.motionpulse.ui.screens.community

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.ActivityFeedEntry
import com.example.motionpulse.data.local.entity.CommunityNotification
import com.example.motionpulse.data.local.entity.FeedEventType
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.screens.dashboard.components.MotionPulseHeader
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import com.example.motionpulse.util.AppConstants
import java.util.concurrent.TimeUnit

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?
) {
    val feedState by viewModel.feedState.collectAsState()
    val friendsState by viewModel.friendsState.collectAsState()
    val leaderboardState by viewModel.leaderboardState.collectAsState()
    val challengesState by viewModel.challengesState.collectAsState()
    val duelsState by viewModel.duelsState.collectAsState()
    val notificationsState by viewModel.notificationsState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedMetric by viewModel.leaderboardMetric.collectAsState()
    val incomingRequestsState by viewModel.incomingRequestsState.collectAsState()
    val sentRequestsState by viewModel.sentRequestsState.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val mutedUserIds by viewModel.mutedUserIds.collectAsState()
    val nudgeFeedback by viewModel.nudgeFeedback.collectAsState()
    val hasSeenTip by viewModel.hasSeenCommunityTipState.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Feed", "Leaderboard", "Duels")

    var showNotificationTray by remember { mutableStateOf(false) }
    var showAddFriendOverlay by remember { mutableStateOf(false) }
    var showFriendsManagement by remember { mutableStateOf(false) }

    val incomingRequestsCount = (incomingRequestsState as? UiState.Success)?.data?.size ?: 0
    val unreadCount = ((notificationsState as? UiState.Success)?.data?.count { !it.isRead } ?: 0) + incomingRequestsCount

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(nudgeFeedback) {
        nudgeFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNudgeFeedback()
        }
    }

    Scaffold(
        bottomBar = {
            MotionPulseBottomNav(
                currentRoute = currentRoute,
                onNavigate = onNavigateToNav
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Notification Bell and Add Friend
                Box(modifier = Modifier.fillMaxWidth()) {
                    MotionPulseHeader(
                        title = "Pulse Community",
                        subtitle = "MOTION.PULSE"
                    )
                    
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showFriendsManagement = true }) {
                            Icon(Icons.Default.Group, contentDescription = "Manage Friends", tint = TextPrimary)
                        }

                        IconButton(onClick = { showAddFriendOverlay = true }) {
                            BadgedBox(
                                badge = {
                                    if (incomingRequestsCount > 0) {
                                        Badge { Text(incomingRequestsCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = TextPrimary)
                            }
                        }

                        IconButton(onClick = { showNotificationTray = true }) {
                            BadgedBox(
                                badge = { if (unreadCount > 0) Badge { Text(unreadCount.toString()) } }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                            }
                        }
                    }
                }

                // One-time dismissible tip
                if (!hasSeenTip) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "💡", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Tip: Share your code to connect with a friend!",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            TextButton(onClick = { viewModel.dismissCommunityTip() }) {
                                Text(text = "Got it", color = AccentPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BackgroundDark,
                    contentColor = AccentPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentPrimary
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(text = title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> FeedTab(
                        feed = (feedState as? UiState.Success)?.data ?: emptyList(),
                        currentUserId = userProfile?.uid ?: "",
                        viewModel = viewModel,
                        state = feedState,
                        onRetry = { viewModel.retry() },
                        onAddFriend = { showAddFriendOverlay = true }
                    )
                    1 -> LeaderboardTab(
                        leaderboard = (leaderboardState as? UiState.Success)?.data ?: listOfNotNull(userProfile),
                        selectedMetric = selectedMetric,
                        viewModel = viewModel,
                        state = leaderboardState,
                        onRetry = { viewModel.retry() },
                        onAddFriend = { showAddFriendOverlay = true }
                    )
                    2 -> ChallengesTabSection(
                        challengesState = challengesState,
                        duelsState = duelsState,
                        currentUserId = userProfile?.uid ?: "",
                        viewModel = viewModel,
                        onAddFriend = { showAddFriendOverlay = true }
                    )
                }
            }

            // Notification Tray (Overlay)
            if (showNotificationTray) {
                NotificationTray(
                    notificationsState = notificationsState,
                    incomingRequestsState = incomingRequestsState,
                    onDismiss = { showNotificationTray = false },
                    onMarkRead = { viewModel.markRead(it) },
                    onRespond = { reqId, uid, accept -> viewModel.respondToRequest(reqId, uid, accept) },
                    onBlock = { viewModel.blockUser(it) },
                    onRetry = { viewModel.retry() }
                )
            }

            // Add Friend Overlay
            if (showAddFriendOverlay) {
                AddFriendOverlay(
                    userFriendCode = userProfile?.friendCode ?: "---",
                    onDismiss = { showAddFriendOverlay = false },
                    onSearch = { viewModel.searchByCode(it) },
                    searchResult = searchResult,
                    searchError = searchError,
                    onSendRequest = { uid, name -> viewModel.sendFriendRequest(uid, name) }
                )
            }

            // Friends Management Dialog
            if (showFriendsManagement) {
                FriendsManagementDialog(
                    friendsState = friendsState,
                    sentRequestsState = sentRequestsState,
                    mutedUserIds = mutedUserIds,
                    onDismiss = { showFriendsManagement = false },
                    onUnfriend = { viewModel.unfriend(it) },
                    onCancelSentRequest = { viewModel.cancelSentRequest(it) },
                    onMute = { viewModel.muteUser(it) },
                    onUnmute = { viewModel.unmuteUser(it) },
                    onBlock = { viewModel.blockUser(it) },
                    onRetry = { viewModel.retry() }
                )
            }
        }
    }
}

@Composable
fun <T> UiStateContainer(
    state: UiState<T>,
    onRetry: () -> Unit,
    emptyMessage: String = "No items available",
    content: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        }
        is UiState.Empty -> {
            EmptyState(message = emptyMessage)
        }
        is UiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "⚠️", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.message,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error Code: ${state.kind.name} ${state.cause?.message ?: ""}",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Couldn't load this. Tap to retry")
                }
            }
        }
        is UiState.Success -> {
            content(state.data)
        }
    }
}

@Composable
fun NotificationTray(
    notificationsState: UiState<List<CommunityNotification>>,
    incomingRequestsState: UiState<List<com.example.motionpulse.data.local.entity.FriendEntity>>,
    onDismiss: () -> Unit,
    onMarkRead: (String) -> Unit,
    onRespond: (String, String, Boolean) -> Unit,
    onBlock: (String) -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .background(CardBackground, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Social Hub", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val requests = (incomingRequestsState as? UiState.Success)?.data ?: emptyList()
                val notes = (notificationsState as? UiState.Success)?.data ?: emptyList()

                if (notificationsState is UiState.Error || incomingRequestsState is UiState.Error) {
                    val errorMsg = (notificationsState as? UiState.Error)?.message 
                        ?: (incomingRequestsState as? UiState.Error)?.message ?: "Failed to load notifications"
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⚠️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                            Text("Couldn't load this. Tap to retry")
                        }
                    }
                } else if (notificationsState is UiState.Loading || incomingRequestsState is UiState.Loading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentPrimary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (requests.isNotEmpty()) {
                            item {
                                Text(text = "FRIEND REQUESTS", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            items(requests) { req ->
                                FriendRequestItem(req, onRespond, onBlock)
                            }
                            item {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }

                        if (notes.isEmpty() && requests.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No notifications yet", color = TextSecondary)
                                }
                            }
                        } else {
                            item {
                                Text(text = "RECENT ACTIVITY", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            items(notes) { note ->
                                NotificationItem(note, onMarkRead)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsManagementDialog(
    friendsState: UiState<List<UserProfileEntity>>,
    sentRequestsState: UiState<List<com.example.motionpulse.data.local.entity.FriendEntity>>,
    mutedUserIds: Set<String>,
    onDismiss: () -> Unit,
    onUnfriend: (String) -> Unit,
    onCancelSentRequest: (String) -> Unit,
    onMute: (String) -> Unit,
    onUnmute: (String) -> Unit,
    onBlock: (String) -> Unit,
    onRetry: () -> Unit
) {
    var userToBlock by remember { mutableStateOf<UserProfileEntity?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorderAlt),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Manage Friends", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val sentRequests = (sentRequestsState as? UiState.Success)?.data ?: emptyList()
                    val friends = (friendsState as? UiState.Success)?.data ?: emptyList()

                    if (friendsState is UiState.Error) {
                        Column(modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("⚠️", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Failed to load friends", color = TextPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                                Text("Retry")
                            }
                        }
                    } else if (friendsState is UiState.Loading) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentPrimary)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (sentRequests.isNotEmpty()) {
                                item {
                                    Text(text = "SENT REQUESTS", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(sentRequests) { req ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                        border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = "Pending Request", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text(text = "Waiting for response...", color = TextPrimary, fontSize = 14.sp)
                                            }
                                            TextButton(onClick = { onCancelSentRequest(req.id) }) {
                                                Text("Cancel", color = Color.Red, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                item {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                                    Text(text = "YOUR FRIENDS", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            if (friends.isEmpty() && sentRequests.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("No friends added yet. Tap '+' to add friends!", color = TextSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                items(friends) { friend ->
                                    val isMuted = friend.uid in mutedUserIds
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                        border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(HeaderGradient2Stop),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = friend.displayName.take(1).uppercase(), color = Color.White)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(text = friend.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                                        Text(text = "Lvl ${friend.currentLevel} · ${friend.currentStreak}d streak", color = TextSecondary, fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(onClick = { if (isMuted) onUnmute(friend.uid) else onMute(friend.uid) }) {
                                                    Text(if (isMuted) "Unmute Nudges" else "Mute Nudges", color = TextSecondary, fontSize = 12.sp)
                                                }
                                                TextButton(onClick = { userToBlock = friend }) {
                                                    Text("Block", color = Color.Red.copy(alpha = 0.8f), fontSize = 12.sp)
                                                }
                                                Button(
                                                    onClick = { onUnfriend(friend.uid) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Unfriend", color = Color.Red, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    userToBlock?.let { friend ->
        AlertDialog(
            onDismissRequest = { userToBlock = null },
            title = { Text("Block ${friend.displayName}?") },
            text = { Text("This will remove your friendship, cancel active duels, and prevent them from contacting you.") },
            confirmButton = {
                Button(
                    onClick = {
                        onBlock(friend.uid)
                        userToBlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBlock = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FriendRequestItem(
    req: com.example.motionpulse.data.local.entity.FriendEntity,
    onRespond: (String, String, Boolean) -> Unit,
    onBlock: (String) -> Unit
) {
    var showBlockConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Incoming Request", color = AccentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = "User challenged you to be friends!", color = TextPrimary, fontSize = 14.sp)
            }
            Row {
                IconButton(onClick = { showBlockConfirm = true }) {
                    Text("🚫", fontSize = 12.sp)
                }
                IconButton(onClick = { onRespond(req.id, req.requesterUid, false) }) {
                    Text("✖", color = Color.Red.copy(alpha = 0.7f))
                }
                IconButton(onClick = { onRespond(req.id, req.requesterUid, true) }) {
                    Text("✔", color = Color.Green.copy(alpha = 0.7f))
                }
            }
        }
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block Request Sender?") },
            text = { Text("This will decline the request, block the user, and prevent future interactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRespond(req.id, req.requesterUid, false)
                        onBlock(req.requesterUid)
                        showBlockConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddFriendOverlay(
    userFriendCode: String,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    searchResult: UserProfileEntity?,
    searchError: String?,
    onSendRequest: (String, String?) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 32.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorderAlt),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Add Friend", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Your Friend Code Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "YOUR FRIEND CODE", color = AccentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = userFriendCode, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (userFriendCode.isNotBlank() && userFriendCode != "---") {
                                            clipboardManager.setText(AnnotatedString(userFriendCode))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = TextPrimary, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (userFriendCode.isNotBlank() && userFriendCode != "---") {
                                            val text = "Join me on Motion.Pulse! Add me with my friend code: $userFriendCode\nGet the app: ${AppConstants.APP_INVITE_URL}"
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, text)
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Share Friend Code")
                                            context.startActivity(shareIntent)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Friend Code (MP-XXXX-XXXX)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(code) }),
                        trailingIcon = {
                            IconButton(onClick = { onSearch(code) }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentPrimary)
                            }
                        }
                    )
                    
                    if (searchError != null) {
                        Text(text = searchError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    
                    if (searchResult != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(HeaderGradient2Stop), contentAlignment = Alignment.Center) {
                                    Text(text = searchResult.displayName.take(1).uppercase(), color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = searchResult.displayName, color = TextPrimary, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { onSendRequest(searchResult.uid, searchResult.displayName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Invite", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(note: CommunityNotification, onMarkRead: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onMarkRead(note.id) },
        colors = CardDefaults.cardColors(containerColor = if (note.isRead) Color.Transparent else Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, if (note.isRead) Color.Transparent else CardBorderAlt.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when(note.type) {
                    "DUEL_INVITE" -> "⚔️ DUEL CHALLENGE"
                    "DUEL_RESULT" -> "🏆 DUEL RESULT"
                    else -> "👋 COMMUNITY NUDGE"
                },
                color = AccentPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.message ?: "You have a new message", color = TextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
fun FeedTab(
    feed: List<ActivityFeedEntry>,
    currentUserId: String,
    viewModel: CommunityViewModel,
    state: UiState<List<ActivityFeedEntry>>,
    onRetry: () -> Unit,
    onAddFriend: () -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        }
        is UiState.Empty -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "🌱", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your feed is quiet. Add a friend to see their milestones.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddFriend,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Friend")
                }
            }
        }
        is UiState.Error -> {
            Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("⚠️", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.message, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                    Text("Tap to retry")
                }
            }
        }
        is UiState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(text = "Today's feed", color = TextSecondary, fontSize = 14.sp)
                }
                items(feed) { entry ->
                    FeedItem(entry, currentUserId, viewModel)
                }
            }
        }
    }
}

@Composable
fun FeedItem(entry: ActivityFeedEntry, currentUserId: String, viewModel: CommunityViewModel) {
    val nudgedUsersToday by viewModel.nudgedUsersToday.collectAsState()
    val isNudgedToday = entry.actorId in nudgedUsersToday

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(HeaderGradient2Stop),
            contentAlignment = Alignment.Center
        ) {
            Text(text = entry.actorName.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            val message = when (entry.eventType) {
                FeedEventType.STREAK_MILESTONE -> "${entry.actorName} hit a ${entry.streakCount}-day streak 🔥"
                FeedEventType.ALL_HABITS_COMPLETED -> "${entry.actorName} completed all habits today"
                FeedEventType.STREAK_AT_RISK -> "${entry.actorName} hasn't logged today"
            }
            
            Text(text = message, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(text = formatTimeAgo(entry.timestamp), color = TextSecondary, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val hasReacted = entry.reactions[currentUserId] == true
                ReactionButton(
                    count = entry.reactions.size,
                    isActive = hasReacted,
                    onClick = { viewModel.toggleReaction(entry.id) }
                )
                
                NudgeButton(
                    isNudgedToday = isNudgedToday,
                    onClick = { viewModel.sendNudge(entry.actorId) }
                )
            }
        }
    }
}

@Composable
fun ReactionButton(count: Int, isActive: Boolean, onClick: () -> Unit) {
    val containerColor = if (isActive) AccentPrimary.copy(alpha = 0.2f) else CardBackground
    val contentColor = if (isActive) AccentPrimary else TextSecondary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, if (isActive) AccentPrimary else CardBorderAlt),
        modifier = Modifier.height(40.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔥", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = count.toString(), color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NudgeButton(isNudgedToday: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !isNudgedToday,
        shape = RoundedCornerShape(20.dp),
        color = if (isNudgedToday) CardBackground.copy(alpha = 0.5f) else CardBackground,
        border = BorderStroke(1.dp, CardBorderAlt),
        modifier = Modifier.height(40.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (isNudgedToday) "Nudged today" else "Nudge",
                color = if (isNudgedToday) TextSecondary else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LeaderboardTab(
    leaderboard: List<UserProfileEntity>,
    selectedMetric: CommunityViewModel.LeaderboardMetric,
    viewModel: CommunityViewModel,
    state: UiState<List<UserProfileEntity>>,
    onRetry: () -> Unit,
    onAddFriend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricChip("XP", selectedMetric == CommunityViewModel.LeaderboardMetric.WEEKLY_XP) {
                viewModel.setLeaderboardMetric(CommunityViewModel.LeaderboardMetric.WEEKLY_XP)
            }
            MetricChip("Streak", selectedMetric == CommunityViewModel.LeaderboardMetric.STREAK) {
                viewModel.setLeaderboardMetric(CommunityViewModel.LeaderboardMetric.STREAK)
            }
            MetricChip("Level", selectedMetric == CommunityViewModel.LeaderboardMetric.LEVEL) {
                viewModel.setLeaderboardMetric(CommunityViewModel.LeaderboardMetric.LEVEL)
            }
        }

        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            }
            is UiState.Empty, is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(leaderboard.take(10)) { profile ->
                        LeaderboardItem(profile, selectedMetric, viewModel)
                    }

                    if (leaderboard.size <= 1) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "🚀 Invite friends to compete", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Compare streaks and aura XP on the leaderboard by adding friends.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onAddFriend,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Add Friend")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("⚠️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                        Text("Tap to retry")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentPrimary else CardBackground,
        border = BorderStroke(1.dp, if (isSelected) AccentPrimary else CardBorderAlt.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LeaderboardItem(profile: UserProfileEntity, metric: CommunityViewModel.LeaderboardMetric, viewModel: CommunityViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(HeaderGradient2Stop),
                contentAlignment = Alignment.Center
            ) {
                Text(text = profile.displayName.take(1).uppercase(), color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = "Lvl ${profile.currentLevel}", color = TextSecondary, fontSize = 12.sp)
            }
            
            val valueText = when(metric) {
                CommunityViewModel.LeaderboardMetric.WEEKLY_XP -> "${profile.totalAuraXp} XP"
                CommunityViewModel.LeaderboardMetric.STREAK -> "${profile.currentStreak}d 🔥"
                CommunityViewModel.LeaderboardMetric.LEVEL -> "Level ${profile.currentLevel}"
            }
            
            Text(text = valueText, color = AccentPrimary, fontWeight = FontWeight.Black)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(
                onClick = { viewModel.startDuel(profile.uid, "General") },
                modifier = Modifier.size(24.dp)
            ) {
                Text("⚔️", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ChallengesTabSection(
    challengesState: UiState<List<com.example.motionpulse.data.local.entity.Challenge>>,
    duelsState: UiState<List<com.example.motionpulse.data.local.entity.Duel>>,
    currentUserId: String,
    viewModel: CommunityViewModel,
    onAddFriend: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "⚔️ Habit Duels", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Duels are 1-on-1 habit competitions over 7 days. Challenge a friend and race to see who logs the most consistency!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddFriend,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Challenge a Friend")
                    }
                }
            }
        }

        item {
            Text(text = "Group Challenges", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        
        item {
            UiStateContainer(
                state = challengesState,
                onRetry = { viewModel.retry() },
                emptyMessage = "No active group challenges"
            ) { challenges ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    challenges.forEach { challenge ->
                        ChallengeCard(challenge)
                    }
                }
            }
        }

        item {
            Text(text = "Live Duels", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        item {
            UiStateContainer(
                state = duelsState,
                onRetry = { viewModel.retry() },
                emptyMessage = "No live duels. Challenge a friend!"
            ) { duels ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    duels.forEach { duel ->
                        DuelCard(duel, currentUserId)
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeCard(challenge: com.example.motionpulse.data.local.entity.Challenge) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = challenge.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = "Group challenge · Day 5 of ${challenge.durationDays}", color = TextSecondary, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(60.dp).height(32.dp)) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .padding(start = (i * 14).dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(HeaderGradient3Stop)
                                .border(2.dp, CardBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "?", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                val avgProgress = if (challenge.progress.isEmpty()) 0f else challenge.progress.values.average().toFloat()
                LinearProgressIndicator(
                    progress = { avgProgress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = AccentPrimary,
                    trackColor = Color.Gray.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun DuelCard(duel: com.example.motionpulse.data.local.entity.Duel, currentUserId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorderAlt),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Habit Duel: ${duel.habitType}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = if (duel.status == "ACTIVE") "Ends soon!" else "Duel Completed",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${duel.scores[currentUserId] ?: 0} vs ${duel.scores.values.sum() - (duel.scores[currentUserId] ?: 0)}",
                    color = AccentPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
                Text(text = "YOUR SCORE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✨", fontSize = 32.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun EmptyStateSmall(message: String) {
    Text(
        text = message,
        color = TextSecondary,
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        textAlign = TextAlign.Center
    )
}

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} mins ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
    }
}
