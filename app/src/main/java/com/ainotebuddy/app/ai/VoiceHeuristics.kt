package com.ainotebuddy.app.ai

import androidx.annotation.VisibleForTesting

/**
 * Centralized, minimal heuristics for voice/topic/action detection used by both
 * production code and tests to avoid drift.
 */
object VoiceHeuristics {

    /**
     * Quick, heuristic topic detection: picks top repeated words (> 4 chars)
     * and scales confidence by frequency.
     */
    @JvmStatic
    @VisibleForTesting
    internal fun performQuickTopicDetection(text: String): List<TopicResult> {
        val cleaned = text.replace(Regex("[^A-Za-z0-9\\n\\r\\s]"), " ").trim()
        if (cleaned.isBlank()) return emptyList()
        return cleaned
            .split("\\s+".toRegex())
            .filter { it.length > 4 }
            .groupBy { it.lowercase() }
            .toList()
            .sortedByDescending { it.second.size }
            .take(3)
            .map { TopicResult(it.first, (it.second.size.toFloat() / 10f).coerceIn(0f, 1f)) }
    }

    /**
     * Quick action item detection: flag lines containing simple action phrases.
     */
    @JvmStatic
    @VisibleForTesting
    internal fun performQuickActionItemDetection(text: String): List<ActionItem> {
        val cleaned = text.replace(Regex("[^A-Za-z0-9\\n\\r\\s]"), " ").trim()
        if (cleaned.isBlank()) return emptyList()
        val actionWords = listOf("todo", "need to", "should", "must", "remember to")
        return cleaned.split("\n").filter { line ->
            actionWords.any { actionWord -> line.contains(actionWord, ignoreCase = true) }
        }.map { ActionItem(it, ActionPriority.MEDIUM) }
    }
}