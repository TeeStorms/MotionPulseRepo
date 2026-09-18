package com.example.motionpulse.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.components.MotionPulseTextField
import com.example.motionpulse.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Edit Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Update your display name", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                MotionPulseTextField(value = name, onValueChange = { name = it }, placeholder = "Full Name")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("Save", color = AccentPrimary, fontWeight = FontWeight.Bold)
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

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: suspend (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Change Password", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (error != null) {
                    Text(error!!, color = Color.Red, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                MotionPulseTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    placeholder = "Current Password",
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(12.dp))
                MotionPulseTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = "New Password",
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            onConfirm(currentPassword, newPassword)
                            onDismiss()
                        } catch (e: Exception) {
                            error = "Current password is incorrect."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && currentPassword.isNotBlank() && newPassword.length >= 6
            ) {
                if (isLoading) CircularProgressIndicator(size(16.dp))
                else Text("Update", color = AccentPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = TextSecondary)
            }
        },
        modifier = Modifier.border(1.dp, CardBorderAlt, RoundedCornerShape(24.dp))
    )
}

@Composable
fun LanguageSelectorDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf("English", "Spanish", "French", "German")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Select Language", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(if (lang == currentLanguage) AccentPrimary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onSelect(lang); onDismiss() }
                            .padding(16.dp)
                    ) {
                        Text(lang, color = if (lang == currentLanguage) AccentPrimary else TextPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        modifier = Modifier.border(1.dp, CardBorderAlt, RoundedCornerShape(24.dp))
    )
}

private fun size(dp: androidx.compose.ui.unit.Dp) = Modifier.size(dp)
