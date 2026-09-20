package com.example.motionpulse.data.repository

import com.example.motionpulse.data.local.entity.ActivityFeedEntry
import com.example.motionpulse.data.local.entity.Challenge
import com.example.motionpulse.data.local.entity.CommunityNotification
import com.example.motionpulse.data.local.entity.Duel
import com.example.motionpulse.data.local.entity.FeedEventType
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CommunityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getActivityFeed(): Flow<List<ActivityFeedEntry>> = callbackFlow {
        val subscription = firestore.collection("activityFeed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull { it.toObject(ActivityFeedEntry::class.java)?.copy(id = it.id) }
                    trySend(entries)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun postFeedEntry(entry: ActivityFeedEntry) {
        firestore.collection("activityFeed").add(entry).await()
    }

    suspend fun toggleReaction(entryId: String, userId: String) {
        val docRef = firestore.collection("activityFeed").document(entryId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val reactions = snapshot.get("reactions") as? MutableMap<String, Boolean> ?: mutableMapOf()
            if (reactions[userId] == true) {
                reactions.remove(userId)
            } else {
                reactions[userId] = true
            }
            transaction.update(docRef, "reactions", reactions)
        }.await()
    }

    suspend fun sendNudge(targetUserId: String, senderId: String, senderName: String) {
        val notification = CommunityNotification(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            type = "NUDGE",
            timestamp = System.currentTimeMillis()
        )
        firestore.collection("users").document(targetUserId)
            .collection("notifications").document(notification.id)
            .set(notification).await()
    }

    fun getNotifications(userId: String): Flow<List<CommunityNotification>> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notes = snapshot.documents.mapNotNull { it.toObject(CommunityNotification::class.java)?.copy(id = it.id) }
                    trySend(notes)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        firestore.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("isRead", true).await()
    }

    fun getChallenges(): Flow<List<Challenge>> = callbackFlow {
        val subscription = firestore.collection("challenges")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val challenges = snapshot.documents.mapNotNull { it.toObject(Challenge::class.java)?.copy(id = it.id) }
                    trySend(challenges)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun joinChallenge(challengeId: String, userId: String) {
        firestore.collection("challenges").document(challengeId)
            .update(
                "participantIds", FieldValue.arrayUnion(userId),
                "progress.$userId", 0.0f
            ).await()
    }

    fun getDuels(userId: String): Flow<List<Duel>> = callbackFlow {
        val subscription = firestore.collection("duels")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val duels = snapshot.documents.mapNotNull { it.toObject(Duel::class.java)?.copy(id = it.id) }
                    trySend(duels)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createDuel(duel: Duel) {
        firestore.collection("duels").document(duel.id).set(duel).await()
    }

    suspend fun updateDuelScore(duelId: String, userId: String) {
        firestore.collection("duels").document(duelId)
            .update("scores.$userId", FieldValue.increment(1)).await()
    }

    fun getFriendsProfiles(friendIds: List<String>): Flow<List<UserProfileEntity>> = callbackFlow {
        if (friendIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val subscription = firestore.collection("users")
            .whereIn("uid", friendIds)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val profiles = snapshot.documents.mapNotNull { it.toObject(UserProfileEntity::class.java) }
                    trySend(profiles)
                }
            }
        awaitClose { subscription.remove() }
    }
}
