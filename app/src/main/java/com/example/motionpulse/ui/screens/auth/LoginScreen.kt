package com.example.motionpulse.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.repository.GoogleAuthUiClient
import com.example.motionpulse.domain.auth.AuthErrorType
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.components.GradientButton
import com.example.motionpulse.ui.components.MotionPulseTextField
import com.example.motionpulse.ui.screens.auth.components.AuthOverlay
import com.example.motionpulse.ui.screens.auth.components.VerificationRequiredPanel
import com.example.motionpulse.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val resendTimer by viewModel.resendTimer.collectAsState()
    val cooldownTimer by viewModel.cooldownTimer.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current
    
    val googleAuthUiClient = remember {
        GoogleAuthUiClient(context = context)
    }

    // Pre-populates the email field with the last successfully used address.
    var email by remember { mutableStateOf(viewModel.lastUsedEmail) }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var showSuccessFeedback by remember { mutableStateOf(false) }

    val isLoading = authState is AuthState.Verifying
    val defaultWebClientId = stringResource(id = com.example.motionpulse.R.string.default_web_client_id)
    
    // Resets password visibility to hidden upon navigating to this screen.
    DisposableEffect(Unit) {
        passwordVisible = false
        onDispose {}
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            showSuccessFeedback = true
            // Delays navigation slightly to allow the "Welcome back" animation to be seen.
            delay(800)
            onLoginSuccess((authState as AuthState.Authenticated).uid)
        }
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotDialog = false },
            onConfirm = { resetEmail ->
                viewModel.resetPassword(resetEmail)
                showForgotDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(HeaderGradient2Stop),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MOTION.PULSE",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            if (isOffline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.8f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You're offline — connect to sign in",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Logo
                Image(
                    painter = painterResource(id = com.example.motionpulse.R.drawable.ic_logo),
                    contentDescription = "Motion Pulse Logo",
                    modifier = Modifier
                        .size(150.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Build your rhythm.",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Email Verification Panel
                if (authState is AuthState.Error && (authState as AuthState.Error).type == AuthErrorType.EMAIL_NOT_VERIFIED) {
                    VerificationRequiredPanel(
                        resendTimer = resendTimer,
                        onResend = { viewModel.resendVerificationEmail() }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                MotionPulseTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = null 
                    },
                    placeholder = "Email Address",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CardBorderAlt) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.padding(bottom = 4.dp),
                    enabled = !isLoading
                )
                if (emailError != null) {
                    Text(text = emailError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }

                Spacer(modifier = Modifier.height(12.dp))

                MotionPulseTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = null
                    },
                    placeholder = "Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CardBorderAlt) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = CardBorderAlt
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (cooldownTimer == 0) {
                                // Trigger Login logic
                                if (email.isBlank()) {
                                    emailError = "Email is required"
                                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    emailError = "Invalid email format"
                                }
                                if (password.isBlank()) {
                                    passwordError = "Password is required"
                                }
                                
                                if (emailError == null && passwordError == null) {
                                    viewModel.login(email, password)
                                }
                            }
                        }
                    ),
                    modifier = Modifier.padding(bottom = 4.dp),
                    enabled = !isLoading
                )
                if (passwordError != null) {
                    Text(text = passwordError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (authState is AuthState.Error && (authState as AuthState.Error).type != AuthErrorType.EMAIL_NOT_VERIFIED) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                GradientButton(
                    text = if (cooldownTimer > 0) "Try again in ${cooldownTimer}s" else "Log In",
                    onClick = { 
                        if (email.isBlank()) {
                            emailError = "Email is required"
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailError = "Invalid email format"
                        }
                        if (password.isBlank()) {
                            passwordError = "Password is required"
                        }
                        
                        if (emailError == null && passwordError == null) {
                            viewModel.login(email, password)
                        }
                    },
                    isLoading = false, // Handled by overlay
                    enabled = !isLoading && cooldownTimer == 0 && !isOffline
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Forgot Password?",
                    color = TextLinkColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !isLoading) { showForgotDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                    Text(
                        text = "or",
                        color = CardBorder,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google Sign-In
                Button(
                    onClick = { 
                        viewModel.clearError()
                        scope.launch {
                            val nonce = googleAuthUiClient.generateNonce()
                            val webClientId = if (defaultWebClientId.contains("YOUR_REAL_ID_HERE")) {
                                "1076060472554-dummy.apps.googleusercontent.com"
                            } else {
                                defaultWebClientId
                            }

                            val result = googleAuthUiClient.signIn(webClientId, nonce)
                            viewModel.handleGoogleSignInResult(result)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.5f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    enabled = !isLoading && !isOffline
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.motionpulse.R.drawable.ic_google),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(text = "New Here? ", color = TextPrimary)
                    Text(
                        text = "Create an Account",
                        color = TextLinkColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToRegister() }
                    )
                }
            }
        }

        // Authentication Overlay
        if (isLoading || showSuccessFeedback) {
            AuthOverlay(isSuccess = showSuccessFeedback)
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text(text = "Reset Password", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = "Enter your email to receive a reset link.", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                MotionPulseTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email Address",
                    enabled = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(email) }) {
                Text("Send", color = AccentPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        modifier = Modifier.border(1.dp, CardBorderAlt, RoundedCornerShape(24.dp))
    )
}
