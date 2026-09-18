package com.example.motionpulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.BadgeType
import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.GoalType
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.domain.scoring.GamificationEngine
import com.example.motionpulse.domain.scoring.HabitScoreCalculator
import com.example.motionpulse.domain.scoring.StreakCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import com.example.motionpulse.data.repository.AuthRepository
import com.example.motionpulse.data.repository.HabitRepository

class HabitsViewModel(
    private val db: AppDatabase,
    private val userId: String,
    private val habitRepository: HabitRepository = HabitRepository(db),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _badgeEvent = MutableSharedFlow<BadgeType>()
    val badgeEvent: SharedFlow<BadgeType> = _badgeEvent.asSharedFlow()

    private val _syncingHabitIds = MutableStateFlow<Set<String>>(emptySet())
    val syncingHabitIds: StateFlow<Set<String>> = _syncingHabitIds.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow<Instant?>(null)
    val lastSyncedTime: StateFlow<Instant?> = _lastSyncedTime.asStateFlow()

    init {
        if (userId.isNotEmpty()) {
            // Synchronizes the user profile from the remote data source in real-time.
            viewModelScope.launch {
                authRepository.getUserProfileFlow(userId)
                    .catch { e -> android.util.Log.e("HabitsViewModel", "Profile flow error for $userId", e) }
                    .collect { remoteProfile ->
                        remoteProfile?.let {
                            db.userProfileDao().insertProfile(it)
                        }
                    }
            }

            // Manages real-time habit synchronization with Firestore.
            // Automatically merges duplicate records by title and updates the local database.
            viewModelScope.launch {
                habitRepository.getRemoteHabitsFlow(userId)
                    .catch { e -> 
                        android.util.Log.e("HabitsViewModel", "Habits flow error for $userId", e)
                        _isLoading.value = false 
                    }
                    .collect { remoteHabits ->
                        for (remoteHabit in remoteHabits) {
                            if (remoteHabit.ownerId == userId) {
                                // Performs a title-based check to prevent multiple records for the same habit name.
                                val duplicateByTitle = db.habitDao().getHabitByTitle(userId, remoteHabit.title)
                                if (duplicateByTitle != null && duplicateByTitle.id != remoteHabit.id) {
                                    // Resolves conflicts by preserving the record with the most recent update timestamp.
                                    if (remoteHabit.updatedAt.isAfter(duplicateByTitle.updatedAt)) {
                                        db.habitDao().updateHabit(duplicateByTitle.copy(isArchived = true))
                                        db.habitDao().insertHabit(remoteHabit)
                                    } else if (!remoteHabit.isArchived) {
                                        // If the remote record is older and not already archived, mark it as archived in Firestore to clean up the cloud data.
                                        launch {
                                            try {
                                                habitRepository.saveHabit(userId, remoteHabit.copy(isArchived = true))
                                            } catch (e: Exception) { /* Automatic retry handled by sync worker */ }
                                        }
                                    }
                                } else {
                                    // Executes standard ID-based synchronization.
                                    val localHabit = db.habitDao().getHabitById(remoteHabit.id)
                                    if (localHabit == null) {
                                        db.habitDao().insertHabit(remoteHabit)
                                    } else if (remoteHabit.updatedAt.isAfter(localHabit.updatedAt)) {
                                        db.habitDao().updateHabit(remoteHabit)
                                    }
                                }
                            }
                        }
                        _lastSyncedTime.value = Instant.now()
                        _isLoading.value = false
                        
                        // Triggers a thorough local deduplication task after the initial sync completes.
                        launch {
                            cleanDuplicatesLocally()
                        }
                    }
            }
        } else {
            _isLoading.value = false
        }
    }

    /**
     * Identifies and archives duplicate habit records to ensure data integrity.
     * Only the most recently updated instance of a habit name is kept active.
     */
    private suspend fun cleanDuplicatesLocally() {
        val allLocalHabits = db.habitDao().getAllActiveHabits().first()
        val groupedByTitle = allLocalHabits.groupBy { it.title.lowercase().trim() }
        
        for ((_, habitGroup) in groupedByTitle) {
            if (habitGroup.size > 1) {
                // Keeps the newest record and archives all redundant instances.
                val toArchive = habitGroup.sortedByDescending { it.updatedAt }.drop(1)
                
                for (habit in toArchive) {
                    val updated = habit.copy(isArchived = true, updatedAt = Instant.now())
                    db.habitDao().updateHabit(updated)
                    try {
                        habitRepository.saveHabit(userId, updated)
                    } catch (e: Exception) { /* Automatic retry handled by sync worker */ }
                }
            }
        }
    }

    /**
     * Triggers a manual foreground synchronization of habit data.
     */
    fun refreshHabits() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                habitRepository.getRemoteHabitsFlow(userId).first()
                _lastSyncedTime.value = Instant.now()
            } catch (e: Exception) {
                // Network errors are ignored here as the real-time listener provides eventual consistency.
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val quotes = listOf(
        "Consistency is key",
        "Small steps lead to big results",
        "Discipline is choosing between what you want now and what you want most",
        "The secret of your future is hidden in your daily routine",
        "Habits are the compound interest of self-improvement",
        "Your rhythm defines your life",
        "Master your habits, master your destiny"
    )

    val dailyQuote: String
        get() = quotes[LocalDate.now().dayOfYear % quotes.size]

    val userProfile: StateFlow<UserProfileEntity?> = db.userProfileDao()
        .getProfile(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val habits: StateFlow<List<HabitWithStatus>> = db.habitDao()
        .getHabitsForUser(userId)
        .combine(_selectedCategory) { habitList, category ->
            if (category == null) habitList else habitList.filter { it.category == category }
        }
        .combine(_searchQuery) { habitList, query ->
            if (query.isBlank()) habitList else habitList.filter { it.title.contains(query, ignoreCase = true) }
        }
        .combine(db.habitCompletionDao().getAllCompletionsFlow()) { habitList, allCompletions ->
            Pair(habitList, allCompletions)
        }
        .combine(_syncingHabitIds) { pair, syncingIds ->
            val habitList = pair.first
            val allCompletions = pair.second
            val today = LocalDate.now()
            val now = LocalTime.now()

            habitList
                .filter { habit ->
                    val config = FrequencyConfig.decodeSafe(habit.frequencyConfig)
                    config.isScheduled(today)
                }
                .map { habit ->
                    val todayCompletion = allCompletions.find { it.habitId == habit.id && it.date == today }
                    val isCompleted = todayCompletion?.status == CompletionStatus.COMPLETED

                    val isOverdue = if (!isCompleted && habit.reminderTime != null) {
                        try {
                            val reminderTime = LocalTime.parse(habit.reminderTime)
                            reminderTime.isBefore(now)
                        } catch (e: Exception) { false }
                    } else false

                    HabitWithStatus(
                        habit = habit,
                        isCompletedToday = isCompleted,
                        isSyncing = syncingIds.contains(habit.id),
                        isOverdue = isOverdue
                    )
                }.distinctBy { it.habit.title.lowercase().trim() }
// Deep Deduplication
                .sortedWith(
                    compareBy<HabitWithStatus> { it.isCompletedToday } // Completed at bottom
                    .thenByDescending { it.isOverdue } // Overdue at top
                    .thenBy { it.habit.manualOrder } // Manual priority
                    .thenBy { it.habit.reminderTime ?: "23:59" } // Then by time
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitWithStatus>> = db.habitDao()
        .getHabitsForUser(userId)
        .combine(_selectedCategory) { habitList, category ->
            if (category == null) habitList else habitList.filter { it.category == category }
        }
        .combine(_searchQuery) { habitList, query ->
            if (query.isBlank()) habitList else habitList.filter { it.title.contains(query, ignoreCase = true) }
        }
        .combine(db.habitCompletionDao().getAllCompletionsFlow()) { habitList, allCompletions ->
            Pair(habitList, allCompletions)
        }
        .combine(_syncingHabitIds) { pair, syncingIds ->
            val habitList = pair.first
            val allCompletions = pair.second
            val today = LocalDate.now()
            val oneDayAgo = Instant.now().minus(java.time.Duration.ofDays(1))

            habitList.map { habit ->
                val todayCompletion = allCompletions.find { it.habitId == habit.id && it.date == today }
                val isRecent = habit.createdAt.isAfter(oneDayAgo)
                val isCompleted = todayCompletion?.status == CompletionStatus.COMPLETED

                HabitWithStatus(
                    habit = habit,
                    isCompletedToday = isCompleted,
                    isSyncing = syncingIds.contains(habit.id),
                    isRecent = isRecent
                )
            }.distinctBy { it.habit.title.lowercase().trim() } // Deep Deduplication
            .sortedWith(
                compareByDescending<HabitWithStatus> { it.isRecent } // Newest at front
                .thenBy { it.habit.manualOrder } // Then manual priority
                .thenBy { it.habit.reminderTime ?: "23:59" }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedHabits: StateFlow<List<HabitEntity>> = db.habitDao()
        .getArchivedHabitsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestionsMap = mapOf(
        "Mind & Focus" to listOf("Read a Book", "Meditate", "Journaling"),
        "Fitness & Health" to listOf("Go for a Run", "30 Push-ups", "Drink Water"),
        "Daily Routine" to listOf("Wash My Hair", "Clean Desk", "Early Wake-up")
    )

    fun getSuggestionsFor(category: String): List<String> {
        return suggestionsMap[category] ?: emptyList()
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun reorderHabit(habit: HabitEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val habits = allHabits.value.map { it.habit }
            val index = habits.indexOfFirst { it.id == habit.id }
            if (index == -1) return@launch
            
            val newIndex = if (moveUp) index - 1 else index + 1
            if (newIndex !in habits.indices) return@launch
            
            val targetHabit = habits[newIndex]
            
            // If they have the same order (default migration case), 
            // we need to re-index or adjust more than just two.
            // But for simple swap:
            val updatedHabit: HabitEntity
            val updatedTarget: HabitEntity
            
            if (habit.manualOrder == targetHabit.manualOrder) {
                // Fix: if they are equal, give the target a clear separation
                updatedHabit = habit.copy(manualOrder = if (moveUp) targetHabit.manualOrder - 1 else targetHabit.manualOrder + 1, updatedAt = Instant.now())
                updatedTarget = targetHabit
            } else {
                // Standard swap
                updatedHabit = habit.copy(manualOrder = targetHabit.manualOrder, updatedAt = Instant.now())
                updatedTarget = targetHabit.copy(manualOrder = habit.manualOrder, updatedAt = Instant.now())
            }
            
            db.habitDao().updateHabit(updatedHabit)
            db.habitDao().updateHabit(updatedTarget)
            
            try {
                habitRepository.saveHabit(userId, updatedHabit)
                habitRepository.saveHabit(userId, updatedTarget)
            } catch (e: Exception) { /* Sync later */ }
        }
    }

    fun addHabit(
        title: String,
        category: String,
        goalType: GoalType,
        frequencyType: FrequencyType,
        frequencyConfig: FrequencyConfig,
        reminderTime: String?,
        numericTarget: Float? = null,
        numericUnit: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val maxOrder = allHabits.value.maxOfOrNull { it.habit.manualOrder } ?: 0
            val habit = HabitEntity(
                id = UUID.randomUUID().toString(),
                ownerId = userId,
                title = title,
                category = category,
                colorTag = "default",
                goalType = goalType,
                numericTarget = numericTarget,
                numericUnit = numericUnit,
                frequencyType = frequencyType,
                frequencyConfig = Json.encodeToString(frequencyConfig),
                reminderTime = reminderTime,
                manualOrder = maxOrder + 1,
                updatedAt = Instant.now()
            )
            
            try {
                // 1. Write locally
                db.habitDao().insertHabit(habit)
                
                // 2. Sync to Firestore
                _syncingHabitIds.update { it + habit.id }
                habitRepository.saveHabit(userId, habit)
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to sync habit. It is saved locally.")
            } finally {
                _syncingHabitIds.update { it - habit.id }
            }
        }
    }

    /**
     * Modifies an existing habit's configuration and synchronizes the changes with Firestore.
     */
    fun updateHabit(
        habitId: String,
        title: String,
        category: String,
        goalType: GoalType,
        frequencyType: FrequencyType,
        frequencyConfig: FrequencyConfig,
        reminderTime: String?,
        numericTarget: Float? = null,
        numericUnit: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val existing = db.habitDao().getHabitById(habitId) ?: return@launch
            val updated = existing.copy(
                title = title,
                category = category,
                goalType = goalType,
                frequencyType = frequencyType,
                frequencyConfig = Json.encodeToString(frequencyConfig),
                reminderTime = reminderTime,
                numericTarget = numericTarget,
                numericUnit = numericUnit,
                updatedAt = Instant.now()
            )
            
            try {
                db.habitDao().updateHabit(updated)
                _syncingHabitIds.update { it + habitId }
                habitRepository.saveHabit(userId, updated)
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to sync changes. Saved locally.")
            } finally {
                _syncingHabitIds.update { it - habitId }
            }
        }
    }

    /**
     * Determines if a habit with the specified title already exists for the user.
     */
    fun isNameDuplicate(title: String, excludeId: String? = null): Boolean {
        return allHabits.value.any { it.habit.title.equals(title, ignoreCase = true) && it.habit.id != excludeId }
    }

    /**
     * Marks a habit as archived to hide it from active views while preserving its history.
     */
    fun archiveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val updatedHabit = habit.copy(isArchived = true, updatedAt = Instant.now())
            db.habitDao().updateHabit(updatedHabit)
            _syncingHabitIds.update { it + habit.id }
            try {
                habitRepository.archiveHabit(userId, habit.id)
            } catch (e: Exception) {
                // Background sync task will retry persistence.
            } finally {
                _syncingHabitIds.update { it - habit.id }
            }
        }
    }

    /**
     * Restores an archived habit to the active management list.
     */
    fun unarchiveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val now = Instant.now()
            db.habitDao().unarchiveHabit(habit.id, now)
            _syncingHabitIds.update { it + habit.id }
            try {
                // Pushes the updated archive status to the remote store.
                habitRepository.saveHabit(userId, habit.copy(isArchived = false, updatedAt = now))
            } catch (e: Exception) {
                // Background sync will retry persistence.
            } finally {
                _syncingHabitIds.update { it - habit.id }
            }
        }
    }

    fun logHabitProgress(habit: HabitEntity, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            _syncingHabitIds.update { it + habit.id }
            
            try {
                val completionId = UUID.randomUUID().toString()
                val completion = HabitCompletionEntity(
                    id = completionId,
                    habitId = habit.id,
                    date = date,
                    status = CompletionStatus.COMPLETED,
                    numericValueLogged = habit.numericTarget, // Full goal for numeric
                    updatedAt = Instant.now()
                )
                db.habitCompletionDao().insertCompletion(completion)

                // Full recalculation from history for accuracy
                val allCompletions = db.habitCompletionDao().getAllCompletionsForHabit(habit.id).first()
                val freqConfig = FrequencyConfig.decodeSafe(habit.frequencyConfig)
                
                val newScore = HabitScoreCalculator.calculateScore(0f, allCompletions, freqConfig, habit.numericTarget)
                val newCurrentStreak = StreakCalculator.calculateCurrentStreak(allCompletions, freqConfig)
                val newLongestStreak = maxOf(habit.longestStreak, newCurrentStreak)

                val updatedHabit = habit.copy(
                    habitStrengthScore = newScore,
                    currentStreak = newCurrentStreak,
                    longestStreak = newLongestStreak,
                    updatedAt = Instant.now()
                )
                db.habitDao().updateHabit(updatedHabit)

                // Update XP
                val profile = userProfile.value
                if (profile != null) {
                    val isNewPb = newCurrentStreak > habit.longestStreak
                    val xpResult = GamificationEngine.calculateXpGain(profile.totalAuraXp, isNewPb)
                    db.userProfileDao().updateProfile(profile.copy(
                        totalAuraXp = xpResult.newTotalXp,
                        currentLevel = xpResult.newLevel
                    ))

                    // Badge Check
                    val currentBadges = db.badgeDao().getBadgesForUser(userId).first().map { it.badgeType }.toSet()
                    val totalHabits = db.habitDao().getAllActiveHabits().first().size
                    
                    val newBadges = GamificationEngine.checkBadgeUnlocks(
                        userId = userId,
                        currentStreak = newCurrentStreak,
                        longestStreak = newLongestStreak,
                        totalHabitsCount = totalHabits,
                        hasFriends = false,
                        alreadyUnlockedBadges = currentBadges
                    )
                    
                    for (badgeType in newBadges) {
                        val badgeId = UUID.randomUUID().toString()
                        db.badgeDao().insertBadge(com.example.motionpulse.data.local.entity.BadgeEntity(
                            id = badgeId,
                            userId = userId,
                            badgeType = badgeType,
                            unlockedAt = Instant.now(),
                            triggeredByCompletionId = completionId
                        ))
                        _badgeEvent.emit(badgeType)
                    }
                }

                // Push to Firestore
                habitRepository.saveCompletion(userId, completion)
                habitRepository.saveHabit(userId, updatedHabit)

            } catch (e: Exception) {
                // Background sync worker will retry
            } finally {
                _syncingHabitIds.update { it - habit.id }
            }
        }
    }

    fun removeHabitProgress(habit: HabitEntity, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            _syncingHabitIds.update { it + habit.id }
            try {
                val completion = db.habitCompletionDao().getAllCompletionsFlow().first().find { it.habitId == habit.id && it.date == date }
                if (completion != null) {
                    // Revoke Badges
                    val badges = db.badgeDao().getBadgesForUser(userId).first().filter { it.triggeredByCompletionId == completion.id }
                    for (badge in badges) {
                        db.badgeDao().deleteBadge(badge)
                    }
                    
                    // Deduct XP (Base XP + PB if it was one)
                    val allCompletionsBefore = db.habitCompletionDao().getAllCompletionsForHabit(habit.id).first()
                    val freqConfig = FrequencyConfig.decodeSafe(habit.frequencyConfig)
                    val streakBefore = StreakCalculator.calculateCurrentStreak(allCompletionsBefore, freqConfig)
                    
                    db.habitCompletionDao().deleteCompletion(completion)
                    habitRepository.deleteCompletion(userId, habit.id, date)
                    
                    // Re-calculate habit
                    val allCompletionsAfter = db.habitCompletionDao().getAllCompletionsForHabit(habit.id).first()
                    val newScore = HabitScoreCalculator.calculateScore(0f, allCompletionsAfter, freqConfig, habit.numericTarget)
                    val newCurrentStreak = StreakCalculator.calculateCurrentStreak(allCompletionsAfter, freqConfig)
                    val newLongestStreak = StreakCalculator.calculateLongestStreak(allCompletionsAfter, freqConfig)
                    
                    val updatedHabit = habit.copy(
                        habitStrengthScore = newScore,
                        currentStreak = newCurrentStreak,
                        longestStreak = newLongestStreak,
                        updatedAt = Instant.now()
                    )
                    db.habitDao().updateHabit(updatedHabit)
                    habitRepository.saveHabit(userId, updatedHabit)
                    
                    // Revert XP
                    val profile = userProfile.value
                    if (profile != null) {
                        val isWasPb = streakBefore > habit.longestStreak
                        val xpToDeduct = GamificationEngine.BASE_COMPLETION_XP + if (isWasPb) GamificationEngine.NEW_PERSONAL_BEST_BONUS else 0L
                        val newTotalXp = maxOf(0, profile.totalAuraXp - xpToDeduct)
                        db.userProfileDao().updateProfile(profile.copy(
                            totalAuraXp = newTotalXp,
                            currentLevel = (newTotalXp / 500L).toInt() + 1
                        ))
                    }
                }
            } catch (e: Exception) {
            } finally {
                _syncingHabitIds.update { it - habit.id }
            }
        }
    }

    fun toggleHabitCompletion(habit: HabitEntity) {
        val today = LocalDate.now()
        viewModelScope.launch {
            val existing = db.habitCompletionDao().getAllCompletionsFlow().first().find { it.habitId == habit.id && it.date == today }
            if (existing == null) {
                logHabitProgress(habit, today)
            }
        }
    }

    class Factory(private val db: AppDatabase, private val userId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HabitsViewModel(db, userId) as T
        }
    }
}

data class HabitWithStatus(
    val habit: HabitEntity,
    val isCompletedToday: Boolean,
    val isSyncing: Boolean = false,
    val isOverdue: Boolean = false,
    val isRecent: Boolean = false
)
