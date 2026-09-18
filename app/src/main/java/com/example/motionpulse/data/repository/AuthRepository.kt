package com.example.motionpulse.data.repository

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
     * Updates the user's display name across both Authentication and the Firestore profile.
     */
    suspend fun updateDisplayName(newName: String) {
        val user = firebaseAuth.currentUser ?: return
        val profileUpdates = userProfileChangeRequest {
            displayName = newName
        }
        user.updateProfile(profileUpdates).await()
        firestore.collection("users").document(user.uid).update("displayName", newName).await()
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
                    close(error)
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
     * Ensures that a corresponding Firestore document exists for the authenticated user.
     * Reconstructs the profile from Auth metadata if the document is missing.
     */
    suspend fun syncUserProfile(user: FirebaseUser, preferredDisplayName: String? = null) {
        val uid = user.uid
        try {
            android.util.Log.d("AuthRepository", "Starting profile sync for UID: $uid")
            val userDoc = firestore.collection("users").document(uid).get().await()
            
            if (!userDoc.exists()) {
                android.util.Log.i("AuthRepository", "Creating new Firestore profile for UID: $uid")
                val profile = UserProfileEntity(
                    uid = uid,
                    displayName = preferredDisplayName ?: user.displayName ?: "User",
                    email = user.email ?: "",
                    avatarUrl = user.photoUrl?.toString(),
                    totalAuraXp = 0,
                    currentLevel = 1,
                    createdAt = Instant.now()
                )
                firestore.collection("users").document(uid).set(profile).await()
                android.util.Log.d("AuthRepository", "Profile created successfully")
            } else {
                android.util.Log.d("AuthRepository", "Profile already exists for UID: $uid")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to sync Firestore profile for $uid: ${e.message}", e)
            // Non-fatal: allows the user to use the app locally even if sync fails.
        }
    }
}
