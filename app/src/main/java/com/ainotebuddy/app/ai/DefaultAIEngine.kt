package com.ainotebuddy.app.ai

import javax.inject.Inject

/**
 * Default implementation of the minimal text AI engine.
 * Keep simple to avoid FIR/KSP churn; wire heuristics behind an abstraction if needed.
 */
class DefaultAIEngine @Inject constructor(
    private val heuristics: VoiceHeuristicsGateway
) : AIEngine {
    override suspend fun analyze(text: String): AIResult {
        // Placeholder: delegate to heuristics or adapters; keep deterministic.
        return AIResult(summary = "ok", confidence = 0.9)
    }
}