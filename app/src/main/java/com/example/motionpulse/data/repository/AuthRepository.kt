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
        firestore.collection("users").document(user.uid).update("displayName", newName).await()
        try {
            firestore.collection("publicProfiles").document(user.uid).update("displayName", newName).await()
        } catch (e: Exception) {
            // Ignore if public profile not created yet
        }
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
        firestore.collection("users").document(user.uid).update(field, value).await()
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

    /**
     * Ensures that corresponding private and public Firestore documents exist for the authenticated user.
     * Backfills public profile and friendCodes mapping document from private profile if missing.
     */
    suspend fun syncUserProfile(user: FirebaseUser, preferredDisplayName: String? = null) {
        val uid = user.uid
        try {
            android.util.Log.d("AuthRepository", "Starting profile sync for UID: $uid")
            val userDocRef = firestore.collection("users").document(uid)
            val publicDocRef = firestore.collection("publicProfiles").document(uid)
            val userDoc = userDocRef.get().await()
            
            var profile = if (!userDoc.exists()) {
                android.util.Log.i("AuthRepository", "Creating new Firestore private profile for UID: $uid")
                val friendCode = createUniqueFriendCodeDoc(uid)
                val newProfile = UserProfileEntity(
                    uid = uid,
                    displayName = preferredDisplayName ?: user.displayName ?: "User",
                    email = user.email ?: "",
                    avatarUrl = user.photoUrl?.toString(),
                    friendCode = friendCode,
                    totalAuraXp = 0,
                    currentLevel = 1,
                    createdAt = Instant.now()
                )
                userDocRef.set(newProfile).await()
                newProfile
            } else {
                userDoc.toObject(UserProfileEntity::class.java)
            }

            if (profile != null) {
                // Ensure friendCodes/{code} document exists for legacy or existing code
                val validCode = ensureFriendCodeDocExists(uid, profile.friendCode)
                if (validCode != profile.friendCode) {
                    profile = profile.copy(friendCode = validCode)
                    userDocRef.update("friendCode", validCode).await()
                }

                // Sync / backfill public profile
                val publicProfile = PublicProfileEntity(
                    uid = profile.uid,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    totalAuraXp = profile.totalAuraXp,
                    currentLevel = profile.currentLevel,
                    currentStreak = profile.currentStreak,
                    friendCode = profile.friendCode,
                    recentXp = profile.recentXp
                )
                publicDocRef.set(publicProfile, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to sync Firestore profile for $uid: ${e.message}", e)
        }
    }

    suspend fun createUniqueFriendCodeDoc(uid: String): String {
        var attempts = 0
        while (attempts < 5) {
            val candidateCode = generateNewFriendCode()
            try {
                val codeDocRef = firestore.collection("friendCodes").document(candidateCode)
                val doc = codeDocRef.get().await()
                if (!doc.exists()) {
                    codeDocRef.set(mapOf("uid" to uid)).await()
                    return candidateCode
                }
            } catch (e: Exception) {
                // Collision or error, retry
            }
            attempts++
        }
        val fallback = generateNewFriendCode()
        try {
            firestore.collection("friendCodes").document(fallback).set(mapOf("uid" to uid)).await()
        } catch (e: Exception) {}
        return fallback
    }

    suspend fun ensureFriendCodeDocExists(uid: String, existingCode: String): String {
        if (existingCode.isNotBlank()) {
            try {
                val codeDocRef = firestore.collection("friendCodes").document(existingCode)
                val doc = codeDocRef.get().await()
                if (doc.exists()) {
                    return existingCode
                } else {
                    codeDocRef.set(mapOf("uid" to uid)).await()
                    return existingCode
                }
            } catch (e: Exception) {
                // Failover to new code if error
            }
        }
        return createUniqueFriendCodeDoc(uid)
    }

    suspend fun resetFriendCode(uid: String, oldCode: String): String {
        if (oldCode.isNotBlank()) {
            try {
                firestore.collection("friendCodes").document(oldCode).delete().await()
            } catch (e: Exception) {}
        }
        val newCode = createUniqueFriendCodeDoc(uid)
        try {
            firestore.collection("users").document(uid).update("friendCode", newCode).await()
            firestore.collection("publicProfiles").document(uid).update("friendCode", newCode).await()
        } catch (e: Exception) {}
        return newCode
    }

    private fun generateNewFriendCode(): String {
        val chars = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        val code1 = (1..4).map { chars.random() }.joinToString("")
        val code2 = (1..4).map { chars.random() }.joinToString("")
        return "MP-$code1-$code2"
    }
}
