package com.example.motionpulse.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Global Tokens
val BackgroundDark = Color(0xFF05020A)
val CardBackground = Color(0xFF1C0620)
val CardBorder = Color(0xFFE01E79)
val CardBorderAlt = Color(0xFFFF2A85)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFD1C7D7)
val AccentPrimary = Color(0xFFFF2A85) // Hot Pink
val AccentBlue = Color(0xFF4FC3F7) // Apps Blue
val BottomNavBackground = Color(0xFF140217)
val BottomNavActive = Color(0xFFFF2A85)
val BottomNavInactive = Color(0xFFB182B8)
val TextLinkColor = Color(0xFFFF3D94)

// Gradients
val HeaderGradient3Stop = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF2A85),
        Color(0xFF8C52FF),
        Color(0xFF4FC3F7)
    )
)

val HeaderGradient2Stop = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF2A85),
        Color(0xFF4FC3F7)
    )
)

val PrimaryButtonGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF2A85),
        Color(0xFF5BBBE9)
    )
)

// Screen-specific accents
// Profile Screen
val ProfileAvatarBadge = Color(0xFF51B0FF)
val ProfileAchievementsBg = Color(0xFF18041C)
val ProfileLogoutBg = Color(0xFF680D12)
val ProfileLogoutOutline = Color(0xFFA81A22)

// Stats Screen
val StatsHabitColor1 = Color(0xFFFFEE00) // Read a Book
val StatsHabitColor2 = Color(0xFFFF2E2E) // Take a Walk
val StatsHabitColor3 = Color(0xFF5E82FF) // Wash My Hair

// Dashboard Screen
val DashboardActiveDot = Color(0xFFFF006E)
val DashboardCompletedDot = Color(0xFF8B0032)

// Habits Screen
val HabitsCategoryFocusActive = Color(0xFFA3A000)
val HabitsCategoryFitnessDot = Color(0xFFFF2E2E)
val HabitsCategoryRoutineDot = Color(0xFF5E82FF)
val AddHabitInputBg = Color(0xFF0A010D)
