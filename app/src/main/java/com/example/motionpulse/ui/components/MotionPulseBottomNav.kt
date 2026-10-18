package com.example.motionpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val fontScale = density.fontScale

    // Show label on all tabs only when screen width >= 360dp AND fontScale <= 1.15f
    val showAllLabels = screenWidthDp >= 360.dp && fontScale <= 1.15f

    CapFontScale(maxFontScale = 1.3f) {
        Surface(
            color = BottomNavBackground,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val showLabel = isSelected || showAllLabels

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .semantics { contentDescription = screen.label }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onNavigate(screen.route) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) HeaderGradient3Stop else androidx.compose.ui.graphics.SolidColor(Color.Transparent)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else BottomNavInactive,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (showLabel) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = screen.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) BottomNavActive else BottomNavInactive,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.wrapContentWidth(unbounded = true)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@AdaptivePreviews
@Composable
private fun MotionPulseBottomNavPreview() {
    MotionPulseTheme {
        MotionPulseBottomNav(
            currentRoute = "community",
            onNavigate = {}
        )
    }
}
