package com.example.motionpulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.data.repository.AuthRepository
import com.example.motionpulse.data.repository.HabitRepository
import com.example.motionpulse.domain.models.DayMoodHabitData
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.domain.models.WeeklyRhythmUiState
import com.example.motionpulse.domain.scoring.GamificationEngine
import com.example.motionpulse.domain.scoring.HabitScoreCalculator
import com.example.motionpulse.domain.scoring.StreakCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class StatsViewMode { WEEK, MONTH }

class StatsViewModel(
    private val db: AppDatabase,
    private val userId: String,
    private val habitRepository: HabitRepository = HabitRepository(db),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _viewMode = MutableStateFlow(StatsViewMode.WEEK)
    val viewMode: StateFlow<StatsViewMode> = _viewMode.asStateFlow()

    private val _celebrationEvent = MutableSharedFlow<Unit>()
    val celebrationEvent: SharedFlow<Unit> = _celebrationEvent.asSharedFlow()

    init {
        if (userId.isNotEmpty()) {
            // Maintains real-time local persistence of the user's remote profile.
            viewModelScope.launch {
                authRepository.getUserProfileFlow(userId).collect { remoteProfile ->
                    remoteProfile?.let {
                        db.userProfileDao().insertProfile(it)
                    }
                }
            }

            // Handles multi-layered synchronization for habits.
            // Completions are synced individually via repository snapshots.
            viewModelScope.launch {
                habitRepository.getRemoteHabitsFlow(userId).collect { remoteHabits ->
                    for (habit in remoteHabits) {
                        db.habitDao().insertHabit(habit)
                    }
                }
            }
        }
    }

    val userProfile: StateFlow<UserProfileEntity?> = db.userProfileDao()
        .getProfile(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayMood: StateFlow<com.example.motionpulse.data.local.entity.MoodEntity?> = db.moodDao()
        .getMoodFlowForDate(userId, LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val summaryStats: StateFlow<SummaryStats> = db.habitDao().getHabitsForUser(userId)
        .combine(db.habitCompletionDao().getAllCompletionsFlow()) { habits, completions ->
            val today = LocalDate.now()
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }
            
            // Previous week range
            val startOfPrevWeek = startOfWeek.minusWeeks(1)
            val prevWeekDates = (0..6).map { startOfPrevWeek.plusDays(it.toLong()) }

            val habitCompletionsMap = completions.groupBy { it.habitId }
            
            // Done Today: count of today's scheduled habits with a COMPLETED record
            val scheduledToday = habits.filter { habit ->
                FrequencyConfig.decodeSafe(habit.frequencyConfig).isScheduled(today)
            }
            val doneToday = scheduledToday.count { habit ->
                habitCompletionsMap[habit.id]?.any { it.date == today && it.status == CompletionStatus.COMPLETED } ?: false
            }
            
            val totalCompleted = completions.count { it.status == CompletionStatus.COMPLETED }
            val currentStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
            
            // Aggregates performance metrics for the current week (Monday–Sunday).
            var totalScheduledWeek = 0
            var totalCompletedWeek = 0
            for (habit in habits) {
                val config = FrequencyConfig.decodeSafe(habit.frequencyConfig)
                val habitCompletions = habitCompletionsMap[habit.id] ?: emptyList()
                for (date in weekDates) {
                    if (config.isScheduled(date)) {
                        totalScheduledWeek++
                        if (habitCompletions.any { it.date == date && it.status == CompletionStatus.COMPLETED }) {
                            totalCompletedWeek++
                        }
                    }
                }
            }
            
            val weeklyPercent = if (totalScheduledWeek > 0) {
                (totalCompletedWeek.toFloat() / totalScheduledWeek * 100).toInt()
            } else 0

            // Compares the current week's performance against the previous week to determine the trend.
            var totalScheduledPrev = 0
            var totalCompletedPrev = 0
            for (habit in habits) {
                val config = FrequencyConfig.decodeSafe(habit.frequencyConfig)
                val habitCompletions = habitCompletionsMap[habit.id] ?: emptyList()
                for (date in prevWeekDates) {
                    if (config.isScheduled(date)) {
                        totalScheduledPrev++
                        if (habitCompletions.any { it.date == date && it.status == CompletionStatus.COMPLETED }) {
                            totalCompletedPrev++
                        }
                    }
                }
            }
            val prevWeeklyPercent = if (totalScheduledPrev > 0) {
                (totalCompletedPrev.toFloat() / totalScheduledPrev * 100).toInt()
            } else 0

            SummaryStats(
                doneToday = doneToday,
                totalHabits = scheduledToday.size,
                currentStreak = currentStreak,
                totalCompleted = totalCompleted,
                weeklyPercent = weeklyPercent,
                weeklyTrend = weeklyPercent - prevWeeklyPercent
            )
        }
        .catch { emit(SummaryStats()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryStats())

    private val weeklyRawDataFlow = combine(
        db.moodDao().getMoodsForDateRange(
            userId, 
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        ),
        db.habitDao().getHabitsForUser(userId),
        db.habitCompletionDao().getAllCompletionsFlow()
    ) { moods, habits, completions ->
        Triple(moods, habits, completions)
    }

    val weeklyCorrelationData: StateFlow<com.example.motionpulse.domain.stats.WeeklyCorrelationData> = weeklyRawDataFlow.map { (moods, habits, completions) ->
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        
        val days = (0..6).map { i ->
            val date = startOfWeek.plusDays(i.toLong())
            val mood = moods.find { it.date == date }
            
            val habitCompletions = habits.filter { 
                FrequencyConfig.decodeSafe(it.frequencyConfig).isScheduled(date) 
            }.map { habit ->
                completions.any { it.habitId == habit.id && it.date == date && it.status == CompletionStatus.COMPLETED }
            }
            
            com.example.motionpulse.domain.stats.DayData(
                date = date,
                moodLevel = mood?.moodLevel,
                habitCompletions = habitCompletions
            )
        }
        com.example.motionpulse.domain.stats.WeeklyCorrelationData(days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.motionpulse.domain.stats.WeeklyCorrelationData())

    val correlationInsight: StateFlow<String> = weeklyRawDataFlow.map { (moods, habits, completions) ->
        com.example.motionpulse.domain.scoring.MoodHabitInsightGenerator.generateInsight(
            moods, habits, completions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val bestDayInfo: StateFlow<BestDayInfo?> = weeklyCorrelationData.map { state ->
        state.days.filter { it.moodLevel != null }.maxByOrNull { day ->
            val moodScore = moodToScore(day.moodLevel!!)
            val totalHabits = day.habitCompletions.size
            val habitsDone = day.habitCompletions.count { it }
            val habitRatio = if (totalHabits > 0) habitsDone.toFloat() / totalHabits else 0f
            moodScore + (habitRatio * 5)
        }?.let { bestDayData ->
            BestDayInfo(
                date = bestDayData.date,
                moodLevel = bestDayData.moodLevel!!,
                habitsDone = bestDayData.habitCompletions.count { it },
                totalHabits = maxOf(1, bestDayData.habitCompletions.size)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun scoreToMoodLevel(score: Int): com.example.motionpulse.data.local.entity.MoodLevel {
        return when (score) {
            1 -> com.example.motionpulse.data.local.entity.MoodLevel.DRAINED
            2 -> com.example.motionpulse.data.local.entity.MoodLevel.LOW
            3 -> com.example.motionpulse.data.local.entity.MoodLevel.STEADY
            4 -> com.example.motionpulse.data.local.entity.MoodLevel.GOOD
            else -> com.example.motionpulse.data.local.entity.MoodLevel.ENERGIZED
        }
    }

    private fun moodToScore(level: com.example.motionpulse.data.local.entity.MoodLevel): Int = when (level) {
        com.example.motionpulse.data.local.entity.MoodLevel.DRAINED -> 1
        com.example.motionpulse.data.local.entity.MoodLevel.LOW -> 2
        com.example.motionpulse.data.local.entity.MoodLevel.STEADY -> 3
        com.example.motionpulse.data.local.entity.MoodLevel.GOOD -> 4
        com.example.motionpulse.data.local.entity.MoodLevel.ENERGIZED -> 5
    }

    // Dedicated celebration trigger
    init {
        summaryStats
            .map { it.weeklyPercent }
            .distinctUntilChanged()
            .onEach { percent ->
                if (percent == 100) {
                    _celebrationEvent.emit(Unit)
                }
            }
            .launchIn(viewModelScope)
    }

    val habitConsistencyStats: StateFlow<List<HabitConsistencyStatus>> = combine(
        db.habitDao().getHabitsForUser(userId),
        db.habitCompletionDao().getAllCompletionsFlow(),
        _viewMode
    ) { habits, completions, mode ->
        val today = LocalDate.now()
        val habitCompletionsMap = completions.groupBy { it.habitId }
        val dateRange = if (mode == StatsViewMode.WEEK) {
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0..6).map { startOfWeek.plusDays(it.toLong()) }
        } else {
            val startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
            (0 until today.lengthOfMonth()).map { startOfMonth.plusDays(it.toLong()) }
        }

        habits.map { habit ->
            val habitCompletions = habitCompletionsMap[habit.id] ?: emptyList()
            val gridStatus = dateRange.map { date ->
                val completion = habitCompletions.find { it.date == date }
                
                DayStatus(
                    date = date,
                    status = completion?.status ?: CompletionStatus.MISSED,
                    valueLogged = completion?.numericValueLogged
                )
            }
            
            HabitConsistencyStatus(
                habit = habit,
                dayStatuses = gridStatus,
                completedCount = gridStatus.count { it.status == CompletionStatus.COMPLETED }
            )
        }
    }
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setViewMode(mode: StatsViewMode) {
        _viewMode.value = mode
    }

    fun logHabitProgress(habit: HabitEntity, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            try {
                val completionId = UUID.randomUUID().toString()
                val completion = HabitCompletionEntity(
                    id = completionId,
                    habitId = habit.id,
                    date = date,
                    status = CompletionStatus.COMPLETED,
                    numericValueLogged = habit.numericTarget,
                    updatedAt = Instant.now()
                )
                db.habitCompletionDao().insertCompletion(completion)

                // Full recalculation
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

                if (userProfile.value != null) {
                    val isNewPb = newCurrentStreak > habit.longestStreak
                    val xpResult = GamificationEngine.calculateXpGain(userProfile.value!!.totalAuraXp, isNewPb)
                    db.userProfileDao().updateProfile(userProfile.value!!.copy(
                        totalAuraXp = xpResult.newTotalXp,
                        currentLevel = xpResult.newLevel
                    ))

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
                        db.badgeDao().insertBadge(com.example.motionpulse.data.local.entity.BadgeEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            badgeType = badgeType,
                            unlockedAt = Instant.now(),
                            triggeredByCompletionId = completionId
                        ))
                    }
                }

                habitRepository.saveCompletion(userId, completion)
                habitRepository.saveHabit(userId, updatedHabit)
            } catch (e: Exception) {}
        }
    }

    fun removeHabitProgress(habit: HabitEntity, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            try {
                val completion = db.habitCompletionDao().getAllCompletionsFlow().first().find { it.habitId == habit.id && it.date == date }
                if (completion != null) {
                    // Revoke Badges
                    val badges = db.badgeDao().getBadgesForUser(userId).first().filter { it.triggeredByCompletionId == completion.id }
                    for (badge in badges) {
                        db.badgeDao().deleteBadge(badge)
                    }
                    
                    // Deduct XP
                    val allCompletionsBefore = db.habitCompletionDao().getAllCompletionsForHabit(habit.id).first()
                    val freqConfig = FrequencyConfig.decodeSafe(habit.frequencyConfig)
                    val streakBefore = StreakCalculator.calculateCurrentStreak(allCompletionsBefore, freqConfig)
                    
                    db.habitCompletionDao().deleteCompletion(completion)
                    habitRepository.deleteCompletion(userId, habit.id, date)
                    
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
            } catch (e: Exception) {}
        }
    }

    class Factory(private val db: AppDatabase, private val userId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(db, userId) as T
        }
    }
}

data class SummaryStats(
    val doneToday: Int = 0,
    val totalHabits: Int = 0,
    val currentStreak: Int = 0,
    val totalCompleted: Int = 0,
    val weeklyPercent: Int = 0,
    val weeklyTrend: Int? = null
)

data class HabitConsistencyStatus(
    val habit: HabitEntity,
    val dayStatuses: List<DayStatus>,
    val completedCount: Int
)

data class DayStatus(
    val date: LocalDate,
    val status: CompletionStatus,
    val valueLogged: Float? = null
)

data class BestDayInfo(
    val date: LocalDate,
    val moodLevel: com.example.motionpulse.data.local.entity.MoodLevel,
    val habitsDone: Int,
    val totalHabits: Int
)
