package com.example.motionpulse.domain.scoring

import com.example.motionpulse.data.local.entity.BadgeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationEngineTest {

    @Test
    fun `xp calculation handles base and bonus`() {
        val currentXp = 0L
        
        val result1 = GamificationEngine.calculateXpGain(currentXp, false)
        assertEquals(10L, result1.auraXpGained)
        assertEquals(1L, result1.newLevel.toLong())

        val result2 = GamificationEngine.calculateXpGain(currentXp, true)
        assertEquals(60L, result2.auraXpGained)
    }

    @Test
    fun `badge unlocking triggers at correct milestones`() {
        val unlocked = setOf<BadgeType>()
        
        val newBadges = GamificationEngine.checkBadgeUnlocks(
            userId = "u1",
            currentStreak = 7,
            longestStreak = 7,
            totalHabitsCount = 1,
            hasFriends = false,
            alreadyUnlockedBadges = unlocked
        )
        
        assertTrue(newBadges.contains(BadgeType.STREAK_7_DAY))
        assertTrue(newBadges.contains(BadgeType.FIRST_HABIT_CREATED))
    }

    @Test
    fun `already unlocked badges are not triggered again`() {
        val unlocked = setOf(BadgeType.STREAK_7_DAY)
        
        val newBadges = GamificationEngine.checkBadgeUnlocks(
            userId = "u1",
            currentStreak = 7,
            longestStreak = 7,
            totalHabitsCount = 1,
            hasFriends = false,
            alreadyUnlockedBadges = unlocked
        )
        
        assertTrue("Should not unlock 7-day streak badge again", !newBadges.contains(BadgeType.STREAK_7_DAY))
    }
}
