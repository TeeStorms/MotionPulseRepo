package com.example.motionpulse.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.export.CsvExportManager
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.screens.profile.components.*
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.ProfileViewModel
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
}


