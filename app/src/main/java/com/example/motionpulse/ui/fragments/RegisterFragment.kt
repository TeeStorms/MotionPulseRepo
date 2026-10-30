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
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentRegisterBinding
import com.example.motionpulse.domain.auth.AuthErrorType
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    private var confirmPasswordTouched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("MOTION.PULSE")

        setupFooterLink()
        setupTextWatchers()
        setupListeners()
        observeViewModel()
    }

    private fun setupFooterLink() {
        val footerText = "Have an Account? Log In"
        val spannable = SpannableString(footerText)
        val pinkColor = ContextCompat.getColor(requireContext(), R.color.text_link_color)
        val startIdx = footerText.indexOf("Log In")
        if (startIdx != -1) {
            spannable.setSpan(
                ForegroundColorSpan(pinkColor),
                startIdx,
                footerText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tvFooterLogin.text = spannable
        binding.tvFooterLogin.setOnClickListener {
            findNavController().navigate(R.id.login)
        }
    }

    private fun setupTextWatchers() {
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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
                updatePasswordStrength(s?.toString() ?: "")
                if (confirmPasswordTouched) {
                    validateConfirmPassword()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) confirmPasswordTouched = true
        }

        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                confirmPasswordTouched = true
                validateConfirmPassword()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister()
                true
            } else false
        }
    }

    private fun updatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            binding.tvPasswordStrength.visibility = View.GONE
            return
        }
        binding.tvPasswordStrength.visibility = View.VISIBLE
        when {
            password.length < 6 -> {
                binding.tvPasswordStrength.text = "Strength: Weak (min 6 characters)"
                binding.tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            }
            password.length < 10 || !password.any { it.isDigit() } -> {
                binding.tvPasswordStrength.text = "Strength: Fair"
                binding.tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
            }
            else -> {
                binding.tvPasswordStrength.text = "Strength: Strong"
                binding.tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = binding.etPassword.text?.toString() ?: ""
        val confirm = binding.etConfirmPassword.text?.toString() ?: ""
        return if (password != confirm) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            false
        } else {
            binding.tilConfirmPassword.error = null
            true
        }
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        hideKeyboard()
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        var isValid = true

        if (name.length < 2) {
            binding.tilName.error = "Name must be at least 2 characters"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (!validateConfirmPassword()) {
            isValid = false
        }

        if (isValid) {
            authViewModel.register(email, password, confirmPassword, name)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
            }
            is AuthState.AwaitingEmailVerification -> {
                setInputsEnabled(true)
                if (isResumed) {
                    findNavController().navigate(
                        R.id.email_verification,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.login, false)
                            .build()
                    )
                }
            }
            is AuthState.Error -> {
                setInputsEnabled(true)
                when (state.type) {
                    AuthErrorType.INVALID_CREDENTIALS -> {
                        if (state.message.contains("email", ignoreCase = true)) {
                            binding.tilEmail.error = "An account with that email already exists."
                        } else {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    AuthErrorType.NETWORK_ERROR -> {
                        Toast.makeText(requireContext(), "No internet connection. Check your network and try again.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            else -> {
                setInputsEnabled(true)
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etName.isEnabled = enabled
        binding.etEmail.isEnabled = enabled
        binding.etPassword.isEnabled = enabled
        binding.etConfirmPassword.isEnabled = enabled
        binding.btnRegister.isEnabled = enabled
        binding.tvFooterLogin.isEnabled = enabled
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
