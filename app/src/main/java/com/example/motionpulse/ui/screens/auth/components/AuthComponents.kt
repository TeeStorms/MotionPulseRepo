package com.example.motionpulse.ui.screens.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.*

@Composable
fun AuthOverlay(isSuccess: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        if (isSuccess) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(1.dp, AccentPrimary, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandedSuccessIcon()
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            text = "Welcome Back!",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "You have successfully signed in.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .border(1.dp, AccentPrimary, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Signing you in...", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BrandedSuccessIcon() {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AccentPrimary.copy(alpha = 0.7f),
                        AccentPrimary,
                        AccentPrimary.copy(alpha = 0.8f)
                    )
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun VerificationRequiredPanel(
    resendTimer: Int,
    onResend: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Verification Required",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your account exists, but this email hasn't been verified yet.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onResend,
                enabled = resendTimer == 0
            ) {
                Text(
                    text = if (resendTimer > 0) "Resend in ${resendTimer}s" else "Resend Verification Email",
                    color = if (resendTimer > 0) TextSecondary else AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
