package com.example.motionpulse.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.screens.dashboard.components.DashboardHeader
import com.example.motionpulse.ui.screens.dashboard.components.HabitCard
import com.example.motionpulse.ui.screens.dashboard.components.HabitCardSkeleton
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HabitsViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?,
    isSyncFailed: Boolean = false
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val habitsWithStatus by viewModel.habits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.badgeEvent.collect { badge ->
            snackbarHostState.showSnackbar("Achievement Unlocked: ${badge.name.replace("_", " ")}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MotionPulseBottomNav(
                currentRoute = currentRoute,
                onNavigate = onNavigateToNav
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshHabits() },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isSyncFailed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Yellow.copy(alpha = 0.2f))
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Local Mode: Cloud sync is disabled due to permissions. Fix rules to backup data.",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                DashboardHeader(
                    displayName = userProfile?.displayName ?: "User",
                    journeyDay = userProfile?.createdAt?.let { 
                        ChronoUnit.DAYS.between(it.atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()).toInt() + 1
                    } ?: 1
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Motivation Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardBorderAlt, RoundedCornerShape(20.dp))
                                .background(CardBackground, RoundedCornerShape(20.dp))
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Motivation of the day: ${viewModel.dailyQuote}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (habitsWithStatus.isEmpty() && !isLoading) "Ready to start?" else "Today's Habits",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            lastSyncedTime?.let {
                                Text(
                                    text = "Synced ${formatRelativeTime(it)}",
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (isLoading && habitsWithStatus.isEmpty()) {
                        items(3) {
                            HabitCardSkeleton()
                        }
                    } else if (habitsWithStatus.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "You haven't added any habits yet.",
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onNavigateToNav("habits") },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                                ) {
                                    Text("Add Your First Habit")
                                }
                            }
                        }
                    }

                    items(
                        items = habitsWithStatus,
                        key = { it.habit.id }
                    ) { item ->
                        HabitCard(
                            habit = item.habit,
                            isCompletedToday = item.isCompletedToday,
                            isSyncing = item.isSyncing,
                            isOverdue = item.isOverdue,
                            onToggleComplete = { viewModel.toggleHabitCompletion(item.habit) },
                            onRemoveProgress = { viewModel.removeHabitProgress(item.habit) },
                            onClick = { onNavigateToDetail(item.habit.id) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(time: Instant): String {
    val duration = Duration.between(time, Instant.now())
    return when {
        duration.toMinutes() < 1 -> "just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        else -> "today"
    }
}
