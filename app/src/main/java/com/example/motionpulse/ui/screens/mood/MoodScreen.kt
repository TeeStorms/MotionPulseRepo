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
import com.example.motionpulse.ui.screens.dashboard.components.MotionPulseHeader
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
    val saveError by viewModel.saveError.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    var selectedLevel by remember { mutableStateOf<MoodLevel?>(null) }
    var selectedFactor by remember { mutableStateOf<MoodFactor?>(null) }
    var note by remember { mutableStateOf("") }
    var showSuccessPopup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            showSuccessPopup = true
            focusManager.clearFocus()
            // We don't manually clear the 'note' variable here because 
            // the screen is in 'Update' mode once saved, and we want 
            // the user to see their saved reflection if they return.
        }
    }

    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(todayMood) {
        todayMood?.let {
            selectedLevel = it.moodLevel
            selectedFactor = it.factors.firstOrNull()
            note = it.note ?: ""
        }
    }

    if (showSuccessPopup) {
        AlertDialog(
            onDismissRequest = { showSuccessPopup = false },
            containerColor = CardBackground,
            title = { 
                Text(
                    text = "Rhythm Logged!", 
                    color = TextPrimary, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) 
            },
            text = { 
                Text(
                    text = "Your daily mood has been saved successfully.", 
                    color = TextSecondary 
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = { showSuccessPopup = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentPrimary)
                ) {
                    Text("Great", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, CardBorderAlt.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            MotionPulseHeader(
                title = "How's your rhythm\ntoday?",
                subtitle = "MOTION.PULSE"
            )

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
                        .height(180.dp), // Increased from 160.dp to prevent clipping tallest bar labels
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    MoodLevel.entries.forEachIndexed { index, level ->
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
                                text = level.name.lowercase().replaceFirstChar { it.uppercase() },
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
                            text = selectedLevel?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
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
                            items(MoodFactor.entries) { factor ->
                                val isSelected = selectedFactor == factor
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFactor = if (isSelected) null else factor
                                    },
                                    label = { Text(factor.name.lowercase().replaceFirstChar { it.uppercase() }) },
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
                                    viewModel.logMood(it, listOfNotNull(selectedFactor), note.ifBlank { null })
                                }
                            },
                            isLoading = isSaving,
                            enabled = selectedLevel != null && !isSaving
                        )
                    }
                }
            }
        }
    }
}
