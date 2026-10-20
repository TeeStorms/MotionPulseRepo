package com.example.motionpulse.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.MoodEntity
import com.example.motionpulse.ui.components.GradientHeader
import com.example.motionpulse.ui.theme.*

@Composable
fun MotionPulseHeader(
    title: String,
    subtitle: String? = null,
    showProfileIcon: Boolean = false,
    userName: String? = null,
    todayMood: MoodEntity? = null,
    gradient: Brush = HeaderGradient3Stop,
    topBarContent: (@Composable () -> Unit)? = null
) {
    GradientHeader(
        title = title,
        subtitle = subtitle,
        gradient = gradient,
        showProfileAvatar = showProfileIcon,
        userName = userName ?: title,
        topBarContent = topBarContent
    ) {
        if (todayMood != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val moodColor = getMoodColor(todayMood.moodLevel)
            Surface(
                color = Color.Black.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(moodColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = todayMood.moodLevel.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getMoodColor(level: com.example.motionpulse.data.local.entity.MoodLevel): Color {
    return when (level) {
        com.example.motionpulse.data.local.entity.MoodLevel.ENERGIZED -> Color(0xFFE040FB)
        com.example.motionpulse.data.local.entity.MoodLevel.GOOD -> Color(0xFFFF2A85)
        com.example.motionpulse.data.local.entity.MoodLevel.STEADY -> Color(0xFF4FC3F7)
        com.example.motionpulse.data.local.entity.MoodLevel.LOW -> Color(0xFFFFB74D)
        com.example.motionpulse.data.local.entity.MoodLevel.DRAINED -> Color(0xFF607D8B)
    }
}
