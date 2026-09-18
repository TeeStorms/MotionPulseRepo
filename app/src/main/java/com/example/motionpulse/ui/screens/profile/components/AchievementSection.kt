package com.example.motionpulse.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.BadgeEntity
import com.example.motionpulse.ui.theme.*
import java.time.Instant
import java.time.Duration

@Composable
fun AchievementSection(
    badges: List<BadgeEntity>,
    onSeeAll: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProfileAchievementsBg, RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Badges",
                color = TextPrimary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            
            if (badges.isNotEmpty()) {
                Text(
                    text = "See all",
                    color = AccentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSeeAll() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (badges.isEmpty()) {
            Text(
                text = "No achievements yet",
                color = TextSecondary,
                fontSize = 16.sp
            )
        } else {
            badges.take(2).forEach { badge ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badge.badgeType.name.replace("_", " ").lowercase().capitalize(),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRelativeTime(badge.unlockedAt),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(time: Instant): String {
    val duration = Duration.between(time, Instant.now())
    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()} mins ago"
        duration.toHours() < 24 -> "${duration.toHours()} hours ago"
        else -> "${duration.toDays()} days ago"
    }
}
