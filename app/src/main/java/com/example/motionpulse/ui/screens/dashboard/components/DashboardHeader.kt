package com.example.motionpulse.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.HeaderGradient3Stop
import com.example.motionpulse.ui.theme.TextPrimary

@Composable
fun DashboardHeader(
    displayName: String,
    journeyDay: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(HeaderGradient3Stop)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Welcome back $displayName! \uD83D\uDC4B",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Day $journeyDay of your journey",
                color = TextPrimary.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
