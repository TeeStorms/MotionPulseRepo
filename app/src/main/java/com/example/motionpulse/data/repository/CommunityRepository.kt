package com.example.motionpulse.data.repository

import android.util.Log
import com.example.motionpulse.data.local.AppDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        Log.w("FirestoreError", "Path: '$path' | Query: '$queryDesc' | Code: ${error.code} | Message: ${error.message}", error)
    }

    // ───────── FEED ─────────
    fun getActivityFeed(userId: String): Flow<UiState<List<ActivityFeedEntry>>> = callbackFlow {
        if (userId.isEmpty()) { trySend(UiState.Empty); close(); return@callbackFlow }
        trySend(UiState.Loading)
        val sub = firestore.collection("activityFeed")
            .whereArrayContains("visibleTo", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    logFirestoreError("activityFeed", "getActivityFeed", err)
                    trySend(UiState.Error(mapFirestoreError(err), err.message ?: "Failed", err))
                    return@addSnapshotListener
                }
                val entries = snap?.documents?.mapNotNull { it.toObject(ActivityFeedEntry::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(if (entries.isEmpty()) UiState.Empty else UiState.Success(entries))
            }
        awaitClose { sub.remove() }
    }

    suspend fun postFeedEntry(entry: ActivityFeedEntry) {
        val dayBucket = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
        val entryId = when (entry.eventType) {
            FeedEventType.ALL_HABITS_COMPLETED -> "${entry.actorId}_ALL_HABITS_COMPLETED_$dayBucket"
            FeedEventType.STREAK_MILESTONE -> "${entry.actorId}_STREAK_${entry.streakCount ?: 0}"
            FeedEventType.STREAK_AT_RISK -> "${entry.actorId}_STREAK_AT_RISK_$dayBucket"
        }
        val friendUids = try {
            firestore.collection("friendRequests").whereArrayContains("participants", entry.actorId).get().await()
                .documents.filter { it.getString("status") == "ACCEPTED" }
                .mapNotNull { d -> (d.get("participants") as? List<*>)?.find { it != entry.actorId } as? String }
        } catch (_: Exception) { emptyList() }
        val audience = (listOf(entry.actorId) + friendUids).distinct()
        try { firestore.collection("activityFeed").document(entryId).set(entry.copy(id = entryId, visibleTo = audience)).await() } catch (_: Exception) {}
    }

    suspend fun cleanupDuplicateFeedEntries(userId: String) {
        try {
            val snap = firestore.collection("activityFeed").whereArrayContains("visibleTo", userId).get().await()
            val grouped = mutableMapOf<String, MutableList<com.google.firebase.firestore.DocumentSnapshot>>()
            snap.documents.forEach { d ->
                if (d.getString("actorId") != userId) return@forEach
                val type = d.getString("eventType") ?: return@forEach
                val date = java.time.Instant.ofEpochMilli(d.getLong("timestamp") ?: 0L).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                grouped.getOrPut("${type}_$date") { mutableListOf() }.add(d)
            }
            grouped.values.forEach { list ->
                if (list.size > 1) {
                    list.sortByDescending { it.getLong("timestamp") ?: 0L }
                    (1 until list.size).forEach { list[it].reference.delete().await() }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun toggleReaction(entryId: String, userId: String) {
        val ref = firestore.collection("activityFeed").document(entryId)
        firestore.runTransaction { txn ->
            val snap = txn.get(ref)
            val reactions = (snap.get("reactions") as? MutableMap<String, Boolean>) ?: mutableMapOf()
            if (reactions[userId] == true) reactions.remove(userId) else reactions[userId] = true
            txn.update(ref, "reactions", reactions)
        }.await()
    }

    // ───────── NUDGES ─────────
    suspend fun sendNudge(targetUserId: String, senderId: String, senderName: String) {
        val day = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
        val id = "nudge_${senderId}_${targetUserId}_$day"
        val n = CommunityNotification(id = id, senderId = senderId, senderName = senderName, type = "NUDGE",
            timestamp = System.currentTimeMillis(), isRead = false,
            message = "$senderName nudged you! Keep your streak going.")
        firestore.collection("users").document(targetUserId).collection("notifications").document(id).set(n).await()
    }

    // ───────── BLOCKS ─────────
    suspend fun blockUser(userId: String, blockedUserId: String) {
        firestore.collection("users").document(userId).collection("blocks").document(blockedUserId)
            .set(mapOf("blockedUid" to blockedUserId, "blockedAt" to FieldValue.serverTimestamp())).await()
        unfriend(userId, blockedUserId)
    }
    suspend fun unblockUser(userId: String, blockedUserId: String) {
        firestore.collection("users").document(userId).collection("blocks").document(blockedUserId).delete().await()
    }
    fun getBlockedUserIds(userId: String): Flow<List<String>> = callbackFlow {
        if (userId.isEmpty()) { trySend(emptyList()); close(); return@callbackFlow }
        val sub = firestore.collection("users").document(userId).collection("blocks")
            .addSnapshotListener { snap, _ -> trySend(snap?.documents?.map { it.id } ?: emptyList()) }
        awaitClose { sub.remove() }
    }

    // ───────── NOTIFICATIONS ─────────
    fun getNotifications(userId: String): Flow<UiState<List<CommunityNotification>>> = callbackFlow {
        if (userId.isEmpty()) { trySend(UiState.Empty); close(); return@callbackFlow }
        trySend(UiState.Loading)
        val sub = firestore.collection("users").document(userId).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(UiState.Error(mapFirestoreError(err), err.message ?: "Failed", err)); return@addSnapshotListener }
                val notes = snap?.documents?.mapNotNull { it.toObject(CommunityNotification::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(if (notes.isEmpty()) UiState.Empty else UiState.Success(notes))
            }
        awaitClose { sub.remove() }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        firestore.collection("users").document(userId).collection("notifications").document(notificationId).update("isRead", true).await()
    }
    suspend fun markAllNotificationsRead(userId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        ids.chunked(500).forEach { chunk ->
            val b = firestore.batch()
            chunk.forEach { b.update(firestore.collection("users").document(userId).collection("notifications").document(it), "isRead", true) }
            b.commit().await()
        }
    }
    suspend fun deleteNotification(userId: String, id: String) {
        firestore.collection("users").document(userId).collection("notifications").document(id).delete().await()
    }
    suspend fun clearAllNotifications(userId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        ids.chunked(500).forEach { chunk ->
            val b = firestore.batch()
            chunk.forEach { b.delete(firestore.collection("users").document(userId).collection("notifications").document(it)) }
            b.commit().await()
        }
    }
    suspend fun cleanupOldNotifications(userId: String) {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        try {
            val snap = firestore.collection("users").document(userId).collection("notifications")
                .whereLessThan("timestamp", cutoff).limit(50).get().await()
            if (!snap.isEmpty) {
                val b = firestore.batch(); snap.documents.forEach { b.delete(it.reference) }; b.commit().await()
            }
        } catch (_: Exception) {}
    }

    // ───────── CHALLENGES ─────────
    fun getChallenges(userId: String): Flow<UiState<List<Challenge>>> = callbackFlow {
        if (userId.isEmpty()) { trySend(UiState.Empty); close(); return@callbackFlow }
        trySend(UiState.Loading)
        val sub = firestore.collection("challenges").whereArrayContains("participantIds", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(UiState.Error(mapFirestoreError(err), err.message ?: "Failed", err)); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(Challenge::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(if (list.isEmpty()) UiState.Empty else UiState.Success(list))
            }
        awaitClose { sub.remove() }
    }
    suspend fun joinChallenge(challengeId: String, userId: String) {
        firestore.collection("challenges").document(challengeId)
            .update("participantIds", FieldValue.arrayUnion(userId), "progress.$userId", 0.0f).await()
    }

    // ───────── DUELS ─────────
    fun getDuels(userId: String): Flow<UiState<List<Duel>>> = callbackFlow {
        if (userId.isEmpty()) { trySend(UiState.Empty); close(); return@callbackFlow }
        trySend(UiState.Loading)
        val sub = firestore.collection("duels").whereArrayContains("participants", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(UiState.Error(mapFirestoreError(err), err.message ?: "Failed", err)); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(Duel::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(if (list.isEmpty()) UiState.Empty else UiState.Success(list))
            }
        awaitClose { sub.remove() }
    }
    suspend fun createDuel(duel: Duel) { firestore.collection("duels").document(duel.id).set(duel).await() }
    suspend fun sendDuelInvite(targetUserId: String, senderId: String, senderName: String, habitType: String, duelId: String) {
        val n = CommunityNotification(id = UUID.randomUUID().toString(), senderId = senderId, senderName = senderName,
            type = "DUEL_INVITE", timestamp = System.currentTimeMillis(), habitType = habitType, duelId = duelId,
            message = "$senderName has challenged you to a $habitType duel!")
        firestore.collection("users").document(targetUserId).collection("notifications").document(n.id).set(n).await()
    }
    suspend fun sendDuelResult(targetUserId: String, senderId: String, senderName: String, duelId: String, message: String) {
        val n = CommunityNotification(id = UUID.randomUUID().toString(), senderId = senderId, senderName = senderName,
            type = "DUEL_RESULT", timestamp = System.currentTimeMillis(), duelId = duelId, message = message)
        firestore.collection("users").document(targetUserId).collection("notifications").document(n.id).set(n).await()
    }

    suspend fun resolveAndCompleteDuel(duelId: String) {
        val ref = firestore.collection("duels").document(duelId)
        firestore.runTransaction { txn ->
            val snap = txn.get(ref)
            if (!snap.exists()) return@runTransaction
            if ((snap.getString("status") ?: "ACTIVE") != "ACTIVE") return@runTransaction
            val endStr = snap.getString("endDate") ?: ""
            try {
                if (endStr.isNotEmpty()) {
                    val end = java.time.LocalDate.parse(endStr)
                    if (!java.time.LocalDate.now().isAfter(end)) return@runTransaction
                }
            } catch (_: Exception) { return@runTransaction }
            val parts = snap.get("participants") as? List<*> ?: emptyList<Any>()
            if (parts.size < 2) return@runTransaction
            val p1 = parts[0] as? String ?: return@runTransaction
            val p2 = parts[1] as? String ?: return@runTransaction
            val scores = snap.get("scores") as? Map<*, *> ?: emptyMap<Any, Any>()
            val s1 = (scores[p1] as? Number)?.toInt() ?: 0
            val s2 = (scores[p2] as? Number)?.toInt() ?: 0
            val winner = when { s1 > s2 -> p1; s2 > s1 -> p2; else -> "DRAW" }
            txn.update(ref, mapOf("status" to "COMPLETED", "winnerId" to winner, "resolvedAt" to FieldValue.serverTimestamp()))
            val habitType = snap.getString("habitType") ?: "General"
            listOf(p1 to (if (winner == p1) "You won the duel against $habitType!" else if (winner == p2) "You lost the duel against $habitType." else "The duel ended in a draw."),
                   p2 to (if (winner == p2) "You won the duel against $habitType!" else if (winner == p1) "You lost the duel against $habitType." else "The duel ended in a draw."))
                .forEach { (uid, msg) ->
                    val n = CommunityNotification(id = "duelresult_${duelId}_$uid", senderId = "SYSTEM", senderName = "Motion.Pulse",
                        type = "DUEL_RESULT", timestamp = System.currentTimeMillis(), duelId = duelId, message = msg)
                    txn.set(firestore.collection("users").document(uid).collection("notifications").document(n.id), n)
                }
        }.await()
    }

    suspend fun getActiveDuelsOnce(userId: String): List<Duel> {
        val snap = firestore.collection("duels").whereArrayContains("participants", userId)
            .whereEqualTo("status", "ACTIVE").get().await()
        return snap.documents.mapNotNull { it.toObject(Duel::class.java)?.copy(id = it.id) }
    }

    suspend fun updateDuelScore(duelId: String, userId: String) {
        val ref = firestore.collection("duels").document(duelId)
        firestore.runTransaction { txn ->
            val snap = txn.get(ref)
            val scores = (snap.get("scores") as? MutableMap<String, Any>) ?: mutableMapOf()
            scores[userId] = ((scores[userId] as? Number)?.toLong() ?: 0L) + 1L
            txn.update(ref, "scores", scores)
        }.await()
    }
    suspend fun decrementDuelScore(duelId: String, userId: String) {
        val ref = firestore.collection("duels").document(duelId)
        firestore.runTransaction { txn ->
            val snap = txn.get(ref)
            val scores = (snap.get("scores") as? MutableMap<String, Any>) ?: mutableMapOf()
            scores[userId] = maxOf(0L, ((scores[userId] as? Number)?.toLong() ?: 0L) - 1L)
            txn.update(ref, "scores", scores)
        }.await()
    }
    suspend fun completeDuelStatus(duelId: String, winnerId: String?) {
        firestore.collection("duels").document(duelId).update("status", "COMPLETED", "winnerId", winnerId).await()
    }

    // ───────── FRIEND CODES ─────────
    suspend fun findUserByFriendCode(rawCode: String, myUid: String, myCode: String?, context: android.content.Context) =
        FriendCodeDataSource(context).lookup(rawCode, myUid, myCode)

    // ───────── FRIEND REQUESTS ─────────
    fun getFriendRequests(userId: String): Flow<UiState<List<com.example.motionpulse.data.local.entity.FriendEntity>>> = callbackFlow {
        if (userId.isEmpty()) { trySend(UiState.Empty); close(); return@callbackFlow }
        trySend(UiState.Loading)
        val sub = firestore.collection("friendRequests").whereArrayContains("participants", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(UiState.Error(mapFirestoreError(err), err.message ?: "Failed", err)); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { d ->
                    val r = d.getString("requesterUid") ?: ""
                    val c = d.getString("recipientUid") ?: ""
                    val s = try { com.example.motionpulse.data.local.entity.FriendStatus.valueOf(d.getString("status") ?: "PENDING") }
                            catch (_: Exception) { com.example.motionpulse.data.local.entity.FriendStatus.PENDING }
                    com.example.motionpulse.data.local.entity.FriendEntity(id = d.id, requesterUid = r, recipientUid = c, status = s)
                } ?: emptyList()
                trySend(if (list.isEmpty()) UiState.Empty else UiState.Success(list))
            }
        awaitClose { sub.remove() }
    }

    /**
     * IMPORTANT: The FRIEND REQUEST document ID and the NOTIFICATION ID are now identical
     * (both are `${min(sender,recipient)}_${max(sender,recipient)}`). This is required for
     * the Accept button in the Notification tray to correctly target the friend request doc.
     */
    suspend fun sendFriendRequest(senderId: String, senderName: String, recipientId: String) {
        if (senderId == recipientId) throw IllegalArgumentException("That's your own code.")

        // --- 1) Make sure the sender's profile exists (heals "Player 1" from fresh logins) ---
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null && currentUser.uid == senderId) {
                val publicRef = firestore.collection("publicProfiles").document(senderId)
                val usersRef = firestore.collection("users").document(senderId)
                val pubSnap = publicRef.get().await()
                if (!pubSnap.exists()) {
                    val name = currentUser.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                        ?: "User"
                    publicRef.set(hashMapOf(
                        "uid" to senderId,
                        "displayName" to name,
                        "totalAuraXp" to 0L,
                        "currentLevel" to 1,
                        "currentStreak" to 0,
                        "recentXp" to emptyMap<String, Long>()
                    )).await()
                }
                val userSnap = usersRef.get().await()
                if (!userSnap.exists()) {
                    val name = currentUser.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                        ?: "User"
                    usersRef.set(hashMapOf(
                        "uid" to senderId,
                        "displayName" to name,
                        "email" to (currentUser.email ?: ""),
                        "totalAuraXp" to 0L,
                        "currentLevel" to 1,
                        "currentStreak" to 0,
                        "language" to "English",
                        "remindersEnabled" to true,
                        "shareMilestonesWithFriends" to true,
                        "showHabitNamesInPosts" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )).await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CommunityRepository", "Profile heal skipped: ${e.message}")
        }

        // --- 2) Existing limit checks ---
        val myReqs = firestore.collection("friendRequests").whereArrayContains("participants", senderId).get().await()
        val pendingSent = myReqs.documents.count { it.getString("status") == "PENDING" && it.getString("requesterUid") == senderId }
        if (pendingSent >= 20) throw IllegalStateException("You have too many pending sent requests (max 20).")
        val friendCount = myReqs.documents.count { it.getString("status") == "ACCEPTED" }
        if (friendCount >= 100) throw IllegalStateException("You have reached the maximum limit of 100 friends.")

        // --- 3) Write the friend request document ---
        val requestId = if (senderId < recipientId) "${senderId}_${recipientId}" else "${recipientId}_${senderId}"
        val existing = firestore.collection("friendRequests").document(requestId).get().await()
        if (existing.exists()) {
            val status = existing.getString("status")
            val requester = existing.getString("requesterUid")
            if (status == "ACCEPTED") throw IllegalStateException("You're already friends with this user.")
            if (status == "PENDING") {
                if (requester == recipientId) { acceptRequest(requestId, senderId, senderName); return }
                else throw IllegalStateException("Request already sent.")
            }
            if (status == "DECLINED") {
                val respondedAt = existing.getTimestamp("respondedAt")?.toDate()?.time ?: existing.getLong("createdAt") ?: 0L
                val seven = 7L * 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - respondedAt < seven) throw IllegalStateException("You can send another request later.")
                existing.reference.delete().await()
            }
        }

        val request = mapOf(
            "requesterUid" to senderId,
            "recipientUid" to recipientId,
            "participants" to listOf(senderId, recipientId),
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "respondedAt" to null
        )
        firestore.collection("friendRequests").document(requestId).set(request).await()

        // --- 4) Notification (ID == requestId, so accept works) ---
        val effectiveSenderName = senderName.ifBlank { "User" }
        val notification = CommunityNotification(
            id = requestId,
            senderId = senderId,
            senderName = effectiveSenderName,
            type = "FRIEND_REQUEST",
            timestamp = System.currentTimeMillis(),
            message = "$effectiveSenderName sent you a friend request!"
        )
        firestore.collection("users").document(recipientId)
            .collection("notifications").document(notification.id)
            .set(notification).await()
    }

    suspend fun cancelFriendRequest(requestId: String) {
        firestore.collection("friendRequests").document(requestId).delete().await()
    }

    suspend fun respondToFriendRequest(requestId: String, userId: String, userName: String, otherUserId: String, accept: Boolean) {
        if (accept) acceptRequest(requestId, userId, userName) else declineRequest(requestId, userId)
    }

    suspend fun acceptRequest(docId: String, recipientUid: String, recipientName: String): Result<Unit> {
        return try {
            val reqRef = firestore.collection("friendRequests").document(docId)
            val snap = reqRef.get().await()
            if (!snap.exists()) return Result.failure(Exception("This request is no longer available."))
            if (snap.getString("status") != "PENDING" || snap.getString("recipientUid") != recipientUid)
                return Result.failure(Exception("This request has already been processed."))
            val requester = snap.getString("requesterUid")

            // --- heal recipient's public profile so the feed has a real actorName ---
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null && currentUser.uid == recipientUid) {
                    val pubRef = firestore.collection("publicProfiles").document(recipientUid)
                    val pubSnap = pubRef.get().await()
                    if (!pubSnap.exists()) {
                        val name = recipientName.ifBlank {
                            currentUser.displayName?.takeIf { it.isNotBlank() }
                                ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                                ?: "User"
                        }
                        pubRef.set(hashMapOf(
                            "uid" to recipientUid,
                            "displayName" to name,
                            "totalAuraXp" to 0L,
                            "currentLevel" to 1,
                            "currentStreak" to 0,
                            "recentXp" to emptyMap<String, Long>()
                        )).await()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("CommunityRepository", "acceptRequest heal skipped: ${e.message}")
            }

            val batch = firestore.batch()
            batch.update(reqRef, mapOf("status" to "ACCEPTED", "respondedAt" to FieldValue.serverTimestamp()))
            batch.delete(firestore.collection("users").document(recipientUid)
                .collection("notifications").document(docId))
            if (requester != null) {
                val feedId = "friendaccepted_$docId"
                batch.set(firestore.collection("activityFeed").document(feedId), hashMapOf(
                    "id" to feedId,
                    "actorId" to recipientUid,
                    "actorName" to recipientName.ifBlank { "User" },
                    "type" to "FRIEND_ACCEPTED",
                    "timestamp" to System.currentTimeMillis(),
                    "visibleTo" to listOf(recipientUid, requester),
                    "reactions" to emptyMap<String, Boolean>()
                ))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "acceptRequest failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun declineRequest(docId: String, recipientUid: String): Result<Unit> {
        return try {
            val reqRef = firestore.collection("friendRequests").document(docId)
            val snap = reqRef.get().await()
            if (!snap.exists()) return Result.failure(Exception("This request is no longer available."))
            if (snap.getString("status") != "PENDING" || snap.getString("recipientUid") != recipientUid)
                return Result.failure(Exception("This request has already been processed."))

            val batch = firestore.batch()
            batch.update(reqRef, mapOf("status" to "DECLINED", "respondedAt" to FieldValue.serverTimestamp()))
            batch.delete(firestore.collection("users").document(recipientUid).collection("notifications").document(docId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "declineRequest failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ───────── UNFRIEND ─────────
    suspend fun unfriendAtomic(userId: String, otherUserId: String, db: AppDatabase? = null) {
        val batch = firestore.batch()

        // 1. Delete the friend request (ACCEPTED) between the two users
        try {
            val reqs = firestore.collection("friendRequests")
                .whereArrayContains("participants", userId)
                .get().await()
            reqs.documents.find { (it.get("participants") as? List<*>)?.contains(otherUserId) == true }
                ?.let { batch.delete(it.reference) }
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepository", "unfriend: reqs lookup failed: ${e.message}", e)
        }

        // 2. Cancel active duels with that user
        try {
            val duels = firestore.collection("duels")
                .whereArrayContains("participants", userId)
                .get().await()
            duels.documents.forEach { d ->
                if ((d.get("participants") as? List<*>)?.contains(otherUserId) == true &&
                    d.getString("status") == "ACTIVE") {
                    batch.update(d.reference, "status", "CANCELLED")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepository", "unfriend: duels lookup failed: ${e.message}", e)
        }

        // 3. Remove otherUserId from visibleTo of the user's own feed entries
        //    Use per-document reads to avoid queries Firestore rules reject.
        try {
            val myFeed = firestore.collection("activityFeed")
                .whereEqualTo("actorId", userId)
                .limit(20)
                .get().await()
            myFeed.documents.forEach { doc ->
                try {
                    val currentVisible = (doc.get("visibleTo") as? List<*>)?.map { it.toString() } ?: emptyList()
                    if (currentVisible.contains(otherUserId)) {
                        batch.update(doc.reference, "visibleTo", FieldValue.arrayRemove(otherUserId))
                    }
                } catch (_: Exception) { /* ignore individual doc failures */ }
            }
        } catch (e: Exception) {
            // If this still throws PERMISSION_DENIED, swallow it — it's not critical for unfriending.
            android.util.Log.w("CommunityRepository", "unfriend: feed cleanup skipped (${e.message})")
        }

        // 4. Commit the batch (deletes friend request, cancels duels)
        try {
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepository", "unfriend: batch commit failed: ${e.message}", e)
        }

        // 5. Remove the local Room friendship row
        db?.let {
            try {
                val all = it.friendDao().getAllFriendships(userId).first()
                all.find { f ->
                    (f.requesterUid == userId && f.recipientUid == otherUserId) ||
                    (f.requesterUid == otherUserId && f.recipientUid == userId)
                }?.let { f -> it.friendDao().deleteFriend(f) }
            } catch (e: Exception) {
                android.util.Log.e("CommunityRepository", "unfriend: local DB cleanup failed: ${e.message}", e)
            }
        }
    }

    suspend fun unfriend(userId: String, otherUserId: String) { unfriendAtomic(userId, otherUserId, null) }

    // ───────── FRIEND PROFILES ─────────
    fun getFriendsProfiles(friendIds: List<String>): Flow<UiState<List<UserProfileEntity>>> = callbackFlow {
        if (friendIds.isEmpty()) { trySend(UiState.Empty); close(); return@callbackFlow }
        trySend(UiState.Loading)
        val scope = CoroutineScope(Dispatchers.IO)
        val job = scope.launch {
            try {
                val profiles = friendIds.map { uid ->
                    async {
                        try { firestore.collection("publicProfiles").document(uid).get().await().toObject(UserProfileEntity::class.java) }
                        catch (_: Exception) { UserProfileEntity(uid = uid, displayName = "Unknown user") }
                    }
                }.awaitAll().filterNotNull()
                trySend(if (profiles.isEmpty()) UiState.Empty else UiState.Success(profiles))
            } catch (e: Exception) {
                val kind = if (e is FirebaseFirestoreException) mapFirestoreError(e) else ErrorKind.UNKNOWN
                trySend(UiState.Error(kind, e.message ?: "Failed to load profiles", e))
            }
        }
        awaitClose { job.cancel() }
    }

    // ───────── FEED CLEANUP / ACHIEVEMENTS ─────────
    suspend fun deleteActorFeedEntriesInBatches(actorId: String) {
        if (actorId.isEmpty()) return
        val snap = firestore.collection("activityFeed").whereEqualTo("actorId", actorId).limit(400).get().await()
        if (!snap.isEmpty) {
            val b = firestore.batch(); snap.documents.forEach { b.delete(it.reference) }; b.commit().await()
        }
    }
    suspend fun removeHabitNameFromActorFeedEntries(actorId: String) {
        if (actorId.isEmpty()) return
        val snap = firestore.collection("activityFeed").whereEqualTo("actorId", actorId).limit(400).get().await()
        if (!snap.isEmpty) {
            val b = firestore.batch(); snap.documents.forEach { b.update(it.reference, "habitName", FieldValue.delete()) }; b.commit().await()
        }
    }
    suspend fun postAchievementToFeed(entryId: String, actorId: String, actorName: String, habitName: String, type: String, visibleTo: List<String>) {
        try {
            firestore.collection("activityFeed").document(entryId).set(hashMapOf(
                "id" to entryId, "actorId" to actorId, "actorName" to actorName, "habitName" to habitName,
                "type" to type, "timestamp" to System.currentTimeMillis(), "visibleTo" to visibleTo,
                "reactions" to emptyMap<String, Boolean>()
            )).await()
        } catch (_: Exception) {}
    }
    suspend fun deleteAchievementFromFeed(entryId: String) {
        try { firestore.collection("activityFeed").document(entryId).delete().await() } catch (_: Exception) {}
    }
}
