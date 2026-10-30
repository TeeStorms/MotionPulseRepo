package com.example.motionpulse.util

import android.view.HapticFeedbackConstants
import android.view.View

object HapticHelper {
    fun performClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun performLongPress(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
