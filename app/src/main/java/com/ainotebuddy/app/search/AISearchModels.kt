package com.ainotebuddy.app.search

import com.ainotebuddy.app.ai.*
import com.ainotebuddy.app.data.Note

// AI-Enhanced Search Result Models

/**
 * Comprehensive AI-enhanced search result
 */
data class AISearchResult(
    val query: String,
    val results: List<EnhancedSearchResult>,
    val suggestions: List<AISearchSuggestion>,
    val queryAnalysis: SearchQueryAnalysis,
    val searchMetrics: SearchMetrics,
    val timestamp: Long
)

/**
 * Enhanced search result with AI insights
 */
data class EnhancedSearchResult(
    val note: Note,
    val relevanceScore: Float,
    val semanticSimilarity: Float,
    val contextualRelevance: Float,
    val baseRelevance: Float,
    val matchHighlights: List<String>,
    val relevanceExplanation: String,
    val aiInsights: List<String>
)

/**
 * Semantic search result
 */
data class SemanticSearchResult(
    val note: Note,
    val similarity: Float,
    val matchingConcepts: List<String>,
    val relevanceExplanation: String
)

/**
 * Contextual search result based on user context
 */
data class ContextualSearchResult(
    val note: Note,
    val contextualRelevance: Float,
    val contextFactors: List<String>,
    val personalizedScore: Float
)

// Query Analysis Models

/**
 * Comprehensive analysis of search query using AI
 */
data class SearchQueryAnalysis(
    val originalQuery: String,
    val topics: List<TopicResult>,
    val entities: List<EntityResult>,
    val sentiment: SentimentResult,
    val intent: SearchIntent,
    val temporalContext: TemporalContext?,
    val complexity: QueryComplexity,
    val confidence: Float
)

enum class SearchIntent {
    FIND,              // Looking for specific information
    BROWSE,            // Exploring content
    TEMPORAL,          // Time-based search
    PERSON_RELATED,    // Searching for person-related content
    LOCATION_RELATED,  // Searching for location-related content
    TOPIC_FOCUSED,     // Focused on specific topic
    GENERAL           // General exploration
}

data class TemporalContext(
    val timeRange: String,
    val description: String
)

enum class QueryComplexity {
    SIMPLE,           // Single word or simple phrase
    MODERATE,         // Multiple concepts, basic structure
    COMPLEX,          // Multiple entities, topics, filters
    VERY_COMPLEX     // Advanced query with multiple constraints
}

// AI Search Suggestion Models

/**
 * AI-powered search suggestion
 */
data class AISearchSuggestion(
    val suggestion: String,
    val type: AISearchSuggestionType,
    val confidence: Float,
    val explanation: String,
    val expectedResults: Int
)

enum class AISearchSuggestionType {
    TOPIC_EXPANSION,      // Expand to related topics
    ENTITY_FILTER,        // Filter by specific entities
    SENTIMENT_FILTER,     // Filter by sentiment
    TEMPORAL_FILTER,      // Filter by time
    SEMANTIC_SIMILAR,     // Semantically similar queries
    CONTEXTUAL_REFINEMENT // Context-based refinements
}

/**
 * Intelligent search suggestion with advanced features
 */
data class IntelligentSearchSuggestion(
    val suggestion: String,
    val type: IntelligentSuggestionType,
    val relevanceScore: Float,
    val confidence: Float,
    val explanation: String,
    val previewResults: List<String> = emptyList()
)

enum class IntelligentSuggestionType {
    QUERY_COMPLETION,     // Complete partial queries
    TOPIC_BASED,          // Based on topic analysis
    ENTITY_BASED,         // Based on entity recognition
    SENTIMENT_BASED,      // Based on sentiment analysis
    CONTEXTUAL,           // Based on user context
    SEMANTIC_EXPANSION,   // Semantic query expansion
    HISTORICAL           // Based on search history
}

// Search Options and Configuration

/**
 * Options for AI-enhanced search
 */
data class AISearchOptions(
    val enableSemanticSearch: Boolean = true,
    val enableContextualSearch: Boolean = true,
    val enableSentimentFiltering: Boolean = true,
    val enableEntityRecognition: Boolean = true,
    val enableTopicModeling: Boolean = true,
    val maxResults: Int = 50,
    val confidenceThreshold: Float = 0.3f,
    val includeAISuggestions: Boolean = true,
    val personalizeResults: Boolean = true
)

