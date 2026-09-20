package com.example.motionpulse.data.local.entity

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "GROUP",
    val durationDays: Int = 14,
    val startDate: String = "", // ISO date
    val participantIds: List<String> = emptyList(),
    val progress: Map<String, Float> = emptyMap() // userId -> progress (0.0 to 1.0)
)
