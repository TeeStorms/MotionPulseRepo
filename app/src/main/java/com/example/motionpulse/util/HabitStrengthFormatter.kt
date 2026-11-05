package com.example.motionpulse.util

import kotlin.math.roundToInt

object HabitStrengthFormatter {

    fun toPercent(score: Float?): Int {
        if (score == null || score.isNaN() || score.isInfinite() || score < 0f) {
            return 0
        }
        return score.roundToInt().coerceIn(0, 100)
    }
}
