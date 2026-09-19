package com.example.motionpulse.ui.screens.mood

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.MoodFactor
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.ui.components.GradientButton
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.components.MotionPulseTextField
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.MoodViewModel

@Composable
fun MoodScreen(
    viewModel: MoodViewModel,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?
) {
    val todayMood by viewModel.todayMood.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    var selectedLevel by remember { mutableStateOf<MoodLevel?>(null) }
    var selectedFactors by remember { mutableStateOf<Set<MoodFactor>>(emptySet()) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(todayMood) {
        todayMood?.let {
            selectedLevel = it.moodLevel
            selectedFactors = it.factors.toSet()
            note = it.note ?: ""
        }
    }

    Scaffold(
        bottomBar = {
            MotionPulseBottomNav(
                currentRoute = currentRoute,
                onNavigate = onNavigateToNav
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(HeaderGradient3Stop)
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = "MOTION.PULSE",
                        color = TextPrimary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "How's your rhythm\ntoday?",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rising Bars Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    MoodLevel.values().forEachIndexed { index, level ->
                        val isSelected = selectedLevel == level
                        val barHeight = 40.dp + (index * 25).dp
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = if (isSelected) HeaderGradient2Stop 
                                        else androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.3f))
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedLevel = level
                                    }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = level.name.lowercase().capitalize(),
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(visible = selectedLevel != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedLevel?.name?.lowercase()?.capitalize() ?: "",
                            color = AccentPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Tap a beat to log how you feel right now",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "What's shaping it?",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(MoodFactor.values()) { factor ->
                                val isSelected = selectedFactors.contains(factor)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFactors = if (isSelected) {
                                            selectedFactors - factor
                                        } else {
                                            selectedFactors + factor
                                        }
                                    },
                                    label = { Text(factor.name.lowercase().capitalize()) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = CardBackground,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = CardBorderAlt,
                                        selectedBorderColor = AccentPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        MotionPulseTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = "Add a note — optional"
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        GradientButton(
                            text = if (todayMood != null) "Update mood" else "Log mood",
                            onClick = {
                                selectedLevel?.let {
                                    viewModel.logMood(it, selectedFactors.toList(), note.ifBlank { null })
                                }
                            },
                            isLoading = isSaving
                        )
                    }
                }
            }
        }
    }
}
