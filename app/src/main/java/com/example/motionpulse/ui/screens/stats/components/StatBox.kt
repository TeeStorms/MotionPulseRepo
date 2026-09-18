package com.example.motionpulse.ui.screens.stats.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.*

@Composable
fun StatBox(
    value: String,
    label: String,
    trend: Int? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = CardBorderAlt,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (trend != null && trend != 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val isUp = trend > 0
                    Text(
                        text = if (isUp) "↑" else "↓",
                        color = if (isUp) AccentPrimary else Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
