package com.ainotebuddy.app.ai.embeddings

import kotlin.math.sqrt

/** Owner type examples: "note", "task", etc. */
data class ScoredId(val id: Long, val score: Float)

interface AISemanticSearch {
    suspend fun index(ownerType: String, ownerId: Long, vector: FloatArray)
    suspend fun remove(ownerId: Long)
    suspend fun query(vector: FloatArray, topK: Int = 10): List<ScoredId>
    suspend fun size(): Int
}

/**
 * In-memory cosine similarity index. Suitable for <= 5k items.
 * Swap with a disk-backed implementation later without touching callers.
 */
class InMemorySemanticSearch : AISemanticSearch {
    private val store = LinkedHashMap<Long, FloatArray>()

    override suspend fun index(ownerType: String, ownerId: Long, vector: FloatArray) {
        store[ownerId] = vector
    }

    override suspend fun remove(ownerId: Long) {
        store.remove(ownerId)
    }

    override suspend fun query(vector: FloatArray, topK: Int): List<ScoredId> {
        if (store.isEmpty()) return emptyList()
        val scores = ArrayList<ScoredId>(store.size)
        store.forEach { (id, vec) ->
            val score = cosine(vector, vec)
            scores.add(ScoredId(id, score))
        }
        return scores.sortedByDescending { it.score }.take(topK)
    }

    override suspend fun size(): Int = store.size

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var dot = 0f
        var na = 0f
        var nb = 0f
        var i = 0
        while (i < n) {
            val av = a[i]
            val bv = b[i]
            dot += av * bv
            na += av * av
            nb += bv * bv
            i++
        }
        val denom = (sqrt(na) * sqrt(nb))
        return if (denom == 0f) 0f else dot / denom
    }
}