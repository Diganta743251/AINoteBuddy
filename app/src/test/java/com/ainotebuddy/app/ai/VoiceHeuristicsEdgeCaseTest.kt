package com.ainotebuddy.app.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceHeuristicsEdgeCaseTest {

    @Test
    fun `empty and punctuation-only input should return empty results`() {
        assertTrue(VoiceHeuristics.performQuickTopicDetection("   !!!   ").isEmpty())
        assertTrue(VoiceHeuristics.performQuickActionItemDetection("   ...   ").isEmpty())
    }
}