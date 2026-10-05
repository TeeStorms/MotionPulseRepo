package com.example.motionpulse.data.repository

import android.util.Log
import com.example.motionpulse.data.local.entity.ActivityFeedEntry
import com.example.motionpulse.data.local.entity.Challenge
import com.example.motionpulse.data.local.entity.CommunityNotification
import com.example.motionpulse.data.local.entity.Duel
import com.example.motionpulse.data.local.entity.FeedEventType
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.domain.models.ErrorKind
import com.example.motionpulse.domain.models.UiState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CommunityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun mapFirestoreError(e: FirebaseFirestoreException): ErrorKind = when (e.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> ErrorKind.PERMISSION
        FirebaseFirestoreException.Code.UNAVAILABLE -> ErrorKind.OFFLINE
        else -> ErrorKind.UNKNOWN
    }

    private fun logFirestoreError(path: String, queryDesc: String, error: FirebaseFirestoreException) {
        Log.w(
            "FirestoreError",
            "Path: '$path' | Query: '$queryDesc' | Code: ${error.code} | Message: ${error.message}",
            error
        )
    }

    fun getActivityFeed(userId: String): Flow<UiState<List<ActivityFeedEntry>>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UiState.Empty)
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading)
        val subscription = firestore.collection("activityFeed")
            .whereArrayContains("visibleTo", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreError("activityFeed", "getActivityFeed (whereArrayContains visibleTo $userId, limit 30)", error)
                    val kind = mapFirestoreError(error)
                    trySend(UiState.Error(kind, error.message ?: "Failed to load activity feed", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull { it.toObject(ActivityFeedEntry::class.java)?.copy(id = it.id) }
                    if (entries.isEmpty()) {
                        trySend(UiState.Empty)
                    } else {
                        trySend(UiState.Success(entries))
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun postFeedEntry(entry: ActivityFeedEntry) {
        val friendUids = try {
            val friendsSnapshot = firestore.collection("friendRequests")
                .whereArrayContains("participants", entry.actorId)
                .get().await()

            friendsSnapshot.documents
                .filter { it.getString("status") == "ACCEPTED" }
                .mapNotNull { doc ->
                    val parts = doc.get("participants") as? List<*> ?: emptyList<Any>()
                    parts.find { it != entry.actorId } as? String
                }
        } catch (e: Exception) {
            emptyList()
        }

        val audience = (listOf(entry.actorId) + friendUids).distinct()
        val entryWithAudience = entry.copy(visibleTo = audience)
        firestore.collection("activityFeed").add(entryWithAudience).await()
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
        val dayBucket = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
        val notifId = "nudge_${senderId}_${dayBucket}"
        val notification = CommunityNotification(
            id = notifId,
            senderId = senderId,
            senderName = senderName,
            type = "NUDGE",
            timestamp = System.currentTimeMillis()
        )
        try {
            firestore.collection("users").document(targetUserId)
                .collection("notifications").document(notifId)
                .set(notification).await()
        } catch (e: Exception) {
            Log.i("CommunityRepository", "Nudge skipped/already exists for today: ${e.message}")
        }
    }

    suspend fun blockUser(userId: String, blockedUserId: String) {
        val blockData = mapOf(
            "blockedUid" to blockedUserId,
            "blockedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(userId)
            .collection("blocks").document(blockedUserId)
            .set(blockData).await()

        unfriend(userId, blockedUserId)
    }

    suspend fun unblockUser(userId: String, blockedUserId: String) {
        firestore.collection("users").document(userId)
            .collection("blocks").document(blockedUserId)
            .delete().await()
    }

    fun getBlockedUserIds(userId: String): Flow<List<String>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("users").document(userId)
            .collection("blocks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val ids = snapshot.documents.map { it.id }
                    trySend(ids)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getNotifications(userId: String): Flow<UiState<List<CommunityNotification>>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UiState.Empty)
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading)
        val subscription = firestore.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreError("users/$userId/notifications", "getNotifications (orderBy timestamp DESC)", error)
                    val kind = mapFirestoreError(error)
                    trySend(UiState.Error(kind, error.message ?: "Failed to load notifications", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notes = snapshot.documents.mapNotNull { it.toObject(CommunityNotification::class.java)?.copy(id = it.id) }
                    if (notes.isEmpty()) {
                        trySend(UiState.Empty)
                    } else {
                        trySend(UiState.Success(notes))
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        firestore.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("isRead", true).await()
    }

    fun getChallenges(userId: String): Flow<UiState<List<Challenge>>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UiState.Empty)
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading)
        val subscription = firestore.collection("challenges")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreError("challenges", "getChallenges (whereArrayContains participantIds $userId)", error)
                    val kind = mapFirestoreError(error)
                    trySend(UiState.Error(kind, error.message ?: "Failed to load challenges", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val challenges = snapshot.documents.mapNotNull { it.toObject(Challenge::class.java)?.copy(id = it.id) }
                    if (challenges.isEmpty()) {
                        trySend(UiState.Empty)
                    } else {
                        trySend(UiState.Success(challenges))
                    }
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

    fun getDuels(userId: String): Flow<UiState<List<Duel>>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UiState.Empty)
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading)
        val subscription = firestore.collection("duels")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreError("duels", "getDuels (whereArrayContains participants $userId)", error)
                    val kind = mapFirestoreError(error)
                    trySend(UiState.Error(kind, error.message ?: "Failed to load duels", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val duels = snapshot.documents.mapNotNull { it.toObject(Duel::class.java)?.copy(id = it.id) }
                    if (duels.isEmpty()) {
                        trySend(UiState.Empty)
                    } else {
                        trySend(UiState.Success(duels))
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createDuel(duel: Duel) {
        firestore.collection("duels").document(duel.id).set(duel).await()
    }

    suspend fun sendDuelInvite(targetUserId: String, senderId: String, senderName: String, habitType: String, duelId: String) {
        val notification = CommunityNotification(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            type = "DUEL_INVITE",
            timestamp = System.currentTimeMillis(),
            habitType = habitType,
            duelId = duelId,
            message = "$senderName has challenged you to a $habitType duel!"
        )
        firestore.collection("users").document(targetUserId)
            .collection("notifications").document(notification.id)
            .set(notification).await()
    }

    suspend fun sendDuelResult(targetUserId: String, senderId: String, senderName: String, duelId: String, message: String) {
        val notification = CommunityNotification(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            type = "DUEL_RESULT",
            timestamp = System.currentTimeMillis(),
            duelId = duelId,
            message = message
        )
        firestore.collection("users").document(targetUserId)
            .collection("notifications").document(notification.id)
            .set(notification).await()
    }

    suspend fun resolveAndCompleteDuel(duelId: String) {
        val duelRef = firestore.collection("duels").document(duelId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(duelRef)
            if (!snapshot.exists()) return@runTransaction
            val status = snapshot.getString("status") ?: "ACTIVE"
            if (status != "ACTIVE") return@runTransaction

            val endDateStr = snapshot.getString("endDate") ?: ""
            if (endDateStr.isNotEmpty()) {
                try {
                    val endDate = java.time.LocalDate.parse(endDateStr)
                    if (java.time.LocalDate.now().isBefore(endDate) || java.time.LocalDate.now().isEqual(endDate)) {
                        return@runTransaction
                    }
                } catch (e: Exception) {}
            }

            val participants = snapshot.get("participants") as? List<*> ?: emptyList<Any>()
            if (participants.size < 2) return@runTransaction
            val p1 = participants[0] as? String ?: return@runTransaction
            val p2 = participants[1] as? String ?: return@runTransaction

            val scores = snapshot.get("scores") as? Map<*, *> ?: emptyMap<Any, Any>()
            val s1 = (scores[p1] as? Number)?.toInt() ?: 0
            val s2 = (scores[p2] as? Number)?.toInt() ?: 0

            val winnerId = when {
                s1 > s2 -> p1
                s2 > s1 -> p2
                else -> "DRAW"
            }

            transaction.update(
                duelRef,
                mapOf(
                    "status" to "COMPLETED",
                    "winnerId" to winnerId,
                    "resolvedAt" to FieldValue.serverTimestamp()
                )
            )

            val habitType = snapshot.getString("habitType") ?: "General"
            val msg1 = if (winnerId == p1) "You won the duel against $habitType!" else if (winnerId == p2) "You lost the duel against $habitType." else "The duel ended in a draw."
            val msg2 = if (winnerId == p2) "You won the duel against $habitType!" else if (winnerId == p1) "You lost the duel against $habitType." else "The duel ended in a draw."

            val notifRef1 = firestore.collection("users").document(p1).collection("notifications").document("duelresult_${duelId}_$p1")
            val notifRef2 = firestore.collection("users").document(p2).collection("notifications").document("duelresult_${duelId}_$p2")

            val notif1 = CommunityNotification(
                id = "duelresult_${duelId}_$p1",
                senderId = "SYSTEM",
                senderName = "Motion.Pulse",
                type = "DUEL_RESULT",
                timestamp = System.currentTimeMillis(),
                duelId = duelId,
                message = msg1
            )
            val notif2 = CommunityNotification(
                id = "duelresult_${duelId}_$p2",
                senderId = "SYSTEM",
                senderName = "Motion.Pulse",
                type = "DUEL_RESULT",
                timestamp = System.currentTimeMillis(),
                duelId = duelId,
                message = msg2
            )

            transaction.set(notifRef1, notif1)
            transaction.set(notifRef2, notif2)
        }.await()
    }

    suspend fun completeDuelStatus(duelId: String, winnerId: String?) {
        firestore.collection("duels").document(duelId)
            .update(
                "status", "COMPLETED",
                "winnerId", winnerId
            ).await()
    }

    suspend fun updateDuelScore(duelId: String, userId: String) {
        firestore.collection("duels").document(duelId)
            .update("scores.$userId", FieldValue.increment(1)).await()
    }

    suspend fun decrementDuelScore(duelId: String, userId: String) {
        firestore.collection("duels").document(duelId)
            .update("scores.$userId", FieldValue.increment(-1)).await()
    }

    suspend fun getActiveDuelsOnce(userId: String): List<Duel> {
        val snapshot = firestore.collection("duels")
            .whereArrayContains("participants", userId)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Duel::class.java)?.copy(id = it.id) }
    }

    suspend fun findUserByFriendCode(rawCode: String): UserProfileEntity? {
        val clean = rawCode.uppercase().trim().replace(" ", "").replace("-", "")
        val body = if (clean.startsWith("MP")) clean.removePrefix("MP") else clean
        
        val formattedCode = when (body.length) {
            6 -> "MP-$body"
            8 -> "MP-${body.take(4)}-${body.drop(4)}"
            else -> "MP-$body"
        }

        // Single document lookup against friendCodes/{code}
        val codeDoc = firestore.collection("friendCodes").document(formattedCode).get().await()
        if (!codeDoc.exists()) return null

        val targetUid = codeDoc.getString("uid") ?: return null
        val publicProfileDoc = firestore.collection("publicProfiles").document(targetUid).get().await()
        return publicProfileDoc.toObject(UserProfileEntity::class.java)
    }

    fun getFriendRequests(userId: String): Flow<UiState<List<com.example.motionpulse.data.local.entity.FriendEntity>>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UiState.Empty)
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading)
        val subscription = firestore.collection("friendRequests")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreError("friendRequests", "getFriendRequests (whereArrayContains participants $userId)", error)
                    val kind = mapFirestoreError(error)
                    trySend(UiState.Error(kind, error.message ?: "Failed to load friend requests", error))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val requests = it.documents.mapNotNull { doc ->
                        val requesterUid = doc.getString("requesterUid") ?: ""
                        val recipientUid = doc.getString("recipientUid") ?: ""
                        val statusStr = doc.getString("status") ?: "PENDING"
                        val status = com.example.motionpulse.data.local.entity.FriendStatus.valueOf(statusStr)
                        
                        com.example.motionpulse.data.local.entity.FriendEntity(
                            id = doc.id,
                            requesterUid = requesterUid,
                            recipientUid = recipientUid,
                            status = status
                        )
                    }
                    if (requests.isEmpty()) {
                        trySend(UiState.Empty)
                    } else {
                        trySend(UiState.Success(requests))
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendFriendRequest(senderId: String, senderName: String, recipientId: String) {
        if (senderId == recipientId) return

        // Search for existing request in either direction
        val querySnapshot = firestore.collection("friendRequests")
            .whereArrayContains("participants", senderId)
            .get().await()

        val existing = querySnapshot.documents.find { doc ->
            val parts = doc.get("participants") as? List<*> ?: emptyList<Any>()
            parts.contains(recipientId)
        }

        if (existing != null) {
            val status = existing.getString("status")
            val requester = existing.getString("requesterUid")
            
            if (status == "ACCEPTED") {
                return // Already friends
            }
            if (status == "PENDING") {
                if (requester == recipientId) {
                    // Simultaneous intent: Auto-accept
                    respondToFriendRequest(existing.id, senderId, senderName, recipientId, true)
                    return
                } else {
                    return // Already sent PENDING request
                }
            }
            if (status == "DECLINED") {
                // Check 24-hour cooldown
                val respondedAtTimestamp = existing.getTimestamp("respondedAt")?.toDate()?.time ?: 0L
                val twentyFourHours = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - respondedAtTimestamp < twentyFourHours) {
                    throw IllegalStateException("Recent request was declined. Please wait 24 hours before trying again.")
                }
            }
        }

        val requestId = if (senderId < recipientId) "${senderId}_${recipientId}" else "${recipientId}_${senderId}"
        val request = mapOf(
            "requesterUid" to senderId,
            "recipientUid" to recipientId,
            "participants" to listOf(senderId, recipientId),
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "respondedAt" to null
        )
        
        firestore.collection("friendRequests").document(requestId).set(request).await()
        
        // Notify recipient
        val notification = CommunityNotification(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            type = "FRIEND_REQUEST",
            timestamp = System.currentTimeMillis(),
            message = "$senderName sent you a friend request!"
        )
        firestore.collection("users").document(recipientId)
            .collection("notifications").document(notification.id)
            .set(notification).await()
    }

    suspend fun cancelFriendRequest(requestId: String) {
        firestore.collection("friendRequests").document(requestId).delete().await()
    }

    suspend fun respondToFriendRequest(requestId: String, userId: String, userName: String, otherUserId: String, accept: Boolean) {
        val status = if (accept) "ACCEPTED" else "DECLINED"
        firestore.collection("friendRequests").document(requestId)
            .update(
                "status", status,
                "respondedAt", FieldValue.serverTimestamp()
            ).await()

        if (accept) {
            val notification = CommunityNotification(
                id = UUID.randomUUID().toString(),
                senderId = userId,
                senderName = userName,
                type = "FRIEND_ACCEPTED",
                timestamp = System.currentTimeMillis(),
                message = "$userName accepted your friend request!"
            )
            firestore.collection("users").document(otherUserId)
                .collection("notifications").document(notification.id)
                .set(notification).await()
        }
    }

    suspend fun unfriendAtomic(userId: String, otherUserId: String, db: com.example.motionpulse.data.local.AppDatabase? = null) {
        val batch = firestore.batch()

        val querySnapshot = firestore.collection("friendRequests")
            .whereArrayContains("participants", userId)
            .get().await()

        val doc = querySnapshot.documents.find { d ->
            val parts = d.get("participants") as? List<*> ?: emptyList<Any>()
            parts.contains(otherUserId)
        }
        doc?.let {
            batch.delete(firestore.collection("friendRequests").document(it.id))
        }

        val duelsSnapshot = firestore.collection("duels")
            .whereArrayContains("participants", userId)
            .get().await()

        for (duelDoc in duelsSnapshot.documents) {
            val participants = duelDoc.get("participants") as? List<*> ?: emptyList<Any>()
            val status = duelDoc.getString("status")
            if (participants.contains(otherUserId) && status == "ACTIVE") {
                batch.update(firestore.collection("duels").document(duelDoc.id), "status", "CANCELLED")
            }
        }

        val feedSnapshot = firestore.collection("activityFeed")
            .whereEqualTo("actorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()

        for (feedDoc in feedSnapshot.documents) {
            batch.update(feedDoc.reference, "visibleTo", FieldValue.arrayRemove(otherUserId))
        }

        batch.commit().await()

        db?.let {
            try {
                val friendships = it.friendDao().getAllFriendships(userId).first()
                val friendship = friendships.find { f -> 
                    (f.requesterUid == userId && f.recipientUid == otherUserId) || 
                    (f.requesterUid == otherUserId && f.recipientUid == userId)
                }
                friendship?.let { f ->
                    it.friendDao().deleteFriend(f)
                }
            } catch (e: Exception) {}
        }
    }

    suspend fun unfriend(userId: String, otherUserId: String) {
        unfriendAtomic(userId, otherUserId, null)
    }

    fun getFriendsProfiles(friendIds: List<String>): Flow<UiState<List<UserProfileEntity>>> = callbackFlow {
        if (friendIds.isEmpty()) {
            trySend(UiState.Empty)
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading)
        val subscription = firestore.collection("publicProfiles")
            .whereIn("uid", friendIds)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreError("publicProfiles", "getFriendsProfiles (whereIn uid [${friendIds.joinToString()}])", error)
                    val kind = mapFirestoreError(error)
                    trySend(UiState.Error(kind, error.message ?: "Failed to load friend profiles", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val profiles = snapshot.documents.mapNotNull { it.toObject(UserProfileEntity::class.java) }
                    if (profiles.isEmpty()) {
                        trySend(UiState.Empty)
                    } else {
                        trySend(UiState.Success(profiles))
                    }
                }
            }
        awaitClose { subscription.remove() }
    }
}
