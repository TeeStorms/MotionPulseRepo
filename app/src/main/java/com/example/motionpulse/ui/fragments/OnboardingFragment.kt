package com.example.motionpulse.ui.fragments

import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.screens.onboarding.OnboardingScreen

class OnboardingFragment : BaseComposeHostFragment() {

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
        OnboardingScreen(
            onGetStarted = {
                authViewModel.completeOnboarding()
                findNavController().navigate(
                    R.id.login,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.onboarding, true)
                        .build()
                )
            }
        )
    }
}
