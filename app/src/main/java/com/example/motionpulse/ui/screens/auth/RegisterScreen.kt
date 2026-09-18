package com.example.motionpulse.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.R
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.components.GradientButton
import com.example.motionpulse.ui.components.MotionPulseTextField
import com.example.motionpulse.ui.theme.*

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: (String) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var fullNameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val isLoading = authState is AuthState.Verifying
    
    val fullNameError = if (fullNameTouched && fullName.length < 2) stringResource(R.string.error_name_too_short) else null
    val emailError = if (emailTouched && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) stringResource(R.string.error_invalid_email) else null
    val passwordError = if (passwordTouched && password.length < 6) stringResource(R.string.error_short_password) else null
    val confirmPasswordError = if (confirmPasswordTouched && confirmPassword != password) stringResource(R.string.error_password_mismatch) else null

    val passwordStrength = remember(password) { calculatePasswordStrength(password) }

    LaunchedEffect(Unit) {
        // Automatically focuses the first text field for a smoother onboarding experience.
        focusRequester.requestFocus()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegistrationSuccess((authState as AuthState.Authenticated).uid)
        }
    }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Motion Pulse Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Build your rhythm.",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create an Account",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            MotionPulseTextField(
                value = fullName,
                onValueChange = { 
                    fullName = it
                    fullNameTouched = true
                },
                placeholder = "Full Name",
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CardBorderAlt) },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.focusRequester(focusRequester)
            )
            if (fullNameError != null) {
                Text(text = fullNameError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 12.dp), textAlign = TextAlign.Start)
            }

            Spacer(modifier = Modifier.height(12.dp))

            MotionPulseTextField(
                value = email,
                onValueChange = { 
                    email = it
                    emailTouched = true
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
                enabled = !isLoading
            )
            if (emailError != null) {
                Text(text = emailError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 12.dp), textAlign = TextAlign.Start)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                MotionPulseTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordTouched = true
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
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    enabled = !isLoading
                )
                
                if (password.isNotEmpty()) {
                    PasswordStrengthIndicator(strength = passwordStrength)
                }
                
                if (passwordError != null) {
                    Text(text = passwordError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 12.dp), textAlign = TextAlign.Start)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MotionPulseTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                },
                placeholder = "Confirm Password",
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CardBorderAlt) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }, enabled = !isLoading) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = CardBorderAlt
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        confirmPasswordTouched = true
                        if (fullNameError == null && emailError == null && passwordError == null && confirmPassword == password) {
                            viewModel.register(email, password, confirmPassword, fullName)
                        }
                    }
                ),
                enabled = !isLoading,
                modifier = Modifier.onFocusChanged { 
                    if (!it.isFocused && confirmPassword.isNotEmpty()) {
                        confirmPasswordTouched = true
                    }
                }
            )
            if (confirmPasswordError != null) {
                Text(text = confirmPasswordError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 12.dp), textAlign = TextAlign.Start)
            } else if (confirmPassword.isNotEmpty() && password.isNotEmpty()) {
                val matches = confirmPassword == password
                Text(
                    text = if (matches) "Passwords match" else "Passwords do not match",
                    color = if (matches) AccentBlue else Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            GradientButton(
                text = "Register",
                onClick = { 
                    fullNameTouched = true
                    emailTouched = true
                    passwordTouched = true
                    confirmPasswordTouched = true
                    
                    if (fullNameError == null && emailError == null && passwordError == null && confirmPasswordError == null) {
                        viewModel.register(email, password, confirmPassword, fullName)
                    }
                },
                isLoading = isLoading,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By registering, you agree to our Terms of Service and Privacy Policy.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(text = "Have an Account? ", color = TextPrimary)
                Text(
                    text = "Log In",
                    color = TextLinkColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val color = when (strength) {
        PasswordStrength.WEAK -> Color.Red
        PasswordStrength.FAIR -> Color.Yellow
        PasswordStrength.STRONG -> AccentBlue
    }
    
    val label = when (strength) {
        PasswordStrength.WEAK -> "Weak"
        PasswordStrength.FAIR -> "Fair"
        PasswordStrength.STRONG -> "Strong"
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Password Strength", color = TextSecondary, fontSize = 11.sp)
            Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index <= strength.ordinal) color else CardBorderAlt.copy(alpha = 0.3f))
                )
            }
        }
    }
}

enum class PasswordStrength {
    WEAK, FAIR, STRONG
}

fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.length < 6) return PasswordStrength.WEAK
    
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    if (password.any { it.isDigit() }) score++
    
    return when {
        score >= 3 -> PasswordStrength.STRONG
        score >= 1 -> PasswordStrength.FAIR
        else -> PasswordStrength.WEAK
    }
}
