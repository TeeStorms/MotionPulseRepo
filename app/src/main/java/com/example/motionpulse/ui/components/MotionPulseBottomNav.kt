package com.example.motionpulse.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.motionpulse.ui.theme.BottomNavActive
import com.example.motionpulse.ui.theme.BottomNavBackground
import com.example.motionpulse.ui.theme.BottomNavInactive

sealed class BottomNavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavScreen("dashboard", "Home", Icons.Default.Home)
    object Habits : BottomNavScreen("habits", "Habits", Icons.Default.Checklist)
    object Stats : BottomNavScreen("stats", "Stats", Icons.Default.BarChart)
    object Profile : BottomNavScreen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun MotionPulseBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Habits,
        BottomNavScreen.Stats,
        BottomNavScreen.Profile
    )

    NavigationBar(
        containerColor = BottomNavBackground
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(text = screen.label) },
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BottomNavActive,
                    selectedTextColor = BottomNavActive,
                    unselectedIconColor = BottomNavInactive,
                    unselectedTextColor = BottomNavInactive,
                    indicatorColor = BottomNavBackground // Hide indicator background
                )
            )
        }
    }
}
