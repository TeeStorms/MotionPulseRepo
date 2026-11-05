package com.example.motionpulse.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.data.repository.AuthRepository
import com.example.motionpulse.data.repository.HabitRepository
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.domain.models.FrequencyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class StatsViewModel(
    private val db: AppDatabase,
    private val userId: String,
    private val context: Context,
    private val habitRepository: HabitRepository = HabitRepository(db),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val preferenceRepository = PreferenceRepository(context)

    private val _celebrationEvent = MutableSharedFlow<Unit>()
    val celebrationEvent: SharedFlow<Unit> = _celebrationEvent.asSharedFlow()

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    val weekLabel: StateFlow<String> = _weekOffset.map { offset ->
        val range = weekRange(offset)
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        "${range.start.format(formatter)} - ${range.endInclusive.format(formatter)}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun previousWeek() {
        _weekOffset.value -= 1
    }

    fun nextWeek() {
        if (_weekOffset.value < 0) {
            _weekOffset.value += 1
        }
    }

    init {
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                authRepository.getUserProfileFlow(userId).collect { remoteProfile ->
                    remoteProfile?.let {
                        db.userProfileDao().insertProfile(it)
                    }
                }
            }

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

    val summaryStats: StateFlow<SummaryStats> = combine(
        db.habitDao().getHabitsForUser(userId),
        db.habitCompletionDao().getAllCompletionsFlow(),
        _weekOffset
    ) { habits, completions, offset ->
        val today = LocalDate.now()
        val targetDate = today.plusWeeks(offset.toLong())
        val startOfWeek = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfRange = if (offset == 0) today else startOfWeek.plusDays(6)
        val weekDates = generateSequence(startOfWeek) { it.plusDays(1) }.takeWhile { !it.isAfter(endOfRange) }.toList()
        
        val startOfPrevWeek = startOfWeek.minusWeeks(1)
        val prevWeekDates = (0..6).map { startOfPrevWeek.plusDays(it.toLong()) }

        val habitCompletionsMap = completions.groupBy { it.habitId }
        
        val scheduledToday = habits.filter { habit ->
            !habit.isArchived && FrequencyConfig.decodeSafe(habit.frequencyConfig).isScheduled(today)
        }
        val doneToday = scheduledToday.count { habit ->
            habitCompletionsMap[habit.id]?.any { it.date == today && it.status == CompletionStatus.COMPLETED } ?: false
        }
        
        val totalCompleted = completions.count { it.status == CompletionStatus.COMPLETED }
        val currentStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        
        var totalScheduledWeek = 0
        var totalCompletedWeek = 0
        for (habit in habits) {
            if (habit.isArchived) continue
            val config = FrequencyConfig.decodeSafe(habit.frequencyConfig)
            val habitCompletions = habitCompletionsMap[habit.id] ?: emptyList()
            for (date in weekDates) {
                val createdDate = habit.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                if (!date.isBefore(createdDate) && config.isScheduled(date)) {
                    totalScheduledWeek++
                    if (habitCompletions.any { it.date == date && it.status == CompletionStatus.COMPLETED }) {
                        totalCompletedWeek++
                    }
                }
            }
        }
        
        val weeklyPercent = if (totalScheduledWeek > 0) {
            (totalCompletedWeek.toFloat() / totalScheduledWeek * 100).toInt().coerceIn(0, 100)
        } else 0

        var totalScheduledPrev = 0
        var totalCompletedPrev = 0
        for (habit in habits) {
            if (habit.isArchived) continue
            val config = FrequencyConfig.decodeSafe(habit.frequencyConfig)
            val habitCompletions = habitCompletionsMap[habit.id] ?: emptyList()
            for (date in prevWeekDates) {
                val createdDate = habit.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                if (!date.isBefore(createdDate) && config.isScheduled(date)) {
                    totalScheduledPrev++
                    if (habitCompletions.any { it.date == date && it.status == CompletionStatus.COMPLETED }) {
                        totalCompletedPrev++
                    }
                }
            }
        }
        val weeklyTrendStr = if (totalScheduledPrev > 0) {
            val prevWeeklyPercent = (totalCompletedPrev.toFloat() / totalScheduledPrev * 100).toInt()
            val diff = weeklyPercent - prevWeeklyPercent
            if (diff >= 0) "+$diff%" else "$diff%"
        } else {
            "--"
        }

        SummaryStats(
            doneToday = doneToday,
            totalHabits = scheduledToday.size,
            currentStreak = currentStreak,
            totalCompleted = totalCompleted,
            weeklyPercent = weeklyPercent,
            weeklyTrendStr = weeklyTrendStr
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .catch { emit(SummaryStats()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryStats())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val weeklyRawDataFlow = _weekOffset.flatMapLatest { offset ->
        val range = weekRange(offset)
        combine(
            db.moodDao().getMoodsForDateRange(userId, range.start, range.endInclusive),
            db.habitDao().getHabitsForUser(userId),
            db.habitCompletionDao().getAllCompletionsFlow()
        ) { moods, habits, completions ->
            val days = (0..6).map { i ->
                val date = range.start.plusDays(i.toLong())
                val mood = moods.find { it.date == date }

                val habitCompletions = habits.filter { habit ->
                    !habit.isArchived && FrequencyConfig.decodeSafe(habit.frequencyConfig).isScheduled(date)
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
        }
    }

    val weeklyCorrelationData: StateFlow<com.example.motionpulse.domain.stats.WeeklyCorrelationData> = weeklyRawDataFlow
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.motionpulse.domain.stats.WeeklyCorrelationData())

    val correlationInsight: StateFlow<String> = weeklyRawDataFlow.map { state ->
        val moods = state.days.mapNotNull { day -> day.moodLevel?.let { com.example.motionpulse.data.local.entity.MoodEntity(userId = userId, date = day.date, moodLevel = it) } }
        com.example.motionpulse.domain.scoring.MoodHabitInsightGenerator.generateInsight(
            moods, emptyList(), emptyList()
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

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
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun moodToScore(level: com.example.motionpulse.data.local.entity.MoodLevel): Int = when (level) {
        com.example.motionpulse.data.local.entity.MoodLevel.DRAINED -> 1
        com.example.motionpulse.data.local.entity.MoodLevel.LOW -> 2
        com.example.motionpulse.data.local.entity.MoodLevel.STEADY -> 3
        com.example.motionpulse.data.local.entity.MoodLevel.GOOD -> 4
        com.example.motionpulse.data.local.entity.MoodLevel.ENERGIZED -> 5
    }

    // 100% celebration tracked once per week
    init {
        summaryStats
            .map { it.weeklyPercent }
            .distinctUntilChanged()
            .onEach { percent ->
                if (percent == 100 && _weekOffset.value == 0) {
                    val currentWeekKey = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                    val lastCelebrated = preferenceRepository.getLastCelebratedWeek()
                    if (lastCelebrated != currentWeekKey) {
                        preferenceRepository.setLastCelebratedWeek(currentWeekKey)
                        _celebrationEvent.emit(Unit)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    class Factory(private val db: AppDatabase, private val userId: String, private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(db, userId, context.applicationContext) as T
        }
    }

    companion object {
        fun weekRange(offset: Int, today: LocalDate = LocalDate.now()): ClosedRange<LocalDate> {
            val startOfWeek = today.plusWeeks(offset.toLong()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endOfWeek = startOfWeek.plusDays(6)
            return startOfWeek..endOfWeek
        }
    }
}

data class SummaryStats(
    val doneToday: Int = 0,
    val totalHabits: Int = 0,
    val currentStreak: Int = 0,
    val totalCompleted: Int = 0,
    val weeklyPercent: Int = 0,
    val weeklyTrendStr: String = "--"
)

data class BestDayInfo(
    val date: LocalDate,
    val moodLevel: com.example.motionpulse.data.local.entity.MoodLevel,
    val habitsDone: Int,
    val totalHabits: Int
)
