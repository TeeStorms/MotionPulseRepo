package com.example.motionpulse.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.screens.dashboard.components.*
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HabitsViewModel,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?,
    isSyncFailed: Boolean = false
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val habitsWithStatus by viewModel.habits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val todayMood by viewModel.todayMood.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isExpanded by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.fillMaxSize()) {
                MotionPulseHeader(
                    title = "Welcome back,\n${userProfile?.displayName ?: "User"}",
                    showProfileIcon = true,
                    todayMood = todayMood
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    item {
                        if (isSyncFailed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Yellow.copy(alpha = 0.2f))
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Local Mode: Cloud sync is disabled. Fix rules to backup data.",
                                    color = Color.DarkGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    item {
                        val doneCount = habitsWithStatus.count { it.isCompletedToday }
                        DashboardProgressCircle(
                            doneCount = doneCount,
                            totalCount = habitsWithStatus.size
                        )
                    }

                    item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Motivation & Reflection Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorderAlt, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "💡", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "MOTIVATION OF THE DAY",
                                    color = TextPrimary.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = viewModel.dailyQuote,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            )
                            
                            todayMood?.note?.let { note ->
                                if (note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = CardBorderAlt.copy(alpha = 0.2f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(text = "📝", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "YOUR REFLECTION",
                                                color = AccentPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = note,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Today's habits",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    } else {
                        // Expansion logic
                        val visibleHabits = if (isExpanded) habitsWithStatus else habitsWithStatus.take(1)
                        
                        items(visibleHabits, key = { it.habit.id }) { item ->
                            DashboardHabitCard(
                                habit = item.habit,
                                isCompleted = item.isCompletedToday,
                                loggedAt = item.loggedAt,
                                onToggle = { viewModel.toggleHabitCompletion(item.habit) },
                                onRemove = { viewModel.removeHabitProgress(item.habit) }
                            )
                        }

                        if (!isExpanded && habitsWithStatus.size > 1) {
                            item {
                                TextButton(
                                    onClick = { isExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(contentColor = AccentPrimary)
                                ) {
                                    Text("Show more", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
