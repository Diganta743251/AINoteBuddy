package com.ainotebuddy.app.ai

// Minimal placeholders to satisfy compile where richer models were referenced

// data class TopicResult moved to AIDataClasses.kt

enum class VoiceUrgency { LOW, MEDIUM, HIGH }

data class VoiceContext(
    val topic: String? = null,
    val participants: List<String> = emptyList(),
    val location: String? = null,
    val urgency: VoiceUrgency = VoiceUrgency.MEDIUM,
    val summary: String = "",
    val details: Map<String, String> = emptyMap()
)