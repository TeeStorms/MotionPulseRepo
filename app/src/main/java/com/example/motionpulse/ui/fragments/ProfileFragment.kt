package com.example.motionpulse.ui.fragments

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentProfileBinding
import com.example.motionpulse.databinding.LayoutChangePasswordDialogBinding
import com.example.motionpulse.databinding.LayoutChangeProfileNameDialogBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.viewmodels.ProfileViewModel
import com.example.motionpulse.util.NameValidationResult
import com.example.motionpulse.util.PasswordStrength
import com.example.motionpulse.util.PasswordValidationResult
import com.example.motionpulse.util.TimeAgoFormatter
import com.example.motionpulse.util.ValidationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    private val db by lazy {
        Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).fallbackToDestructiveMigration().build()
    }

    private var currentProfileViewModel: ProfileViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val profileViewModel = ViewModelProvider(
                                this@ProfileFragment,
                                ProfileViewModel.Factory(db, uid)
                            )[ProfileViewModel::class.java]

                            currentProfileViewModel = profileViewModel
                            setupProfileContent(profileViewModel)
                        }
                    }
                }
            }
        }
    }

    private fun setupProfileContent(viewModel: ProfileViewModel) {
        binding.btnSeeAllBadges.setOnClickListener {
            findNavController().navigate(R.id.badges_gallery)
        }

        binding.switchReminders.isChecked = preferenceRepository.isRemindersEnabled()
        binding.switchReminders.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleReminders(isChecked, requireContext())
            Toast.makeText(requireContext(), if (isChecked) "Reminders enabled" else "Reminders disabled", Toast.LENGTH_SHORT).show()
        }

        binding.switchShareMilestones.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                val checkbox = android.widget.CheckBox(requireContext()).apply {
                    text = "Also remove my past posts"
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                    .setTitle("Stop sharing new milestones?")
                    .setView(checkbox)
                    .setPositiveButton("Confirm") { _, _ ->
                        viewModel.toggleShareMilestones(false)
                        binding.switchShowHabitNames.isEnabled = false
                        binding.layoutRowShowHabitNames.alpha = 0.5f
                        if (checkbox.isChecked) {
                            viewModel.deleteActorFeedEntries()
                            Toast.makeText(requireContext(), "Past posts removed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        buttonView.isChecked = true
                    }
                    .setOnCancelListener {
                        buttonView.isChecked = true
                    }
                    .show()
            } else {
                viewModel.toggleShareMilestones(true)
                binding.switchShowHabitNames.isEnabled = true
                binding.layoutRowShowHabitNames.alpha = 1.0f
            }
        }

        binding.switchShowHabitNames.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                val checkbox = android.widget.CheckBox(requireContext()).apply {
                    text = "Also remove habit names from my past posts"
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                    .setTitle("Hide habit names?")
                    .setView(checkbox)
                    .setPositiveButton("Confirm") { _, _ ->
                        viewModel.toggleShowHabitNames(false)
                        if (checkbox.isChecked) {
                            viewModel.removeHabitNamesFromFeedEntries()
                            Toast.makeText(requireContext(), "Habit names removed from past posts", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        buttonView.isChecked = true
                    }
                    .setOnCancelListener {
                        buttonView.isChecked = true
                    }
                    .show()
            } else {
                viewModel.toggleShowHabitNames(true)
            }
        }

        binding.btnResetCode.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Reset Friend Code?")
                .setMessage("This will invalidate your old friend code and generate a new one.")
                .setPositiveButton("Reset") { _, _ ->
                    viewModel.resetFriendCode()
                    Toast.makeText(requireContext(), "Friend code reset", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvFriendCode.text?.toString() ?: ""
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Friend Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareCode.setOnClickListener {
            val code = binding.tvFriendCode.text?.toString() ?: ""
            val shareText = "Join me on Motion.Pulse! Add me with my friend code: $code"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Friend Code"))
        }

        binding.btnChangeProfile.setOnClickListener {
            val currentName = viewModel.profileState.value.userProfile?.displayName ?: ""
            showChangeProfileNameDialog(viewModel, currentName)
        }

        binding.btnChangePassword.setOnClickListener {
            val providers = viewModel.profileState.value.linkedProviders
            if (providers.contains("google.com") && !providers.contains("password")) {
                Toast.makeText(
                    requireContext(),
                    "You sign in with Google. Add a password under Linked accounts.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                showChangePasswordDialog(viewModel)
            }
        }

        val languages = arrayOf("English", "Afrikaans", "Setswana", "isiZulu", "Xitsonga")
        val tags = arrayOf("en", "af", "tn", "zu", "ts")
        val currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.language 
            ?: preferenceRepository.getUserSettings().language 
            ?: "en"
        val initialIndex = tags.indexOf(currentTag).coerceAtLeast(0)
        binding.btnLanguage.text = "Language: ${languages[initialIndex]}"

        binding.btnLanguage.setOnClickListener {
            val updatedTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.language 
                ?: preferenceRepository.getUserSettings().language 
                ?: "en"
            val selectedIndex = tags.indexOf(updatedTag).coerceAtLeast(0)

            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Language")
                .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                    val newTag = tags[which]
                    viewModel.changeLanguage(newTag)
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags(newTag)
                    )
                    binding.btnLanguage.text = "Language: ${languages[which]}"
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnLinkedAccounts.setOnClickListener {
            val sheet = com.example.motionpulse.ui.dialogs.LinkedAccountsBottomSheetDialogFragment()
            sheet.show(childFragmentManager, "LINKED_ACCOUNTS_SHEET")
        }

        binding.btnExportCsv.setOnClickListener {
            val sheet = com.example.motionpulse.ui.dialogs.ExportDataBottomSheetDialogFragment()
            sheet.show(childFragmentManager, "EXPORT_DATA_SHEET")
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog(viewModel)
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Log Out?")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Log Out") { _, _ ->
                    authViewModel.logout()
                    findNavController().navigate(
                        R.id.login,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(0, true)
                            .build()
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profileState.collectLatest { state ->
                        val profile = state.userProfile
                        if (profile != null) {
                            val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
                            val memberSince = profile.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter)

                            binding.dropletHeader.setTitle(profile.displayName)
                            binding.dropletHeader.setCaption("Member since $memberSince")

                            val avatar = com.example.motionpulse.ui.views.UserAvatarView(requireContext()).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    (40 * resources.displayMetrics.density).toInt(),
                                    (40 * resources.displayMetrics.density).toInt()
                                )
                                setDisplayName(profile.displayName)
                            }
                            binding.dropletHeader.setTrailingView(avatar)

                            binding.tvStreakStat.text = "🔥 ${state.currentStreak}d"
                            binding.tvBadgesStat.text = "🏅 ${state.badgeCount}"

                            binding.tvFriendCode.text = profile.friendCode.ifBlank { "MP-XXXX-XXXX" }

                            binding.switchShareMilestones.isChecked = profile.shareMilestonesWithFriends
                            binding.switchShowHabitNames.isChecked = profile.showHabitNamesInPosts

                            if (state.badges.isNotEmpty()) {
                                val recent = state.badges.take(2)
                                val summary = recent.joinToString("\n") { badge ->
                                    val readableName = badge.badgeType.name.lowercase().replace("_", " ").split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                                    "• $readableName (${TimeAgoFormatter.format(badge.unlockedAt.toEpochMilli())})"
                                }
                                binding.tvRecentBadgesSummary.text = summary
                            } else {
                                binding.tvRecentBadgesSummary.text = "No badges unlocked yet."
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showChangeProfileNameDialog(viewModel: ProfileViewModel, currentName: String) {
        val dialogBinding = LayoutChangeProfileNameDialogBinding.inflate(layoutInflater)
        dialogBinding.etProfileName.setText(currentName)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
            .setView(dialogBinding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            saveBtn.isEnabled = false

            dialogBinding.etProfileName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val input = s?.toString() ?: ""
                    when (val result = ValidationUtils.validateProfileName(input, currentName)) {
                        is NameValidationResult.Valid -> {
                            dialogBinding.tilProfileName.error = null
                            saveBtn.isEnabled = true
                        }
                        is NameValidationResult.Invalid -> {
                            dialogBinding.tilProfileName.error = result.reason
                            saveBtn.isEnabled = false
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            dialogBinding.etProfileName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && saveBtn.isEnabled) {
                    saveBtn.performClick()
                    true
                } else false
            }

            saveBtn.setOnClickListener {
                val newName = dialogBinding.etProfileName.text?.toString()?.trim() ?: ""
                viewModel.updateName(newName)
                Snackbar.make(binding.root, "Profile name updated", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showChangePasswordDialog(viewModel: ProfileViewModel) {
        val dialogBinding = LayoutChangePasswordDialogBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
            .setView(dialogBinding.root)
            .setPositiveButton("Update Password", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val updateBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            updateBtn.isEnabled = false

            val validatePasswordForm = {
                val current = dialogBinding.etCurrentPassword.text?.toString() ?: ""
                val newPass = dialogBinding.etNewPassword.text?.toString() ?: ""
                val confirmPass = dialogBinding.etConfirmPassword.text?.toString() ?: ""

                // Strength meter calculation
                if (newPass.isNotEmpty()) {
                    dialogBinding.tvPasswordStrength.visibility = View.VISIBLE
                    when (ValidationUtils.calculatePasswordStrength(newPass)) {
                        PasswordStrength.WEAK -> {
                            dialogBinding.tvPasswordStrength.text = "Strength: Weak"
                            dialogBinding.tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                        }
                        PasswordStrength.FAIR -> {
                            dialogBinding.tvPasswordStrength.text = "Strength: Fair"
                            dialogBinding.tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
                        }
                        PasswordStrength.STRONG -> {
                            dialogBinding.tvPasswordStrength.text = "Strength: Strong"
                            dialogBinding.tvPasswordStrength.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                        }
                    }
                } else {
                    dialogBinding.tvPasswordStrength.visibility = View.GONE
                }

                when (val result = ValidationUtils.validatePasswordChange(current, newPass, confirmPass)) {
                    is PasswordValidationResult.Valid -> {
                        dialogBinding.tilConfirmPassword.error = null
                        updateBtn.isEnabled = true
                    }
                    is PasswordValidationResult.Invalid -> {
                        updateBtn.isEnabled = false
                    }
                }
            }

            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validatePasswordForm()
                }
                override fun afterTextChanged(s: Editable?) {}
            }

            dialogBinding.etCurrentPassword.addTextChangedListener(textWatcher)
            dialogBinding.etNewPassword.addTextChangedListener(textWatcher)
            dialogBinding.etConfirmPassword.addTextChangedListener(textWatcher)

            updateBtn.setOnClickListener {
                val current = dialogBinding.etCurrentPassword.text?.toString() ?: ""
                val newPass = dialogBinding.etNewPassword.text?.toString() ?: ""

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        viewModel.reauthenticate(current)
                        viewModel.updatePassword(newPass)
                        Snackbar.make(binding.root, "Password updated", Snackbar.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        dialogBinding.tilCurrentPassword.error = "Current password is incorrect."
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteAccountConfirmationDialog(viewModel: ProfileViewModel) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Type DELETE to confirm"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
            .setTitle("Delete Account?")
            .setMessage("Permanently delete your account and all data? This cannot be undone. Type DELETE below to confirm.")
            .setView(input)
            .setPositiveButton("Delete Account", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val deleteBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            deleteBtn.isEnabled = false

            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    deleteBtn.isEnabled = s?.toString()?.trim() == "DELETE"
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            deleteBtn.setOnClickListener {
                viewModel.deleteAccount {
                    authViewModel.logout()
                    findNavController().navigate(
                        R.id.login,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(0, true)
                            .build()
                    )
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
