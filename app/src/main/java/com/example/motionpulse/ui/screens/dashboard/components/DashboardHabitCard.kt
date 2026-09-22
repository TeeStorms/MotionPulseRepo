package com.example.motionpulse.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.ui.theme.*
import java.time.Duration
import java.time.Instant

@Composable
fun DashboardHabitCard(
    habit: HabitEntity,
    isCompleted: Boolean,
    loggedAt: Instant? = null,
    onToggle: () -> Unit,
    onRemove: () -> Unit
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
                        onRemove()
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
            .border(
                width = 1.dp,
                color = if (isCompleted) AccentPrimary else CardBorderAlt.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        onClick = {
            if (isCompleted) {
                showRemoveDialog = true
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Icon
                Icon(
                    imageVector = if (isCompleted) Icons.Outlined.MonitorHeart else Icons.Outlined.Remove,
                    contentDescription = null,
                    tint = if (isCompleted) AccentPrimary else TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    Text(
                        text = habit.title,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCompleted && loggedAt != null) "Logged ${formatTimeAgo(loggedAt)}" else "Not logged yet",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            // Action Button
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryButtonGradient)
                    .height(36.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Badge",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(time: Instant): String {
    val duration = Duration.between(time, Instant.now())
    return when {
        duration.toMinutes() < 1 -> "just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()} mins ago"
        duration.toHours() < 24 -> "${duration.toHours()} hours ago"
        else -> "today"
    }
}
