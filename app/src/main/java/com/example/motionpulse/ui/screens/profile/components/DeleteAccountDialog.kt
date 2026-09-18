package com.example.motionpulse.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.components.MotionPulseTextField
import com.example.motionpulse.ui.theme.*

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmationText by remember { mutableStateOf("") }
    val isConfirmed = confirmationText.equals("DELETE", ignoreCase = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { 
            Text(
                text = "Delete Account?", 
                color = Color.Red, 
                fontWeight = FontWeight.Bold 
            ) 
        },
        text = {
            Column {
                Text(
                    text = "This action is permanent and will wipe all your data from our servers. This cannot be undone.",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Type 'DELETE' to confirm:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                MotionPulseTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    placeholder = "DELETE"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmed
            ) {
                Text(
                    text = "Permanently Delete", 
                    color = if (isConfirmed) Color.Red else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        modifier = Modifier.border(1.dp, CardBorderAlt, RoundedCornerShape(24.dp))
    )
}
