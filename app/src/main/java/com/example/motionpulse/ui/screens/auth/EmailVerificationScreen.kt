package com.example.motionpulse.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.components.GradientButton
import com.example.motionpulse.ui.screens.auth.components.AuthOverlay
import com.example.motionpulse.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationScreen(
    email: String,
    viewModel: AuthViewModel,
    onVerificationSuccess: (String) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val resendTimer by viewModel.resendTimer.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            showSuccessFeedback = true
            delay(800)
            onVerificationSuccess((authState as AuthState.Authenticated).uid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📩",
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Check Your Inbox",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We sent a verification link to:",
                color = TextSecondary,
                fontSize = 16.sp
            )
            
            Text(
                text = email,
                color = AccentPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .background(CardBackground, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Please click the link in the email to verify your account. If you don't see it, check your spam folder.",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            GradientButton(
                text = "I've verified — Continue",
                onClick = { 
                    errorMessage = null
                    viewModel.checkVerificationStatus(
                        onStillUnverified = {
                            errorMessage = "Still not verified — please click the link in your email first"
                        }
                    ) 
                },
                isLoading = false // Handled by overlay
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { viewModel.resendVerificationEmail() },
                enabled = resendTimer == 0
            ) {
                Text(
                    text = if (resendTimer > 0) "Resend in ${resendTimer}s" else "Resend Verification Email",
                    color = if (resendTimer > 0) TextSecondary else AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            TextButton(onClick = { viewModel.logout() }) {
                Text("Back to Login", color = TextSecondary)
            }
        }

        if (authState is AuthState.Verifying || showSuccessFeedback) {
            AuthOverlay(isSuccess = showSuccessFeedback)
        }
    }
}
