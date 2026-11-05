package com.example.motionpulse.domain.models

import com.google.firebase.firestore.PropertyName

data class UserSettings(
    val language: String? = null,
    @get:PropertyName("remindersEnabled") @set:PropertyName("remindersEnabled") var remindersEnabled: Boolean = true,
    @get:PropertyName("shareMilestones") @set:PropertyName("shareMilestones") var shareMilestones: Boolean = true,
    @get:PropertyName("showHabitNames") @set:PropertyName("showHabitNames") var showHabitNames: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
