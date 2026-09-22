package com.example.motionpulse.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.MoodEntity
import com.example.motionpulse.ui.theme.*

@Composable
fun MotionPulseHeader(
    title: String,
    subtitle: String? = null,
    showProfileIcon: Boolean = false,
    todayMood: MoodEntity? = null,
    gradient: Brush = HeaderGradient3Stop
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp) // Slightly taller for better spacing
            .background(gradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showProfileIcon) {
                // Profile Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Mood Badge
            if (todayMood != null) {
                Spacer(modifier = Modifier.height(20.dp))
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
}

private fun getMoodColor(level: com.example.motionpulse.data.local.entity.MoodLevel): Color {
    return when (level) {
        com.example.motionpulse.data.local.entity.MoodLevel.ENERGIZED -> Color(0xFFE040FB) // Vibrant Purple
        com.example.motionpulse.data.local.entity.MoodLevel.GOOD -> Color(0xFFFF2A85)      // Hot Pink (AccentPrimary)
        com.example.motionpulse.data.local.entity.MoodLevel.STEADY -> Color(0xFF4FC3F7)    // Apps Blue (AccentBlue)
        com.example.motionpulse.data.local.entity.MoodLevel.LOW -> Color(0xFFFFB74D)       // Muted Orange
        com.example.motionpulse.data.local.entity.MoodLevel.DRAINED -> Color(0xFF607D8B)   // Steel Gray
    }
}
