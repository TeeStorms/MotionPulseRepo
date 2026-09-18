package com.example.motionpulse.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    object Cancelled : GoogleSignInResult()
    object NoAccounts : GoogleSignInResult()
    data class Failure(val errorResId: Int) : GoogleSignInResult()
}

/**
 * Client to handle the Google Sign-In UI flow using the modern Credential Manager API.
 */
class GoogleAuthUiClient(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(webClientId: String, nonce: String): GoogleSignInResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            val credential = result.credential
            
            android.util.Log.d("GoogleAuth", "Credential Type: ${credential.type}")
            
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleSignInResult.Success(googleIdTokenCredential.idToken)
                } catch (e: Exception) {
                    android.util.Log.e("GoogleAuth", "Failed to parse Google ID Token", e)
                    GoogleSignInResult.Failure(com.example.motionpulse.R.string.error_google_generic)
                }
            } else {
                android.util.Log.e("GoogleAuth", "Received unexpected credential type: ${credential.type}")
                GoogleSignInResult.Failure(com.example.motionpulse.R.string.error_google_generic)
            }
        } catch (e: GetCredentialCancellationException) {
            android.util.Log.d("GoogleAuth", "User cancelled sign-in")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            android.util.Log.e("GoogleAuth", "No accounts found on device")
            GoogleSignInResult.NoAccounts
        } catch (e: GetCredentialException) {
            android.util.Log.e("GoogleAuth", "Credential error: ${e.message}", e)
            GoogleSignInResult.Failure(com.example.motionpulse.R.string.error_google_config)
        } catch (e: Exception) {
            android.util.Log.e("GoogleAuth", "Unknown sign-in error", e)
            GoogleSignInResult.Failure(com.example.motionpulse.R.string.error_google_generic)
        }
    }

    /**
     * Generates a secure random SHA-256 nonce for use in Google ID token requests.
     */
    fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
