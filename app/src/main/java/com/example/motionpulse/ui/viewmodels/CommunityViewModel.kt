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
import com.example.motionpulse.data.repository.CommunityPreferences
import com.example.motionpulse.data.repository.CommunityRepository
import com.example.motionpulse.domain.models.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi

class CommunityViewModel(
    private val db: AppDatabase,
    private val userId: String,
    private val communityRepository: CommunityRepository = CommunityRepository(),
    private val communityPreferences: CommunityPreferences? = null
) : ViewModel() {

    val hasSeenCommunityTipState: StateFlow<Boolean> = communityPreferences?.hasSeenCommunityTipFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        ?: MutableStateFlow(true).asStateFlow()

    fun dismissCommunityTip() {
        viewModelScope.launch {
            communityPreferences?.setCommunityTipSeen(true)
        }
    }

    private val _userProfile = MutableStateFlow<UserProfileEntity?>(null)
    val userProfile: StateFlow<UserProfileEntity?> = _userProfile.asStateFlow()

    val userHabits: StateFlow<List<com.example.motionpulse.data.local.entity.HabitEntity>> = db.habitDao()
        .getHabitsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val retryTrigger = MutableStateFlow(0)

    fun retry() {
        retryTrigger.value += 1
    }

    // Local tracking of users nudged today
    private val _nudgedUsersToday = MutableStateFlow<Set<String>>(emptySet())
    val nudgedUsersToday: StateFlow<Set<String>> = _nudgedUsersToday.asStateFlow()

    // Local tracking of muted users
    private val _mutedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val mutedUserIds: StateFlow<Set<String>> = _mutedUserIds.asStateFlow()

    // Honest delivery feedback message
    private val _nudgeFeedback = MutableStateFlow<String?>(null)
    val nudgeFeedback: StateFlow<String?> = _nudgeFeedback.asStateFlow()

    fun clearNudgeFeedback() {
        _nudgeFeedback.value = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val blockedUserIdsState: StateFlow<List<String>> = retryTrigger
        .flatMapLatest { communityRepository.getBlockedUserIds(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val feedState: StateFlow<UiState<List<ActivityFeedEntry>>> = retryTrigger
        .flatMapLatest { communityRepository.getActivityFeed(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notificationsState: StateFlow<UiState<List<CommunityNotification>>> = combine(
        retryTrigger.flatMapLatest { communityRepository.getNotifications(userId) },
        _mutedUserIds
    ) { state, mutedIds ->
        if (state is UiState.Success) {
            val filtered = state.data.filterNot { it.type == "NUDGE" && it.senderId in mutedIds }
            if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
        } else state
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val challengesState: StateFlow<UiState<List<Challenge>>> = retryTrigger
        .flatMapLatest { communityRepository.getChallenges(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val duelsState: StateFlow<UiState<List<Duel>>> = retryTrigger
        .flatMapLatest { communityRepository.getDuels(userId) }
        .onEach { state ->
            if (state is UiState.Success) {
                state.data.filter { it.status == "ACTIVE" }.forEach { duel ->
                    try {
                        val start = LocalDate.parse(duel.startDate)
                        val end = start.plusDays(duel.durationDays.toLong())
                        if (LocalDate.now().isAfter(end)) {
                            resolveDuel(duel)
                        }
                    } catch (e: Exception) {}
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val friendsState: StateFlow<UiState<List<UserProfileEntity>>> = retryTrigger
        .flatMapLatest {
            db.friendDao().getActiveFriendsForUser(userId)
                .map { list -> list.map { if (it.requesterUid == userId) it.recipientUid else it.requesterUid } }
                .flatMapLatest { ids -> communityRepository.getFriendsProfiles(ids) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomingRequestsState: StateFlow<UiState<List<com.example.motionpulse.data.local.entity.FriendEntity>>> = retryTrigger
        .flatMapLatest { communityRepository.getFriendRequests(userId) }
        .map { state ->
            if (state is UiState.Success) {
                val filtered = state.data.filter { it.recipientUid == userId && it.status == com.example.motionpulse.data.local.entity.FriendStatus.PENDING }
                if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
            } else state
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sentRequestsState: StateFlow<UiState<List<com.example.motionpulse.data.local.entity.FriendEntity>>> = retryTrigger
        .flatMapLatest { communityRepository.getFriendRequests(userId) }
        .map { state ->
            if (state is UiState.Success) {
                val filtered = state.data.filter { it.requesterUid == userId && it.status == com.example.motionpulse.data.local.entity.FriendStatus.PENDING }
                if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
            } else state
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _searchResult = MutableStateFlow<UserProfileEntity?>(null)
    val searchResult: StateFlow<UserProfileEntity?> = _searchResult.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    enum class LeaderboardMetric { WEEKLY_XP, STREAK, LEVEL }
    private val _leaderboardMetric = MutableStateFlow(LeaderboardMetric.WEEKLY_XP)
    val leaderboardMetric: StateFlow<LeaderboardMetric> = _leaderboardMetric.asStateFlow()

    val leaderboardState: StateFlow<UiState<List<UserProfileEntity>>> = combine(
        friendsState,
        _leaderboardMetric,
        db.habitDao().getHabitsForUser(userId)
    ) { state, metric, habits ->
        val maxStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        val correctedSelf = _userProfile.value?.copy(currentStreak = maxStreak)
        when (state) {
            is UiState.Success -> {
                val all = state.data + listOfNotNull(correctedSelf)
                val sorted = when (metric) {
                    LeaderboardMetric.WEEKLY_XP -> all.sortedByDescending { computeWeeklyXp(it) }
                    LeaderboardMetric.STREAK -> all.sortedByDescending { it.currentStreak }
                    LeaderboardMetric.LEVEL -> all.sortedByDescending { it.currentLevel }
                }
                UiState.Success(sorted)
            }
            is UiState.Empty -> {
                val selfOnly = listOfNotNull(correctedSelf)
                if (selfOnly.isEmpty()) UiState.Empty else UiState.Success(selfOnly)
            }
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setLeaderboardMetric(metric: LeaderboardMetric) {
        _leaderboardMetric.value = metric
    }

    private suspend fun resolveDuel(duel: Duel) {
        communityRepository.resolveAndCompleteDuel(duel.id)
    }

    fun startDuel(friendId: String, habitType: String, friendName: String? = null) {
        val duelId = java.util.UUID.randomUUID().toString()
        val startDate = LocalDate.now()
        val duration = 7
        val endDate = startDate.plusDays(duration.toLong()).toString()
        val newDuel = Duel(
            id = duelId,
            habitType = habitType,
            startDate = startDate.toString(),
            durationDays = duration,
            endDate = endDate,
            participants = listOf(userId, friendId),
            scores = mapOf(userId to 0, friendId to 0),
            status = "ACTIVE"
        )
        viewModelScope.launch {
            try {
                communityRepository.createDuel(newDuel)
                _userProfile.value?.let {
                    communityRepository.sendDuelInvite(friendId, userId, it.displayName, habitType, duelId)
                }
                _nudgeFeedback.value = "Duel started! Complete your $habitType habits to score."
            } catch (e: Exception) {
                _nudgeFeedback.value = "Couldn't send challenge — please try again"
            }
        }
    }

    init {
        viewModelScope.launch {
            db.userProfileDao().getProfile(userId).collect {
                _userProfile.value = it
            }
        }

        // Sync Firestore Friend Requests to Local DB
        viewModelScope.launch {
            communityRepository.getFriendRequests(userId).collect { state ->
                if (state is UiState.Success) {
                    for (req in state.data) {
                        db.friendDao().insertFriend(req)
                    }
                    val localAll = db.friendDao().getAllFriendships(userId).first()
                    val remoteIds = state.data.map { it.id }.toSet()
                    localAll.filter { it.id !in remoteIds }.forEach { 
                        db.friendDao().deleteFriend(it)
                    }
                }
            }
        }
    }

    private val searchTimestamps = mutableListOf<Long>()

    fun searchByCode(code: String) {
        val now = System.currentTimeMillis()
        searchTimestamps.removeAll { now - it > 60_000L }
        if (searchTimestamps.size >= 10) {
            _searchError.value = "Too many search attempts. Try again in a moment."
            _searchResult.value = null
            return
        }
        searchTimestamps.add(now)

        viewModelScope.launch {
            _searchError.value = null
            _searchResult.value = null
            try {
                val result = communityRepository.findUserByFriendCode(code)
                if (result == null) {
                    _searchError.value = "No account found with that code."
                } else if (result.uid == userId) {
                    _searchError.value = "You cannot add yourself"
                } else {
                    _searchResult.value = result
                }
            } catch (e: Exception) {
                _searchError.value = "Search failed"
            }
        }
    }

    fun sendFriendRequest(recipientId: String, recipientName: String? = null) {
        viewModelScope.launch {
            _userProfile.value?.let {
                try {
                    communityRepository.sendFriendRequest(userId, it.displayName, recipientId)
                    _searchResult.value = null
                    val displayName = recipientName ?: searchResult.value?.displayName ?: "user"
                    _nudgeFeedback.value = "Request sent to $displayName"
                } catch (e: Exception) {
                    _searchError.value = e.message ?: "Failed to send friend request"
                }
            }
        }
    }

    fun cancelSentRequest(requestId: String) {
        viewModelScope.launch {
            try {
                communityRepository.cancelFriendRequest(requestId)
                _nudgeFeedback.value = "Friend request cancelled"
            } catch (e: Exception) {
                _nudgeFeedback.value = e.message ?: "Failed to cancel request"
            }
        }
    }

    fun respondToRequest(requestId: String, otherUserId: String, accept: Boolean) {
        viewModelScope.launch {
            _userProfile.value?.let {
                communityRepository.respondToFriendRequest(requestId, userId, it.displayName, otherUserId, accept)
            }
        }
    }

    fun unfriend(otherUserId: String) {
        viewModelScope.launch {
            communityRepository.unfriendAtomic(userId, otherUserId, db)
        }
    }

    fun muteUser(targetUserId: String) {
        _mutedUserIds.value = _mutedUserIds.value + targetUserId
    }

    fun unmuteUser(targetUserId: String) {
        _mutedUserIds.value = _mutedUserIds.value - targetUserId
    }

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            communityRepository.blockUser(userId, targetUserId)
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            communityRepository.unblockUser(userId, targetUserId)
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
        if (targetUserId == userId) return
        if (targetUserId in _nudgedUsersToday.value) return
        
        viewModelScope.launch {
            val dayBucket = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
            val alreadyNudged = communityPreferences?.hasNudgedTodayFlow(targetUserId, dayBucket)?.first() ?: false
            if (alreadyNudged) {
                _nudgedUsersToday.update { it + targetUserId }
                return@launch
            }

            _userProfile.value?.let { profile ->
                try {
                    communityRepository.sendNudge(targetUserId, userId, profile.displayName)
                    _nudgedUsersToday.update { it + targetUserId }
                    communityPreferences?.setNudgedToday(targetUserId, dayBucket, true)
                } catch (e: Exception) {
                    _nudgedUsersToday.update { it + targetUserId }
                    communityPreferences?.setNudgedToday(targetUserId, dayBucket, true)
                }
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

    class Factory(
        private val db: AppDatabase,
        private val userId: String,
        private val context: android.content.Context? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = context?.let { CommunityPreferences(it.applicationContext) }
            return CommunityViewModel(db, userId, CommunityRepository(), prefs) as T
        }
    }
}
