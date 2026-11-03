package com.example.motionpulse.ui.dialogs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.GoalType
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.LayoutAddHabitSheetBinding
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import androidx.room.Room
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AddHabitBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutAddHabitSheetBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    private val db by lazy {
        Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).fallbackToDestructiveMigration().build()
    }

    private var selectedCategory: String? = null
    private var selectedReminderTime: String? = null // Stored as "HH:mm"
    private var isNumericGoal = false
    private var numericTargetValue = 10f
    private var selectedUnit = "reps"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAddHabitSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySelection()
        setupTextWatcher()
        setupGoalToggle()
        setupStepper()
        setupTimePicker()
        setupSaveButton()
    }

    private fun setupCategorySelection() {
        binding.cgSheetCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when {
                checkedIds.contains(R.id.chip_cat_mind) -> "Mind & Focus"
                checkedIds.contains(R.id.chip_cat_fitness) -> "Fitness & Health"
                checkedIds.contains(R.id.chip_cat_routine) -> "Daily Routine"
                else -> null
            }
            updateSuggestions()
            validateInputs()
        }
    }

    private fun updateSuggestions() {
        binding.cgSuggestions.removeAllViews()
        val category = selectedCategory ?: return
        val suggestions = when (category) {
            "Mind & Focus" -> listOf("Read a Book", "Meditate", "Journaling")
            "Fitness & Health" -> listOf("Go for a Run", "30 Push-ups", "Drink Water")
            "Daily Routine" -> listOf("Wash My Hair", "Clean Desk", "Early Wake-up")
            else -> emptyList()
        }

        suggestions.forEach { suggestion ->
            val chip = Chip(requireContext()).apply {
                text = "Try: $suggestion"
                isClickable = true
                isCheckable = false
                setOnClickListener {
                    binding.etTitle.setText(suggestion)
                    binding.etTitle.setSelection(suggestion.length)
                }
            }
            binding.cgSuggestions.addView(chip)
        }
    }

    private fun setupTextWatcher() {
        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val title = s?.toString()?.trim() ?: ""
                binding.tilTitle.error = null
                
                val uid = authViewModel.getGoogleIdTokenNonce() // Placeholder check
                binding.tvDuplicateWarning.visibility = View.GONE

                validateInputs()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupGoalToggle() {
        binding.toggleGoalType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isNumericGoal = checkedId == R.id.btn_goal_numeric
                binding.layoutNumericGoal.visibility = if (isNumericGoal) View.VISIBLE else View.GONE
                updateGoalPreview()
                validateInputs()
            }
        }

        binding.cgUnits.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedUnit = when {
                checkedIds.contains(R.id.chip_unit_mins) -> "mins"
                checkedIds.contains(R.id.chip_unit_pages) -> "pages"
                checkedIds.contains(R.id.chip_unit_glasses) -> "glasses"
                checkedIds.contains(R.id.chip_unit_steps) -> "steps"
                else -> "reps"
            }
            updateGoalPreview()
        }
    }

    private fun setupStepper() {
        binding.btnDecrement.setOnClickListener {
            if (numericTargetValue > 1f) {
                numericTargetValue -= 1f
                binding.etNumericTarget.setText(numericTargetValue.toInt().toString())
                updateGoalPreview()
            }
        }

        binding.btnIncrement.setOnClickListener {
            numericTargetValue += 1f
            binding.etNumericTarget.setText(numericTargetValue.toInt().toString())
            updateGoalPreview()
        }
    }

    private fun updateGoalPreview() {
        if (isNumericGoal) {
            binding.tvGoalPreview.text = "Preview: ${numericTargetValue.toInt()} $selectedUnit"
        }
    }

    private fun setupTimePicker() {
        binding.btnReminderTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(9)
                .setMinute(0)
                .setTitleText("Select Reminder Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                val formattedTime = String.format("%02d:%02d", picker.hour, picker.minute)
                selectedReminderTime = formattedTime
                val localTime = LocalTime.of(picker.hour, picker.minute)
                val displayTime = localTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
                binding.btnReminderTime.text = displayTime
                binding.btnClearReminder.visibility = View.VISIBLE
            }

            picker.show(childFragmentManager, "REMINDER_TIME_PICKER")
        }

        binding.btnClearReminder.setOnClickListener {
            selectedReminderTime = null
            binding.btnReminderTime.text = "Set Reminder Time"
            binding.btnClearReminder.visibility = View.GONE
        }
    }

    private fun validateInputs() {
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val isTitleValid = title.isNotEmpty()
        val isCategoryValid = selectedCategory != null
        binding.btnSaveHabit.isEnabled = isTitleValid && isCategoryValid
    }

    private fun setupSaveButton() {
        binding.btnSaveHabit.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim() ?: return@setOnClickListener
            val category = selectedCategory ?: return@setOnClickListener
            val goalType = if (isNumericGoal) GoalType.NUMERIC else GoalType.YES_NO
            val freqConfig = FrequencyConfig(FrequencyType.EVERY_DAY)

            val uid = authViewModel.authState.value.let {
                (it as? com.example.motionpulse.domain.auth.AuthState.Authenticated)?.uid ?: ""
            }

            if (uid.isNotEmpty()) {
                val habitsViewModel = ViewModelProvider(
                    requireActivity(),
                    HabitsViewModel.Factory(db, uid, requireContext())
                )[HabitsViewModel::class.java]

                habitsViewModel.addHabit(
                    title = title,
                    category = category,
                    goalType = goalType,
                    frequencyType = FrequencyType.EVERY_DAY,
                    frequencyConfig = freqConfig,
                    reminderTime = selectedReminderTime,
                    numericTarget = if (isNumericGoal) numericTargetValue else null,
                    numericUnit = if (isNumericGoal) selectedUnit else null,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Habit created!", Toast.LENGTH_SHORT).show()
                        dismiss()
                    },
                    onError = { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        dismiss()
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
