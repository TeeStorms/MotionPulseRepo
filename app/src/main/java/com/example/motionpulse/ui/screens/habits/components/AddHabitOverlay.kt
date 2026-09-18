package com.example.motionpulse.ui.screens.habits.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.GoalType
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.ui.components.GradientButton
import com.example.motionpulse.ui.components.HabitGoalStepper
import com.example.motionpulse.ui.components.MotionPulseTextField
import com.example.motionpulse.ui.theme.*
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitOverlay(
    habitToEdit: HabitEntity? = null,
    onDismiss: () -> Unit,
    viewModel: com.example.motionpulse.ui.viewmodels.HabitsViewModel
) {
    val scrollState = rememberScrollState()
    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: "Mind & Focus") }
    var customCategory by remember { mutableStateOf("") }
    var isOtherSelected by remember { mutableStateOf(false) }
    
    var frequency by remember { mutableStateOf(habitToEdit?.frequencyType ?: FrequencyType.EVERY_DAY) }
    val initialConfig = habitToEdit?.frequencyConfig?.let { FrequencyConfig.decodeSafe(it) }
    var selectedFrequencyPreset by remember {
        mutableStateOf(
            when {
                habitToEdit?.frequencyType == FrequencyType.EVERY_DAY -> "Everyday"
                habitToEdit?.frequencyType == FrequencyType.X_TIMES_PER_WEEK -> "${initialConfig?.timesPerWeek ?: 3} x a week"
                else -> "Everyday"
            }
        )
    }
    var goalValue by remember { mutableStateOf(habitToEdit?.numericTarget?.toString()?.removeSuffix(".0") ?: "1") }
    
    val standardUnits = listOf("reps", "minutes", "pages", "glasses", "steps")
    var selectedUnit by remember { mutableStateOf(habitToEdit?.numericUnit ?: "reps") }
    var customUnit by remember { mutableStateOf(if (habitToEdit?.numericUnit != null && habitToEdit.numericUnit !in standardUnits) habitToEdit.numericUnit else "") }
    var isCustomUnitSelected by remember { mutableStateOf(customUnit.isNotEmpty()) }
    
    var reminderTime by remember { mutableStateOf(habitToEdit?.reminderTime ?: "") }
    
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    
    val isEditMode = habitToEdit != null
    val isDuplicate = viewModel.isNameDuplicate(title, habitToEdit?.id)

    // Ensures that the "Add" or "Save" button is only enabled when the form is valid and not already saving.
    val finalUnit = if (isCustomUnitSelected) customUnit else selectedUnit
    val isEnabled = title.isNotBlank() && 
            (!isOtherSelected || customCategory.isNotBlank()) && 
            !isDuplicate && 
            !isSaving

    /**
     * Prevents accidental loss of data by prompting the user before dismissing a modified form.
     */
    val handleDismiss = {
        if (title.isNotBlank() && title != habitToEdit?.title && !isSaving) {
            showDiscardConfirm = true
        } else if (!isSaving) {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = handleDismiss,
        containerColor = BackgroundDark, // Uses a deep background to contrast the main habits list.
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.5f)) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = handleDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            }

            Text(
                text = if (isEditMode) "Edit Habit" else "Add New Habit",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CATEGORIES",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("Mind & Focus", "Fitness and Health", "Daily Routine", "Other")
                categories.forEach { cat ->
                    val isActive = if (cat == "Other") isOtherSelected else (selectedCategory == cat && !isOtherSelected)
                    CategoryTag(
                        label = cat, // Displays truncated labels to ensure consistent fit within the category row.
                        isActive = isActive,
                        onClick = {
                            if (cat == "Other") {
                                isOtherSelected = true
                            } else {
                                isOtherSelected = false
                                selectedCategory = cat
                            }
                        }
                    )
                }
            }

            if (isOtherSelected) {
                Spacer(modifier = Modifier.height(16.dp))
                MotionPulseTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    placeholder = "Custom Category Name"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            MotionPulseTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Habit Name (e.g., Read a book)"
            )

            if (isDuplicate) {
                Text(
                    text = "⚠️ You already have an active habit with this name.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Frequency",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedCard(
                    onClick = { showFrequencyDropdown = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = AddHabitInputBg),
                    border = BorderStroke(1.dp, CardBorderAlt)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = selectedFrequencyPreset, color = TextPrimary)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }
                }
                DropdownMenu(
                    expanded = showFrequencyDropdown,
                    onDismissRequest = { showFrequencyDropdown = false },
                    modifier = Modifier.background(CardBackground)
                ) {
                    val presets = listOf("Everyday", "1 x a week", "2 x a week", "3 x a week", "4 x a week", "5 x a week", "6 x a week")
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset, color = TextPrimary) },
                            onClick = {
                                selectedFrequencyPreset = preset
                                frequency = if (preset == "Everyday") FrequencyType.EVERY_DAY else FrequencyType.X_TIMES_PER_WEEK
                                showFrequencyDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Reminder",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Reminder Time", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = AddHabitInputBg),
                    border = BorderStroke(1.dp, CardBorderAlt)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val timeText = if (reminderTime.isNotEmpty()) {
                            try {
                                LocalTime.parse(reminderTime).format(DateTimeFormatter.ofPattern("h:mm a"))
                            } catch (_: Exception) { reminderTime }
                        } else "Set time"
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeText,
                                color = if (reminderTime.isNotEmpty()) TextPrimary else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        
                        if (reminderTime.isNotEmpty()) {
                            IconButton(
                                onClick = { reminderTime = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (showTimePicker) {
                val initialTime = if (reminderTime.isNotEmpty()) {
                    try { LocalTime.parse(reminderTime) } catch (_: Exception) { LocalTime.of(9, 0) }
                } else LocalTime.of(9, 0)
                
                val timePickerState = rememberTimePickerState(
                    initialHour = initialTime.hour,
                    initialMinute = initialTime.minute,
                    is24Hour = false
                )
                
                TimePickerDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Select Reminder Time", color = TextPrimary) },
                    confirmButton = {
                        TextButton(onClick = {
                            reminderTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text("Confirm", color = AccentPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = CardBackground,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = TextSecondary,
                            selectorColor = AccentPrimary,
                            periodSelectorSelectedContainerColor = AccentPrimary,
                            periodSelectorUnselectedContainerColor = CardBackground,
                            periodSelectorSelectedContentColor = Color.White,
                            periodSelectorUnselectedContentColor = TextSecondary,
                            timeSelectorSelectedContainerColor = AccentPrimary,
                            timeSelectorUnselectedContainerColor = CardBackground,
                            timeSelectorSelectedContentColor = Color.White,
                            timeSelectorUnselectedContentColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            GradientButton(
                text = if (isEditMode) "Save Changes" else "Add Habit",
                onClick = {
                    if (isEnabled) {
                        isSaving = true
                        val finalCategory = if (isOtherSelected) customCategory else selectedCategory
                        
                        val timesPerWeek = if (selectedFrequencyPreset == "Everyday") 7 else {
                            selectedFrequencyPreset.split(" ").firstOrNull()?.toIntOrNull() ?: 3
                        }
                        
                        val finalFrequencyConfig = FrequencyConfig(
                            type = frequency,
                            timesPerWeek = if (frequency == FrequencyType.X_TIMES_PER_WEEK) timesPerWeek else null
                        )

                        if (isEditMode) {
                            viewModel.updateHabit(
                                habitId = habitToEdit!!.id,
                                title = title,
                                category = finalCategory,
                                goalType = GoalType.NUMERIC,
                                frequencyType = frequency,
                                frequencyConfig = finalFrequencyConfig,
                                reminderTime = reminderTime.ifBlank { null },
                                numericTarget = goalValue.toFloatOrNull(),
                                numericUnit = finalUnit,
                                onSuccess = { 
                                    isSaving = false
                                    onDismiss() 
                                },
                                onError = { 
                                    isSaving = false
                                    syncError = it 
                                }
                            )
                        } else {
                            viewModel.addHabit(
                                title = title,
                                category = finalCategory,
                                goalType = GoalType.NUMERIC,
                                frequencyType = frequency,
                                frequencyConfig = finalFrequencyConfig,
                                reminderTime = reminderTime.ifBlank { null },
                                numericTarget = goalValue.toFloatOrNull(),
                                numericUnit = finalUnit,
                                onSuccess = { 
                                    isSaving = false
                                    onDismiss() 
                                },
                                onError = { 
                                    isSaving = false
                                    syncError = it 
                                }
                            )
                        }
                    }
                },
                isLoading = isSaving,
                enabled = isEnabled
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            containerColor = CardBackground,
            title = { Text("Discard Changes?", color = TextPrimary) },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Discard", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Editing", color = TextSecondary)
                }
            }
        )
    }
}
