package com.example.motionpulse.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MoodPillView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val dotView: View
    private val labelView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padHorizontal = dpToPx(12)
        val padVertical = dpToPx(6)
        setPadding(padHorizontal, padVertical, padHorizontal, padVertical)

        // Pill background (#361B6A, fully rounded)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#361B6A"))
            cornerRadius = dpToPx(17).toFloat()
        }

        // 10dp dot
        dotView = View(context).apply {
            val dotSize = dpToPx(10)
            layoutParams = LayoutParams(dotSize, dotSize).apply {
                marginEnd = dpToPx(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EE44CB")) // Default Energized
            }
        }
        addView(dotView)

        // White label
        labelView = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        addView(labelView)
    }

    fun setMood(label: String, colorHex: String) {
        labelView.text = label
        (dotView.background as? GradientDrawable)?.setColor(Color.parseColor(colorHex))
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
