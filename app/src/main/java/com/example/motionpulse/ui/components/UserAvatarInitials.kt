package com.example.motionpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.HeaderGradient2Stop
import com.example.motionpulse.ui.theme.ProfileAvatarBadge

@Composable
fun UserAvatarInitials(
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showStatusDot: Boolean = false
) {
    val trimmed = displayName?.trim().orEmpty()
    val initials = when {
        trimmed.isEmpty() -> "?"
        trimmed.length == 1 -> trimmed.uppercase()
        else -> trimmed.take(2).uppercase()
    }

    val textSize = (size.value * 0.4f).sp

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(HeaderGradient2Stop),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.Bold
            )
        }

        if (showStatusDot) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(ProfileAvatarBadge)
                    .border(1.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
