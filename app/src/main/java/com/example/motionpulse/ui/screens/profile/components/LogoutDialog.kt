package com.example.motionpulse.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(CardBackground, RoundedCornerShape(24.dp))
            .border(1.dp, CardBorderAlt, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Log Out",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Are you sure you want to log out of Motion.Pulse?",
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileLogoutBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Log Out", color = TextPrimary)
                }
            }
        }
    }
}
