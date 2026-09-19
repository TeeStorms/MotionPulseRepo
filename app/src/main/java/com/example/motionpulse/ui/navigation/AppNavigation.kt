package com.example.motionpulse.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.screens.LandingScreen
import com.example.motionpulse.ui.screens.auth.EmailVerificationScreen
import com.example.motionpulse.ui.screens.auth.LoginScreen
import com.example.motionpulse.ui.screens.auth.RegisterScreen
import com.example.motionpulse.ui.screens.onboarding.OnboardingScreen
import com.example.motionpulse.ui.screens.dashboard.DashboardScreen
import com.example.motionpulse.ui.screens.dashboard.HabitDetailScreen
import com.example.motionpulse.ui.screens.habits.HabitsScreen
import com.example.motionpulse.ui.screens.profile.BadgesGalleryScreen
import com.example.motionpulse.ui.screens.profile.ProfileScreen
import com.example.motionpulse.ui.screens.stats.StatsScreen
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
import com.example.motionpulse.ui.viewmodels.ProfileViewModel
import com.example.motionpulse.ui.viewmodels.StatsViewModel

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val preferenceRepository = remember { PreferenceRepository(context) }
    val authViewModel: AuthViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    )
    val authState by authViewModel.authState.collectAsState()
    val isSyncFailed by authViewModel.isSyncingFailed.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Connectivity monitoring
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                authViewModel.updateConnectivity(false)
            }
            override fun onLost(network: Network) {
                authViewModel.updateConnectivity(true)
            }
        }
        
        connectivityManager?.registerNetworkCallback(networkRequest, callback)
        
        // Initial check
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        authViewModel.updateConnectivity(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true)
    }

    // Simple DB provider
    val db = remember {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).fallbackToDestructiveMigration().build()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.AwaitingEmailVerification) {
            navController.navigate("email_verification") {
                popUpTo(0)
            }
        }
    }

    NavHost(navController = navController, startDestination = "landing") {
        composable("landing") {
            LandingScreen(
                viewModel = authViewModel,
                onTimeout = {
                    val destination = when (authState) {
                        is AuthState.Authenticated -> "dashboard"
                        is AuthState.AwaitingEmailVerification -> "email_verification"
                        is AuthState.OnboardingRequired -> "onboarding"
                        else -> "login"
                    }
                    navController.navigate(destination) {
                        popUpTo("landing") { inclusive = true }
                    }
                }
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                onGetStarted = {
                    authViewModel.completeOnboarding()
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                } }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate("login") },
                onRegistrationSuccess = { navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                } }
            )
        }
        composable("email_verification") {
            val email = (authState as? AuthState.AwaitingEmailVerification)?.email ?: ""
            EmailVerificationScreen(
                email = email,
                viewModel = authViewModel,
                onVerificationSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("dashboard") {
            val uid = (authState as? AuthState.Authenticated)?.uid ?: ""
            if (uid.isNotEmpty()) {
                val habitsViewModel: HabitsViewModel = viewModel(
                    key = uid,
                    factory = HabitsViewModel.Factory(db, uid)
                )
                DashboardScreen(
                    viewModel = habitsViewModel,
                    onNavigateToDetail = { habitId -> navController.navigate("habit_detail/$habitId") },
                    onNavigateToNav = { route -> 
                        if (route != "dashboard") {
                            navController.navigate(route)
                        }
                    },
                    onNavigateToMood = { navController.navigate("mood") },
                    currentRoute = currentRoute,
                    isSyncFailed = isSyncFailed
                )
            }
        }
        composable("mood") {
            val uid = (authState as? AuthState.Authenticated)?.uid ?: ""
            if (uid.isNotEmpty()) {
                val moodViewModel: com.example.motionpulse.ui.viewmodels.MoodViewModel = viewModel(
                    key = uid,
                    factory = com.example.motionpulse.ui.viewmodels.MoodViewModel.Factory(db, uid)
                )
                com.example.motionpulse.ui.screens.mood.MoodScreen(
                    viewModel = moodViewModel,
                    onNavigateToNav = { route -> 
                        if (route != "mood") {
                            navController.navigate(route)
                        }
                    },
                    currentRoute = currentRoute
                )
            }
        }
        composable("habit_detail/{habitId}") { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId") ?: ""
            val uid = (authState as? AuthState.Authenticated)?.uid ?: ""
            if (uid.isNotEmpty()) {
                val habitsViewModel: HabitsViewModel = viewModel(
                    key = uid,
                    factory = HabitsViewModel.Factory(db, uid)
                )
                HabitDetailScreen(
                    habitId = habitId,
                    viewModel = habitsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("habits") {
            val uid = (authState as? AuthState.Authenticated)?.uid ?: ""
            if (uid.isNotEmpty()) {
                val habitsViewModel: HabitsViewModel = viewModel(
                    key = uid,
                    factory = HabitsViewModel.Factory(db, uid)
                )
                HabitsScreen(
                    viewModel = habitsViewModel,
                    onNavigateToDetail = { habitId -> navController.navigate("habit_detail/$habitId") },
                    onNavigateToNav = { route -> 
                        if (route != "habits") {
                            navController.navigate(route)
                        }
                    },
                    currentRoute = currentRoute
                )
            }
        }
        composable("stats") {
            val uid = (authState as? AuthState.Authenticated)?.uid ?: ""
            if (uid.isNotEmpty()) {
                val statsViewModel: StatsViewModel = viewModel(
                    key = uid,
                    factory = StatsViewModel.Factory(db, uid)
                )
                StatsScreen(
                    viewModel = statsViewModel,
                    onNavigateToNav = { route -> 
                        if (route != "stats") {
                            navController.navigate(route)
                        }
                    },
                    currentRoute = currentRoute
                )
            }
        }
        composable("profile") { 
            val uid = (authState as? AuthState.Authenticated)?.uid ?: ""
            if (uid.isNotEmpty()) {
                val profileViewModel: ProfileViewModel = viewModel(
                    key = uid,
                    factory = ProfileViewModel.Factory(db, uid)
                )
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    },
                    onSeeAllBadges = { navController.navigate("badges_gallery") },
                    onNavigateToNav = { route ->
                        if (route != "profile") {
                            navController.navigate(route)
                        }
                    },
                    currentRoute = currentRoute
                )
            }
        }
        composable("badges_gallery") {
            val uid = (authState as? AuthState.Authenticated)?.uid ?: ""
            if (uid.isNotEmpty()) {
                val profileViewModel: ProfileViewModel = viewModel(
                    key = uid,
                    factory = ProfileViewModel.Factory(db, uid)
                )
                BadgesGalleryScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String, navController: androidx.navigation.NavController, onLogout: (() -> Unit)? = null) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    androidx.compose.material3.Scaffold(
        bottomBar = {
            com.example.motionpulse.ui.components.MotionPulseBottomNav(
                currentRoute = currentRoute,
                onNavigate = { navController.navigate(it) }
            )
        },
        containerColor = com.example.motionpulse.ui.theme.BackgroundDark
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$name Screen Placeholder", color = com.example.motionpulse.ui.theme.TextPrimary)
                onLogout?.let {
                    androidx.compose.material3.Button(onClick = it) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}
