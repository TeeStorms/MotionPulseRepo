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
import com.example.motionpulse.data.local.entity.MoodFactor
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentMoodBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.viewmodels.MoodViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoodFragment : Fragment() {

    private var _binding: FragmentMoodBinding? = null
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

    private var selectedMoodLevel: MoodLevel? = null
    private var isUpdatingExistingMood = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("How's your rhythm\ntoday?")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        setupMoodBarsListener()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val moodViewModel = ViewModelProvider(
                                this@MoodFragment,
                                MoodViewModel.Factory(db, uid)
                            )[MoodViewModel::class.java]

                            setupMoodContent(moodViewModel)
                        }
                    }
                }
            }
        }
    }

    private fun setupMoodBarsListener() {
        binding.moodBars.onMoodSelectedListener = { level ->
            selectedMoodLevel = level
            binding.btnLogMood.isEnabled = true
            binding.tvSelectedMoodTitle.text = level.name
            binding.tvSelectedMoodTitle.visibility = View.VISIBLE
        }
    }

    private fun setupMoodContent(viewModel: MoodViewModel) {
        binding.btnLogMood.setOnClickListener {
            val level = selectedMoodLevel ?: return@setOnClickListener
            val factors = getSelectedFactors()
            val note = binding.etNote.text?.toString()?.trim()

            viewModel.logMood(level, factors, note)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.todayMood.collectLatest { todayMood ->
                        if (todayMood != null) {
                            isUpdatingExistingMood = true
                            selectedMoodLevel = todayMood.moodLevel
                            binding.moodBars.setMoodLevel(todayMood.moodLevel)

                            binding.tvSelectedMoodTitle.text = todayMood.moodLevel.name
                            binding.tvSelectedMoodTitle.visibility = View.VISIBLE

                            setSelectedFactors(todayMood.factors)

                            if (!todayMood.note.isNullOrBlank()) {
                                binding.etNote.setText(todayMood.note)
                            }

                            binding.btnLogMood.text = "Update mood"
                            binding.btnLogMood.isEnabled = true
                        } else {
                            isUpdatingExistingMood = false
                            binding.btnLogMood.text = "Log mood"
                            if (selectedMoodLevel == null) {
                                binding.btnLogMood.isEnabled = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.saveSuccess.collect {
                        val message = if (isUpdatingExistingMood) "Rhythm updated!" else "Rhythm logged for today!"
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getSelectedFactors(): List<MoodFactor> {
        val list = mutableListOf<MoodFactor>()
        if (binding.chipSleep.isChecked) list.add(MoodFactor.SLEEP)
        if (binding.chipWork.isChecked) list.add(MoodFactor.WORK)
        if (binding.chipHealth.isChecked) list.add(MoodFactor.HEALTH)
        if (binding.chipSocial.isChecked) list.add(MoodFactor.SOCIAL)
        if (binding.chipWeather.isChecked) list.add(MoodFactor.WEATHER)
        return list
    }

    private fun setSelectedFactors(factors: List<MoodFactor>) {
        binding.chipSleep.isChecked = factors.contains(MoodFactor.SLEEP)
        binding.chipWork.isChecked = factors.contains(MoodFactor.WORK)
        binding.chipHealth.isChecked = factors.contains(MoodFactor.HEALTH)
        binding.chipSocial.isChecked = factors.contains(MoodFactor.SOCIAL)
        binding.chipWeather.isChecked = factors.contains(MoodFactor.WEATHER)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
