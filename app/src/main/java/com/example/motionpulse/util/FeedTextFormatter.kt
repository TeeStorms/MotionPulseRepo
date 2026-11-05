package com.example.motionpulse.util

import android.content.Context
import com.example.motionpulse.R
import com.example.motionpulse.data.local.entity.ActivityFeedEntry
import com.example.motionpulse.data.local.entity.FeedEventType

object FeedTextFormatter {

    fun format(context: Context, entry: ActivityFeedEntry): String {
        return when (entry.eventType) {
            FeedEventType.ALL_HABITS_COMPLETED -> {
                context.getString(R.string.feed_completed_all_habits, entry.actorName)
            }
            FeedEventType.STREAK_MILESTONE -> {
                val count = entry.streakCount ?: 0
                val habit = entry.habitName
                if (!habit.isNullOrBlank()) {
                    context.resources.getQuantityString(
                        R.plurals.feed_streak_milestone_with_habit,
                        count,
                        entry.actorName,
                        habit,
                        count
                    )
                } else {
                    context.resources.getQuantityString(
                        R.plurals.feed_streak_milestone,
                        count,
                        entry.actorName,
                        count
                    )
                }
            }
            FeedEventType.STREAK_AT_RISK -> {
                context.getString(R.string.feed_streak_at_risks, entry.actorName)
            }
        }
    }

    fun formatPure(actorName: String, eventType: FeedEventType, streakCount: Int?, habitName: String?): String {
        return when (eventType) {
            FeedEventType.ALL_HABITS_COMPLETED -> "$actorName completed all habits"
            FeedEventType.STREAK_MILESTONE -> {
                val count = streakCount ?: 0
                if (!habitName.isNullOrBlank()) {
                    "$actorName hit a $count-day streak on \"$habitName\""
                } else {
                    "$actorName hit a $count-day streak"
                }
            }
            FeedEventType.STREAK_AT_RISK -> "$actorName's streak is at risk"
        }
    }
}
