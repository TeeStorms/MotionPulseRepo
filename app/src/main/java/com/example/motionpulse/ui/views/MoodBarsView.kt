package com.example.motionpulse.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.example.motionpulse.data.local.entity.MoodLevel
import com.example.motionpulse.util.HapticHelper

class MoodBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val moodLevels = MoodLevel.values()
    private val heightsFractions = floatArrayOf(0.35f, 0.50f, 0.65f, 0.80f, 1.0f)
    private val labels = arrayOf("Drained", "Low", "Steady", "Good", "Energized")

    var selectedLevel: MoodLevel? = null
        private set

    var onMoodSelectedListener: ((MoodLevel) -> Unit)? = null

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A1A5C")
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#996B2F8F")
        style = Paint.Style.STROKE
    }

    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDA8D0")
        textAlign = Paint.Align.CENTER
    }

    private val barBounds = RectF()

    fun setMoodLevel(level: MoodLevel?, triggerCallback: Boolean = false) {
        this.selectedLevel = level
        invalidate()
        if (triggerCallback && level != null) {
            onMoodSelectedListener?.invoke(level)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val totalBars = moodLevels.size
            val barWidthWithSpacing = width / totalBars.toFloat()
            val index = (event.x / barWidthWithSpacing).toInt().coerceIn(0, totalBars - 1)
            val level = moodLevels[index]

            if (selectedLevel != level) {
                HapticHelper.performClick(this)
                setMoodLevel(level, triggerCallback = true)
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalBars = moodLevels.size
        val availableWidth = width.toFloat()
        val slotWidth = availableWidth / totalBars
        val barWidth = dpToPx(40f).coerceAtMost(slotWidth * 0.7f)
        val spacing = (slotWidth - barWidth) / 2f

        val labelPadding = dpToPx(28f)
        val availableHeight = height.toFloat() - labelPadding - dpToPx(16f)

        strokePaint.strokeWidth = dpToPx(1f)

        for (i in 0 until totalBars) {
            val level = moodLevels[i]
            val isSelected = selectedLevel == level
            val barHeight = availableHeight * heightsFractions[i]

            val left = i * slotWidth + spacing
            val right = left + barWidth
            val bottom = height.toFloat() - labelPadding
            val top = bottom - barHeight

            barBounds.set(left, top, right, bottom)
            val cornerRadius = barWidth / 2f

            if (isSelected) {
                val shader = LinearGradient(
                    left, top, right, bottom,
                    Color.parseColor("#FF2A85"), Color.parseColor("#4FC3F7"),
                    Shader.TileMode.CLAMP
                )
                selectedPaint.shader = shader
                canvas.drawRoundRect(barBounds, cornerRadius, cornerRadius, selectedPaint)

                // White Dot under bar
                val dotY = bottom + dpToPx(10f)
                canvas.drawCircle(left + barWidth / 2f, dotY, dpToPx(4f), dotPaint)
            } else {
                canvas.drawRoundRect(barBounds, cornerRadius, cornerRadius, dimPaint)
                canvas.drawRoundRect(barBounds, cornerRadius, cornerRadius, strokePaint)
            }

            // Draw Label text
            labelPaint.textSize = dpToPx(12f)
            labelPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#BDA8D0")
            canvas.drawText(
                labels[i],
                left + barWidth / 2f,
                height.toFloat() - dpToPx(4f),
                labelPaint
            )
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
