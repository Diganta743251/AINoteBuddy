package com.ainotebuddy.app.ai

import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sentiment analysis engine
 */
@Singleton
class SentimentAnalyzer @Inject constructor() {
    
    suspend fun analyzeSentiment(content: String): SentimentResult {
        val words = content.lowercase().split(" ")
        val positiveWords = listOf("good", "great", "excellent", "amazing", "wonderful", "fantastic", "love", "like", "happy", "joy")
        val negativeWords = listOf("bad", "terrible", "awful", "hate", "dislike", "sad", "angry", "frustrated", "disappointed")
        
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        val total = words.size.coerceAtLeast(1)
        
        val positiveScore = positiveCount.toFloat() / total
        val negativeScore = negativeCount.toFloat() / total
        val neutralScore = 1.0f - positiveScore - negativeScore
        
        val sentiment = when {
            positiveScore > negativeScore -> Sentiment.POSITIVE
            negativeScore > positiveScore -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
        
        val confidence = maxOf(positiveScore, negativeScore, neutralScore)
        
        return SentimentResult(sentiment, confidence, positiveScore, negativeScore, neutralScore)
    }
}

@Serializable
data class SentimentResult(
    val sentiment: Sentiment,
    val confidence: Float,
    val positiveScore: Float,
    val negativeScore: Float,
    val neutralScore: Float
)

@Serializable
enum class Sentiment {
    POSITIVE, NEGATIVE, NEUTRAL
}