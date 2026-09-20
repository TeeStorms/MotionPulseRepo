package com.example.motionpulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.ActivityFeedEntry
import com.example.motionpulse.data.local.entity.Challenge
import com.example.motionpulse.data.local.entity.CommunityNotification
import com.example.motionpulse.data.local.entity.Duel
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.data.repository.CommunityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi

class CommunityViewModel(
    private val db: AppDatabase,
    private val userId: String,
    private val communityRepository: CommunityRepository = CommunityRepository()
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfileEntity?>(null)
    val userProfile: StateFlow<UserProfileEntity?> = _userProfile.asStateFlow()

    val feed: StateFlow<List<ActivityFeedEntry>> = communityRepository.getActivityFeed()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<CommunityNotification>> = communityRepository.getNotifications(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges: StateFlow<List<Challenge>> = communityRepository.getChallenges()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duels: StateFlow<List<Duel>> = communityRepository.getDuels(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val friends: StateFlow<List<UserProfileEntity>> = db.friendDao().getActiveFriendsForUser(userId)
        .map { list -> list.map { it.friendUid } }
        .flatMapLatest { ids -> communityRepository.getFriendsProfiles(ids) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<UserProfileEntity>> = friends
        .map { list -> 
            val all = list + listOfNotNull(_userProfile.value)
            all.sortedByDescending { computeWeeklyXp(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            db.userProfileDao().getProfile(userId).collect {
                _userProfile.value = it
            }
        }
    }

    private fun computeWeeklyXp(profile: UserProfileEntity): Long {
        val weekAgo = LocalDate.now().minusDays(7)
        return profile.recentXp.filterKeys { 
            try { LocalDate.parse(it).isAfter(weekAgo.minusDays(1)) } catch(e: Exception) { false }
        }.values.sum()
    }

    fun toggleReaction(entryId: String) {
        viewModelScope.launch {
            communityRepository.toggleReaction(entryId, userId)
        }
    }

    fun sendNudge(targetUserId: String) {
        viewModelScope.launch {
            _userProfile.value?.let {
                communityRepository.sendNudge(targetUserId, userId, it.displayName)
            }
        }
    }

    fun joinChallenge(challengeId: String) {
        viewModelScope.launch {
            communityRepository.joinChallenge(challengeId, userId)
        }
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            communityRepository.markNotificationRead(userId, notificationId)
        }
    }

    class Factory(private val db: AppDatabase, private val userId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityViewModel(db, userId) as T
        }
    }
}
