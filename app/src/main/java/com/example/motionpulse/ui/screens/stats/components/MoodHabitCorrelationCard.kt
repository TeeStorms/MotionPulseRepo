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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.ui.theme.*
import com.example.motionpulse.ui.viewmodels.BestDayInfo
import java.time.format.TextStyle
import java.util.*

@Composable
fun MoodHabitCorrelationCard(
    dailyMoods: List<MoodLevel?>,
    dailyCompletions: List<Int>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderAlt),
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

            // Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                MoodLineChart(moods = dailyMoods)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days Labels and Habit Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                days.forEachIndexed { index, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HabitDotCluster(count = dailyCompletions[index])
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = AccentBlue, label = "Mood")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = Color(0xFF8C52FF), label = "Habit done")
            }
        }
    }
}

@Composable
fun MoodLineChart(moods: List<MoodLevel?>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / 6
        val points = mutableListOf<androidx.compose.ui.geometry.Offset>()

        moods.forEachIndexed { index, level ->
            if (level != null) {
                val x = index * stepX
                val moodScore = when (level) {
                    MoodLevel.DRAINED -> 1
                    MoodLevel.LOW -> 2
                    MoodLevel.STEADY -> 3
                    MoodLevel.GOOD -> 4
                    MoodLevel.ENERGIZED -> 5
                }
                // Map 1-5 to height (invert because y=0 is top)
                val y = height - ((moodScore - 1) * (height / 4))
                points.add(androidx.compose.ui.geometry.Offset(x, y))
            }
        }

        if (points.size > 1) {
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            drawPath(
                path = path,
                brush = HeaderGradient2Stop,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw points
            points.forEach { point ->
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
fun HabitDotCluster(count: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show up to 3 dots
        repeat(minOf(count, 3)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8C52FF))
            )
        }
        // If 0, show a placeholder ghost dot
        if (count == 0) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f))
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun InsightCard(insight: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderAlt),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            // Lightbulb Icon in a gradient circle
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                fontSize = 14.sp,
                lineHeight = 20.sp
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
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderAlt),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val dayName = remember(bestDay.date) {
                bestDay.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
            Text(
                text = "Best day",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            val moodName = bestDay.moodLevel.name.lowercase().replaceFirstChar { it.uppercase() }
            Text(
                text = "$dayName · $moodName · ${bestDay.habitsDone}/${bestDay.totalHabits} habits done",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
