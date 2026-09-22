package com.example.motionpulse.ui.screens.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.ActivityFeedEntry
import com.example.motionpulse.data.local.entity.FeedEventType
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.screens.dashboard.components.MotionPulseHeader
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?
) {
    val feed by viewModel.feed.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val challenges by viewModel.challenges.collectAsState()
    val duels by viewModel.duels.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Feed", "Leaderboard", "Duels")

    Scaffold(
        bottomBar = {
            MotionPulseBottomNav(
                currentRoute = currentRoute,
                onNavigate = onNavigateToNav
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            MotionPulseHeader(
                title = "Pulse Community",
                subtitle = "MOTION.PULSE"
            )

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
                0 -> FeedTab(feed, userProfile?.uid ?: "", viewModel)
                1 -> LeaderboardTab(leaderboard)
                2 -> ChallengesTab(challenges, duels, userProfile?.uid ?: "", viewModel)
            }
        }
    }
}

@Composable
fun FeedTab(feed: List<ActivityFeedEntry>, currentUserId: String, viewModel: CommunityViewModel) {
    if (feed.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No activity yet. Add some friends!", color = TextSecondary)
        }
    } else {
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

@Composable
fun FeedItem(entry: ActivityFeedEntry, currentUserId: String, viewModel: CommunityViewModel) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(HeaderGradient2Stop),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.actorName.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
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
                
                NudgeButton(onClick = { viewModel.sendNudge(entry.actorId) })
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
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔥", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = count.toString(), color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NudgeButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, CardBorderAlt),
        modifier = Modifier.height(40.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text = "Nudge", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun LeaderboardTab(leaderboard: List<UserProfileEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(leaderboard.take(10)) { profile ->
            LeaderboardItem(profile)
        }
    }
}

@Composable
fun LeaderboardItem(profile: UserProfileEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorderAlt),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HeaderGradient2Stop),
                contentAlignment = Alignment.Center
            ) {
                Text(text = profile.displayName.take(1).uppercase(), color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = "Lvl ${profile.currentLevel}", color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = "${profile.totalAuraXp} XP",
                color = AccentPrimary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ChallengesTab(
    challenges: List<com.example.motionpulse.data.local.entity.Challenge>,
    duels: List<com.example.motionpulse.data.local.entity.Duel>,
    currentUserId: String,
    viewModel: CommunityViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(text = "Active Challenges", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        
        items(challenges) { challenge ->
            ChallengeCard(challenge)
        }

        item {
            Text(text = "Live Duels", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        items(duels) { duel ->
            DuelCard(duel, currentUserId)
        }
    }
}

@Composable
fun ChallengeCard(challenge: com.example.motionpulse.data.local.entity.Challenge) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorderAlt),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = challenge.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Group challenge · Day 5 of ${challenge.durationDays}", color = TextSecondary, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            val avgProgress = challenge.progress.values.average().toFloat()
            LinearProgressIndicator(
                progress = { avgProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = AccentPrimary,
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun DuelCard(duel: com.example.motionpulse.data.local.entity.Duel, currentUserId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorderAlt),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Habit Duel: ${duel.habitType}", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = "First to ${duel.durationDays} days wins!", color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = "${duel.scores[currentUserId] ?: 0} vs ${(duel.scores.values.sum() - (duel.scores[currentUserId] ?: 0))}",
                color = AccentPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
        }
    }
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
