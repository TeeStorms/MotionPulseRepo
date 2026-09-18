package com.example.motionpulse.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.BackgroundDark
import com.example.motionpulse.ui.theme.TextPrimary
import com.example.motionpulse.ui.theme.TextSecondary
import com.example.motionpulse.ui.viewmodels.HabitsViewModel

@Composable
fun HabitDetailScreen(
    habitId: String,
    viewModel: HabitsViewModel,
    onBack: () -> Unit
) {
    // In a real app we'd fetch the specific habit
    val habit = null // viewModel.getHabit(habitId)

    Scaffold(
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Habit Detail",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ID: $habitId",
                color = TextSecondary,
                fontSize = 16.sp
            )
            
            // Stub content
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Completion history and detailed stats will appear here.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
