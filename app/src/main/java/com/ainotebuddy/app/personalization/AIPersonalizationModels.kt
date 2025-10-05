package com.ainotebuddy.app.personalization

import com.ainotebuddy.app.integration.DeviceState
import com.ainotebuddy.app.integration.FABAction
import com.ainotebuddy.app.ui.dashboard.DashboardWidgetType
import com.ainotebuddy.app.ai.*

// AI-Enhanced Personalization Models

/**
 * AI-enhanced personalization data
 */
data class AIPersonalizationData(
    val intelligentProfile: IntelligentUserProfile,
    val adaptiveRecommendations: List<AdaptiveRecommendation>,
    val personalizedExperiences: List<PersonalizedExperience>,
    val learningInsights: List<LearningInsight>,
    val lastUpdated: Long
)

/**
 * Intelligent user profile built using AI analysis
 */
data class IntelligentUserProfile(
    val userId: String,
    val writingPatterns: WritingPatterns,
    val behaviorPatterns: BehaviorPatterns,
    val contentPreferences: ContentPreferences,
    val productivityPatterns: ProductivityPatterns,
    val personalityInsights: PersonalityInsights,
    val learningStyle: LearningStyle,
    val workingStyle: WorkingStyle,
    val communicationStyle: CommunicationStyle,
    val adaptationLevel: AdaptationLevel,
    val lastUpdated: Long
)

// Writing Pattern Models

/**
 * AI-analyzed writing patterns
 */
data class WritingPatterns(
    val averageNoteLength: Float,
    val preferredWritingTime: Int, // Hour of day
    val writingFrequency: Float, // Notes per day
    val vocabularyComplexity: Float,
    val sentenceStructure: String,
    val topicDiversity: Float,
    val emotionalRange: Float
)

// Behavior Pattern Models

/**
 * AI-analyzed behavior patterns
 */
data class BehaviorPatterns(
    val peakActivityHours: List<Int>,
    val sessionDuration: Long,
    val interactionFrequency: Float,
    val featureUsagePatterns: Map<String, Float>,
    val navigationPatterns: List<String>
)

// Content Preference Models

/**
 * AI-inferred content preferences
 */
data class ContentPreferences(
    val preferredTopics: List<String>,
    val preferredCategories: List<String>,
    val contentComplexity: Float,
    val structuralPreferences: List<String>,
    val mediaPreferences: List<String>
)

// Productivity Pattern Models

/**
 * AI-analyzed productivity patterns
 */
data class ProductivityPatterns(
    val peakHours: List<Int>,
    val averageNotesPerDay: Float,
    val taskCompletionRate: Float,
    val focusSessionLength: Long,
    val productivityTrends: List<String>
)

// Personality Insight Models

/**
 * AI-inferred personality insights
 */
data class PersonalityInsights(
    val openness: Float,
    val conscientiousness: Float,
    val extraversion: Float,
    val agreeableness: Float,
    val neuroticism: Float,
    val dominantTraits: List<String>,
    val personalityType: String
)

// Learning and Working Style Models

enum class LearningStyle {
    DEEP_LEARNER,      // Prefers detailed, comprehensive content
    BROAD_LEARNER,     // Explores many different topics
    ACTIVE_LEARNER,    // High interaction and engagement
    BALANCED_LEARNER   // Balanced approach to learning
}

enum class WorkingStyle {
    DEEP_FOCUS,        // Long, concentrated work sessions
    HIGH_OUTPUT,       // Frequent, productive sessions
    CONCENTRATED,      // Specific time-based productivity
    FLEXIBLE          // Adaptable working patterns
}

enum class CommunicationStyle {
    DETAILED,         // Comprehensive, thorough communication
    EXPRESSIVE,       // Emotionally rich communication
    SOCIAL,           // People-focused communication
    CONCISE          // Brief, to-the-point communication
}

enum class AdaptationLevel {
    HIGH,            // Quickly adopts new features and patterns
    MEDIUM,          // Moderate adaptation to changes
    LOW,             // Slow to adopt new patterns
    MINIMAL         // Resistant to change
}

// Intelligent Dashboard Models

/**
 * AI-generated intelligent dashboard configuration
 */
data class IntelligentDashboardConfig(
    val widgetRecommendations: List<AIWidgetRecommendation>,
    val fabRecommendations: List<IntelligentFABRecommendation>,
    val layoutSuggestions: List<String>,
    val quickActions: List<String>,
    val personalizedGreeting: String,
    val adaptiveTheme: String,
    val intelligentShortcuts: List<String>,
    val contextualHints: List<String>,
    val timestamp: Long
)

/**
 * AI-powered widget recommendation
 */
data class AIWidgetRecommendation(
    val widgetType: DashboardWidgetType,
    val priority: RecommendationPriority,
    val confidence: Float,
    val reason: String,
    val aiInsight: String,
    val expectedBenefit: String,
    val customization: Map<String, Any> = emptyMap()
)

