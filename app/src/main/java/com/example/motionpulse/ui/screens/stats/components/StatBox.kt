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
import androidx.compose.ui.text.style.TextOverflow
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
    // Step-down font sizing based on value length to prevent clipping on small screens
    val valueFontSize = when {
        value.length > 7 -> 16.sp
        value.length > 5 -> 18.sp
        value.length > 4 -> 20.sp
        else -> 24.sp
    }

    Card(
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    color = CardBorderAlt,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                if (trend != null && trend != 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val isUp = trend > 0
                    Text(
                        text = if (isUp) "↑" else "↓",
                        color = if (isUp) AccentPrimary else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
