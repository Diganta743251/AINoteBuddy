package com.ainotebuddy.app.ai

import com.ainotebuddy.app.voice.VoiceQuickInsight

/**
 * Minimal voice analysis models to replace phantom types until full models are added.
 * Keep names stable so future, richer implementations can be drop-in replacements.
 */

data class BasicAudioAnalysis(
    val summary: String,
    val quickInsight: VoiceQuickInsight? = null,
    val topics: List<TopicResult> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val sentiment: SentimentResult? = null,
    val confidence: Float = 0.0f
)

/**
 * Lightweight stand-in for voice pattern analysis
 */
data class BasicVoicePattern(
    val pace: String,
    val clarity: String,
    val fillerWordCount: Int
)

/**
 * Minimal enhanced note content produced from voice input
 */
data class EnhancedNoteContent(
    val title: String,
    val content: String,
    val tags: List<String>,
    val category: String
)

/**
 * Minimal replacement for insights related to voice notes
 * (replaces ContentInsights/ContentInsight/InsightType temporarily)
 */
data class SimpleInsight(
    val title: String,
    val description: String,
    val confidence: Float,
    val actionable: Boolean
)

// NOTE: TopicResult is already defined in AIDataClasses.kt to avoid duplication.
// This file intentionally reuses that single source-of-truth definition.