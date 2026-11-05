package com.example.motionpulse.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.motionpulse.domain.models.UserSettings

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
        private const val KEY_LANGUAGE = "user_language"
        private const val KEY_SHARE_MILESTONES = "share_milestones"
        private const val KEY_SHOW_HABIT_NAMES = "show_habit_names"
        private const val KEY_SETTINGS_UPDATED_AT = "settings_updated_at"
        private const val KEY_LAST_CELEBRATED_WEEK = "last_celebrated_week"
    }

    fun getLastCelebratedWeek(): String? {
        return prefs.getString(KEY_LAST_CELEBRATED_WEEK, null)
    }

    fun setLastCelebratedWeek(weekKey: String) {
        prefs.edit().putString(KEY_LAST_CELEBRATED_WEEK, weekKey).apply()
    }

    fun getUserSettings(): UserSettings {
        val lang = prefs.getString(KEY_LANGUAGE, null)
        val reminders = prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
        val share = prefs.getBoolean(KEY_SHARE_MILESTONES, true)
        val showNames = prefs.getBoolean(KEY_SHOW_HABIT_NAMES, false)
        val updatedAt = prefs.getLong(KEY_SETTINGS_UPDATED_AT, 0L)
        return UserSettings(
            language = lang,
            remindersEnabled = reminders,
            shareMilestones = share,
            showHabitNames = showNames,
            updatedAt = updatedAt
        )
    }

    fun saveUserSettings(settings: UserSettings) {
        prefs.edit().apply {
            putString(KEY_LANGUAGE, settings.language)
            putBoolean(KEY_REMINDERS_ENABLED, settings.remindersEnabled)
            putBoolean(KEY_SHARE_MILESTONES, settings.shareMilestones)
            putBoolean(KEY_SHOW_HABIT_NAMES, settings.showHabitNames)
            putLong(KEY_SETTINGS_UPDATED_AT, settings.updatedAt)
            apply()
        }
    }

    fun clearUserSettingsOnLogout() {
        prefs.edit().apply {
            remove(KEY_REMINDERS_ENABLED)
            remove(KEY_SHARE_MILESTONES)
            remove(KEY_SHOW_HABIT_NAMES)
            remove(KEY_SETTINGS_UPDATED_AT)
            apply()
        }
    }

    fun isRemindersEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

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
