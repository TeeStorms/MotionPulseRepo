package com.example.motionpulse.data.repository

import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json

class HabitRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Observes real-time habit updates for a specific user from Firestore.
     */
    fun getRemoteHabitsFlow(userId: String): Flow<List<HabitEntity>> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .collection("habits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val habits = snapshot.documents.mapNotNull { it.toObject(HabitEntity::class.java) }
                    trySend(habits)
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Persists a habit entity to the remote Firestore collection.
     */
    suspend fun saveHabit(userId: String, habit: HabitEntity) {
        firestore.collection("users").document(userId)
            .collection("habits").document(habit.id)
            .set(habit).await()
    }

    /**
     * Records a habit completion event in Firestore.
     */
    suspend fun saveCompletion(userId: String, completion: HabitCompletionEntity) {
        val dateStr = completion.date.toString()
        firestore.collection("users").document(userId)
            .collection("habits").document(completion.habitId)
            .collection("completions").document(dateStr)
            .set(completion).await()
    }

    /**
     * Deletes a habit completion record for a specific date.
     */
    suspend fun deleteCompletion(userId: String, habitId: String, date: LocalDate) {
        firestore.collection("users").document(userId)
            .collection("habits").document(habitId)
            .collection("completions").document(date.toString())
            .delete().await()
    }

    /**
     * Marks a habit as archived in the remote data store.
     */
    suspend fun archiveHabit(userId: String, habitId: String) {
        firestore.collection("users").document(userId)
            .collection("habits").document(habitId)
            .update("isArchived", true).await()
    }

    /**
     * Fetches all completion records for a specific habit from the cloud.
     */
    fun getRemoteCompletionsFlow(userId: String, habitId: String): Flow<List<HabitCompletionEntity>> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .collection("habits").document(habitId)
            .collection("completions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val completions = snapshot.documents.mapNotNull { it.toObject(HabitCompletionEntity::class.java) }
                    trySend(completions)
                }
            }
        awaitClose { subscription.remove() }
    }
}
