package com.example.motionpulse.data.repository

import android.content.Context
import com.example.motionpulse.domain.models.UserSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val preferenceRepository: PreferenceRepository = PreferenceRepository(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var listenerRegistration: ListenerRegistration? = null
    private var currentUserId: String? = null

    private val _userSettings = MutableStateFlow(preferenceRepository.getUserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private var syncJob: Job? = null

    fun current(): UserSettings = _userSettings.value

    fun startListener(userId: String) {
        if (userId.isEmpty()) return
        if (currentUserId == userId && listenerRegistration != null) return

        stopListener()
        currentUserId = userId

        val docRef = firestore.collection("users").document(userId).collection("settings").document("preferences")
        listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("SettingsRepository", "Error listening to settings for $userId: ${error.message}", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val cloudSettings = snapshot.toObject(UserSettings::class.java)
                if (cloudSettings != null) {
                    val local = _userSettings.value
                    if (cloudSettings.updatedAt >= local.updatedAt) {
                        _userSettings.value = cloudSettings
                        preferenceRepository.saveUserSettings(cloudSettings)
                    }
                }
            } else {
                // First login on device: push local defaults to cloud
                scope.launch {
                    try {
                        docRef.set(_userSettings.value, SetOptions.merge()).await()
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsRepository", "Failed to initialize cloud settings: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun stopListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
        currentUserId = null
    }

    fun clearOnLogout() {
        stopListener()
        preferenceRepository.clearUserSettingsOnLogout()
        _userSettings.value = preferenceRepository.getUserSettings()
    }

    fun updateSettings(newSettings: UserSettings, onError: (() -> Unit)? = null) {
        val previous = _userSettings.value
        val updated = newSettings.copy(updatedAt = System.currentTimeMillis())
        
        // 1. Write locally first
        _userSettings.value = updated
        preferenceRepository.saveUserSettings(updated)

        // 2. Debounce sync to Firestore (300ms)
        syncJob?.cancel()
        syncJob = scope.launch {
            delay(300)
            val userId = currentUserId ?: return@launch
            try {
                firestore.collection("users").document(userId)
                    .collection("settings").document("preferences")
                    .set(updated, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("SettingsRepository", "Failed to sync settings to Firestore: ${e.message}", e)
                // Revert on error
                _userSettings.value = previous
                preferenceRepository.saveUserSettings(previous)
                onError?.invoke()
            }
        }
    }

    fun setLanguage(language: String?, onError: (() -> Unit)? = null) {
        updateSettings(_userSettings.value.copy(language = language), onError)
    }

    fun setRemindersEnabled(enabled: Boolean, onError: (() -> Unit)? = null) {
        updateSettings(_userSettings.value.copy(remindersEnabled = enabled), onError)
    }

    fun setShareMilestones(enabled: Boolean, onError: (() -> Unit)? = null) {
        updateSettings(_userSettings.value.copy(shareMilestones = enabled), onError)
    }

    fun setShowHabitNames(enabled: Boolean, onError: (() -> Unit)? = null) {
        updateSettings(_userSettings.value.copy(showHabitNames = enabled), onError)
    }
}
