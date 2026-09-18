package com.example.motionpulse.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class HabitActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("HABIT_ID") ?: return
        val action = intent.action ?: return

        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).build()

        val status = when (action) {
            HabitReminderReceiver.ACTION_COMPLETE -> CompletionStatus.COMPLETED
            HabitReminderReceiver.ACTION_SKIP -> CompletionStatus.SKIPPED
            else -> return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val completion = HabitCompletionEntity(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                date = LocalDate.now(),
                status = status
            )
            db.habitCompletionDao().insertCompletion(completion)
            // Trigger sync worker here...
        }
    }
}
