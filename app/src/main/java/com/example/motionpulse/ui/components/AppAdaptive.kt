package com.example.motionpulse.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Caps system font scale at maxFontScale (default 1.3x) for "Chrome" UI
 * (bottom nav labels, tab labels, headers, chips, badges) to prevent layout breakages.
 */
@Composable
fun CapFontScale(
    maxFontScale: Float = 1.3f,
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    val cappedDensity = remember(currentDensity, maxFontScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceAtMost(maxFontScale)
        )
    }
    CompositionLocalProvider(LocalDensity provides cappedDensity, content = content)
}

/**
 * Centers content and caps width at [maxWidth] (default 600.dp) on wide screens / tablets / foldables.
 */
fun Modifier.responsiveContentWidth(maxWidth: Dp = 600.dp): Modifier {
    return this
        .fillMaxWidth()
        .widthIn(max = maxWidth)
}
