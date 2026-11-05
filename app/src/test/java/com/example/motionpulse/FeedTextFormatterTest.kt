package com.example.motionpulse

import com.example.motionpulse.data.local.entity.FeedEventType
import com.example.motionpulse.util.FeedTextFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedTextFormatterTest {

    @Test
    fun formatPure_allHabitsCompleted_returnsCorrectString() {
        val result = FeedTextFormatter.formatPure("Alice", FeedEventType.ALL_HABITS_COMPLETED, null, null)
        assertEquals("Alice completed all habits", result)
    }

    @Test
    fun formatPure_streakAtRisk_returnsCorrectString() {
        val result = FeedTextFormatter.formatPure("Bob", FeedEventType.STREAK_AT_RISK, null, null)
        assertEquals("Bob's streak is at risk", result)
    }

    @Test
    fun formatPure_streakMilestoneWithoutHabit_returnsCorrectString() {
        val result = FeedTextFormatter.formatPure("Charlie", FeedEventType.STREAK_MILESTONE, 7, null)
        assertEquals("Charlie hit a 7-day streak", result)
    }

    @Test
    fun formatPure_streakMilestoneWithHabit_returnsCorrectString() {
        val result = FeedTextFormatter.formatPure("Diana", FeedEventType.STREAK_MILESTONE, 5, "Running")
        assertEquals("Diana hit a 5-day streak on \"Running\"", result)
    }
}
