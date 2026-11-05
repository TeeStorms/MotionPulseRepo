package com.example.motionpulse.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.AuthRepository
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.LayoutLinkedAccountsBinding
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.adapters.LinkedAccountItem
import com.example.motionpulse.ui.adapters.LinkedAccountsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LinkedAccountsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutLinkedAccountsBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()
    private lateinit var accountsAdapter: LinkedAccountsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutLinkedAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadLinkedAccounts()
    }

    private fun setupRecyclerView() {
        accountsAdapter = LinkedAccountsAdapter(
            allProviders = emptyList(),
            onAction = { item -> handleAccountAction(item) },
            onResendEmail = {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        authRepository.sendEmailVerification()
                        Toast.makeText(requireContext(), "Verification email resent", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to resend verification email", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvLinkedAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLinkedAccounts.adapter = accountsAdapter
    }

    private fun loadLinkedAccounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                val providers = authRepository.getLinkedProviders()
                val email = user?.email ?: ""
                val isVerified = user?.isEmailVerified ?: true

                val items = listOf(
                    LinkedAccountItem(
                        providerId = "password",
                        displayName = "Email and password",
                        email = email,
                        isLinked = providers.contains("password"),
                        isVerified = isVerified
                    ),
                    LinkedAccountItem(
                        providerId = "google.com",
                        displayName = "Google",
                        email = email,
                        isLinked = providers.contains("google.com"),
                        isVerified = true
                    )
                )

                accountsAdapter = LinkedAccountsAdapter(
                    allProviders = providers,
                    onAction = { item -> handleAccountAction(item) },
                    onResendEmail = {
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                authRepository.sendEmailVerification()
                                Toast.makeText(requireContext(), "Verification email resent", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Failed to resend verification email", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                binding.rvLinkedAccounts.adapter = accountsAdapter
                accountsAdapter.submitList(items)
            } catch (e: Exception) {
                android.util.Log.e("LinkedAccountsDialog", "Error loading linked accounts: ${e.message}", e)
            }
        }
    }

    private fun handleAccountAction(item: LinkedAccountItem) {
        if (item.isLinked) {
            // Unlink
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Unlink ${item.displayName}?")
                .setMessage("Are you sure you want to unlink this sign-in method?")
                .setPositiveButton("Unlink") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            authRepository.unlinkProvider(item.providerId)
                            Snackbar.make(binding.root, "${item.displayName} unlinked successfully", Snackbar.LENGTH_SHORT).show()
                            loadLinkedAccounts()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Failed to unlink: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            if (item.providerId == "password") {
                // Add password dialog
                val input = android.widget.EditText(requireContext()).apply {
                    hint = "New password (min 6 chars)"
                    inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                    .setTitle("Add Password")
                    .setView(input)
                    .setPositiveButton("Add") { _, _ ->
                        val pass = input.text.toString()
                        if (pass.length >= 6) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                try {
                                    authRepository.addPassword(pass)
                                    Snackbar.make(binding.root, "Password added successfully", Snackbar.LENGTH_SHORT).show()
                                    loadLinkedAccounts()
                                } catch (e: Exception) {
                                    Toast.makeText(requireContext(), "Failed to add password: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else if (item.providerId == "google.com") {
                // Link Google (using GoogleAuthUiClient)
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val client = com.example.motionpulse.data.repository.GoogleAuthUiClient(requireContext())
                        val webClientId = getString(R.string.default_web_client_id)
                        val nonce = java.util.UUID.randomUUID().toString()
                        val result = client.signIn(webClientId, nonce)
                        if (result is com.example.motionpulse.data.repository.GoogleSignInResult.Success) {
                            authRepository.linkGoogle(result.idToken)
                            Snackbar.make(binding.root, "Google account linked successfully", Snackbar.LENGTH_SHORT).show()
                            loadLinkedAccounts()
                        } else {
                            Toast.makeText(requireContext(), "Google sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), e.message ?: "Failed to link Google account", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
