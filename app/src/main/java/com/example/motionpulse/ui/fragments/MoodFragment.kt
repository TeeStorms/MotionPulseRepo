package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentMoodBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.viewmodels.MoodViewModel
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
        binding.selectedMoodPill.setMood("Energized", "#EE44CB")

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                if (uid.isNotEmpty()) {
                    val moodViewModel = ViewModelProvider(
                        this@MoodFragment,
                        MoodViewModel.Factory(db, uid)
                    )[MoodViewModel::class.java]

                    binding.btnSaveMood.setOnClickListener {
                        val note = binding.etNote.text?.toString() ?: ""
                        moodViewModel.logMood(MoodLevel.ENERGIZED, emptyList(), note)
                        Toast.makeText(requireContext(), "Saved rhythm!", Toast.LENGTH_SHORT).show()
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
