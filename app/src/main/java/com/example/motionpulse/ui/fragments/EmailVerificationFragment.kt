package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentEmailVerificationBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EmailVerificationFragment : Fragment() {

    private var _binding: FragmentEmailVerificationBinding? = null
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
        _binding = FragmentEmailVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("Check Your Inbox")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                if (state is AuthState.AwaitingEmailVerification) {
                    binding.tvEmail.text = state.email
                } else if (state is AuthState.Authenticated) {
                    if (isResumed) {
                        findNavController().navigate(
                            R.id.dashboard,
                            null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(0, true)
                                .build()
                        )
                    }
                }
            }
        }

        binding.btnContinue.setOnClickListener {
            authViewModel.checkVerificationStatus(
                onStillUnverified = {
                    // Stay on page
                }
            )
        }

        binding.btnResend.setOnClickListener {
            authViewModel.resendVerificationEmail()
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(
                R.id.login,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(0, true)
                    .build()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
