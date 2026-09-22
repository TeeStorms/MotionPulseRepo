package com.example.motionpulse.ui.screens.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.domain.stats.WeeklyCorrelationData
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.BestDayInfo
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun MoodHabitsGraphCard(
    data: WeeklyCorrelationData,
    modifier: Modifier = Modifier
) {
    val contentDesc = remember(data) {
        val range = if (data.days.isNotEmpty()) {
            "${data.days.first().date} – ${data.days.last().date}"
        } else ""
        
        val details = data.days.joinToString(". ") { day ->
            val mood = day.moodLevel?.name?.lowercase() ?: "not logged"
            val done = day.habitCompletions.count { it }
            val total = day.habitCompletions.size
            "${day.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} mood $mood, $done of $total habits completed"
        }
        "Mood and habit completion for the week of $range: $details"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .semantics { contentDescription = contentDesc },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Mood & habits, side by side",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mon – Sun",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Layer 1: Mood Line (Top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                MoodLineLayer(data = data)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shared Axis: Day Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                data.days.forEach { day ->
                    Text(
                        text = day.date.dayOfWeek.name.take(1),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer 2: Habit Dot Clusters (Bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                data.days.forEach { day ->
                    HabitDotCluster(
                        completions = day.habitCompletions,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GraphLegendItem(color = AccentBlue, label = "Mood")
                Spacer(modifier = Modifier.width(16.dp))
                GraphLegendItem(color = AccentPrimary, label = "Habit done")
            }
        }
    }
}

@Composable
private fun MoodLineLayer(data: WeeklyCorrelationData) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val colWidth = size.width / 7f
        val height = size.height
        
        fun getMoodY(level: MoodLevel): Float {
            val score = when (level) {
                MoodLevel.DRAINED -> 1
                MoodLevel.LOW -> 2
                MoodLevel.STEADY -> 3
                MoodLevel.GOOD -> 4
                MoodLevel.ENERGIZED -> 5
            }
            // 5 levels, map to height. 5 (Energized) at top, 1 (Drained) at bottom.
            return height - ((score - 1) * (height / 4f))
        }

        val points = data.days.mapIndexed { index, day ->
            day.moodLevel?.let { Offset((index + 0.5f) * colWidth, getMoodY(it)) }
        }

        // Draw connecting line segments ONLY between consecutive valid points (Genuine Gaps)
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (p1 != null && p2 != null) {
                drawLine(
                    brush = HeaderGradient3Stop,
                    start = p1,
                    end = p2,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw mood level points
        points.forEach { point ->
            if (point != null) {
                drawCircle(
                    color = AccentBlue,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
private fun HabitDotCluster(completions: List<Boolean>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        completions.forEach { isCompleted ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) AccentPrimary else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isCompleted) Color.Transparent else CardBorderAlt.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
        // If zero habits scheduled, this column is empty, showing no dots.
    }
}

@Composable
private fun GraphLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InsightCard(insight: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryButtonGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💡", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = insight,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BestDayCard(bestDay: BestDayInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderAlt.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            val label = remember(bestDay.date) {
                if (bestDay.date == LocalDate.now()) "Today"
                else bestDay.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
            Text(
                text = "Best day",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            val moodName = bestDay.moodLevel.name.lowercase().replaceFirstChar { it.uppercase() }
            Text(
                text = "$label · $moodName · ${bestDay.habitsDone}/${bestDay.totalHabits} habits done",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun MoodHabitsGraphCardPreview() {
    MotionPulseTheme {
        Box(modifier = Modifier.padding(16.dp).background(BackgroundDark)) {
            val today = LocalDate.now()
            MoodHabitsGraphCard(
                data = WeeklyCorrelationData(
                    days = listOf(
                        com.example.motionpulse.domain.stats.DayData(today.minusDays(6), MoodLevel.STEADY, listOf(true, false)),
                        com.example.motionpulse.domain.stats.DayData(today.minusDays(5), MoodLevel.GOOD, listOf(true, true)),
                        com.example.motionpulse.domain.stats.DayData(today.minusDays(4), null, listOf(false, false)),
                        com.example.motionpulse.domain.stats.DayData(today.minusDays(3), MoodLevel.LOW, listOf(true)),
                        com.example.motionpulse.domain.stats.DayData(today.minusDays(2), MoodLevel.GOOD, listOf(true, true, true)),
                        com.example.motionpulse.domain.stats.DayData(today.minusDays(1), MoodLevel.ENERGIZED, listOf(true, true)),
                        com.example.motionpulse.domain.stats.DayData(today, MoodLevel.STEADY, listOf(false))
                    )
                )
            )
        }
    }
}
