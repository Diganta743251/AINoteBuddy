package com.ainotebuddy.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceHeuristicsTest {

    @Test
    fun `performQuickTopicDetection picks repeated long words`() {
        val input = """
            Today we discussed the project timeline and project budget.
            The project requires additional planning and development resources.
        """.trimIndent()

        val topics = VoiceHeuristics.performQuickTopicDetection(input)
        assertTrue(topics.isNotEmpty())
        // Expect "project" to be among the detected topics
        assertTrue(topics.any { it.topic == "project" })
        // Confidence should be between 0 and 1
        assertTrue(topics.all { it.confidence in 0f..1f })
    }

    @Test
    fun `performQuickActionItemDetection flags action lines`() {
        val input = """
            We should prepare the slides.
            Remember to email the client tomorrow.
            It was a good meeting.
        """.trimIndent()

        val actions = VoiceHeuristics.performQuickActionItemDetection(input)
        assertEquals(2, actions.size)
        assertTrue(actions.all { it.priority == ActionPriority.MEDIUM })
    }


}