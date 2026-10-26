package com.example.motionpulse.util

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.widget.TextView

object ChromeFontScaleHelper {
    const val MAX_CHROME_FONT_SCALE = 1.3f

    fun applyMaxFontScale(textView: TextView, maxScale: Float = MAX_CHROME_FONT_SCALE) {
        val currentScale = textView.resources.configuration.fontScale
        if (currentScale > maxScale) {
            val textSizePx = textView.textSize
            val baseSizePx = textSizePx / currentScale
            val cappedSizePx = baseSizePx * maxScale
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, cappedSizePx)
        }
    }

    fun getCappedContext(context: Context, maxScale: Float = MAX_CHROME_FONT_SCALE): Context {
        val configuration = context.resources.configuration
        if (configuration.fontScale > maxScale) {
            val cappedConfig = Configuration(configuration).apply {
                fontScale = maxScale
            }
            return context.createConfigurationContext(cappedConfig)
        }
        return context
    }
}
