package com.example.motionpulse.ui.fragments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.screens.LandingScreen

class LandingFragment : BaseComposeHostFragment() {

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    @Composable
    override fun Content() {
        val authState by authViewModel.authState.collectAsState()

        LandingScreen(
            viewModel = authViewModel,
            onTimeout = {
                val actionId = when (authState) {
                    is AuthState.Authenticated -> R.id.dashboard
                    is AuthState.AwaitingEmailVerification -> R.id.email_verification
                    is AuthState.OnboardingRequired -> R.id.onboarding
                    else -> R.id.login
                }
                findNavController().navigate(
                    actionId,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.landing, true)
                        .build()
                )
            }
        )
    }
}
