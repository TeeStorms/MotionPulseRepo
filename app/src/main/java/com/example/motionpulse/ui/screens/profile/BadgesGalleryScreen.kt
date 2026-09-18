package com.example.motionpulse.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.BadgeType
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesGalleryScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.profileState.collectAsState()
    val unlockedBadgeTypes = state.badges.map { it.badgeType }.toSet()
    val allBadgeTypes = BadgeType.values()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Badge Gallery", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = "Total Badges: ${state.badgeCount}/${allBadgeTypes.size}",
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(allBadgeTypes) { badgeType ->
                    val isUnlocked = unlockedBadgeTypes.contains(badgeType)
                    BadgeItem(badgeType = badgeType, isUnlocked = isUnlocked)
                }
            }
        }
    }
}

@Composable
fun BadgeItem(badgeType: BadgeType, isUnlocked: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (isUnlocked) 1f else 0.4f)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) AccentPrimary.copy(alpha = 0.1f) else CardBackground)
                .border(2.dp, if (isUnlocked) AccentPrimary else CardBorderAlt, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (badgeType) {
                    BadgeType.STREAK_7_DAY -> "🔥"
                    BadgeType.STREAK_30_DAY -> "🏅"
                    BadgeType.FIRST_HABIT_CREATED -> "✨"
                },
                fontSize = 32.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = badgeType.name.replace("_", " ").lowercase().capitalize(),
            color = if (isUnlocked) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
