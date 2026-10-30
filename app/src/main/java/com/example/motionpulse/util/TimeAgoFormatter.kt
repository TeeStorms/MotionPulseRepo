package com.example.motionpulse.util

import java.util.concurrent.TimeUnit

object TimeAgoFormatter {
    fun format(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                "${mins}m ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "${hours}h ago"
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                if (days == 1L) "1 day ago" else "$days days ago"
            }
        }
    }
}