// Semantic Search Models

/**
 * Semantic search index for AI-enhanced search
 */
data class SemanticSearchIndex(
    val embeddings: Map<String, NoteEmbedding>,
    val conceptMap: Map<String, List<String>>,
    val lastUpdated: Long
)

/**
 * Query embedding for semantic search
 */
data class QueryEmbedding(
    val query: String,
    val embedding: List<Float>
)

/**
 * Note embedding for semantic search
 */
data class NoteEmbedding(
    val noteId: String,
    val embedding: List<Float>
)

// Search Intelligence Models

/**
 * Search intelligence metrics
 */
data class SearchIntelligenceMetrics(
    val averageQueryComplexity: Float,
    val semanticAccuracy: Float,
    val contextualRelevance: Float,
    val userSatisfactionScore: Float,
    val averageResponseTime: Long,
    val cacheHitRate: Float
)

/**
 * Search metrics for individual search
 */
data class SearchMetrics(
    val totalResults: Int,
    val semanticResults: Int,
    val contextualResults: Int,
    val baseResults: Int,
    val processingTime: Long,
    val cacheHit: Boolean
)

// Advanced Search Features

/**
 * Search filter based on AI analysis
 */
data class AISearchFilter(
    val type: AIFilterType,
    val value: String,
    val confidence: Float,
    val description: String
)

enum class AIFilterType {
    TOPIC,
    ENTITY_PERSON,
    ENTITY_ORGANIZATION,
    ENTITY_LOCATION,
    ENTITY_DATE,
    SENTIMENT_POSITIVE,
    SENTIMENT_NEGATIVE,
    SENTIMENT_NEUTRAL,
    ACTION_ITEMS,
    PRIORITY_HIGH,
    PRIORITY_MEDIUM,
    PRIORITY_LOW
}

/**
 * Search facets for advanced filtering
 */
data class SearchFacets(
    val topics: List<TopicFacet>,
    val entities: List<EntityFacet>,
    val sentiments: List<SentimentFacet>,
    val timeRanges: List<TimeRangeFacet>,
    val categories: List<CategoryFacet>
)

data class TopicFacet(
    val topic: String,
    val count: Int,
    val confidence: Float
)

data class EntityFacet(
    val entity: String,
    val type: EntityType,
    val count: Int,
    val confidence: Float
)

data class SentimentFacet(
    val sentiment: String,
    val count: Int,
    val averagePolarity: Float
)

data class TimeRangeFacet(
    val range: String,
    val count: Int,
    val description: String
)

data class CategoryFacet(
    val category: String,
    val count: Int
)

/**
 * Search analytics data
 */
data class SearchAnalytics(
    val totalSearches: Long = 0L,
    val averageResultsPerSearch: Float = 0f,
    val topQueries: List<QueryFrequency> = emptyList(),
    val searchPatterns: SearchPatterns = SearchPatterns(
        topQueries = emptyList(),
        commonFilters = emptyList(),
        searchTimes = emptyMap(),
        queryComplexityDistribution = emptyMap()
    ),
    val userEngagement: SearchEngagement = SearchEngagement(
        averageResultsClicked = 0f,
        averageTimeOnResults = 0L,
        searchRefinementRate = 0f,
        suggestionAcceptanceRate = 0f
    ),
    val performanceMetrics: SearchPerformanceMetrics = SearchPerformanceMetrics(
        averageSearchTime = 0L,
        cacheHitRate = 0f,
        indexUpdateFrequency = 0L,
        errorRate = 0f
    )
)

data class QueryFrequency(
    val query: String,
    val frequency: Int,
    val lastSearched: Long
)

data class SearchPatterns(
    val topQueries: List<String>,
    val commonFilters: List<String>,
    val searchTimes: Map<Int, Int>, // Hour -> Count
    val queryComplexityDistribution: Map<QueryComplexity, Int>
)

data class SearchEngagement(
    val averageResultsClicked: Float,
    val averageTimeOnResults: Long,
    val searchRefinementRate: Float,
    val suggestionAcceptanceRate: Float
)

data class SearchPerformanceMetrics(
    val averageSearchTime: Long,
    val cacheHitRate: Float,
    val indexUpdateFrequency: Long,
    val errorRate: Float
)

