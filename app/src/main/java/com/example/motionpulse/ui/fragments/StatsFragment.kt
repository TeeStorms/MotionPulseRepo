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
import androidx.room.Room
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentStatsBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.views.MoodPillView
import com.example.motionpulse.ui.viewmodels.StatsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("Your Rhythm")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val statsViewModel = ViewModelProvider(
                                this@StatsFragment,
                                StatsViewModel.Factory(db, uid, requireContext())
                            )[StatsViewModel::class.java]

                            setupStatsContent(statsViewModel)
                        }
                    }
                }
            }
        }
    }

    private fun setupStatsContent(viewModel: StatsViewModel) {
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnPrevWeek.setOnClickListener { viewModel.previousWeek() }
        binding.btnNextWeek.setOnClickListener { viewModel.nextWeek() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.weekLabel.collectLatest { label ->
                        binding.tvWeekLabel.text = label
                    }
                }

                launch {
                    viewModel.weekOffset.collectLatest { offset ->
                        binding.btnNextWeek.isEnabled = offset < 0
                        binding.btnNextWeek.alpha = if (offset < 0) 1.0f else 0.4f
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
                    viewModel.summaryStats.collectLatest { stats ->
                        binding.tvStatDoneToday.text = com.example.motionpulse.domain.stats.StatsCalculations.formatDoneToday(stats.doneToday, stats.totalHabits)
                        binding.tvStatCurrentStreak.text = "${stats.currentStreak}d"
                        binding.tvStatTotalCompleted.text = stats.totalCompleted.toString()
                        binding.tvStatWeeklyPercent.text = "${stats.weeklyPercent}%"
                        binding.tvStatWeeklyTrend.text = "${stats.weeklyTrendStr} vs last week"
                    }
                }

                launch {
                    viewModel.weeklyCorrelationData.collectLatest { data ->
                        binding.graphCardView.setData(data)
                    }
                }

                launch {
                    viewModel.correlationInsight.collectLatest { insight ->
                        if (insight.isNotBlank()) {
                            binding.tvInsightText.text = insight
                        } else {
                            binding.tvInsightText.text = "Log a few more days to unlock rhythm insights!"
                        }
                    }
                }

                launch {
                    viewModel.bestDayInfo.collectLatest { bestDay ->
                        if (bestDay != null) {
                            val dayName = bestDay.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                            val moodName = bestDay.moodLevel.name.lowercase().replaceFirstChar { it.uppercase() }
                            binding.tvBestDayText.text = "$dayName - $moodName - ${bestDay.habitsDone}/${bestDay.totalHabits} habits done"
                        } else {
                            binding.tvBestDayText.text = "No best day logged yet"
                        }
                    }
                }

                launch {
                    viewModel.celebrationEvent.collect {
                        Snackbar.make(binding.root, "🎉 PERFECT RHYTHM! You hit 100% this week!", Snackbar.LENGTH_LONG).show()
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
