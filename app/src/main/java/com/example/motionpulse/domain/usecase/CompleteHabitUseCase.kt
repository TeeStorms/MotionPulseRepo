package com.example.motionpulse.domain.usecase

import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.repository.AuthRepository
import com.example.motionpulse.data.repository.CommunityRepository
import com.example.motionpulse.domain.scoring.GamificationEngine
import com.example.motionpulse.domain.scoring.HabitScoreCalculator
import com.example.motionpulse.domain.scoring.StreakCalculator
import com.example.motionpulse.domain.models.FrequencyConfig
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class CompleteHabitUseCase(
    private val db: AppDatabase,
    private val userId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val communityRepository: CommunityRepository = CommunityRepository()
) {
    suspend fun execute(habit: HabitEntity, date: LocalDate = LocalDate.now()): HabitEntity {
        val completionDao = db.habitCompletionDao()
        val habitDao = db.habitDao()
        val profileDao = db.userProfileDao()

        val existingCompletion = completionDao.getCompletionByDate(habit.id, date)
        val isCompletedNow = existingCompletion?.status == CompletionStatus.COMPLETED

        val newStatus = if (isCompletedNow) CompletionStatus.MISSED else CompletionStatus.COMPLETED

        if (existingCompletion != null) {
            val updated = existingCompletion.copy(status = newStatus, loggedAt = Instant.now())
            completionDao.insertCompletion(updated)
        } else {
            val newComp = HabitCompletionEntity(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = date,
                status = newStatus,
                loggedAt = Instant.now()
            )
            completionDao.insertCompletion(newComp)
        }

        val allCompletions = completionDao.getAllCompletionsForHabit(habit.id).first()
        val freqConfig = FrequencyConfig.decodeSafe(habit.frequencyConfig)
        val newScore = HabitScoreCalculator.calculateScore(0f, allCompletions, freqConfig, habit.numericTarget)
        val newCurrentStreak = StreakCalculator.calculateCurrentStreak(allCompletions, freqConfig, date)
        val newLongestStreak = maxOf(habit.longestStreak, newCurrentStreak)

        val updatedHabit = habit.copy(
            habitStrengthScore = newScore,
            currentStreak = newCurrentStreak,
            longestStreak = newLongestStreak,
            updatedAt = Instant.now()
        )
        habitDao.updateHabit(updatedHabit)

        val todayStr = date.toString()
        val profile = profileDao.getProfile(userId).first()

        if (newStatus == CompletionStatus.COMPLETED) {
            if (profile != null) {
                val allActiveHabits = habitDao.getHabitsForUser(userId).first()
                val maxStreak = allActiveHabits.maxOfOrNull { it.currentStreak } ?: 0
                val isNewPb = newCurrentStreak > habit.longestStreak
                val xpResult = GamificationEngine.calculateXpGain(profile.totalAuraXp, isNewPb)

                val newRecentXp = profile.recentXp.toMutableMap()
                val currentTodayXp = newRecentXp[todayStr] ?: 0L
                newRecentXp[todayStr] = currentTodayXp + xpResult.auraXpGained

                val updatedProfile = profile.copy(
                    totalAuraXp = xpResult.newTotalXp,
                    currentLevel = xpResult.newLevel,
                    currentStreak = maxStreak,
                    recentXp = newRecentXp
                )
                profileDao.insertProfile(updatedProfile)

                val firestoreUpdates = mapOf<String, Any>(
                    "totalAuraXp" to updatedProfile.totalAuraXp,
                    "currentLevel" to updatedProfile.currentLevel,
                    "currentStreak" to updatedProfile.currentStreak,
                    "recentXp.$todayStr" to com.google.firebase.firestore.FieldValue.increment(xpResult.auraXpGained)
                )
                authRepository.updateUserProfileFields(userId, firestoreUpdates)

                if (profile.shareMilestonesWithFriends) {
                    val friends = db.friendDao().getActiveFriendsForUser(userId).first()
                    val friendIds = friends.map { if (it.requesterUid == userId) it.recipientUid else it.requesterUid }
                    val entryId = "${habit.id}_${todayStr}"
                    val habitNameToShare = if (profile.showHabitNamesInPosts) habit.title else "a habit"

                    communityRepository.postAchievementToFeed(
                        entryId = entryId,
                        actorId = userId,
                        actorName = profile.displayName.ifBlank { "User" },
                        habitName = habitNameToShare,
                        type = "HABIT_COMPLETED",
                        visibleTo = friendIds + userId
                    )
                }
            }
        } else {
            if (profile != null) {
                val allActiveHabits = habitDao.getHabitsForUser(userId).first()
                val maxStreak = allActiveHabits.maxOfOrNull { it.currentStreak } ?: 0

                val currentTodayXp = profile.recentXp[todayStr] ?: 0L
                val xpToDeduct = minOf(10L, currentTodayXp)
                val newTotalXp = maxOf(0L, profile.totalAuraXp - xpToDeduct)

                val newRecentXp = profile.recentXp.toMutableMap()
                val newXpForToday = maxOf(0L, currentTodayXp - xpToDeduct)
                newRecentXp[todayStr] = newXpForToday

                val updatedProfile = profile.copy(
                    totalAuraXp = newTotalXp,
                    currentLevel = (newTotalXp / 500L).toInt() + 1,
                    currentStreak = maxStreak,
                    recentXp = newRecentXp
                )
                profileDao.insertProfile(updatedProfile)

                val firestoreUpdates = mapOf<String, Any>(
                    "totalAuraXp" to updatedProfile.totalAuraXp,
                    "currentLevel" to updatedProfile.currentLevel,
                    "currentStreak" to updatedProfile.currentStreak,
                    "recentXp.$todayStr" to newXpForToday
                )
                authRepository.updateUserProfileFields(userId, firestoreUpdates)

                val entryId = "${habit.id}_${todayStr}"
                communityRepository.deleteAchievementFromFeed(entryId)
            }
        }

        return updatedHabit
    }
}