// Search Personalization Models

/**
 * Personalized search preferences
 */
data class PersonalizedSearchPreferences(
    val preferredTopics: List<String>,
    val preferredEntityTypes: List<EntityType>,
    val sentimentPreference: SentimentPreference?,
    val timeRangePreference: TimeRangePreference?,
    val resultOrderingPreference: ResultOrderingPreference
)

enum class SentimentPreference {
    POSITIVE_FOCUSED,
    NEGATIVE_FOCUSED,
    NEUTRAL_FOCUSED,
    ALL_SENTIMENTS
}

enum class TimeRangePreference {
    RECENT_FIRST,
    OLDEST_FIRST,
    RELEVANCE_BASED,
    NO_PREFERENCE
}

enum class ResultOrderingPreference {
    RELEVANCE,
    RECENCY,
    ALPHABETICAL,
    CATEGORY,
    AI_RECOMMENDED
}

// Search Context Models

/**
 * Current search context
 */
data class SearchContext(
    val currentQuery: String,
    val previousQueries: List<String>,
    val selectedFilters: List<AISearchFilter>,
    val viewedResults: List<String>,
    val searchSession: SearchSession
)

data class SearchSession(
    val sessionId: String,
    val startTime: Long,
    val queries: List<String>,
    val interactions: List<SearchInteraction>
)

data class SearchInteraction(
    val type: SearchInteractionType,
    val target: String,
    val timestamp: Long,
    val duration: Long? = null
)

enum class SearchInteractionType {
    QUERY_ENTERED,
    RESULT_CLICKED,
    FILTER_APPLIED,
    SUGGESTION_ACCEPTED,
    RESULT_SHARED,
    SEARCH_REFINED
}

// Advanced AI Features

/**
 * Conversational search capability
 */
data class ConversationalSearch(
    val conversation: List<SearchTurn>,
    val context: ConversationalContext,
    val currentIntent: SearchIntent
)

data class SearchTurn(
    val userInput: String,
    val systemResponse: AISearchResult,
    val timestamp: Long
)

data class ConversationalContext(
    val entities: List<EntityResult>,
    val topics: List<TopicResult>,
    val constraints: List<SearchConstraint>,
    val userPreferences: PersonalizedSearchPreferences
)

data class SearchConstraint(
    val type: ConstraintType,
    val value: String,
    val operator: ConstraintOperator
)

enum class ConstraintType {
    DATE_RANGE,
    CATEGORY,
    SENTIMENT,
    ENTITY,
    TOPIC,
    PRIORITY
}

enum class ConstraintOperator {
    EQUALS,
    CONTAINS,
    GREATER_THAN,
    LESS_THAN,
    BETWEEN,
    NOT_EQUALS
}

// Search Quality Models

/**
 * Search quality assessment
 */
data class SearchQualityAssessment(
    val relevanceScore: Float,
    val diversityScore: Float,
    val freshnessScore: Float,
    val personalizedScore: Float,
    val overallQuality: Float,
    val improvementSuggestions: List<String>
)

/**
 * Search result ranking factors
 */
data class RankingFactors(
    val semanticSimilarity: Float,
    val contextualRelevance: Float,
    val personalizedScore: Float,
    val recencyScore: Float,
    val popularityScore: Float,
    val qualityScore: Float,
    val diversityBoost: Float
)

// Error and Fallback Models

/**
 * Search error information
 */
data class SearchError(
    val type: SearchErrorType,
    val message: String,
    val query: String,
    val timestamp: Long,
    val fallbackResults: List<EnhancedSearchResult>? = null
)

enum class SearchErrorType {
    PARSING_ERROR,
    AI_ANALYSIS_FAILED,
    INDEX_UNAVAILABLE,
    TIMEOUT,
    INSUFFICIENT_RESULTS,
    SYSTEM_ERROR
}

/**
 * Search fallback strategy
 */
data class SearchFallbackStrategy(
    val strategy: FallbackType,
    val confidence: Float,
    val description: String
)

enum class FallbackType {
    BASIC_TEXT_SEARCH,
    CACHED_RESULTS,
    SIMPLIFIED_QUERY,
    POPULAR_RESULTS,
    RECENT_RESULTS
}