enum class RecommendationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Intelligent FAB action recommendation
 */
data class IntelligentFABRecommendation(
    val action: FABAction,
    val priority: RecommendationPriority,
    val confidence: Float,
    val reason: String,
    val aiInsight: String,
    val contextualHint: String
)

// Personalized Search Models

/**
 * AI-enhanced personalized search experience
 */
data class PersonalizedSearchExperience(
    val searchSuggestions: List<String>,
    val personalizedFilters: List<String>,
    val searchShortcuts: List<String>,
    val adaptiveInterface: String,
    val intelligentDefaults: Map<String, Any>,
    val contextualHelp: List<String>
)

// Adaptive Recommendation Models

/**
 * Adaptive recommendation that evolves with user behavior
 */
data class AdaptiveRecommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val relevanceScore: Float,
    val confidence: Float,
    val aiReasoning: String,
    val expectedOutcome: String,
    val actionRequired: RecommendationAction,
    val priority: RecommendationPriority,
    val expiresAt: Long? = null,
    val metadata: Map<String, Any> = emptyMap()
)

enum class RecommendationType {
    WORKFLOW_OPTIMIZATION,    // Improve user workflows
    CONTENT_ORGANIZATION,     // Better content organization
    PRODUCTIVITY_ENHANCEMENT, // Boost productivity
    FEATURE_DISCOVERY,       // Discover new features
    HABIT_FORMATION,         // Form better habits
    PERSONALIZATION_TUNING,  // Fine-tune personalization
    COLLABORATION_IMPROVEMENT // Improve collaboration
}

/**
 * Action required for recommendation
 */
sealed class RecommendationAction {
    object NoAction : RecommendationAction()
    data class ConfigureWidget(val widgetType: DashboardWidgetType, val config: Map<String, Any>) : RecommendationAction()
    data class UpdatePreferences(val preferences: Map<String, Any>) : RecommendationAction()
    data class TryFeature(val featureName: String, val parameters: Map<String, Any>) : RecommendationAction()
    data class OrganizeContent(val organizationType: String, val criteria: Map<String, Any>) : RecommendationAction()
    data class CreateAutomation(val automationType: String, val rules: Map<String, Any>) : RecommendationAction()
}

// Personalized Experience Models

/**
 * Personalized experience configuration
 */
data class PersonalizedExperience(
    val experienceId: String,
    val name: String,
    val description: String,
    val targetUserProfile: UserProfileCriteria,
    val adaptations: List<ExperienceAdaptation>,
    val effectiveness: Float,
    val userSatisfaction: Float,
    val isActive: Boolean
)

data class UserProfileCriteria(
    val learningStyle: LearningStyle?,
    val workingStyle: WorkingStyle?,
    val communicationStyle: CommunicationStyle?,
    val adaptationLevel: AdaptationLevel?,
    val personalityTraits: List<String> = emptyList(),
    val contentPreferences: List<String> = emptyList()
)

data class ExperienceAdaptation(
    val component: String,
    val adaptationType: AdaptationType,
    val parameters: Map<String, Any>,
    val confidence: Float
)

enum class AdaptationType {
    LAYOUT_MODIFICATION,      // Modify interface layout
    CONTENT_FILTERING,        // Filter content based on preferences
    INTERACTION_OPTIMIZATION, // Optimize user interactions
    VISUAL_CUSTOMIZATION,     // Customize visual elements
    WORKFLOW_STREAMLINING,    // Streamline user workflows
    FEATURE_HIGHLIGHTING     // Highlight relevant features
}

// Learning Insight Models

/**
 * AI-generated learning insights about user behavior
 */
data class LearningInsight(
    val insightId: String,
    val category: InsightCategory,
    val title: String,
    val description: String,
    val confidence: Float,
    val impact: InsightImpact,
    val actionable: Boolean,
    val suggestedActions: List<String>,
    val evidencePoints: List<String>,
    val timestamp: Long
)

enum class InsightCategory {
    PRODUCTIVITY_PATTERN,     // Insights about productivity
    CONTENT_PREFERENCE,       // Insights about content preferences
    BEHAVIORAL_TREND,         // Insights about behavior trends
    LEARNING_OPPORTUNITY,     // Opportunities for improvement
    EFFICIENCY_GAIN,          // Potential efficiency improvements
    PERSONALIZATION_TUNING   // Personalization adjustments
}

enum class InsightImpact {
    LOW,                     // Minor impact on user experience
    MEDIUM,                  // Moderate impact on user experience
    HIGH,                    // Significant impact on user experience
    TRANSFORMATIVE          // Major transformation potential
}

// Advanced Personalization Models

/**
 * Contextual personalization based on current situation
 */
data class ContextualPersonalization(
    val context: PersonalizationContext,
    val adaptations: List<ContextualAdaptation>,
    val confidence: Float,
    val duration: Long? = null
)

