package com.example.motionpulse.domain.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.AuthRepository
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.data.repository.GoogleSignInResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val preferenceRepository: PreferenceRepository? = null
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isSyncingFailed = MutableStateFlow(false)
    val isSyncingFailed: StateFlow<Boolean> = _isSyncingFailed.asStateFlow()

    private val _resendTimer = MutableStateFlow(0)
    val resendTimer: StateFlow<Int> = _resendTimer.asStateFlow()
    private var timerJob: Job? = null

    private val _cooldownTimer = MutableStateFlow(0)
    val cooldownTimer: StateFlow<Int> = _cooldownTimer.asStateFlow()
    private var cooldownJob: Job? = null

    val lastUsedEmail: String = preferenceRepository?.getLastUsedEmail() ?: ""

    /**
     * Generates a unique security nonce for the Google ID Token request process.
     */
    fun getGoogleIdTokenNonce(): String {
        return java.util.UUID.randomUUID().toString()
    }

    /**
     * Verifies the current user session and synchronizes the profile data.
     * Routes users based on their authentication and onboarding status.
     */
    suspend fun checkSession() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            if (preferenceRepository?.isFirstLaunch() == true) {
                _authState.value = AuthState.OnboardingRequired
            } else {
                _authState.value = AuthState.Idle
            }
            return
        }

        _authState.value = AuthState.Verifying
        // Skips the manual verification gate for users authenticated via Google.
        val isGoogleUser = currentUser.providerData.any { it.providerId == "google.com" }
        if (isGoogleUser || currentUser.isEmailVerified) {
            try {
                withTimeout(3000L) {
                    authRepository.syncUserProfile(currentUser)
                }
                _isSyncingFailed.value = false
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Initial session sync failed, proceeding locally: ${e.message}")
                _isSyncingFailed.value = true
            }
            _authState.value = AuthState.Authenticated(currentUser.uid)
        } else {
            _authState.value = AuthState.AwaitingEmailVerification(currentUser.email ?: "")
        }
    }

    /**
     * Registers a new user account and triggers the email verification process.
     */
    fun register(email: String, password: String, confirmPassword: String, fullName: String) {
        if (!validateInput(email, password, fullName)) return
        
        if (password != confirmPassword) {
            _authState.value = AuthState.Error(AuthErrorType.INVALID_CREDENTIALS, "Passwords do not match")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Verifying
            try {
                val user = authRepository.registerWithEmail(email, password, fullName)
                if (user != null) {
                    authRepository.sendEmailVerification()
                    _authState.value = AuthState.AwaitingEmailVerification(email)
                    startResendTimer()
                } else {
                    _authState.value = AuthState.Error(AuthErrorType.UNKNOWN, "Registration failed")
                }
            } catch (e: Exception) {
                mapError(e)
            }
        }
    }

    /**
     * Authenticates a user with email and password credentials.
     * Enforces the email verification requirement for manual logins.
     */
    fun login(email: String, password: String) {
        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _authState.value = AuthState.Verifying
            try {
                val user = authRepository.loginWithEmail(email, password)
                if (user != null) {
                    val isGoogleUser = user.providerData.any { it.providerId == "google.com" }
                    if (isGoogleUser || user.isEmailVerified) {
                        try {
                            withTimeout(4000L) {
                                authRepository.syncUserProfile(user)
                            }
                            _isSyncingFailed.value = false
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Login sync failed, proceeding locally")
                            _isSyncingFailed.value = true
                        }
                        preferenceRepository?.saveLastUsedEmail(email)
                        _authState.value = AuthState.Authenticated(user.uid)
                    } else {
                        _authState.value = AuthState.Error(AuthErrorType.EMAIL_NOT_VERIFIED, "Email not verified")
                    }
                } else {
                    _authState.value = AuthState.Error(AuthErrorType.UNKNOWN, "Login failed")
                }
            } catch (e: Exception) {
                mapError(e)
            }
        }
    }

    /**
     * Completes the authentication process using a Google ID token.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Verifying
            try {
                val user = authRepository.signInWithGoogle(idToken)
                if (user != null) {
                    try {
                        withTimeout(4000L) {
                            authRepository.syncUserProfile(user)
                        }
                        _isSyncingFailed.value = false
                    } catch (e: Exception) {
                        android.util.Log.e("AuthViewModel", "Google login sync failed, proceeding locally")
                        _isSyncingFailed.value = true
                    }
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    _authState.value = AuthState.Error(AuthErrorType.GOOGLE_SIGN_IN_FAILED, "Google sign-in failed")
                }
            } catch (e: Exception) {
                mapError(e, AuthErrorType.GOOGLE_SIGN_IN_FAILED)
            }
        }
    }

    fun handleGoogleSignInResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> signInWithGoogle(result.idToken)
            is GoogleSignInResult.Cancelled -> _authState.value = AuthState.Idle
            is GoogleSignInResult.NoAccounts -> _authState.value = AuthState.Error(AuthErrorType.GOOGLE_SIGN_IN_FAILED, "No Google accounts found")
            is GoogleSignInResult.Failure -> _authState.value = AuthState.Error(AuthErrorType.GOOGLE_SIGN_IN_FAILED, "Google sign-in failed")
        }
    }

    fun resendVerificationEmail() {
        if (_resendTimer.value > 0) return
        
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                startResendTimer()
            } catch (e: Exception) {
                mapError(e)
            }
        }
    }

    fun checkVerificationStatus(onStillUnverified: () -> Unit = {}) {
        viewModelScope.launch {
            _authState.value = AuthState.Verifying
            try {
                val user = authRepository.reloadUser()
                if (user != null && user.isEmailVerified) {
                    authRepository.syncUserProfile(user)
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    val email = user?.email ?: ""
                    _authState.value = AuthState.AwaitingEmailVerification(email)
                    onStillUnverified()
                }
            } catch (e: Exception) {
                mapError(e)
            }
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        _resendTimer.value = 60
        timerJob = viewModelScope.launch {
            while (_resendTimer.value > 0) {
                delay(1000)
                _resendTimer.value -= 1
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error(AuthErrorType.INVALID_CREDENTIALS, "Please enter your email")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Verifying
            try {
                authRepository.sendPasswordResetEmail(email)
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.Idle
    }

    fun completeOnboarding() {
        preferenceRepository?.setFirstLaunchCompleted()
        _authState.value = AuthState.Idle
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun updateConnectivity(isOffline: Boolean) {
        _isOffline.value = isOffline
    }

    private fun mapError(e: Exception, fallbackType: AuthErrorType = AuthErrorType.UNKNOWN) {
        android.util.Log.e("AuthViewModel", "Mapping Auth Error: ${e.message}", e)
        val type = when (e) {
            is FirebaseAuthInvalidUserException -> AuthErrorType.USER_NOT_FOUND
            is FirebaseAuthInvalidCredentialsException -> AuthErrorType.INVALID_CREDENTIALS
            is FirebaseAuthUserCollisionException -> AuthErrorType.INVALID_CREDENTIALS
            is FirebaseAuthWeakPasswordException -> AuthErrorType.INVALID_CREDENTIALS
            is com.google.firebase.firestore.FirebaseFirestoreException -> {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    AuthErrorType.PROFILE_SYNC_FAILED
                } else fallbackType
            }
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_USER_DISABLED" -> AuthErrorType.ACCOUNT_DISABLED
                    "ERROR_TOO_MANY_REQUESTS" -> AuthErrorType.TOO_MANY_ATTEMPTS
                    "17" -> AuthErrorType.GOOGLE_SIGN_IN_FAILED // DEVELOPER_ERROR
                    else -> fallbackType
                }
            }
            is FirebaseNetworkException -> AuthErrorType.NETWORK_ERROR
            is FirebaseTooManyRequestsException -> AuthErrorType.TOO_MANY_ATTEMPTS
            is kotlinx.coroutines.TimeoutCancellationException -> AuthErrorType.NETWORK_ERROR
            else -> fallbackType
        }
        
        if (type == AuthErrorType.TOO_MANY_ATTEMPTS) {
            startCooldownTimer()
        }

        val message = when (type) {
            AuthErrorType.INVALID_CREDENTIALS -> "Incorrect email or password."
            AuthErrorType.USER_NOT_FOUND -> "No account found with this email."
            AuthErrorType.ACCOUNT_DISABLED -> "This account has been disabled. Contact support."
            AuthErrorType.TOO_MANY_ATTEMPTS -> "Too many attempts. Please wait a moment."
            AuthErrorType.NETWORK_ERROR -> "Network error. Please check your connection."
            AuthErrorType.GOOGLE_SIGN_IN_FAILED -> "Google configuration error. Check SHA-1."
            AuthErrorType.PROFILE_SYNC_FAILED -> "Signed in, but profile sync failed (Permissions)."
            else -> "An unexpected error occurred: ${e.localizedMessage}"
        }
        
        _authState.value = AuthState.Error(type, message)
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        _cooldownTimer.value = 30 // 30 second cooldown
        cooldownJob = viewModelScope.launch {
            while (_cooldownTimer.value > 0) {
                delay(1000)
                _cooldownTimer.value -= 1
            }
        }
    }

    private fun validateInput(email: String, password: String, fullName: String? = null): Boolean {
        if (email.isBlank() || password.isBlank() || (fullName != null && fullName.isBlank())) {
            _authState.value = AuthState.Error(AuthErrorType.INVALID_CREDENTIALS, "Please fill in all fields")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error(AuthErrorType.INVALID_CREDENTIALS, "Please enter a valid email")
            return false
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error(AuthErrorType.INVALID_CREDENTIALS, "Password must be at least 6 characters")
            return false
        }

        return true
    }
}
