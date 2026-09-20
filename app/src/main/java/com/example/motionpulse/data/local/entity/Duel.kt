package com.example.motionpulse.data.local.entity

data class Duel(
    val id: String = "",
    val habitType: String = "",
    val startDate: String = "", // ISO date
    val durationDays: Int = 7,
    val participants: List<String> = emptyList(),
    val scores: Map<String, Int> = emptyMap(), // userId -> completionCount
    val winnerId: String? = null,
    val status: String = "ACTIVE" // ACTIVE, COMPLETED
)
