package com.example.motionpulse.data.repository

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages app-level persistent flags and user preferences using SharedPreferences.
 */
class PreferenceRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "motion_pulse_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_LAST_EMAIL = "last_used_email"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    }

    fun isRemindersEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
    }

    /**
     * Returns true if the app has never been launched before (or data was cleared).
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Marks the first-launch intro as completed.
     */
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun saveLastUsedEmail(email: String) {
        prefs.edit().putString(KEY_LAST_EMAIL, email).apply()
    }

    fun getLastUsedEmail(): String? {
        return prefs.getString(KEY_LAST_EMAIL, null)
    }
}
