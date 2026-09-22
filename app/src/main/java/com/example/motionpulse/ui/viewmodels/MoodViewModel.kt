package com.example.motionpulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.MoodEntity
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.data.local.entity.MoodFactor
import com.example.motionpulse.data.repository.MoodRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class MoodViewModel(
    private val db: AppDatabase,
    private val userId: String,
    private val moodRepository: MoodRepository = MoodRepository(db)
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    val todayMood: StateFlow<MoodEntity?> = db.moodDao()
        .getMoodFlowForDate(userId, LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Sync from remote
        viewModelScope.launch {
            moodRepository.getRemoteMoodsFlow(userId)
                .catch { /* Handle error */ }
                .collect { remoteMoods ->
                    remoteMoods.forEach { mood ->
                        val local = db.moodDao().getMoodForDate(userId, mood.date)
                        if (local == null || mood.updatedAt.isAfter(local.updatedAt)) {
                            db.moodDao().insertOrUpdateMood(mood)
                        }
                    }
                }
        }
    }

    fun logMood(
        level: MoodLevel,
        factors: List<MoodFactor>,
        note: String?
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            
            val existing = todayMood.value
            val mood = MoodEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                userId = userId,
                date = LocalDate.now(),
                moodLevel = level,
                factors = factors,
                note = note,
                loggedAt = existing?.loggedAt ?: Instant.now(),
                updatedAt = Instant.now()
            )
            
            try {
                db.moodDao().insertOrUpdateMood(mood)
                moodRepository.saveMood(userId, mood)
                _saveSuccess.emit(Unit)
            } catch (e: Exception) {
                _saveError.value = "Failed to sync mood. Saved locally."
            } finally {
                _isSaving.value = false
            }
        }
    }

    class Factory(private val db: AppDatabase, private val userId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MoodViewModel(db, userId) as T
        }
    }
}
