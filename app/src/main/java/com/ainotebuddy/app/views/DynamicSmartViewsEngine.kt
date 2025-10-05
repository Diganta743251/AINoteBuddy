package com.ainotebuddy.app.views

import android.content.Context
import com.ainotebuddy.app.ai.*
import com.ainotebuddy.app.data.NoteEntity
// import com.ainotebuddy.app.personalization.AIEnhancedPersonalizationEngine
import com.ainotebuddy.app.search.AIEnhancedSearchEngine
import com.ainotebuddy.app.search.SmartSearchEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

/**
 * Dynamic Smart Views Engine - Intelligently organizes and presents notes
 * using AI-powered paradigm switching and hybrid user control
 */
@Singleton
class DynamicSmartViewsEngine @Inject constructor(
    private val context: Context,
    private val aiAnalysisEngine: AIAnalysisEngine,
    // private val personalizationEngine: AIEnhancedPersonalizationEngine,
    private val searchEngine: AIEnhancedSearchEngine,
    private val smartSearchEngine: SmartSearchEngine
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Current active views
    private val _activeViews = MutableStateFlow<List<SmartView>>(emptyList())
    val activeViews: StateFlow<List<SmartView>> = _activeViews.asStateFlow()
    
    // View suggestions from AI
    private val _viewSuggestions = MutableStateFlow<List<ViewSuggestion>>(emptyList())
    val viewSuggestions: StateFlow<List<ViewSuggestion>> = _viewSuggestions.asStateFlow()
    
    // User preferences for views
    private val _userViewPreferences = MutableStateFlow<UserViewPreferences>(UserViewPreferences())
    val userViewPreferences: StateFlow<UserViewPreferences> = _userViewPreferences.asStateFlow()
    
    // Current paradigm being used
    private val _currentParadigm = MutableStateFlow<OrganizationParadigm>(OrganizationParadigm.INTELLIGENT_AUTO)
    val currentParadigm: StateFlow<OrganizationParadigm> = _currentParadigm.asStateFlow()
    
    /**
     * Initialize the Dynamic Smart Views system
     */
    fun initialize() {
        scope.launch {
            loadUserPreferences()
            startIntelligentViewGeneration()
            startParadigmOptimization()
        }
    }
    
    /**
     * Generate smart views for the given notes using intelligent paradigm switching
     */
    suspend fun generateSmartViews(notes: List<NoteEntity>): List<SmartView> {
        if (notes.isEmpty()) return emptyList()
        
        return withContext(Dispatchers.IO) {
            val userPrefs = _userViewPreferences.value
            val currentParadigm = determineOptimalParadigm(notes, userPrefs)
            _currentParadigm.value = currentParadigm
            
            val views = when (currentParadigm) {
                OrganizationParadigm.TIME_BASED -> generateTimeBasedViews(notes)
                OrganizationParadigm.TOPIC_BASED -> generateTopicBasedViews(notes)
                OrganizationParadigm.PRIORITY_BASED -> generatePriorityBasedViews(notes)
                OrganizationParadigm.PROJECT_BASED -> generateProjectBasedViews(notes)
                OrganizationParadigm.SENTIMENT_BASED -> generateSentimentBasedViews(notes)
                OrganizationParadigm.INTELLIGENT_AUTO -> generateIntelligentAutoViews(notes)
            }
            
            _activeViews.value = views
            generateViewSuggestions(notes, views)
            
            views
        }
    }
    
    /**
     * Determine the optimal organization paradigm based on notes and user preferences
     */
    private suspend fun determineOptimalParadigm(
        notes: List<NoteEntity>, 
        userPrefs: UserViewPreferences
    ): OrganizationParadigm {
        // If user has explicit preference, respect it
        if (userPrefs.preferredParadigm != OrganizationParadigm.INTELLIGENT_AUTO) {
            return userPrefs.preferredParadigm
        }
        
        // Use AI to determine best paradigm
        val analysis = analyzeNotesForOptimalOrganization(notes)
        
        return when {
            analysis.hasStrongTemporalPatterns -> OrganizationParadigm.TIME_BASED
            analysis.hasDistinctTopics -> OrganizationParadigm.TOPIC_BASED
            analysis.hasUrgentItems -> OrganizationParadigm.PRIORITY_BASED
            analysis.hasProjectStructure -> OrganizationParadigm.PROJECT_BASED
            analysis.hasEmotionalVariance -> OrganizationParadigm.SENTIMENT_BASED
            else -> OrganizationParadigm.TOPIC_BASED // Default fallback
        }
    }
    
    /**
     * Generate time-based smart views
     */
    private suspend fun generateTimeBasedViews(notes: List<NoteEntity>): List<SmartView> {
        val sortedNotes = notes.sortedByDescending { it.createdAt }
        val views = mutableListOf<SmartView>()
        
        // Today's notes
        val today = LocalDateTime.now()
        val todayNotes = sortedNotes.filter { 
            isFromToday(it.createdAt, today)
        }
        if (todayNotes.isNotEmpty()) {
            views.add(SmartView(
                id = "today",
                title = "Today",
                subtitle = "${todayNotes.size} notes",
                notes = todayNotes,
                viewType = ViewType.TIME_BASED,
                priority = ViewPriority.HIGH,
                icon = "today"
            ))
        }
        
        // This week's notes
        val weekNotes = sortedNotes.filter { 
            isFromThisWeek(it.createdAt, today) && !isFromToday(it.createdAt, today)
        }
        if (weekNotes.isNotEmpty()) {
            views.add(SmartView(
                id = "this_week",
                title = "This Week",
                subtitle = "${weekNotes.size} notes",
                notes = weekNotes,
                viewType = ViewType.TIME_BASED,
                priority = ViewPriority.MEDIUM,
                icon = "date_range"
            ))
        }
        
        // Recent notes (last 30 days)
        val recentNotes = sortedNotes.filter { 
            isFromLastMonth(it.createdAt, today) && 
            !isFromThisWeek(it.createdAt, today)
        }
        if (recentNotes.isNotEmpty()) {
            views.add(SmartView(
                id = "recent",
                title = "Recent",
                subtitle = "${recentNotes.size} notes",
                notes = recentNotes,
                viewType = ViewType.TIME_BASED,
                priority = ViewPriority.MEDIUM,
                icon = "schedule"
            ))
        }
        
        return views
    }
    
    /**
     * Generate topic-based smart views using AI topic modeling
     */
    private suspend fun generateTopicBasedViews(notes: List<NoteEntity>): List<SmartView> {
        val topicAnalysis = performTopicModeling(notes)
        val views = mutableListOf<SmartView>()
        
        topicAnalysis.topics.forEach { topic ->
            val topicNotes = notes.filter { note ->
                val text = "${note.title} ${note.content}".lowercase()
                topic.keywords.any { keyword -> text.contains(keyword.lowercase()) }
            }
            
            if (topicNotes.isNotEmpty()) {
                views.add(SmartView(
                    id = "topic_${topic.id}",
                    title = topic.name,
                    subtitle = "${topicNotes.size} notes • ${(topic.confidence * 100).toInt()}% confidence",
                    notes = topicNotes.sortedByDescending { it.createdAt },
                    viewType = ViewType.TOPIC_BASED,
                    priority = when {
                        topic.confidence > 0.8 -> ViewPriority.HIGH
                        topic.confidence > 0.6 -> ViewPriority.MEDIUM
                        else -> ViewPriority.LOW
                    },
                    icon = topic.suggestedIcon ?: "topic",
                    metadata = mapOf(
                        "confidence" to topic.confidence.toString(),
                        "keywords" to (topic.keywords?.joinToString(", ") ?: "")
                    )
                ))
            }
        }
        
        return views.sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * Generate priority-based smart views
     */
    private suspend fun generatePriorityBasedViews(notes: List<NoteEntity>): List<SmartView> {
        val priorityAnalysis = analyzePriorities(notes)
        val views = mutableListOf<SmartView>()
        
        // Urgent & Important (Do First)
        val urgentImportant = notes.filter { 
            priorityAnalysis.getPriority(it.id) == NotePriority.URGENT_IMPORTANT 
        }
        if (urgentImportant.isNotEmpty()) {
            views.add(SmartView(
                id = "urgent_important",
                title = "🔥 Urgent & Important",
                subtitle = "${urgentImportant.size} notes • Do First",
                notes = urgentImportant.sortedByDescending { it.createdAt },
                viewType = ViewType.PRIORITY_BASED,
                priority = ViewPriority.CRITICAL,
                icon = "priority_high"
            ))
        }
        
        // Important but Not Urgent (Schedule)
        val importantNotUrgent = notes.filter { 
            priorityAnalysis.getPriority(it.id) == NotePriority.IMPORTANT_NOT_URGENT 
        }
        if (importantNotUrgent.isNotEmpty()) {
            views.add(SmartView(
                id = "important_not_urgent",
                title = "📅 Important",
                subtitle = "${importantNotUrgent.size} notes • Schedule",
                notes = importantNotUrgent.sortedByDescending { it.createdAt },
                viewType = ViewType.PRIORITY_BASED,
                priority = ViewPriority.HIGH,
                icon = "event"
            ))
        }
        
        // Urgent but Not Important (Delegate)
        val urgentNotImportant = notes.filter { 
            priorityAnalysis.getPriority(it.id) == NotePriority.URGENT_NOT_IMPORTANT 
        }
        if (urgentNotImportant.isNotEmpty()) {
            views.add(SmartView(
                id = "urgent_not_important",
                title = "⚡ Urgent",
                subtitle = "${urgentNotImportant.size} notes • Delegate",
                notes = urgentNotImportant.sortedByDescending { it.createdAt },
                viewType = ViewType.PRIORITY_BASED,
                priority = ViewPriority.MEDIUM,
                icon = "flash_on"
            ))
        }
        
        return views
    }
    
    /**
     * Generate project-based smart views
     */
    private suspend fun generateProjectBasedViews(notes: List<NoteEntity>): List<SmartView> {
        val projects = identifyProjects(notes)
        val views = mutableListOf<SmartView>()
        
        projects.forEach { project ->
            val projectNotes = project.notes
            
            if (projectNotes.isNotEmpty()) {
                views.add(SmartView(
                    id = "project_${project.id}",
                    title = project.name,
                    subtitle = "${projectNotes.size} notes • ${project.status}",
                    notes = projectNotes.sortedByDescending { it.createdAt },
                    viewType = ViewType.PROJECT_BASED,
                    priority = when (project.status) {
                        ProjectStatus.ACTIVE -> ViewPriority.HIGH
                        ProjectStatus.ON_HOLD -> ViewPriority.MEDIUM
                        else -> ViewPriority.LOW
                    },
                    icon = "work",
                    metadata = mapOf(
                        "status" to project.status.toString(),
                        "progress" to project.progress.toString()
                    )
                ))
            }
        }
        
        return views.sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * Generate sentiment-based smart views
     */
    private suspend fun generateSentimentBasedViews(notes: List<NoteEntity>): List<SmartView> {
        val sentimentAnalysis = analyzeSentiments(notes)
        val views = mutableListOf<SmartView>()
        
        // Positive notes
        val positiveNotes = notes.filter { 
            sentimentAnalysis.getSentiment(it.id).score > 0.3f 
        }
        if (positiveNotes.isNotEmpty()) {
            views.add(SmartView(
                id = "positive",
                title = "😊 Positive",
                subtitle = "${positiveNotes.size} uplifting notes",
                notes = positiveNotes.sortedByDescending { it.createdAt },
                viewType = ViewType.SENTIMENT_BASED,
                priority = ViewPriority.MEDIUM,
                icon = "sentiment_satisfied"
            ))
        }
        
        // Neutral notes
        val neutralNotes = notes.filter { 
            val score = sentimentAnalysis.getSentiment(it.id).score
            score >= -0.3f && score <= 0.3f
        }
        if (neutralNotes.isNotEmpty()) {
            views.add(SmartView(
                id = "neutral",
                title = "📝 Neutral",
                subtitle = "${neutralNotes.size} informational notes",
                notes = neutralNotes.sortedByDescending { it.createdAt },
                viewType = ViewType.SENTIMENT_BASED,
                priority = ViewPriority.LOW,
                icon = "sentiment_neutral"
            ))
        }
        
        // Negative/Concerning notes
        val negativeNotes = notes.filter { 
            sentimentAnalysis.getSentiment(it.id).score < -0.3f 
        }
        if (negativeNotes.isNotEmpty()) {
            views.add(SmartView(
                id = "negative",
                title = "😟 Needs Attention",
                subtitle = "${negativeNotes.size} notes requiring focus",
                notes = negativeNotes.sortedByDescending { it.createdAt },
                viewType = ViewType.SENTIMENT_BASED,
                priority = ViewPriority.HIGH,
                icon = "sentiment_dissatisfied"
            ))
        }
        
        return views.sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * Generate intelligent auto views using multiple paradigms
     */
    private suspend fun generateIntelligentAutoViews(notes: List<NoteEntity>): List<SmartView> {
        val views = mutableListOf<SmartView>()
        
        // Get the most relevant views from each paradigm
        val timeViews = generateTimeBasedViews(notes).take(2)
        val topicViews = generateTopicBasedViews(notes).take(3)
        val priorityViews = generatePriorityBasedViews(notes).take(2)
        
        views.addAll(priorityViews) // Priority first
        views.addAll(timeViews)    // Then time-based
        views.addAll(topicViews)   // Then topics
        
        // Sort by priority and relevance
        return views.sortedWith(compareByDescending<SmartView> { it.priority.ordinal }
            .thenByDescending { it.notes.size })
    }
    
    /**
     * Generate AI suggestions for new views
     */
    private suspend fun generateViewSuggestions(
        notes: List<NoteEntity>, 
        currentViews: List<SmartView>
    ) {
        val suggestions = mutableListOf<ViewSuggestion>()
        
        // Suggest missing paradigms
        val usedParadigms = currentViews.map { it.viewType }.toSet()
        
        if (ViewType.TOPIC_BASED !in usedParadigms && notes.size > 5) {
            suggestions.add(ViewSuggestion(
                id = "suggest_topics",
                title = "Organize by Topics",
                description = "AI detected distinct topics in your notes",
                paradigm = OrganizationParadigm.TOPIC_BASED,
                confidence = 0.8f,
                reason = "Multiple distinct topics identified"
            ))
        }
        
        if (ViewType.PRIORITY_BASED !in usedParadigms) {
            val urgentCount = countUrgentNotes(notes)
            if (urgentCount > 0) {
                suggestions.add(ViewSuggestion(
                    id = "suggest_priority",
                    title = "Focus on Priorities",
                    description = "Found $urgentCount urgent items",
                    paradigm = OrganizationParadigm.PRIORITY_BASED,
                    confidence = 0.9f,
                    reason = "Urgent items detected"
                ))
            }
        }
        
        _viewSuggestions.value = suggestions
    }
    
    /**
     * Apply a suggested view
     */
    suspend fun applySuggestion(suggestion: ViewSuggestion, notes: List<NoteEntity>) {
        val newViews = when (suggestion.paradigm) {
            OrganizationParadigm.TOPIC_BASED -> generateTopicBasedViews(notes)
            OrganizationParadigm.PRIORITY_BASED -> generatePriorityBasedViews(notes)
            OrganizationParadigm.PROJECT_BASED -> generateProjectBasedViews(notes)
            OrganizationParadigm.SENTIMENT_BASED -> generateSentimentBasedViews(notes)
            else -> generateTimeBasedViews(notes)
        }
        
        val currentViews = _activeViews.value.toMutableList()
        currentViews.addAll(newViews)
        _activeViews.value = currentViews.distinctBy { it.id }
        
        // Remove applied suggestion
        _viewSuggestions.value = _viewSuggestions.value.filter { it.id != suggestion.id }
    }
    
    /**
     * Customize a view based on user preferences
     */
    suspend fun customizeView(viewId: String, customization: ViewCustomization) {
        val currentViews = _activeViews.value.toMutableList()
        val viewIndex = currentViews.indexOfFirst { it.id == viewId }
        
        if (viewIndex != -1) {
            val view = currentViews[viewIndex]
            val customizedView = view.copy(
                title = customization.title ?: view.title,
                icon = customization.icon ?: view.icon,
                priority = customization.priority ?: view.priority
            )
            currentViews[viewIndex] = customizedView
            _activeViews.value = currentViews
        }
    }
    
    /**
     * Start intelligent view generation background process
     */
    private fun startIntelligentViewGeneration() {
        scope.launch {
            while (true) {
                delay(300000) // Every 5 minutes
                // Refresh views based on new notes or changed patterns
                // This will be triggered by the main app when notes change
            }
        }
    }
    
    /**
     * Start paradigm optimization background process
     */
    private fun startParadigmOptimization() {
        scope.launch {
            while (true) {
                delay(3600000) // Every hour
                optimizeParadigmSelection()
            }
        }
    }
    
    /**
     * Optimize paradigm selection based on user interaction patterns
     */
    private suspend fun optimizeParadigmSelection() {
        // Analyze which views users interact with most
        // Adjust paradigm preferences accordingly
        // This will learn from user behavior over time
    }
    
    /**
     * Load user preferences for views
     */
    private suspend fun loadUserPreferences() {
        // Load from SharedPreferences or database
        // For now, use defaults
        _userViewPreferences.value = UserViewPreferences()
    }
    
    // Helper functions for time-based organization
    private fun isFromToday(timestamp: Long, now: LocalDateTime): Boolean {
        val noteTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
        return noteTime.toLocalDate() == now.toLocalDate()
    }
    
    private fun isFromThisWeek(timestamp: Long, now: LocalDateTime): Boolean {
        val noteTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
        val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1)
        return noteTime.isAfter(weekStart.toLocalDate().atStartOfDay())
    }
    
    private fun isFromLastMonth(timestamp: Long, now: LocalDateTime): Boolean {
        val noteTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
        val monthAgo = now.minusDays(30)
        return noteTime.isAfter(monthAgo)
    }
    
    /**
     * Analyze priorities of notes
     */
    private suspend fun analyzePriorities(notes: List<NoteEntity>): PriorityAnalysis {
        return withContext(Dispatchers.Default) {
            val priorities = mutableMapOf<Long, NotePriority>()
            
            notes.forEach { note ->
                val content = note.content.lowercase()
                val title = note.title.lowercase()
                val text = "$title $content"
                
                val priority = when {
                    text.contains("urgent") || text.contains("asap") || text.contains("emergency") -> 
                        NotePriority.URGENT_IMPORTANT
                    text.contains("important") || text.contains("critical") -> 
                        NotePriority.IMPORTANT_NOT_URGENT
                    text.contains("later") || text.contains("someday") -> 
                        NotePriority.NOT_URGENT_NOT_IMPORTANT
                    else -> NotePriority.URGENT_NOT_IMPORTANT
                }
                
                priorities[note.id] = priority
            }
            
            PriorityAnalysis(priorities)
        }
    }
    
    /**
     * Identify projects from notes
     */
    private suspend fun identifyProjects(notes: List<NoteEntity>): List<Project> {
        return withContext(Dispatchers.Default) {
            val projects = mutableListOf<Project>()
            val projectKeywords = mapOf(
                "work" to listOf("meeting", "project", "deadline", "client", "task"),
                "personal" to listOf("family", "home", "personal", "hobby"),
                "study" to listOf("learn", "study", "course", "book", "research"),
                "health" to listOf("doctor", "exercise", "diet", "health", "fitness")
            )
            
            projectKeywords.forEach { (projectName, keywords) ->
                val relatedNotes = notes.filter { note ->
                    val text = "${note.title} ${note.content}".lowercase()
                    keywords.any { keyword -> text.contains(keyword) }
                }
                
                if (relatedNotes.isNotEmpty()) {
                    projects.add(Project(
                        id = projectName,
                        name = projectName.capitalize(),
                        status = ProjectStatus.ACTIVE,
                        notes = relatedNotes,
                        progress = calculateProgress(relatedNotes)
                    ))
                }
            }
            
            projects
        }
    }
    
    /**
     * Analyze sentiments of notes
     */
    private suspend fun analyzeSentiments(notes: List<NoteEntity>): SentimentAnalysis {
        return withContext(Dispatchers.Default) {
            val sentiments = mutableMapOf<Long, SentimentScore>()
            
            notes.forEach { note ->
                val sentiment = calculateSentiment(note.content)
                sentiments[note.id] = sentiment
            }
            
            SentimentAnalysis(sentiments)
        }
    }
    
    /**
     * Count urgent notes
     */
    private suspend fun countUrgentNotes(notes: List<NoteEntity>): Int {
        return notes.count { note ->
            val text = "${note.title} ${note.content}".lowercase()
            text.contains("urgent") || text.contains("asap") || text.contains("emergency")
        }
    }
    
    private fun calculateProgress(notes: List<NoteEntity>): Float {
        val completedNotes = notes.count { note ->
            val text = "${note.title} ${note.content}".lowercase()
            text.contains("done") || text.contains("completed") || text.contains("finished")
        }
        return if (notes.isNotEmpty()) completedNotes.toFloat() / notes.size else 0f
    }
    
    private fun calculateSentiment(content: String): SentimentScore {
        val positiveWords = listOf("good", "great", "excellent", "happy", "love", "amazing")
        val negativeWords = listOf("bad", "terrible", "sad", "hate", "awful", "disappointed")
        
        val words = content.lowercase().split("\\s+".toRegex())
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        
        val score = when {
            positiveCount > negativeCount -> 0.7f
            negativeCount > positiveCount -> -0.7f
            else -> 0.0f
        }
        
        return SentimentScore(score, (positiveCount + negativeCount).toFloat() / words.size)
    }
    
    /**
     * Analyze notes for optimal organization
     */
    private suspend fun analyzeNotesForOptimalOrganization(notes: List<NoteEntity>): OrganizationAnalysis {
        return withContext(Dispatchers.Default) {
            val hasStrongTemporalPatterns = notes.groupBy { 
                LocalDateTime.ofEpochSecond(it.createdAt / 1000, 0, java.time.ZoneOffset.UTC).dayOfYear 
            }.size > notes.size * 0.3
            
            val hasDistinctTopics = notes.map { "${it.title} ${it.content}" }
                .flatMap { it.split("\\s+".toRegex()) }
                .groupBy { it.lowercase() }
                .filter { it.value.size > 1 }
                .size > 10
            
            val hasUrgentItems = notes.any { note ->
                val text = "${note.title} ${note.content}".lowercase()
                text.contains("urgent") || text.contains("asap") || text.contains("emergency")
            }
            
            val hasProjectStructure = notes.any { note ->
                val text = "${note.title} ${note.content}".lowercase()
                text.contains("project") || text.contains("task") || text.contains("milestone")
            }
            
            val hasEmotionalVariance = notes.map { calculateSentiment(it.content).score }
                .let { scores -> scores.maxOrNull()!! - scores.minOrNull()!! > 1.0f }
            
            OrganizationAnalysis(
                hasStrongTemporalPatterns = hasStrongTemporalPatterns,
                hasDistinctTopics = hasDistinctTopics,
                hasUrgentItems = hasUrgentItems,
                hasProjectStructure = hasProjectStructure,
                hasEmotionalVariance = hasEmotionalVariance,
                recommendedParadigm = when {
                    hasStrongTemporalPatterns -> OrganizationParadigm.TIME_BASED
                    hasDistinctTopics -> OrganizationParadigm.TOPIC_BASED
                    hasUrgentItems -> OrganizationParadigm.PRIORITY_BASED
                    hasProjectStructure -> OrganizationParadigm.PROJECT_BASED
                    else -> OrganizationParadigm.INTELLIGENT_AUTO
                },
                confidence = listOf(
                    hasStrongTemporalPatterns,
                    hasDistinctTopics,
                    hasUrgentItems,
                    hasProjectStructure,
                    hasEmotionalVariance
                ).count { it }.toFloat() / 5.0f
            )
        }
    }
    
    /**
     * Perform topic modeling on notes
     */
    private suspend fun performTopicModeling(notes: List<NoteEntity>): TopicAnalysis {
        return withContext(Dispatchers.Default) {
            val allWords = notes.flatMap { note ->
                "${note.title} ${note.content}".lowercase()
                    .split("\\s+".toRegex())
                    .filter { it.length > 3 }
            }
            
            val wordFreq = allWords.groupBy { it }.mapValues { it.value.size }
            val topWords = wordFreq.entries.sortedByDescending { it.value }.take(10)
            
            val topics = topWords.mapIndexed { index, (word, freq) ->
                TopicInfo(
                    id = "topic_$index",
                    name = word.capitalize(),
                    keywords = listOf(word),
                    confidence = freq.toFloat() / allWords.size,
                    suggestedIcon = "topic"
                )
            }
            
            TopicAnalysis(topics)
        }
    }
}

// Helper data classes
data class TopicAnalysis(val topics: List<TopicInfo>)

// Helper data classes
data class PriorityAnalysis(private val priorities: Map<Long, NotePriority>) {
    fun getPriority(noteId: Long): NotePriority = priorities[noteId] ?: NotePriority.URGENT_NOT_IMPORTANT
}

data class Project(
    val id: String,
    val name: String,
    val status: ProjectStatus,
    val notes: List<NoteEntity>,
    val progress: Float
)

enum class ProjectStatus { ACTIVE, COMPLETED, ON_HOLD }

data class SentimentAnalysis(private val sentiments: Map<Long, SentimentScore>) {
    fun getSentiment(noteId: Long): SentimentScore = sentiments[noteId] ?: SentimentScore(0f, 0f)
}

data class SentimentScore(val score: Float, val confidence: Float)


