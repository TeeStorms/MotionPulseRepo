package com.example.motionpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.ui.theme.*

sealed class BottomNavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavScreen("dashboard", "Home", Icons.Outlined.Home)
    object Mood : BottomNavScreen("mood", "Mood", Icons.Outlined.FavoriteBorder)
    object Habits : BottomNavScreen("habits", "Habits", Icons.Outlined.Checklist)
    object Community : BottomNavScreen("community", "Community", Icons.Outlined.Groups)
    object Stats : BottomNavScreen("stats", "Stats", Icons.Outlined.BarChart)
    object Profile : BottomNavScreen("profile", "Profile", Icons.Outlined.Person)
}

@Composable
fun MotionPulseBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Mood,
        BottomNavScreen.Habits,
        BottomNavScreen.Community,
        BottomNavScreen.Stats,
        BottomNavScreen.Profile
    )

    NavigationBar(
        containerColor = BottomNavBackground,
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            
            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) HeaderGradient3Stop else androidx.compose.ui.graphics.SolidColor(Color.Transparent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            tint = if (isSelected) Color.White else BottomNavInactive,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                label = { 
                    Text(
                        text = screen.label,
                        fontSize = 10.sp,
                        maxLines = 1
                    ) 
                },
                selected = isSelected,
                alwaysShowLabel = false,
                onClick = { onNavigate(screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = BottomNavActive,
                    unselectedTextColor = BottomNavInactive,
                    indicatorColor = Color.Transparent // Disable default indicator
                )
            )
        }
    }
}
