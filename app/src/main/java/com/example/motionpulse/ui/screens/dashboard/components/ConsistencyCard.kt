package com.example.motionpulse.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.AccentPrimary
import com.example.motionpulse.ui.theme.CardBackground
import com.example.motionpulse.ui.theme.TextPrimary

@Composable
fun ConsistencyCard(
    streakDays: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentPrimary)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Consistency is key — you've kept a rhythm going for $streakDays days.",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
        }
    }
}
