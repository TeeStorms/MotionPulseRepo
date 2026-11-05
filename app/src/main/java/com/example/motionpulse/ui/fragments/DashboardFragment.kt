package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentDashboardBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.ui.adapters.HabitsAdapter
import com.example.motionpulse.ui.views.MoodPillView
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
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

    private lateinit var habitsAdapter: HabitsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardMoodBanner.setOnClickListener {
            findNavController().navigate(R.id.mood)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.isOffline.collectLatest { isOffline ->
                        binding.tvOfflineBanner.visibility = if (isOffline) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    authViewModel.isSyncingFailed.collectLatest { failed ->
                        binding.tvSyncErrorBanner.visibility = if (failed) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    authViewModel.authState.collectLatest { state ->
                        val authenticatedState = state as? AuthState.Authenticated
                        val uid = authenticatedState?.uid ?: ""

                        binding.dropletHeader.setTitle(getString(R.string.welcome_back_placeholder, "User"))
                        binding.dropletHeader.setCaption("MOTION.PULSE")

                        if (uid.isNotEmpty()) {
                            val habitsViewModel = ViewModelProvider(
                                this@DashboardFragment,
                                HabitsViewModel.Factory(db, uid, requireContext())
                            )[HabitsViewModel::class.java]

                            setupDashboardContent(habitsViewModel)

                            viewLifecycleOwner.lifecycleScope.launch {
                                habitsViewModel.userProfile.collectLatest { profile ->
                                    val name = profile?.displayName?.takeIf { it.isNotBlank() } ?: "User"
                                    binding.dropletHeader.setTitle(getString(R.string.welcome_back_placeholder, name))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupDashboardContent(viewModel: HabitsViewModel) {
        habitsAdapter = HabitsAdapter(
            showOverflow = false,
            onToggleComplete = { habit -> viewModel.logHabitProgress(habit) },
            onItemClick = { habitId ->
                val bundle = Bundle().apply { putString("habitId", habitId) }
                findNavController().navigate(R.id.habit_detail, bundle)
            }
        )

        binding.rvHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHabits.adapter = habitsAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshHabits()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.tvDailyQuote.text = viewModel.dailyQuote

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.badgeEvent.collectLatest { badgeType ->
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root,
                            "🎉 BADGE UNLOCKED: ${badgeType.name}",
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                launch {
                    viewModel.isMoodLoggedToday.collectLatest { logged ->
                        binding.cardMoodBanner.visibility = if (!logged) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.todayMood.collectLatest { mood ->
                        if (mood != null) {
                            val moodPill = MoodPillView(requireContext()).apply {
                                val moodName = mood.moodLevel.name.lowercase().replaceFirstChar { it.uppercase() }
                                val colorHex = when (mood.moodLevel) {
                                    com.example.motionpulse.data.local.entity.MoodLevel.ENERGIZED -> "#E040FB"
                                    com.example.motionpulse.data.local.entity.MoodLevel.GOOD -> "#FF2A85"
                                    com.example.motionpulse.data.local.entity.MoodLevel.STEADY -> "#4FC3F7"
                                    com.example.motionpulse.data.local.entity.MoodLevel.LOW -> "#FFB74D"
                                    com.example.motionpulse.data.local.entity.MoodLevel.DRAINED -> "#607D8B"
                                }
                                setMood(moodName, colorHex)
                            }
                            binding.dropletHeader.setTrailingView(moodPill)
                        } else {
                            binding.dropletHeader.setTrailingView(null)
                        }
                    }
                }

                launch {
                    viewModel.allHabits.collectLatest { allHabitsWithStatus ->
                        val today = LocalDate.now()
                        val scheduledToday = allHabitsWithStatus.filter { habitWithStatus ->
                            !habitWithStatus.habit.isArchived &&
                                    FrequencyConfig.decodeSafe(habitWithStatus.habit.frequencyConfig).isScheduled(today)
                        }

                        val completed = scheduledToday.count { it.isCompletedToday }
                        val total = scheduledToday.size

                        binding.tvProgressRatio.text = "$completed/$total"
                        binding.progressRing.setProgress(completed, total)

                        if (allHabitsWithStatus.isEmpty()) {
                            binding.stateLayout.setState(StateLayout.State.EMPTY, "Add your first habit to get started")
                        } else if (scheduledToday.isEmpty()) {
                            binding.stateLayout.setState(StateLayout.State.EMPTY, "No habits scheduled for today")
                        } else {
                            binding.stateLayout.setState(StateLayout.State.CONTENT)
                            habitsAdapter.submitList(scheduledToday)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}