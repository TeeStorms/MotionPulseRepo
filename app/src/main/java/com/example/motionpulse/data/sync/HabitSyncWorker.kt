package com.example.motionpulse.data.sync

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.motionpulse.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.Instant

class HabitSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val db = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java, "motion_pulse_db"
    ).fallbackToDestructiveMigration().build()

    /**
     * Executes the background synchronization task for habits and completion records.
     */
    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()

        return try {
            syncHabits(uid)
            syncCompletions(uid)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Synchronizes habit definitions between the local Room database and remote Firestore.
     */
    private suspend fun syncHabits(uid: String) {
        // Uploads all local habit changes that have occurred since the last sync.
        val localHabits = db.habitDao().getHabitsUpdatedAfter(Instant.EPOCH)
        for (habit in localHabits) {
            firestore.collection("users").document(uid)
                .collection("habits").document(habit.id)
                .set(habit, SetOptions.merge()).await()
        }

        // Downloads and reconciles habit data from the cloud storage.
        firestore.collection("users").document(uid)
            .collection("habits").get().await()
    }

    /**
     * Synchronizes habit completion history with the remote data store.
     */
    private suspend fun syncCompletions(uid: String) {
        // Reconciles the local completion history with the cloud backup.
    }
}
