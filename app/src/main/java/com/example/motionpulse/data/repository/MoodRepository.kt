package com.example.motionpulse.data.repository

import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.MoodEntity
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.data.local.entity.MoodFactor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class MoodRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getRemoteMoodsFlow(userId: String): Flow<List<MoodEntity>> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .collection("moods")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val moods = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: ""
                            val uid = doc.getString("userId") ?: ""
                            val dateStr = doc.getString("date") ?: doc.id
                            val date = LocalDate.parse(dateStr)
                            val levelStr = doc.getString("moodLevel") ?: "STEADY"
                            val moodLevel = MoodLevel.valueOf(levelStr)
                            
                            val factorsList = doc.get("factors") as? List<String> ?: emptyList()
                            val factors = factorsList.mapNotNull { try { MoodFactor.valueOf(it) } catch(e: Exception) { null } }
                            
                            val note = doc.getString("note")
                            val loggedAtLong = doc.getLong("loggedAt") ?: System.currentTimeMillis()
                            val updatedAtLong = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            
                            MoodEntity(
                                id = id,
                                userId = uid,
                                date = date,
                                moodLevel = moodLevel,
                                factors = factors,
                                note = note,
                                loggedAt = java.time.Instant.ofEpochMilli(loggedAtLong),
                                updatedAt = java.time.Instant.ofEpochMilli(updatedAtLong)
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(moods)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveMood(userId: String, mood: MoodEntity) {
        val dateStr = mood.date.toString()
        val data = hashMapOf(
            "id" to mood.id,
            "userId" to mood.userId,
            "date" to dateStr,
            "moodLevel" to mood.moodLevel.name,
            "factors" to mood.factors.map { it.name },
            "note" to mood.note,
            "loggedAt" to mood.loggedAt.toEpochMilli(),
            "updatedAt" to mood.updatedAt.toEpochMilli()
        )
        firestore.collection("users").document(userId)
            .collection("moods").document(dateStr)
            .set(data).await()
    }
}
