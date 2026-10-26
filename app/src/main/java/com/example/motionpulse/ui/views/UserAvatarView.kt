package com.example.motionpulse.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

class UserAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var displayName: String = ""
    private var showStatusDot: Boolean = false

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val statusDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF2A85") // Accent primary
    }
    private val statusBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C0620") // Card background border
    }

    fun setDisplayName(name: String) {
        this.displayName = name
        invalidate()
    }

    fun setShowStatusDot(show: Boolean) {
        this.showStatusDot = show
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intColor("#FF2A85"), intColor("#4FC3F7"),
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy)

        // Circle Background with Gradient
        canvas.drawCircle(cx, cy, radius, backgroundPaint)

        // Initials Text (~40% diameter)
        val initials = getInitials(displayName)
        textPaint.textSize = radius * 0.8f
        val fontMetrics = textPaint.fontMetrics
        val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(initials, cx, textY, textPaint)

        // Optional Status Dot
        if (showStatusDot) {
            val dotRadius = radius * 0.25f
            val dotCx = cx + radius * 0.7f
            val dotCy = cy + radius * 0.7f
            canvas.drawCircle(dotCx, dotCy, dotRadius + 3f, statusBorderPaint)
            canvas.drawCircle(dotCx, dotCy, dotRadius, statusDotPaint)
        }
    }

    private fun getInitials(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "?"
        val parts = trimmed.split("\\s+".toRegex())
        return if (parts.size >= 2) {
            "${parts[0].first()}${parts[1].first()}".uppercase()
        } else {
            trimmed.take(2).uppercase()
        }
    }

    private fun intColor(hex: String): Int = Color.parseColor(hex)
}
