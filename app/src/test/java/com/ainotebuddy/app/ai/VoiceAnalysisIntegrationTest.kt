package com.ainotebuddy.app.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAnalysisIntegrationTest {

    @Test
    fun `analyzeAudioContent returns reasonable confidence and structures`() = runBlocking {
        val engine = AIAnalysisEngine()
        val transcription = """
            Meeting about the project timeline next week. We should prepare slides and send an email update.
            The budget figures look good but we need to confirm with finance.
        """.trimIndent()

        val result = engine.analyzeAudioContent(transcription, recording = null)

        // Confidence should be in 0..1 and not trivial (given content length, punctuation, etc.)
        assertTrue(result.confidence in 0f..1f)
        assertTrue(result.confidence >= 0.5f)

        // Structures should be present
        assertTrue(result.summary.isNotBlank())
        assertTrue(result.topics.size <= 3)
        // If there are action keywords, we expect at least one action item
        assertTrue(result.actionItems.size >= 1)
        assertTrue(result.sentiment != null)
    }
}