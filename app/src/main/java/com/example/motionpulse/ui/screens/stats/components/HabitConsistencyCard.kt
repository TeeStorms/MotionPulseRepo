package com.example.motionpulse.ui.screens.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.DayStatus
import com.example.motionpulse.ui.viewmodels.StatsViewMode
import java.time.LocalDate

@Composable
fun HabitConsistencyCard(
    habit: HabitEntity,
    dayStatuses: List<DayStatus>,
    completedCount: Int,
    viewMode: StatsViewMode,
    onCellClick: (LocalDate) -> Unit = {}
) {
    val today = LocalDate.now()
    var selectedDay by remember { mutableStateOf<DayStatus?>(null) }
    var showRemoveDialogForDate by remember { mutableStateOf<LocalDate?>(null) }

    val habitColor = AccentPrimary
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    if (showRemoveDialogForDate != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialogForDate = null },
            containerColor = CardBackground,
            title = { Text(text = "Remove progress?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    text = "This will remove your completion for ${showRemoveDialogForDate.toString()} and reverse any streaks or XP gained.",
                    color = TextSecondary
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onCellClick(showRemoveDialogForDate!!) // confirm logic in StatsViewModel
                        showRemoveDialogForDate = null
                    }
                ) {
                    Text("Remove", color = AccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialogForDate = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            modifier = Modifier.border(1.dp, CardBorderAlt, RoundedCornerShape(24.dp))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, CardBorderAlt, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedCount ${if (viewMode == StatsViewMode.WEEK) "/7" else ""} days",
                    color = habitColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid logic
            if (viewMode == StatsViewMode.WEEK) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    dayStatuses.forEachIndexed { index, status ->
                        DayCell(
                            label = dayLabels[index],
                            status = status,
                            today = today,
                            habitColor = habitColor,
                            onClick = { 
                                val sevenDaysAgo = today.minusDays(7)
                                if (!status.date.isAfter(today) && !status.date.isBefore(sevenDaysAgo)) {
                                    if (status.status == CompletionStatus.COMPLETED) {
                                        showRemoveDialogForDate = status.date
                                    } else {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onCellClick(status.date)
                                    }
                                } else {
                                    selectedDay = status
                                }
                            }
                        )
                    }
                }
            } else {
                // Monthly grid (7 columns)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val chunks = dayStatuses.chunked(7)
                    chunks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            week.forEach { status ->
                                DayCell(
                                    label = status.date.dayOfMonth.toString(),
                                    status = status,
                                    today = today,
                                    habitColor = habitColor,
                                    size = 28.dp,
                                    onClick = { 
                                        val sevenDaysAgo = today.minusDays(7)
                                        if (!status.date.isAfter(today) && !status.date.isBefore(sevenDaysAgo)) {
                                            if (status.status == CompletionStatus.COMPLETED) {
                                                showRemoveDialogForDate = status.date
                                            } else {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                onCellClick(status.date)
                                            }
                                        } else {
                                            selectedDay = status
                                        }
                                    }
                                )
                            }
                            // Fill empty slots if last week is short
                            repeat(7 - week.size) {
                                Spacer(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Tooltip
    selectedDay?.let { day ->
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = { selectedDay = null }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier
                    .border(1.dp, CardBorderAlt, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = day.date.toString(), color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (day.status) {
                            CompletionStatus.COMPLETED -> "✅ Completed"
                            CompletionStatus.MISSED -> "❌ Missed"
                            CompletionStatus.SKIPPED -> "⏭️ Skipped"
                        },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    label: String,
    status: DayStatus,
    today: LocalDate,
    habitColor: Color,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    onClick: () -> Unit
) {
    val isFuture = status.date.isAfter(today)
    val isCompleted = status.status == CompletionStatus.COMPLETED
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    color = if (isCompleted) habitColor else Color.Transparent,
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isCompleted) habitColor else CardBorderAlt.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .alpha(if (isFuture) 0.3f else 1f)
                .clickable(
                    enabled = true,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isFuture) TextSecondary.copy(alpha = 0.3f) else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
