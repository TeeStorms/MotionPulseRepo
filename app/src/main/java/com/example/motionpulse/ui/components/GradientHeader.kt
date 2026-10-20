package com.example.motionpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.AdaptivePreviews
import com.example.motionpulse.ui.theme.HeaderGradient3Stop
import com.example.motionpulse.ui.theme.MotionPulseTheme
import com.example.motionpulse.ui.theme.TextPrimary

@Composable
fun GradientHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    gradient: Brush = HeaderGradient3Stop,
    showProfileAvatar: Boolean = false,
    userName: String? = null,
    topBarContent: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    CapFontScale(maxFontScale = 1.3f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 220.dp)
                .background(gradient)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (topBarContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    topBarContent()
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (showProfileAvatar && userName != null) {
                    UserAvatarInitials(
                        displayName = userName,
                        size = 52.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (subtitle != null && topBarContent == null) {
                    Text(
                        text = subtitle.uppercase(),
                        color = TextPrimary.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                content?.invoke(this)
            }
        }
    }
}

@AdaptivePreviews
@Composable
private fun GradientHeaderPreview() {
    MotionPulseTheme {
        GradientHeader(
            title = "Pulse Community",
            subtitle = "MOTION.PULSE"
        )
    }
}
