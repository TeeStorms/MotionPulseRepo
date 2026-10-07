package com.example.motionpulse.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.HeaderGradient2Stop
import com.example.motionpulse.ui.theme.ProfileAvatarBadge
import com.example.motionpulse.ui.theme.TextPrimary
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant

@Composable
fun ProfileHeader(
    displayName: String,
    avatarUrl: String? = null,
    createdAt: Instant? = null
) {
    val memberSince = remember(createdAt) {
        createdAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(HeaderGradient2Stop)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar with Badge
            com.example.motionpulse.ui.components.UserAvatarInitials(
                displayName = displayName,
                size = 64.dp,
                showStatusDot = true
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                
                memberSince?.let {
                    Text(
                        text = "Member since $it",
                        color = TextPrimary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
