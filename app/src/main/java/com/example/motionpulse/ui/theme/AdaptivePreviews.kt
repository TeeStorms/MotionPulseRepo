package com.example.motionpulse.ui.theme

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotation testing layouts across key device profiles:
 * - Narrow 320dp screen @ font 1.0x
 * - Standard 360dp screen @ font 1.3x
 * - Pixel 411dp @ font 1.0x
 * - Pixel 411dp @ font 2.0x (max system font scale)
 * - Tablet/Foldable 600dp @ font 1.0x
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Preview(name = "1. Narrow (320dp @ 1.0x)", widthDp = 320, heightDp = 640, fontScale = 1.0f)
@Preview(name = "2. Compact (360dp @ 1.3x)", widthDp = 360, heightDp = 740, fontScale = 1.3f)
@Preview(name = "3. Standard (411dp @ 1.0x)", widthDp = 411, heightDp = 891, fontScale = 1.0f)
@Preview(name = "4. Large Font (411dp @ 2.0x)", widthDp = 411, heightDp = 891, fontScale = 2.0f)
@Preview(name = "5. Tablet/Foldable (600dp)", widthDp = 600, heightDp = 960, fontScale = 1.0f)
annotation class AdaptivePreviews
