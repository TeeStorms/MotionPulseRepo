package com.example.motionpulse.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.GoalType
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.ui.theme.*

@Composable
fun HabitCard(
    habit: HabitEntity,
    isCompletedToday: Boolean,
    isSyncing: Boolean = false,
    isOverdue: Boolean = false,
    onToggleComplete: (() -> Unit)? = null,
    onRemoveProgress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = CardBackground,
            title = { Text(text = "Remove Habit Completion?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    text = "Would you like to continue to remove habit completion or cancel?",
                    color = TextSecondary
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRemoveProgress?.invoke()
                        showRemoveDialog = false
                    }
                ) {
                    Text("Continue", color = AccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
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
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = CardBorderAlt,
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Dot (Quick Complete)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompletedToday) DashboardCompletedDot 
                        else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCompletedToday) Color.Transparent else DashboardActiveDot.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .then(
                        if (onToggleComplete != null) {
                            Modifier.clickable {
                                if (isCompletedToday) {
                                    showRemoveDialog = true
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleComplete()
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isSyncing) "Syncing..." else habit.category,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "+ Badge", // Placeholder for next milestone
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
