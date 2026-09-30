package com.example.motionpulse.ui.screens.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.export.CsvExportManager
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.screens.profile.components.*
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.ProfileViewModel
import com.example.motionpulse.util.AppConstants
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onSeeAllBadges: () -> Unit,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?
) {
    val state by viewModel.profileState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val csvExportManager = remember { CsvExportManager(context) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showResetCodeDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            MotionPulseBottomNav(
                currentRoute = currentRoute,
                onNavigate = onNavigateToNav
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                displayName = state.userProfile?.displayName ?: "User",
                createdAt = state.userProfile?.createdAt
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(32.dp))

                // Stats Cards
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .border(1.dp, CardBorderAlt, RoundedCornerShape(16.dp))
                            .background(CardBackground, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔥 ${state.currentStreak}", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Day Streak", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .border(1.dp, CardBorderAlt, RoundedCornerShape(16.dp))
                            .background(CardBackground, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🏅 ${state.badgeCount}", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Badges", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                AchievementSection(
                    badges = state.badges,
                    onSeeAll = onSeeAllBadges
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Friend Code Card
                val friendCode = state.userProfile?.friendCode ?: "---"
                val clipboardManager = LocalClipboardManager.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "YOUR FRIEND CODE", color = AccentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = friendCode, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { showResetCodeDialog = true }) {
                                Text(text = "Reset", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (friendCode.isNotBlank() && friendCode != "---") {
                                        clipboardManager.setText(AnnotatedString(friendCode))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", color = TextPrimary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (friendCode.isNotBlank() && friendCode != "---") {
                                        val text = "Join me on Motion.Pulse! Add me with my friend code: $friendCode\nGet the app: ${AppConstants.APP_INVITE_URL}"
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, text)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Friend Code")
                                        context.startActivity(shareIntent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Settings",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val profile = state.userProfile
                SettingItem(label = "Change Profile", onClick = { showEditProfileDialog = true })
                SettingItem(label = "Change Password", onClick = { showChangePasswordDialog = true })
                SettingItem(label = "Language: ${profile?.language ?: "English"}", onClick = { showLanguageDialog = true })
                SettingItem(
                    label = "Reminder: ${if (profile?.remindersEnabled == true) "On" else "Off"}",
                    onClick = { viewModel.toggleReminders(profile?.remindersEnabled != true, context) }
                )
                SettingItem(
                    label = "Share my milestones with friends: ${if (profile?.shareMilestonesWithFriends != false) "On" else "Off"}",
                    onClick = { viewModel.toggleShareMilestones(profile?.shareMilestonesWithFriends == false) }
                )
                SettingItem(
                    label = "Show habit names in posts: ${if (profile?.showHabitNamesInPosts == true) "On" else "Off"}",
                    onClick = { viewModel.toggleShowHabitNames(profile?.showHabitNamesInPosts != true) }
                )
                SettingItem(label = "Manage Linked Accounts", onClick = { /* TODO: Provider Dialog */ })
                
                SettingItem(
                    label = "Export My Data (CSV)", 
                    onClick = { 
                        scope.launch {
                            val completions = viewModel.getAllCompletions()
                            csvExportManager.exportCompletions(completions)
                        }
                    }
                )
                
                SettingItem(
                    label = "Delete Account", 
                    onClick = { showDeleteAccountDialog = true },
                    labelColor = Color.Red
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, ProfileLogoutOutline, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileLogoutBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Log Out", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Motion.Pulse v1.0.0", // Hardcoded for now, normally read from BuildConfig
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = state.userProfile?.displayName ?: "",
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { 
                viewModel.updateName(it)
                showEditProfileDialog = false
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, new ->
                viewModel.reauthenticate(current)
                viewModel.updatePassword(new)
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = state.userProfile?.language ?: "English",
            onDismiss = { showLanguageDialog = false },
            onSelect = { viewModel.changeLanguage(it) }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = {
                viewModel.deleteAccount {
                    showDeleteAccountDialog = false
                    onLogout()
                }
            }
        )
    }

    if (showResetCodeDialog) {
        AlertDialog(
            onDismissRequest = { showResetCodeDialog = false },
            title = { Text("Reset Friend Code?") },
            text = { Text("Your existing code will no longer work. Friends will need your new code to add you.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetFriendCode()
                        showResetCodeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetCodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


