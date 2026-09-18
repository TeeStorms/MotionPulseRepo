package com.example.motionpulse.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.R
import com.example.motionpulse.ui.components.GradientButton
import com.example.motionpulse.ui.theme.BackgroundDark
import com.example.motionpulse.ui.theme.TextPrimary
import com.example.motionpulse.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Master Your Habits",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Build your rhythm. Track your habits.\nPulse through your day.",
            color = TextSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(64.dp))

        GradientButton(
            text = "Get Started",
            onClick = onGetStarted
        )
    }
}
