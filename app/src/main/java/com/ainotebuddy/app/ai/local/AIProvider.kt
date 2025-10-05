package com.ainotebuddy.app.ai.local

import android.content.Context
import com.ainotebuddy.app.ai.embeddings.AIEmbeddings
import com.ainotebuddy.app.ai.embeddings.SimpleAIEmbeddings

/**
 * Master provider for AI services with runtime selection.
 */
interface AIProvider {
    val summarizer: AISummarizer
    val embeddings: AIEmbeddings
    val nlp: AINlp
}

object AIProviderFactory {
    fun create(context: Context): AIProvider {
        return if (AICoreDetector.isAvailable(context)) {
            AICoreProvider(context)
        } else {
            TFLiteProvider(context)
        }
    }
}

interface AISummarizer {
    suspend fun summarize(text: String, maxTokens: Int = 128): String
}

interface AINlp {
    suspend fun extractEntities(text: String): List<ExtractedEntity>
    fun keyphrases(text: String): List<String>
}

data class ExtractedEntity(
    val type: String,
    val value: String,
    val start: Int,
    val end: Int
)

/** Placeholder AICore-backed provider. */
class AICoreProvider(context: Context) : AIProvider {
    override val summarizer: AISummarizer = object : AISummarizer {
        override suspend fun summarize(text: String, maxTokens: Int): String {
            return "[AICore] summary: ${text.take(80)}…"
        }
    }
    override val embeddings: AIEmbeddings = SimpleAIEmbeddings()
    override val nlp: AINlp = object : AINlp {
        override suspend fun extractEntities(text: String) = emptyList<ExtractedEntity>()
        override fun keyphrases(text: String) = heuristicKeyphrases(text)
    }
}

/** Placeholder local TFLite/ONNX-backed provider. */
class TFLiteProvider(context: Context) : AIProvider {
    override val summarizer: AISummarizer = object : AISummarizer {
        override suspend fun summarize(text: String, maxTokens: Int): String {
            // Simple extractive summary using first sentences as stub
            val sentences = text.split(Regex("[.!?]+")).map { it.trim() }.filter { it.isNotBlank() }
            return sentences.take(2).joinToString(". ").take(220)
        }
    }
    override val embeddings: AIEmbeddings = SimpleAIEmbeddings()
    override val nlp: AINlp = object : AINlp {
        override suspend fun extractEntities(text: String) = emptyList<ExtractedEntity>()
        override fun keyphrases(text: String) = heuristicKeyphrases(text)
    }
}

private fun heuristicKeyphrases(text: String): List<String> {
    val words = text.lowercase()
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .split(" ")
        .filter { it.length > 3 }
    return words.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }
        .map { it.key }
        .distinct()
        .take(5)
}