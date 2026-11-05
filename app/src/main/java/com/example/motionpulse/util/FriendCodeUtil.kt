package com.example.motionpulse.util

import android.util.Log
import com.example.motionpulse.BuildConfig
import java.util.Locale

sealed class FriendCodeResult {
    data class Valid(val canonicalCode: String) : FriendCodeResult()
    data class InvalidFormat(val message: String) : FriendCodeResult()
}

object FriendCodeUtil {

    private const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    const val INVALID_FORMAT_MESSAGE = "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX."

    fun normalize(input: String?): FriendCodeResult {
        if (input.isNullOrBlank()) {
            return FriendCodeResult.InvalidFormat(INVALID_FORMAT_MESSAGE)
        }

        // 1. Remove all whitespace, zero-width characters (\u200B,\u200C,\u200D,\uFEFF), non-breaking spaces (\u00A0), soft hyphens (\u00AD)
        val cleanedChars = input.replace(Regex("[\\s\\u200B\\u200C\\u200D\\uFEFF\\u00A0\\u00AD]"), "")

        // 2. Uppercase using Locale.ROOT
        val upper = cleanedChars.uppercase(Locale.ROOT)

        // 3. Remove hyphens
        var clean = upper.replace("-", "")

        // 4. Remove ONE leading "MP" prefix only
        if (clean.startsWith("MP")) {
            clean = clean.substring(2)
        }

        if (clean.isEmpty()) {
            return FriendCodeResult.InvalidFormat(INVALID_FORMAT_MESSAGE)
        }

        // 5. Require every character to belong to allowed ALPHABET
        for (ch in clean) {
            if (ch !in ALPHABET) {
                return FriendCodeResult.InvalidFormat(INVALID_FORMAT_MESSAGE)
            }
        }

        // 6. Strict length match (8 canonical or 6 legacy), no padding or length guessing
        return when (clean.length) {
            8 -> FriendCodeResult.Valid("MP-${clean.substring(0, 4)}-${clean.substring(4, 8)}")
            6 -> FriendCodeResult.Valid("MP-$clean")
            else -> FriendCodeResult.InvalidFormat(INVALID_FORMAT_MESSAGE)
        }
    }

    fun extractCodeFromText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val norm = normalize(input)
        if (norm is FriendCodeResult.Valid) {
            return norm.canonicalCode
        }

        val upper = input.uppercase(Locale.ROOT)
        val regex = Regex("(?i)(?:MP-?)?([23456789ABCDEFGHJKMNPQRSTUVWXYZ]{4})-?([23456789ABCDEFGHJKMNPQRSTUVWXYZ]{4})")
        val match = regex.find(upper)
        if (match != null) {
            val p1 = match.groupValues[1]
            val p2 = match.groupValues[2]
            return "MP-$p1-$p2"
        }
        return input.trim()
    }

    fun formatAsYouType(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val norm = normalize(input)
        if (norm is FriendCodeResult.Valid) {
            return norm.canonicalCode
        }

        val upper = input.uppercase(Locale.ROOT)
        var clean = upper.replace(Regex("[\\s\\u200B\\u200C\\u200D\\uFEFF\\u00A0\\u00AD-]"), "")
        if (clean.startsWith("MP")) {
            clean = clean.substring(2)
        }

        val filtered = clean.filter { it in ALPHABET }.take(8)
        if (filtered.isEmpty()) return "MP-"

        val sb = StringBuilder("MP-")
        if (filtered.length > 4) {
            sb.append(filtered.substring(0, 4)).append("-").append(filtered.substring(4))
        } else {
            sb.append(filtered)
        }
        return sb.toString()
    }

    fun logDebugFriendFlow(tag: String, text: String) {
        if (BuildConfig.DEBUG) {
            val codepoints = text.codePoints().toArray().contentToString()
            val norm = normalize(text)
            Log.d("FriendFlow", "$tag raw=[$text] codepoints=$codepoints normalized=$norm")
        }
    }
}
