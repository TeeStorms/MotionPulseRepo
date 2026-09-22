package com.example.motionpulse.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.ui.components.MotionPulseBottomNav
import com.example.motionpulse.ui.components.MotionPulseTextField
import com.example.motionpulse.ui.screens.dashboard.components.HabitCard
import com.example.motionpulse.ui.screens.dashboard.components.MotionPulseHeader
import com.example.motionpulse.ui.screens.habits.components.AddHabitOverlay
import com.example.motionpulse.ui.screens.habits.components.CategoryTag
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.HabitsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToNav: (String) -> Unit,
    currentRoute: String?
) {
    val allHabitsWithStatus by viewModel.allHabits.collectAsState()
    val archivedHabits by viewModel.archivedHabits.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    var showAddOverlay by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<HabitEntity?>(null) }
    var habitToArchive by remember { mutableStateOf<HabitEntity?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            MotionPulseBottomNav(
                currentRoute = currentRoute,
                onNavigate = onNavigateToNav
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HeaderGradient2Stop)
                    .clickable { 
                        habitToEdit = null
                        showAddOverlay = true 
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshHabits() },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                MotionPulseHeader(
                    title = "My Habits",
                    gradient = HeaderGradient2Stop
                )

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Search Bar
                    MotionPulseTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = "Search habits...",
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryTag(
                            label = "All",
                            isActive = selectedCategory == null,
                            onClick = { viewModel.setCategoryFilter(null) }
                        )
                        CategoryTag(
                            label = "Mind & Focus",
                            isActive = selectedCategory == "Mind & Focus",
                            onClick = { viewModel.setCategoryFilter("Mind & Focus") }
                        )
                        CategoryTag(
                            label = "Fitness and Health",
                            isActive = selectedCategory == "Fitness and Health",
                            onClick = { viewModel.setCategoryFilter("Fitness and Health") }
                        )
                        CategoryTag(
                            label = "Daily Routine",
                            isActive = selectedCategory == "Daily Routine",
                            onClick = { viewModel.setCategoryFilter("Daily Routine") }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = allHabitsWithStatus,
                            key = { it.habit.id }
                        ) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    when (it) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            habitToEdit = item.habit
                                            showAddOverlay = true
                                            false
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Color.Blue.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                    val icon = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                        else -> Icons.Default.Add
                                    }
                                    val alignment = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        else -> Alignment.Center
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment
                                    ) {
                                        Icon(icon, contentDescription = null, tint = Color.White)
                                    }
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        IconButton(onClick = { viewModel.reorderHabit(item.habit, moveUp = true) }) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = TextSecondary)
                                        }
                                        IconButton(onClick = { viewModel.reorderHabit(item.habit, moveUp = false) }) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = TextSecondary)
                                        }
                                    }
                                    
                                    Box(modifier = Modifier.weight(1f)) {
                                    HabitCard(
                                        habit = item.habit,
                                        isCompletedToday = item.isCompletedToday,
                                        isSyncing = item.isSyncing,
                                        isOverdue = item.isOverdue,
                                        onToggleComplete = { viewModel.toggleHabitCompletion(item.habit) },
                                        onRemoveProgress = { viewModel.removeHabitProgress(item.habit) },
                                        onClick = { onNavigateToDetail(item.habit.id) }
                                    )
                                    }
                                }
                            }
                        }
                        
                        if (archivedHabits.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                TextButton(
                                    onClick = { showArchived = !showArchived },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (showArchived) "Hide Archived" else "Show Archived (${archivedHabits.size})",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (showArchived) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }

                            if (showArchived) {
                                items(archivedHabits) { habit ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(1.dp, CardBorderAlt, RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = habit.title, color = TextSecondary, fontSize = 16.sp)
                                            IconButton(onClick = { viewModel.unarchiveHabit(habit) }) {
                                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = AccentPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        if (habitToArchive != null) {
            AlertDialog(
                onDismissRequest = { habitToArchive = null },
                containerColor = CardBackground,
                title = { Text("Archive Habit?", color = TextPrimary) },
                text = { Text("This will hide the habit from your active list but preserve its history.", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        habitToArchive?.let { viewModel.archiveHabit(it) }
                        habitToArchive = null
                    }) {
                        Text("Archive", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { habitToArchive = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showAddOverlay) {
            AddHabitOverlay(
                habitToEdit = habitToEdit,
                onDismiss = { 
                    showAddOverlay = false
                    habitToEdit = null
                },
                viewModel = viewModel
            )
        }
    }
}
