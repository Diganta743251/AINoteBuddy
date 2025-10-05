package com.ainotebuddy.app.voice

import androidx.compose.runtime.Immutable

// UI-facing voice state for VoiceNoteScreen
@Immutable
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Recording : VoiceState()
    object Processing : VoiceState()
    data class Error(val message: String) : VoiceState()
}

// Quick analysis chip model used by VoiceNoteScreen
@Immutable
data class VoiceQuickInsight(
    val type: VoiceQuickInsightType,
    val content: String,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

enum class VoiceQuickInsightType {
    TOPIC_DETECTED,
    ACTION_ITEM_FOUND,
    IMPORTANT_POINT,
    SENTIMENT_CHANGE,
    ENTITY_MENTIONED
}