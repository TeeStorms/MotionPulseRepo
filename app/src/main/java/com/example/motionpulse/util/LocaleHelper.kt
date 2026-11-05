package com.example.motionpulse.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    val SUPPORTED_LOCALES = setOf("en", "af", "tn", "zu", "ts")

    fun resolveSupportedLocaleTag(savedTag: String?, deviceTag: String = Locale.getDefault().language): String {
        if (!savedTag.isNullOrEmpty() && savedTag in SUPPORTED_LOCALES) {
            return savedTag
        }
        val cleanDeviceTag = deviceTag.lowercase().take(2)
        if (cleanDeviceTag in SUPPORTED_LOCALES) {
            return cleanDeviceTag
        }
        return "en"
    }

    private fun CharSequence?.isBlank(): Boolean = this == null || this.trim().isEmpty()

    fun wrap(context: Context, languageTag: String): Context {
        val locale = Locale(languageTag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        return context.createConfigurationContext(config)
    }

    fun getDisplayName(tag: String): String {
        return when (tag) {
            "af" -> "Afrikaans"
            "tn" -> "Setswana"
            "zu" -> "isiZulu"
            "ts" -> "Xitsonga"
            else -> "English"
        }
    }
}
