package com.ainotebuddy.app.ai

/**
 * Optional abstraction to hide heuristic implementations from engines.
 * This can be implemented using existing VoiceHeuristics utilities.
 */
import javax.inject.Inject

interface VoiceHeuristicsGateway {
    fun quickSummary(text: String): String
}

class DefaultVoiceHeuristicsGateway @Inject constructor() : VoiceHeuristicsGateway {
    override fun quickSummary(text: String): String {
        // Very lightweight placeholder using existing heuristics if needed
        val topics = VoiceHeuristics.performQuickTopicDetection(text)
        return if (topics.isNotEmpty()) "topics: " + topics.joinToString { it.topic } else "no-topics"
    }
}