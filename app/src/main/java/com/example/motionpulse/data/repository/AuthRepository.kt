package com.example.motionpulse.data.repository

import com.example.motionpulse.data.local.entity.PublicProfileEntity
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Returns the currently authenticated Firebase user, or null if no session exists.
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Creates a new account with email and password, and updates the user's initial display name.
     */
    suspend fun registerWithEmail(email: String, password: String, displayName: String): FirebaseUser? {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user
        if (user != null) {
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName
            }
            user.updateProfile(profileUpdates).await()
            syncUserProfile(user, displayName)
        }
        return user
    }

    /**
     * Signs in a user with email and password credentials.
     * Triggers a profile synchronization task upon success.
     */
    suspend fun loginWithEmail(email: String, password: String): FirebaseUser? {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user = result.user
        if (user != null) {
            syncUserProfile(user)
        }
        return user
    }

    /**
     * Authenticates a user with a Google ID token via Credential Manager.
     */
    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user
        if (user != null) {
            syncUserProfile(user)
        }
        return user
    }

    /**
     * Sends a password recovery email to the specified address.
     */
    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    /**
     * Requests a new verification email for the currently signed-in user.
     */
    suspend fun sendEmailVerification() {
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
    }

    /**
     * Re-fetches the latest user data from the Firebase Auth server to verify recent changes (e.g., verification status).
     */
    suspend fun reloadUser(): FirebaseUser? {
        firebaseAuth.currentUser?.reload()?.await()
        return firebaseAuth.currentUser
    }

    /**
     * Terminates the current user session and signs them out of Firebase.
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Updates the user's display name across Authentication, Private Profile, and Public Profile.
     */
    suspend fun updateDisplayName(newName: String) {
        val user = firebaseAuth.currentUser ?: return
        val profileUpdates = userProfileChangeRequest {
            displayName = newName
        }
        user.updateProfile(profileUpdates).await()

        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(user.uid)
        val publicRef = firestore.collection("publicProfiles").document(user.uid)

        batch.set(userRef, mapOf("displayName" to newName), SetOptions.merge())
        batch.set(publicRef, mapOf("uid" to user.uid, "displayName" to newName), SetOptions.merge())
        batch.commit().await()
    }

    /**
     * Re-authenticates the user with their password before sensitive account operations.
     */
    suspend fun reauthenticate(password: String) {
        val user = firebaseAuth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        user.reauthenticate(credential).await()
    }

    /**
     * Changes the user's password to a new value.
     */
    suspend fun updatePassword(newPassword: String) {
        val user = firebaseAuth.currentUser ?: return
        user.updatePassword(newPassword).await()
    }

    /**
     * Updates a single field in the user's Firestore settings document.
     */
    suspend fun updateUserSetting(field: String, value: Any) {
        val user = firebaseAuth.currentUser ?: return
        val updates = mapOf(field to value)
        firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()
        firestore.collection("publicProfiles").document(user.uid).set(updates, SetOptions.merge()).await()
    }

    suspend fun updateUserProfileFields(uid: String, updates: Map<String, Any>) {
        try {
            firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            firestore.collection("publicProfiles").document(uid).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error updating profile fields for $uid: ${e.message}", e)
        }
    }

    /**
     * Observes real-time profile updates for a specific user ID from Firestore.
     */
    fun getUserProfileFlow(uid: String): Flow<UserProfileEntity?> = callbackFlow {
        val subscription = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                snapshot?.let {
                    if (it.exists()) {
                        val profile = it.toObject(UserProfileEntity::class.java)
                        trySend(profile)
                    } else {
                        trySend(null)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun syncUserProfile(user: FirebaseUser, preferredDisplayName: String? = null) {
        val uid = user.uid
        try {
            val userDocRef = firestore.collection("users").document(uid)
            val publicDocRef = firestore.collection("publicProfiles").document(uid)
            val userDoc = userDocRef.get().await()

            var friendCode = userDoc.getString("friendCode")
            if (friendCode.isNullOrBlank()) {
                friendCode = generateNewFriendCode()
            }
            val canonicalCode = when (val res = com.example.motionpulse.util.FriendCodeUtil.normalize(friendCode)) {
                is com.example.motionpulse.util.FriendCodeResult.Valid -> res.canonicalCode
                is com.example.motionpulse.util.FriendCodeResult.InvalidFormat -> generateNewFriendCode()
            }

            if (!userDoc.exists()) {
                val newProfile = UserProfileEntity(
                    uid = uid,
                    displayName = preferredDisplayName ?: user.displayName ?: "User",
                    email = user.email ?: "",
                    avatarUrl = user.photoUrl?.toString(),
                    friendCode = canonicalCode,
                    totalAuraXp = 0,
                    currentLevel = 1,
                    createdAt = Instant.now()
                )
                userDocRef.set(newProfile).await()
            } else {
                userDocRef.update("friendCode", canonicalCode).await()
            }

            val fcRef = firestore.collection("friendCodes").document(canonicalCode)
            val fcSnap = fcRef.get().await()
            if (!fcSnap.exists()) {
                val batch = firestore.batch()
                batch.set(fcRef, mapOf("uid" to uid))
                val publicProfile = PublicProfileEntity(
                    uid = uid,
                    displayName = user.displayName ?: "User",
                    avatarUrl = user.photoUrl?.toString(),
                    totalAuraXp = 0,
                    currentLevel = 1,
                    currentStreak = 0,
                    recentXp = emptyMap()
                )
                batch.set(publicDocRef, publicProfile, SetOptions.merge())
                batch.commit().await()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to sync Firestore profile for $uid: ${e.message}", e)
        }
    }

    /**
     * Ensures users/{uid} and publicProfiles/{uid} exist for the given Firebase user.
     * Safe to call on every login (including Google Sign-In). Idempotent.
     */
    suspend fun ensureProfileForUser(user: FirebaseUser) {
        val uid = user.uid
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "User"
        val email = user.email ?: ""

        val usersRef = firestore.collection("users").document(uid)
        val publicRef = firestore.collection("publicProfiles").document(uid)

        // 1) Create users/{uid} if it doesn't exist
        val usersSnap = try { usersRef.get().await() } catch (e: Exception) { null }
        if (usersSnap == null || !usersSnap.exists()) {
            val profile = hashMapOf(
                "uid" to uid,
                "displayName" to displayName,
                "email" to email,
                "totalAuraXp" to 0L,
                "currentLevel" to 1,
                "currentStreak" to 0,
                "language" to "English",
                "remindersEnabled" to true,
                "shareMilestonesWithFriends" to true,
                "showHabitNamesInPosts" to false,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            try { usersRef.set(profile).await() } catch (_: Exception) {}
        }

        // 2) Create publicProfiles/{uid} if it doesn't exist
        val publicSnap = try { publicRef.get().await() } catch (e: Exception) { null }
        if (publicSnap == null || !publicSnap.exists()) {
            val publicDoc = hashMapOf(
                "uid" to uid,
                "displayName" to displayName,
                "totalAuraXp" to 0L,
                "currentLevel" to 1,
                "currentStreak" to 0,
                "recentXp" to emptyMap<String, Long>()
            )
            try { publicRef.set(publicDoc).await() } catch (_: Exception) {}
        }
    }

    suspend fun resetFriendCode(uid: String, oldCode: String?): String {
        val newCode = generateNewFriendCode()
        try {
            val batch = firestore.batch()
            if (!oldCode.isNullOrBlank()) {
                batch.delete(firestore.collection("friendCodes").document(oldCode))
            }
            batch.set(firestore.collection("friendCodes").document(newCode), mapOf("uid" to uid))
            batch.update(firestore.collection("users").document(uid), "friendCode", newCode)
            batch.commit().await()
            return newCode
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error resetting friend code: ${e.message}", e)
            return oldCode ?: ""
        }
    }

    private fun generateNewFriendCode(): String {
        val chars = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        val code1 = (1..4).map { chars.random() }.joinToString("")
        val code2 = (1..4).map { chars.random() }.joinToString("")
        return "MP-$code1-$code2"
    }

    suspend fun getLinkedProviders(): List<String> {
        val user = firebaseAuth.currentUser ?: return emptyList()
        user.reload().await()
        return user.providerData.map { it.providerId }
    }

    suspend fun linkGoogle(idToken: String): FirebaseUser? {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("Not signed in")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            user.linkWithCredential(credential).await()
            syncUserProfile(user)
        } catch (e: Exception) {
            if (e.message?.contains("credential-already-in-use", ignoreCase = true) == true) {
                throw Exception("This Google account is already used by another Motion.Pulse account.")
            }
            throw e
        }
        return firebaseAuth.currentUser
    }

    suspend fun addPassword(newPassword: String) {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("Not signed in")
        val email = user.email ?: throw IllegalStateException("No email associated with account")
        val credential = EmailAuthProvider.getCredential(email, newPassword)
        user.linkWithCredential(credential).await()
    }

    suspend fun unlinkProvider(providerId: String) {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("Not signed in")
        user.unlink(providerId).await()
    }
}
