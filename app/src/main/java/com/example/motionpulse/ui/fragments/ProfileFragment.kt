package com.example.motionpulse.ui.fragments

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.viewmodels.ProfileViewModel
import com.example.motionpulse.util.TimeAgoFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        binding.switchShareMilestones.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleShareMilestones(isChecked)
        }

        binding.switchShowHabitNames.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleShowHabitNames(isChecked)
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
            Toast.makeText(requireContext(), "Change Profile Name (R18)", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            Toast.makeText(requireContext(), "Change Password (R18)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLanguage.setOnClickListener {
            Toast.makeText(requireContext(), "Language selection (R18)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLinkedAccounts.setOnClickListener {
            Toast.makeText(requireContext(), "Linked Accounts (R18)", Toast.LENGTH_SHORT).show()
        }

        binding.btnExportCsv.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val completions = viewModel.getAllCompletions()
                val exportManager = com.example.motionpulse.data.export.CsvExportManager(requireContext())
                exportManager.exportCompletions(completions)
                Toast.makeText(requireContext(), "CSV export started", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Delete Account?")
                .setMessage("Permanently delete your account and all data? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
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
                }
                .setNegativeButton("Cancel", null)
                .show()
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
                            binding.avatarView.setDisplayName(profile.displayName)
                            binding.tvProfileName.text = profile.displayName

                            val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
                            val memberSince = profile.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter)
                            binding.tvMemberSince.text = "Member since $memberSince"

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
