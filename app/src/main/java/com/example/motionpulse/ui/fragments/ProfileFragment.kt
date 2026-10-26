package com.example.motionpulse.ui.fragments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.screens.profile.ProfileScreen
import com.example.motionpulse.ui.viewmodels.ProfileViewModel

class ProfileFragment : BaseComposeHostFragment() {

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
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

    @Composable
    override fun Content() {
        val authState by authViewModel.authState.collectAsState()
        val uid = (authState as? AuthState.Authenticated)?.uid ?: ""

        if (uid.isNotEmpty()) {
            val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                key = uid,
                factory = ProfileViewModel.Factory(db, uid)
            )
            ProfileScreen(
                viewModel = profileViewModel,
                onLogout = {
                    authViewModel.logout()
                    findNavController().navigate(
                        R.id.login,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(0, true)
                            .build()
                    )
                },
                onSeeAllBadges = { findNavController().navigate(R.id.badges_gallery) },
                onNavigateToNav = { route -> navigateToRoute(route) },
                currentRoute = "profile"
            )
        }
    }

    private fun navigateToRoute(route: String) {
        val destinationId = when (route) {
            "dashboard" -> R.id.dashboard
            "mood" -> R.id.mood
            "habits" -> R.id.habits
            "community" -> R.id.community
            "stats" -> R.id.stats
            else -> null
        }
        destinationId?.let { findNavController().navigate(it) }
    }
}
