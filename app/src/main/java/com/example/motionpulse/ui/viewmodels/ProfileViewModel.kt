package com.example.motionpulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.BadgeEntity
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val db: AppDatabase,
    private val userId: String,
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    init {
        // Establishes a real-time listener for the user's profile document to ensure the UI stays in sync.
        viewModelScope.launch {
            authRepository.getUserProfileFlow(userId).collect { remoteProfile ->
                remoteProfile?.let {
                    db.userProfileDao().insertProfile(it)
                }
            }
        }
    }

    /**
     * Aggregates user profile, badges, and habit data into a unified UI state.
     * Calculates high-level metrics like streaks and total achievements.
     */
    val profileState: StateFlow<ProfileState> = combine(
        db.userProfileDao().getProfile(userId),
        db.badgeDao().getBadgesForUser(userId),
        db.habitDao().getHabitsForUser(userId)
    ) { profile, badges, habits ->
        val currentStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        val totalBadges = badges.size
        
        ProfileState(
            userProfile = profile,
            badges = badges.sortedByDescending { it.unlockedAt },
            currentStreak = currentStreak,
            badgeCount = totalBadges,
            linkedProviders = FirebaseAuth.getInstance().currentUser?.providerData?.map { it.providerId } ?: emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileState())

    /**
     * Updates the user's public display name in both Auth and Firestore.
     */
    fun updateName(newName: String) {
        viewModelScope.launch {
            authRepository.updateDisplayName(newName)
        }
    }

    /**
     * Toggles the global reminder setting and persists it to the backend.
     */
    fun toggleReminders(enabled: Boolean, context: android.content.Context) {
        val prefs = com.example.motionpulse.data.repository.PreferenceRepository(context)
        prefs.setRemindersEnabled(enabled)

        viewModelScope.launch {
            authRepository.updateUserSetting("remindersEnabled", enabled)
            profileState.value.userProfile?.let {
                db.userProfileDao().updateProfile(it.copy(remindersEnabled = enabled))
            }
            
            val reminderManager = com.example.motionpulse.ui.notifications.ReminderManager(context)
            if (enabled) {
                val habits = db.habitDao().getAllActiveHabits().first()
                reminderManager.scheduleAllReminders(habits)
            } else {
                val habits = db.habitDao().getAllActiveHabits().first()
                reminderManager.cancelAllReminders(habits.map { it.id })
            }
        }
    }

    /**
     * Updates the user's preferred application language.
     */
    fun changeLanguage(language: String) {
        viewModelScope.launch {
            authRepository.updateUserSetting("language", language)
            profileState.value.userProfile?.let {
                db.userProfileDao().updateProfile(it.copy(language = language))
            }
        }
    }

    /**
     * Verifies the user's identity before allowing sensitive account modifications.
     */
    suspend fun reauthenticate(password: String) = authRepository.reauthenticate(password)

    /**
     * Updates the user's account password.
     */
    suspend fun updatePassword(newPassword: String) = authRepository.updatePassword(newPassword)

    /**
     * Retrieves all completion records for the purpose of data export.
     */
    suspend fun getAllCompletions(): List<HabitCompletionEntity> {
        return db.habitCompletionDao().getAllCompletionsFlow().first()
    }

    /**
     * Permanently deletes the user's account and all associated data from the cloud.
     */
    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val uid = user.uid
                
                // Wipes user data from the Firestore collection.
                FirebaseFirestore.getInstance().collection("users").document(uid).delete().await()
                
                // Removes the user account from the Firebase Authentication system.
                user.delete().await()
                
                onComplete()
            } catch (e: Exception) {
                // Deletion may fail if the user's session has expired, requiring a fresh login.
            }
        }
    }

    class Factory(private val db: AppDatabase, private val userId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(db, userId) as T
        }
    }
}

data class ProfileState(
    val userProfile: UserProfileEntity? = null,
    val badges: List<BadgeEntity> = emptyList(),
    val currentStreak: Int = 0,
    val badgeCount: Int = 0,
    val linkedProviders: List<String> = emptyList()
)
