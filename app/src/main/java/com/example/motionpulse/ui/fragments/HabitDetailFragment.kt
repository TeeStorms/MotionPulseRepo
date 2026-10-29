package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentHabitDetailBinding
import com.example.motionpulse.domain.auth.AuthViewModel

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
        binding.tvHabitId.text = "ID: $habitId"

        binding.dropletHeader.setTitle("Habit Detail")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        val backIcon = ImageView(requireContext()).apply {
            setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back))
            setOnClickListener { findNavController().popBackStack() }
        }
        binding.dropletHeader.setNavigationIconView(backIcon)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
