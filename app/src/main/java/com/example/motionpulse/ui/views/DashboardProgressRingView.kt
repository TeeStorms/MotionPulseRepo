package com.example.motionpulse.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

class DashboardProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var completedCount: Int = 0
    private var totalCount: Int = 0

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2A1040") // Dark track
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FF2A85") // Pink accent
        strokeCap = Paint.Cap.ROUND
    }

    private val arcBounds = RectF()

    fun setProgress(completed: Int, total: Int) {
        this.completedCount = completed.coerceAtLeast(0)
        this.totalCount = total.coerceAtLeast(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val maxDiameter = min(width, height).toFloat()
        val strokeWidthPx = dpToPx(10f)

        trackPaint.strokeWidth = strokeWidthPx
        progressPaint.strokeWidth = strokeWidthPx

        val radius = (maxDiameter - strokeWidthPx) / 2f
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Draw background track ring
        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)

        // Draw progress arc
        val sweepAngle = if (totalCount > 0) {
            (completedCount.toFloat() / totalCount.toFloat()) * 360f
        } else {
            0f
        }
        if (sweepAngle > 0f) {
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint)
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
