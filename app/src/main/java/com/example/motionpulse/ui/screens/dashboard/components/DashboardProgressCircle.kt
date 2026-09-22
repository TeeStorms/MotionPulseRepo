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
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Circle (Track)
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(200.dp),
            color = TextSecondary.copy(alpha = 0.1f),
            strokeWidth = 16.dp,
            strokeCap = StrokeCap.Round,
        )
        
        // Foreground Circle (Progress)
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(200.dp),
            color = AccentPrimary,
            strokeWidth = 16.dp,
            strokeCap = StrokeCap.Round,
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$doneCount/$totalCount",
                color = TextPrimary,
                fontSize = 48.sp,
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
