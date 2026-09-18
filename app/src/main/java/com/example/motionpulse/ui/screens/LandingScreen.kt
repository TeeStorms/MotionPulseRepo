package com.example.motionpulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.R
import com.example.motionpulse.ui.theme.BackgroundDark
import com.example.motionpulse.ui.theme.HeaderGradient3Stop
import com.example.motionpulse.ui.theme.TextPrimary
import kotlinx.coroutines.delay

import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.theme.AccentPrimary
import com.example.motionpulse.ui.theme.TextSecondary
import kotlinx.coroutines.*

@Composable
fun LandingScreen(
    viewModel: AuthViewModel,
    onTimeout: () -> Unit
) {
    var showLoadingFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Show feedback if check takes > 2s
        val feedbackJob = launch {
            delay(2000)
            showLoadingFeedback = true
        }

        // Run auth check and minimum delay in parallel
        val authJob = async { viewModel.checkSession() }
        val delayJob = async { delay(1200) }
        
        authJob.await()
        delayJob.await()
        
        feedbackJob.cancel()
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Motion Pulse Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MOTION.PULSE",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Pulsing Underline
                PulsingUnderline()

                if (showLoadingFeedback) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Checking your session...",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingUnderline() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val widthPercent by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "width"
    )

    Box(
        modifier = Modifier
            .width(120.dp * widthPercent)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(HeaderGradient3Stop)
    )
}
