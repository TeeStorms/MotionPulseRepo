package com.example.motionpulse.domain.models

object FriendEligibility {

    fun normalizeFriendCode(raw: String): String {
        val cleaned = raw.uppercase().replace(Regex("[\\s-]"), "")
        val core = if (cleaned.startsWith("MP")) cleaned.removePrefix("MP") else cleaned
        val padded = core.padEnd(8, '0')
        val part1 = padded.take(4)
        val part2 = padded.drop(4).take(4)
        return "MP-$part1-$part2"
    }

    fun generateFriendRequestDocId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    data class EligibilityContext(
        val myUid: String,
        val targetUid: String,
        val isSelf: Boolean,
        val isAlreadyFriend: Boolean,
        val hasPendingOutgoing: Boolean,
        val hasPendingIncoming: Boolean,
        val isBlocked: Boolean,
        val declinedTimestamp: Long?,
        val pendingSentCount: Int,
        val friendCount: Int
    )

    sealed class EligibilityResult {
        object Eligible : EligibilityResult()
        object ReciprocalAccept : EligibilityResult()
        data class Ineligible(val reason: String) : EligibilityResult()
    }

    fun checkEligibility(ctx: EligibilityContext, now: Long = System.currentTimeMillis()): EligibilityResult {
        if (ctx.isSelf || ctx.myUid == ctx.targetUid) {
            return EligibilityResult.Ineligible("You cannot add yourself.")
        }
        if (ctx.isAlreadyFriend) {
            return EligibilityResult.Ineligible("You are already friends.")
        }
        if (ctx.isBlocked) {
            return EligibilityResult.Ineligible("Couldn't send the request.")
        }
        if (ctx.hasPendingIncoming) {
            return EligibilityResult.ReciprocalAccept
        }
        if (ctx.hasPendingOutgoing) {
            return EligibilityResult.Ineligible("Friend request already sent.")
        }
        if (ctx.declinedTimestamp != null) {
            val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
            if (now - ctx.declinedTimestamp < sevenDaysMs) {
                return EligibilityResult.Ineligible("You must wait 7 days after a declined request.")
            }
        }
        if (ctx.pendingSentCount >= 20) {
            return EligibilityResult.Ineligible("You have too many pending sent requests (max 20).")
        }
        if (ctx.friendCount >= 100) {
            return EligibilityResult.Ineligible("You have reached the maximum limit of 100 friends.")
        }
        return EligibilityResult.Eligible
    }
}
