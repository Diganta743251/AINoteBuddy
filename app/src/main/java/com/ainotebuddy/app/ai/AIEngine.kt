package com.ainotebuddy.app.ai

import com.ainotebuddy.app.data.NoteEntity

/**
 * AI Engine interface for note analysis and processing
 */
interface AIEngine {
    // Keep a minimal, text-only API to avoid plugin collisions
    suspend fun analyze(text: String): AIResult
}

/**
 * Enhanced search query with AI improvements
 */
// Moved data classes to AISuggestion.kt to avoid duplicates