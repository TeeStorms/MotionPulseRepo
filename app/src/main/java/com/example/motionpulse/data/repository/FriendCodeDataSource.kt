package com.example.motionpulse.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.domain.models.FriendLookupResult
import com.example.motionpulse.util.ConnectivityMonitor
import com.example.motionpulse.util.FriendCodeResult
import com.example.motionpulse.util.FriendCodeUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.security.SecureRandom

class FriendCodeDataSource(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val secureRandom = SecureRandom()
    private val alphabet = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "motion_pulse_db"
    ).fallbackToDestructiveMigration().build()

    private val offlineMsg = "Can't reach the server. Check your internet connection and try again.\nYour phone could not connect to Firebase. Try another network or turn off Private DNS or your VPN."

    private fun generateSecureCanonicalCode(): String {
        val c1 = (1..4).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
        val c2 = (1..4).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
        return "MP-$c1-$c2"
    }

    suspend fun ensureMyFriendCode(uid: String): Result<String> {
        var attempts = 0
        while (attempts < 5) {
            try {
                val userRef = firestore.collection("users").document(uid)
                val publicRef = firestore.collection("publicProfiles").document(uid)

                val userSnap = userRef.get(Source.SERVER).await()
                val existingCode = userSnap.getString("friendCode")

                val canonicalCode = if (existingCode != null) {
                    when (val res = FriendCodeUtil.normalize(existingCode)) {
                        is FriendCodeResult.Valid -> res.canonicalCode
                        is FriendCodeResult.InvalidFormat -> generateSecureCanonicalCode()
                    }
                } else {
                    generateSecureCanonicalCode()
                }

                val fcRef = firestore.collection("friendCodes").document(canonicalCode)
                val fcSnap = fcRef.get(Source.SERVER).await()

                if (fcSnap.exists()) {
                    val ownerUid = fcSnap.getString("uid")
                    if (ownerUid == uid) {
                        val displayName = userSnap.getString("displayName") ?: "User"
                        db.userProfileDao().insertProfile(
                            UserProfileEntity(uid = uid, displayName = displayName, friendCode = canonicalCode)
                        )
                        return Result.success(canonicalCode)
                    } else {
                        attempts++
                        continue
                    }
                }

                val batch = firestore.batch()
                batch.set(userRef, mapOf("friendCode" to canonicalCode), com.google.firebase.firestore.SetOptions.merge())
                batch.set(publicRef, mapOf("uid" to uid, "friendCode" to canonicalCode), com.google.firebase.firestore.SetOptions.merge())
                batch.set(fcRef, mapOf("uid" to uid))
                batch.commit().await()

                val displayName = userSnap.getString("displayName") ?: "User"
                db.userProfileDao().insertProfile(
                    UserProfileEntity(uid = uid, displayName = displayName, friendCode = canonicalCode)
                )
                return Result.success(canonicalCode)
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    attempts++
                } else {
                    Log.e("FriendCodeDataSource", "Firestore error ensuring friend code: ${e.message}", e)
                    return Result.failure(e)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("FriendCodeDataSource", "Error ensuring friend code: ${e.message}", e)
                return Result.failure(e)
            }
        }
        return Result.failure(IllegalStateException("Failed to generate unique friend code after 5 attempts."))
    }

    suspend fun lookup(input: String, myUid: String, myCode: String?): FriendLookupResult {
        val norm = FriendCodeUtil.normalize(input)
        if (norm is FriendCodeResult.InvalidFormat) {
            return FriendLookupResult.InvalidFormat(norm.message)
        }
        val canonicalCode = (norm as FriendCodeResult.Valid).canonicalCode

        if (myCode != null && canonicalCode == myCode) {
            return FriendLookupResult.Self("That's your own code.")
        }

        if (!connectivityMonitor.isNetworkValidated()) {
            Log.d("FriendFlow", "Lookup aborted: No validated network connection")
            return FriendLookupResult.Offline(offlineMsg)
        }

        val maskedCode = if (canonicalCode.length >= 5) canonicalCode.take(5) + "..." else canonicalCode

        try {
            return withTimeout(10_000) {
                var targetUid: String? = null
                var answeredPath = "mapping"

                // Primary: get friendCodes/{code} from SERVER
                val codeDoc = firestore.collection("friendCodes").document(canonicalCode).get(Source.SERVER).await()
                if (codeDoc.metadata.isFromCache) {
                    Log.d("FriendFlow", "Searched '$maskedCode': Answered from cache -> Offline")
                    return@withTimeout FriendLookupResult.Offline(offlineMsg)
                }

                if (codeDoc.exists()) {
                    targetUid = codeDoc.getString("uid")
                }

                // Fallback when friendCodes/{code} does not exist on server (and ONLY then)
                if (targetUid == null) {
                    answeredPath = "fallback"
                    try {
                        val publicByCode = firestore.collection("publicProfiles")
                            .whereEqualTo("friendCode", canonicalCode)
                            .limit(1)
                            .get(Source.SERVER)
                            .await()

                        if (publicByCode.metadata.isFromCache) {
                            return@withTimeout FriendLookupResult.Offline(offlineMsg)
                        }

                        if (!publicByCode.isEmpty) {
                            targetUid = publicByCode.documents.first().getString("uid")
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.e("FriendFlow", "Fallback query error for $maskedCode", e)
                    }
                }

                if (targetUid == null) {
                    Log.d("FriendFlow", "Searched '$maskedCode' via $answeredPath: NotFound")
                    return@withTimeout FriendLookupResult.NotFound
                }

                if (targetUid == myUid) {
                    return@withTimeout FriendLookupResult.Self("That's your own code.")
                }

                // Self-heal friendCodes mapping if answered via fallback
                if (answeredPath == "fallback") {
                    try {
                        firestore.collection("friendCodes").document(canonicalCode).set(mapOf("uid" to targetUid)).await()
                    } catch (e: Exception) {
                        Log.e("FriendFlow", "Error self-healing friendCode mapping: ${e.message}", e)
                    }
                }

                val publicDoc = firestore.collection("publicProfiles").document(targetUid).get(Source.SERVER).await()
                if (publicDoc.metadata.isFromCache) {
                    return@withTimeout FriendLookupResult.Offline(offlineMsg)
                }

                if (!publicDoc.exists()) {
                    Log.d("FriendFlow", "Searched '$maskedCode' via $answeredPath: NotFound (missing public profile)")
                    return@withTimeout FriendLookupResult.NotFound
                }

                val profile = publicDoc.toObject(UserProfileEntity::class.java)
                if (profile != null) {
                    Log.d("FriendFlow", "Searched '$maskedCode' via $answeredPath: Found (${profile.displayName})")
                    return@withTimeout FriendLookupResult.Found(profile)
                }

                Log.d("FriendFlow", "Searched '$maskedCode' via $answeredPath: NotFound")
                return@withTimeout FriendLookupResult.NotFound
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("FriendFlow", "TimeoutCancellationException during lookup for $maskedCode", e)
            return FriendLookupResult.Offline(offlineMsg)
        } catch (e: FirebaseFirestoreException) {
            Log.e("FriendFlow", "FirestoreException during lookup for $maskedCode: ${e.code} - ${e.message}", e)
            return when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    FriendLookupResult.PermissionDenied("Couldn't search right now. Try again.")
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    FriendLookupResult.Error("Please sign in again.")
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    FriendLookupResult.Offline(offlineMsg)
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    FriendLookupResult.RateLimited(60)
                else ->
                    FriendLookupResult.Error("Couldn't search right now. Try again.")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FriendFlow", "Exception during lookup for $maskedCode: ${e.message}", e)
            return FriendLookupResult.Offline(offlineMsg)
        }
    }

    suspend fun resetMyFriendCode(uid: String, oldCode: String?): Result<String> {
        var attempts = 0
        while (attempts < 5) {
            try {
                val newCode = generateSecureCanonicalCode()
                val oldFcRef = if (!oldCode.isNullOrBlank()) firestore.collection("friendCodes").document(oldCode) else null
                val newFcRef = firestore.collection("friendCodes").document(newCode)
                val userRef = firestore.collection("users").document(uid)
                val publicRef = firestore.collection("publicProfiles").document(uid)

                val newFcSnap = newFcRef.get(Source.SERVER).await()
                if (newFcSnap.exists()) {
                    attempts++
                    continue
                }

                val batch = firestore.batch()
                if (oldFcRef != null) {
                    batch.delete(oldFcRef)
                }
                batch.set(newFcRef, mapOf("uid" to uid))
                batch.set(userRef, mapOf("friendCode" to newCode), com.google.firebase.firestore.SetOptions.merge())
                batch.set(publicRef, mapOf("uid" to uid, "friendCode" to newCode), com.google.firebase.firestore.SetOptions.merge())
                batch.commit().await()

                val userSnap = userRef.get(Source.SERVER).await()
                val displayName = userSnap.getString("displayName") ?: "User"
                db.userProfileDao().insertProfile(
                    UserProfileEntity(uid = uid, displayName = displayName, friendCode = newCode)
                )
                return Result.success(newCode)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                attempts++
            }
        }
        return Result.failure(IllegalStateException("Failed to reset friend code."))
    }
}
