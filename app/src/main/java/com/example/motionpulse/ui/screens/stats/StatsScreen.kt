package com.example.motionpulse.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.screens.dashboard.components.DashboardHeader
import com.example.motionpulse.ui.screens.stats.components.HabitConsistencyCard
import com.example.motionpulse.ui.screens.stats.components.StatBox
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.StatsViewMode
import com.example.motionpulse.ui.viewmodels.StatsViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val summaryStats by viewModel.summaryStats.collectAsState()
    val habitConsistencyStats by viewModel.habitConsistencyStats.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var isCelebrating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.celebrationEvent.collect {
            if (!isCelebrating) {
                isCelebrating = true
                val result = snackbarHostState.showSnackbar(
                    message = "🎉 PERFECT RHYTHM! You hit 100% this week!",
                    duration = SnackbarDuration.Short
                )
                isCelebrating = false
            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                    
                    // Summary Stats Grid
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatBox(
                            value = "${summaryStats.doneToday}/${summaryStats.totalHabits}",
                            label = "Done Today",
                            modifier = Modifier.weight(1f).height(100.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        StatBox(
                            value = "${summaryStats.currentStreak}d",
                            label = "Current Streak",
                            modifier = Modifier.weight(1f).height(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatBox(
                            value = "${summaryStats.totalCompleted}",
                            label = "Total Completed",
                            modifier = Modifier.weight(1f).height(100.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        StatBox(
                            value = "${summaryStats.weeklyPercent}%",
                            label = "This week",
                            trend = summaryStats.weeklyTrend,
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .border(
                                    if (isCelebrating) 2.dp else 0.dp, 
                                    if (isCelebrating) AccentPrimary else Color.Transparent, 
                                    RoundedCornerShape(16.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consistency",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Week/Month Toggle
                        Row(
                            modifier = Modifier
                                .background(CardBackground, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            StatsToggleItem(
                                label = "W",
                                isSelected = viewMode == StatsViewMode.WEEK,
                                onClick = { viewModel.setViewMode(StatsViewMode.WEEK) }
                            )
                            StatsToggleItem(
                                label = "M",
                                isSelected = viewMode == StatsViewMode.MONTH,
                                onClick = { viewModel.setViewMode(StatsViewMode.MONTH) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(
                    items = habitConsistencyStats,
                    key = { it.habit.id }
                ) { stats ->
                    HabitConsistencyCard(
                        habit = stats.habit,
                        dayStatuses = stats.dayStatuses,
                        completedCount = stats.completedCount,
                        viewMode = viewMode,
                        onCellClick = { date ->
                            val status = stats.dayStatuses.find { it.date == date }?.status
                            if (status == com.example.motionpulse.data.local.entity.CompletionStatus.COMPLETED) {
                                viewModel.removeHabitProgress(stats.habit, date)
                            } else {
                                viewModel.logHabitProgress(stats.habit, date)
                            }
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun StatsToggleItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(32.dp)
            .background(
                if (isSelected) AccentPrimary else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
