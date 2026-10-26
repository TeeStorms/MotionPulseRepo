package com.example.motionpulse.ui.fragments

import android.os.Bundle
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
import com.example.motionpulse.ui.screens.habits.HabitsScreen
import com.example.motionpulse.ui.viewmodels.HabitsViewModel

class HabitsFragment : BaseComposeHostFragment() {

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
            val habitsViewModel: HabitsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                key = uid,
                factory = HabitsViewModel.Factory(db, uid, requireContext())
            )
            HabitsScreen(
                viewModel = habitsViewModel,
                onNavigateToDetail = { habitId ->
                    val bundle = Bundle().apply { putString("habitId", habitId) }
                    findNavController().navigate(R.id.habit_detail, bundle)
                },
                onNavigateToNav = { route -> navigateToRoute(route) },
                currentRoute = "habits"
            )
        }
    }

    private fun navigateToRoute(route: String) {
        val destinationId = when (route) {
            "dashboard" -> R.id.dashboard
            "mood" -> R.id.mood
            "community" -> R.id.community
            "stats" -> R.id.stats
            "profile" -> R.id.profile
            else -> null
        }
        destinationId?.let { findNavController().navigate(it) }
    }
}