data class PersonalizationContext(
    val timeOfDay: Int,
    val dayOfWeek: Int,
    val location: String?,
    val deviceState: DeviceState,
    val recentActivity: List<String>,
    val currentMood: String?,
    val workingMode: WorkingMode?
)

enum class WorkingMode {
    FOCUSED_WORK,            // Deep focus mode
    COLLABORATIVE_WORK,      // Working with others
    CREATIVE_MODE,           // Creative work mode
    PLANNING_MODE,           // Planning and organizing
    REVIEW_MODE,             // Reviewing and reflecting
    CASUAL_USE              // Casual, light usage
}

data class ContextualAdaptation(
    val element: String,
    val adaptation: String,
    val reason: String,
    val confidence: Float
)

// Personalization Analytics Models

/**
 * Analytics for personalization effectiveness
 */
data class PersonalizationAnalytics(
    val overallEffectiveness: Float,
    val userSatisfactionScore: Float,
    val adaptationSuccessRate: Float,
    val recommendationAcceptanceRate: Float,
    val featureDiscoveryRate: Float,
    val productivityImprovement: Float,
    val engagementMetrics: EngagementMetrics,
    val personalizationTrends: List<PersonalizationTrend>
)

data class EngagementMetrics(
    val dailyActiveTime: Long,
    val featureUsageDistribution: Map<String, Float>,
    val interactionDepth: Float,
    val returnRate: Float,
    val satisfactionIndicators: Map<String, Float>
)

data class PersonalizationTrend(
    val aspect: String,
    val trend: TrendDirection,
    val magnitude: Float,
    val timeframe: String,
    val significance: Float
)

enum class TrendDirection {
    IMPROVING,               // Getting better over time
    DECLINING,               // Getting worse over time
    STABLE,                  // Remaining consistent
    FLUCTUATING             // Variable performance
}

// Machine Learning Models for Personalization

/**
 * ML model for personalization predictions
 */
data class PersonalizationMLModel(
    val modelId: String,
    val modelType: MLModelType,
    val accuracy: Float,
    val lastTrained: Long,
    val features: List<String>,
    val predictions: Map<String, Float>
)

enum class MLModelType {
    PREFERENCE_PREDICTION,    // Predict user preferences
    BEHAVIOR_FORECASTING,     // Forecast user behavior
    CONTENT_RECOMMENDATION,   // Recommend content
    WORKFLOW_OPTIMIZATION,    // Optimize workflows
    ENGAGEMENT_PREDICTION    // Predict engagement levels
}

// Personalization Experiment Models

/**
 * A/B testing for personalization features
 */
data class PersonalizationExperiment(
    val experimentId: String,
    val name: String,
    val description: String,
    val variants: List<ExperimentVariant>,
    val targetCriteria: UserProfileCriteria,
    val metrics: List<String>,
    val status: ExperimentStatus,
    val results: ExperimentResults?
)

data class ExperimentVariant(
    val variantId: String,
    val name: String,
    val configuration: Map<String, Any>,
    val trafficAllocation: Float
)

enum class ExperimentStatus {
    DRAFT,                   // Experiment being designed
    RUNNING,                 // Currently running
    PAUSED,                  // Temporarily paused
    COMPLETED,               // Finished running
    ANALYZED                // Results analyzed
}

data class ExperimentResults(
    val winningVariant: String?,
    val confidence: Float,
    val metrics: Map<String, Float>,
    val insights: List<String>,
    val recommendations: List<String>
)

// Privacy and Consent Models

/**
 * Privacy settings for personalization
 */
data class PersonalizationPrivacy(
    val userId: String,
    val dataCollectionConsent: Boolean,
    val behaviorAnalysisConsent: Boolean,
    val contentAnalysisConsent: Boolean,
    val mlModelingConsent: Boolean,
    val dataRetentionPeriod: Long,
    val anonymizationLevel: AnonymizationLevel,
    val optOutFeatures: List<String>
)

enum class AnonymizationLevel {
    NONE,                    // No anonymization
    BASIC,                   // Basic anonymization
    ADVANCED,                // Advanced anonymization
    FULL                    // Full anonymization
}

// Integration Models

/**
 * Integration with external personalization services
 */
data class ExternalPersonalizationIntegration(
    val serviceId: String,
    val serviceName: String,
    val integrationType: IntegrationType,
    val configuration: Map<String, Any>,
    val isEnabled: Boolean,
    val lastSync: Long
)

enum class IntegrationType {
    ANALYTICS_PROVIDER,      // Analytics service integration
    ML_SERVICE,              // Machine learning service
    RECOMMENDATION_ENGINE,   // External recommendation engine
    BEHAVIOR_TRACKER,        // Behavior tracking service
    PERSONALIZATION_PLATFORM // Full personalization platform
}