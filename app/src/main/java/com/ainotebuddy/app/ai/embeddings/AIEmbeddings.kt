package com.ainotebuddy.app.ai.embeddings

import kotlin.math.sqrt

/**
 * Lightweight on-device embeddings interface.
 * Replace SimpleAIEmbeddings with a real TFLite/ONNX model when ready.
 */
interface AIEmbeddings {
    suspend fun embed(text: String): FloatArray // 256–384 dims expected
}

/**
 * Simple, deterministic embedding using hashing + bag-of-words.
 * This is only a placeholder to allow wiring and testing without models.
 */
class SimpleAIEmbeddings(
    private val dimensions: Int = 256
) : AIEmbeddings {
    override suspend fun embed(text: String): FloatArray {
        val vec = FloatArray(dimensions)
        val tokens = text.lowercase()
            .replace("[^a-z0-9\n ]".toRegex(), " ")
            .split(" ", "\n")
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return vec

        // Hash trick
        tokens.forEach { token ->
            val idx = (token.hashCode() and Int.MAX_VALUE) % dimensions
            vec[idx] += 1f
        }

        // L2 normalize
        var norm = 0f
        for (v in vec) norm += v * v
        norm = sqrt(norm)
        if (norm > 0f) {
            for (i in vec.indices) vec[i] /= norm
        }
        return vec
    }
}