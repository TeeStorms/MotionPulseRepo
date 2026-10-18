package com.example.motionpulse.ui.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.AccentPrimary
import com.example.motionpulse.ui.theme.TextPrimary
import com.example.motionpulse.ui.theme.TextSecondary

@Composable
fun DashboardProgressCircle(
    doneCount: Int,
    totalCount: Int
) {
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        val circleSize = min(240.dp, maxWidth * 0.6f)

        Box(
            modifier = Modifier.size(circleSize),
            contentAlignment = Alignment.Center
        ) {
            // Background Circle (Track)
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = TextSecondary.copy(alpha = 0.1f),
                strokeWidth = 16.dp,
                strokeCap = StrokeCap.Round,
            )

            // Foreground Circle (Progress)
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = AccentPrimary,
                strokeWidth = 16.dp,
                strokeCap = StrokeCap.Round,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$doneCount/$totalCount",
                    color = TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "habits today",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
