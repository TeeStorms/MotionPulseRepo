package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.GoalType
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentHabitDetailBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.ui.adapters.CompletionHistoryAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HabitDetailFragment : Fragment() {

    private var _binding: FragmentHabitDetailBinding? = null
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

    private lateinit var historyAdapter: CompletionHistoryAdapter
    private var currentHabit: HabitEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val habitId = arguments?.getString("habitId") ?: ""

        setupBackArrow()
        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val habitsViewModel = ViewModelProvider(
                                this@HabitDetailFragment,
                                HabitsViewModel.Factory(db, uid, requireContext())
                            )[HabitsViewModel::class.java]

                            observeHabitDetails(habitsViewModel, habitId)
                        }
                    }
                }
            }
        }
    }

    private fun setupBackArrow() {
        val backIcon = ImageView(requireContext()).apply {
            setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back))
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.bg_chip)
            setOnClickListener { findNavController().popBackStack() }
        }
        binding.dropletHeader.setNavigationIconView(backIcon)
    }

    private fun setupRecyclerView() {
        historyAdapter = CompletionHistoryAdapter()
        binding.rvCompletionHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCompletionHistory.adapter = historyAdapter
    }

    private fun observeHabitDetails(viewModel: HabitsViewModel, habitId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allHabits.collectLatest { allHabits ->
                        val habitWithStatus = allHabits.find { it.habit.id == habitId }
                        if (habitWithStatus != null) {
                            val habit = habitWithStatus.habit
                            currentHabit = habit

                            binding.dropletHeader.setTitle(habit.title)
                            binding.dropletHeader.setCaption(habit.category.uppercase())

                            val scorePct = com.example.motionpulse.util.HabitStrengthFormatter.toPercent(habit.habitStrengthScore)
                            binding.tvStrengthScore.text = "$scorePct%"
                            binding.tvCurrentStreak.text = "${habit.currentStreak}d"
                            binding.tvLongestStreak.text = "${habit.longestStreak}d"

                            val freqLabel = FrequencyConfig.decodeSafe(habit.frequencyConfig).getFrequencyLabel()
                            binding.tvFrequency.text = "Frequency: $freqLabel"

                            val goalLabel = if (habit.goalType == GoalType.NUMERIC) {
                                "Goal: ${habit.numericTarget?.toInt() ?: 1} ${habit.numericUnit ?: "reps"}"
                            } else {
                                "Goal: Yes / No"
                            }
                            binding.tvGoalTarget.text = goalLabel

                            binding.tvReminderTime.text = if (habit.reminderTime != null) {
                                "Reminder: ${habit.reminderTime}"
                            } else {
                                "Reminder: Disabled"
                            }

                            setupActionButtons(viewModel, habit)
                        }
                    }
                }

                launch {
                    db.habitCompletionDao().getAllCompletionsForHabit(habitId).collectLatest { completions ->
                        if (completions.isEmpty()) {
                            binding.stateLayout.setState(StateLayout.State.EMPTY, "No completion records yet")
                        } else {
                            binding.stateLayout.setState(StateLayout.State.CONTENT)
                            historyAdapter.submitList(completions)
                        }
                    }
                }
            }
        }
    }

    private fun setupActionButtons(viewModel: HabitsViewModel, habit: HabitEntity) {
        binding.btnEditHabit.setOnClickListener {
            val sheet = com.example.motionpulse.ui.dialogs.AddHabitBottomSheetDialogFragment()
            sheet.show(childFragmentManager, "EDIT_HABIT_SHEET")
        }

        binding.btnDeleteHabit.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Delete Habit?")
                .setMessage("Are you sure you want to delete '${habit.title}'?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.archiveHabit(habit)
                    Toast.makeText(requireContext(), "Habit deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
