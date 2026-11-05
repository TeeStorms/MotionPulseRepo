package com.example.motionpulse.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.motionpulse.R
import com.example.motionpulse.domain.stats.WeeklyCorrelationData

class MoodHabitsGraphCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var correlationData: WeeklyCorrelationData = WeeklyCorrelationData()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.mood_line)
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
    }

    private val hollowPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.mood_line)
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
    }

    private val pointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.card_background)
        style = Paint.Style.FILL
    }

    private val moodLegendDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.mood_line)
        style = Paint.Style.FILL
    }

    private val habitDonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.habit_done)
        style = Paint.Style.FILL
    }

    private val habitMissedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.habit_missed)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = dpToPx(12f)
    }

    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = dpToPx(12f)
    }

    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = dpToPx(14f)
    }

    private val path = Path()

    private val dayLetters = arrayOf("M", "T", "W", "T", "F", "S", "S")

    fun setData(data: WeeklyCorrelationData) {
        this.correlationData = data

        val sb = StringBuilder("Weekly Correlation Chart: ")
        if (data.days.none { it.moodLevel != null }) {
            sb.append("No mood logged this week.")
        } else {
            val dayNames = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            data.days.forEachIndexed { i, day ->
                val dayName = dayNames.getOrElse(i) { "Day ${i + 1}" }
                val moodStr = day.moodLevel?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "none"
                val totalHabits = day.habitCompletions.size
                val done = day.habitCompletions.count { it }
                sb.append("$dayName: mood $moodStr, $done of $totalHabits habits done. ")
            }
        }
        contentDescription = sb.toString()

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw Legend Row at top
        val legendY = dpToPx(14f)
        canvas.drawCircle(dpToPx(16f), legendY, dpToPx(4f), moodLegendDotPaint)
        canvas.drawText("Mood", dpToPx(26f), legendY + dpToPx(4f), legendTextPaint)

        canvas.drawCircle(dpToPx(80f), legendY, dpToPx(4f), habitDonePaint)
        canvas.drawText("Habit done", dpToPx(90f), legendY + dpToPx(4f), legendTextPaint)

        val days = correlationData.days
        val hasAnyMood = days.any { it.moodLevel != null }

        val topPadding = dpToPx(36f)
        val chartHeight = dpToPx(90f)

        if (!hasAnyMood) {
            // Draw centered "No mood logged this week" empty state inside plot area
            canvas.drawText("No mood logged this week", width / 2f, topPadding + chartHeight / 2f, emptyTextPaint)
            return
        }

        val totalDays = days.size.coerceAtLeast(7)
        val availableWidth = width.toFloat() - dpToPx(32f)
        val colWidth = availableWidth / totalDays
        val startX = dpToPx(16f)

        // Draw day labels row (M, T, W, T, F, S, S)
        val labelY = topPadding + chartHeight + dpToPx(16f)
        for (i in days.indices) {
            val cx = startX + i * colWidth + colWidth / 2f
            val letter = dayLetters.getOrNull(i) ?: "M"
            canvas.drawText(letter, cx, labelY, textPaint)
        }

        // Draw habit dots row 6dp below day labels
        val dotsStartY = labelY + dpToPx(12f)
        for (i in days.indices) {
            val day = days[i]
            val cx = startX + i * colWidth + colWidth / 2f
            val habitCompletions = day.habitCompletions

            if (habitCompletions.isNotEmpty()) {
                val dotRadius = dpToPx(4f) // 8dp diameter
                val spacing = dpToPx(10f)

                for (j in habitCompletions.indices) {
                    val cy = dotsStartY + j * spacing
                    val paint = if (habitCompletions[j]) habitDonePaint else habitMissedPaint
                    canvas.drawCircle(cx, cy, dotRadius, paint)
                }
            }
        }

        // Draw mood line connecting points with real gaps
        path.reset()
        var lastValidX = -1f
        val pointsToDraw = mutableListOf<Pair<Float, Float>>()

        for (i in days.indices) {
            val day = days[i]
            val cx = startX + i * colWidth + colWidth / 2f
            val mood = day.moodLevel

            if (mood != null) {
                val score = when (mood) {
                    com.example.motionpulse.data.local.entity.MoodLevel.DRAINED -> 1f
                    com.example.motionpulse.data.local.entity.MoodLevel.LOW -> 2f
                    com.example.motionpulse.data.local.entity.MoodLevel.STEADY -> 3f
                    com.example.motionpulse.data.local.entity.MoodLevel.GOOD -> 4f
                    com.example.motionpulse.data.local.entity.MoodLevel.ENERGIZED -> 5f
                }
                val cy = topPadding + chartHeight * (1f - (score - 1f) / 4f)
                pointsToDraw.add(Pair(cx, cy))

                if (lastValidX == -1f) {
                    path.moveTo(cx, cy)
                } else {
                    path.lineTo(cx, cy)
                }
                lastValidX = cx
            } else {
                lastValidX = -1f
            }
        }

        if (pointsToDraw.isNotEmpty()) {
            canvas.drawPath(path, linePaint)
            for (pt in pointsToDraw) {
                canvas.drawCircle(pt.first, pt.second, dpToPx(5f), pointFillPaint)
                canvas.drawCircle(pt.first, pt.second, dpToPx(5f), hollowPointPaint)
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
