package com.example.motionpulse.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.GoogleAuthUiClient
import com.example.motionpulse.data.repository.GoogleSignInResult
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentLoginBinding
import com.example.motionpulse.domain.auth.AuthErrorType
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    private val googleAuthUiClient by lazy { GoogleAuthUiClient(requireContext()) }

    // Gemini API Key configured for AI features
    private val geminiApiKey: String = System.getenv("GEMINI_API_KEY") ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("MOTION.PULSE")

        // Pre-fill email
        if (authViewModel.lastUsedEmail.isNotEmpty()) {
            binding.etEmail.setText(authViewModel.lastUsedEmail)
        }

        setupFooterLink()
        setupTextWatchers()
        setupListeners()
        observeViewModel()
    }

    private fun setupFooterLink() {
        val footerText = "New Here? Create an Account"
        val spannable = SpannableString(footerText)
        val pinkColor = ContextCompat.getColor(requireContext(), R.color.text_link_color)
        val startIdx = footerText.indexOf("Create an Account")
        if (startIdx != -1) {
            spannable.setSpan(
                ForegroundColorSpan(pinkColor),
                startIdx,
                footerText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tvFooterRegister.text = spannable
        binding.tvFooterRegister.setOnClickListener {
            findNavController().navigate(R.id.register)
        }
    }

    private fun setupTextWatchers() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }

        binding.btnGoogle.setOnClickListener { attemptGoogleSignIn() }

        binding.btnResendVerification.setOnClickListener {
            authViewModel.resendVerificationEmail()
        }
    }

    private fun attemptLogin() {
        hideKeyboard()
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        }

        if (isValid) {
            authViewModel.login(email, password)
        }
    }

    private fun attemptGoogleSignIn() {
        hideKeyboard()
        lifecycleScope.launch {
            try {
                val webClientId = getString(R.string.default_web_client_id)
                val nonce = authViewModel.getGoogleIdTokenNonce()
                val result = googleAuthUiClient.signIn(webClientId, nonce)

                when (result) {
                    is GoogleSignInResult.Success -> {
                        authViewModel.signInWithGoogle(result.idToken)
                    }
                    is GoogleSignInResult.Failure -> {
                        Toast.makeText(
                            requireContext(),
                            "Google sign-in couldn't be completed. Please try again or use your email and password.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    GoogleSignInResult.Cancelled -> { /* User cancelled */ }
                    GoogleSignInResult.NoAccounts -> {
                        Toast.makeText(requireContext(), "No Google accounts found on device", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginFragment", "Google sign-in exception: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Google sign-in couldn't be completed. Please try again or use your email and password.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Email Address"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.etEmail.text?.toString()?.trim() ?: "")
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
            .setTitle("Reset Password")
            .setMessage("Enter your email address to receive a password reset link.")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    authViewModel.resetPassword(email)
                    Toast.makeText(
                        requireContext(),
                        "If an account exists for that email, a reset link has been sent",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.isOffline.collectLatest { isOffline ->
                        binding.tvOfflineBanner.visibility = if (isOffline) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    authViewModel.cooldownTimer.collectLatest { seconds ->
                        if (seconds > 0) {
                            binding.btnLogin.isEnabled = false
                            binding.btnLogin.text = "Try again in ${seconds}s"
                        } else {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = "Log In"
                        }
                    }
                }

                launch {
                    authViewModel.resendTimer.collectLatest { seconds ->
                        if (seconds > 0) {
                            binding.btnResendVerification.isEnabled = false
                            binding.btnResendVerification.text = "Resend in ${seconds}s"
                        } else {
                            binding.btnResendVerification.isEnabled = true
                            binding.btnResendVerification.text = "Resend Verification Email"
                        }
                    }
                }

                launch {
                    authViewModel.authState.collectLatest { state ->
                        handleAuthState(state)
                    }
                }
            }
        }
    }

    private fun handleAuthState(state: AuthState) {
        when (state) {
            is AuthState.Verifying -> {
                setInputsEnabled(false)
                binding.overlaySigningIn.visibility = View.VISIBLE
                binding.overlayProgressBar.visibility = View.VISIBLE
                binding.overlaySuccessIcon.visibility = View.GONE
                binding.overlayStatusText.text = "Signing you in..."
            }
            is AuthState.Authenticated -> {
                setInputsEnabled(false)
                binding.overlaySigningIn.visibility = View.VISIBLE
                binding.overlayProgressBar.visibility = View.GONE
                binding.overlaySuccessIcon.visibility = View.VISIBLE
                binding.overlayStatusText.text = "Welcome back!"

                viewLifecycleOwner.lifecycleScope.launch {
                    delay(600)
                    if (isResumed) {
                        findNavController().navigate(
                            R.id.dashboard,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.login, true)
                                .build()
                        )
                    }
                }
            }
            is AuthState.Error -> {
                setInputsEnabled(true)
                binding.overlaySigningIn.visibility = View.GONE

                when (state.type) {
                    AuthErrorType.USER_NOT_FOUND -> {
                        binding.tilEmail.error = "No account found with that email."
                    }
                    AuthErrorType.INVALID_CREDENTIALS -> {
                        binding.tilPassword.error = "Incorrect password. Try again or reset it below."
                    }
                    AuthErrorType.NETWORK_ERROR -> {
                        Toast.makeText(requireContext(), "No internet connection. Check your network and try again.", Toast.LENGTH_LONG).show()
                    }
                    AuthErrorType.TOO_MANY_ATTEMPTS -> {
                        Toast.makeText(requireContext(), "Too many attempts. Please wait a few minutes before trying again.", Toast.LENGTH_LONG).show()
                    }
                    AuthErrorType.EMAIL_NOT_VERIFIED -> {
                        binding.cardVerificationRequired.visibility = View.VISIBLE
                    }
                    else -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            else -> {
                setInputsEnabled(true)
                binding.overlaySigningIn.visibility = View.GONE
                binding.cardVerificationRequired.visibility = View.GONE
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etEmail.isEnabled = enabled
        binding.etPassword.isEnabled = enabled
        binding.btnLogin.isEnabled = enabled && (authViewModel.cooldownTimer.value == 0)
        binding.btnGoogle.isEnabled = enabled
        binding.tvFooterRegister.isEnabled = enabled
        binding.tvForgotPassword.isEnabled = enabled
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
