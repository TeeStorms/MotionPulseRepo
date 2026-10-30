package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
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

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnContinue.setOnClickListener {
            authViewModel.checkVerificationStatus(
                onStillUnverified = {
                    Toast.makeText(
                        requireContext(),
                        "Still not verified. Please click the link in your email first",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        binding.btnResend.setOnClickListener {
            authViewModel.resendVerificationEmail()
            Toast.makeText(requireContext(), "Verification email resent", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(
                R.id.login,
                null,
                NavOptions.Builder()
                    .setPopUpTo(0, true)
                    .build()
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.resendTimer.collectLatest { seconds ->
                        if (seconds > 0) {
                            binding.btnResend.isEnabled = false
                            binding.btnResend.text = "Resend in ${seconds}s"
                        } else {
                            binding.btnResend.isEnabled = true
                            binding.btnResend.text = "Resend Verification Email"
                        }
                    }
                }

                launch {
                    authViewModel.authState.collectLatest { state ->
                        handleAuthState(state)
                    }
                }
            }
        }
    }

    private fun handleAuthState(state: AuthState) {
        when (state) {
            is AuthState.AwaitingEmailVerification -> {
                binding.tvEmail.text = state.email
            }
            is AuthState.Authenticated -> {
                if (isResumed) {
                    findNavController().navigate(
                        R.id.dashboard,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(0, true)
                            .build()
                    )
                }
            }
            is AuthState.Error -> {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
