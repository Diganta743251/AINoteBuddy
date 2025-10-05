package com.ainotebuddy.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced action item and task extraction system
 */
@Singleton
class ActionItemExtractor @Inject constructor() {
    
    // Action indicators
    private val actionVerbs = setOf(
        "do", "make", "create", "build", "write", "send", "call", "email", "meet", "schedule",
        "plan", "organize", "prepare", "review", "check", "verify", "confirm", "update",
        "complete", "finish", "submit", "deliver", "implement", "develop", "design",
        "research", "analyze", "investigate", "discuss", "present", "demonstrate",
        "buy", "purchase", "order", "book", "reserve", "contact", "reach", "follow",
        "remember", "remind", "note", "record", "document", "track", "monitor"
    )
    
    private val taskIndicators = setOf(
        "todo", "task", "action", "item", "need to", "have to", "must", "should",
        "required", "necessary", "important", "urgent", "deadline", "due", "by",
        "before", "until", "assignment", "responsibility", "commitment", "promise"
    )
    
    private val priorityIndicators = mapOf(
        ActionPriority.URGENT to listOf("urgent", "asap", "immediately", "critical", "emergency", "now"),
        ActionPriority.HIGH to listOf("important", "high priority", "crucial", "vital", "essential", "key"),
        ActionPriority.MEDIUM to listOf("medium", "normal", "regular", "standard", "typical"),
        ActionPriority.LOW to listOf("low priority", "when possible", "eventually", "someday", "nice to have")
    )
    
    // Date patterns
    private val datePatterns = listOf(
        Pattern.compile("\\b(today|tomorrow|yesterday)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{2,4})\\b"),
        Pattern.compile("\\b(\\d{1,2})-(\\d{1,2})-(\\d{2,4})\\b"),
        Pattern.compile("\\bdue\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bby\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bbefore\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bin\\s+(\\d+)\\s+(days?|weeks?|months?)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bnext\\s+(week|month|year)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bthis\\s+(week|month|year)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    // Time patterns
    private val timePatterns = listOf(
        Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\s*(am|pm)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(\\d{1,2})\\s*(am|pm)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bat\\s+(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?\\b", Pattern.CASE_INSENSITIVE)
    )
    
    /**
     * Extract action items from text content
     */
    suspend fun extractActionItems(content: String): List<ActionItem> = withContext(Dispatchers.IO) {
        if (content.isBlank()) {
            return@withContext emptyList()
        }
        
        val sentences = splitIntoSentences(content)
        val actionItems = mutableListOf<ActionItem>()
        
        sentences.forEach { sentence ->
            val items = extractFromSentence(sentence)
            actionItems.addAll(items)
        }
        
        // Remove duplicates and sort by priority
        actionItems.distinctBy { it.text.lowercase() }
            .sortedWith(compareByDescending<ActionItem> { it.priority.ordinal }
                .thenByDescending { it.confidence })
    }
    
    /**
     * Extract action items from multiple texts in batch
     */
    suspend fun batchExtractActionItems(contents: List<String>): List<List<ActionItem>> = withContext(Dispatchers.IO) {
        contents.map { content ->
            extractActionItems(content)
        }
    }
    
    private fun splitIntoSentences(content: String): List<String> {
        return content.split(Regex("[.!?\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 10 }
    }
    
    private fun extractFromSentence(sentence: String): List<ActionItem> {
        val actionItems = mutableListOf<ActionItem>()
        
        // Check for explicit task markers
        if (containsTaskMarkers(sentence)) {
            val item = createActionItem(sentence)
            if (item != null) {
                actionItems.add(item)
            }
        }
        
        // Check for imperative sentences (commands)
        if (isImperativeSentence(sentence)) {
            val item = createActionItem(sentence)
            if (item != null) {
                actionItems.add(item)
            }
        }
        
        // Check for checkbox patterns
        val checkboxItems = extractCheckboxItems(sentence)
        actionItems.addAll(checkboxItems)
        
        // Check for bullet point actions
        val bulletItems = extractBulletItems(sentence)
        actionItems.addAll(bulletItems)
        
        return actionItems
    }
    
    private fun containsTaskMarkers(sentence: String): Boolean {
        val lowerSentence = sentence.lowercase()
        return taskIndicators.any { indicator ->
            lowerSentence.contains(indicator)
        }
    }
    
    private fun isImperativeSentence(sentence: String): Boolean {
        val words = sentence.trim().split("\\s+".toRegex())
        if (words.isEmpty()) return false
        
        val firstWord = words[0].lowercase().replace(Regex("[^a-z]"), "")
        
        // Check if starts with action verb
        return actionVerbs.contains(firstWord) ||
               // Check for "need to", "have to" patterns
               sentence.lowercase().matches(Regex(".*\\b(need to|have to|must|should)\\s+\\w+.*"))
    }
    
    private fun extractCheckboxItems(sentence: String): List<ActionItem> {
        val checkboxPattern = Pattern.compile("\\[\\s*[x\\s]?\\s*\\]\\s*(.+)", Pattern.CASE_INSENSITIVE)
        val matcher = checkboxPattern.matcher(sentence)
        val items = mutableListOf<ActionItem>()
        
        while (matcher.find()) {
            val itemText = matcher.group(1)?.trim()
            if (!itemText.isNullOrBlank()) {
                val item = createActionItem(itemText, isCheckbox = true)
                if (item != null) {
                    items.add(item)
                }
            }
        }
        
        return items
    }
    
    private fun extractBulletItems(sentence: String): List<ActionItem> {
        val bulletPattern = Pattern.compile("^\\s*[-*•]\\s*(.+)", Pattern.MULTILINE)
        val matcher = bulletPattern.matcher(sentence)
        val items = mutableListOf<ActionItem>()
        
        while (matcher.find()) {
            val itemText = matcher.group(1)?.trim()
            if (!itemText.isNullOrBlank() && containsActionIndicators(itemText)) {
                val item = createActionItem(itemText, isBullet = true)
                if (item != null) {
                    items.add(item)
                }
            }
        }
        
        return items
    }
    
    private fun containsActionIndicators(text: String): Boolean {
        val lowerText = text.lowercase()
        return actionVerbs.any { verb -> lowerText.contains(verb) } ||
               taskIndicators.any { indicator -> lowerText.contains(indicator) }
    }
    
    private fun createActionItem(
        text: String, 
        isCheckbox: Boolean = false, 
        isBullet: Boolean = false
    ): ActionItem? {
        val cleanText = cleanActionText(text)
        if (cleanText.length < 5) return null
        
        val priority = extractPriority(text)
        val dueDate = extractDueDate(text)
        val confidence = calculateConfidence(text, isCheckbox, isBullet, priority, dueDate)
        
        return ActionItem(
            text = cleanText,
            priority = priority,
            dueDate = dueDate,
            confidence = confidence,
            context = extractContext(text)
        )
    }
    
    private fun cleanActionText(text: String): String {
        return text
            .replace(Regex("\\[\\s*[x\\s]?\\s*\\]"), "") // Remove checkbox
            .replace(Regex("^\\s*[-*•]\\s*"), "") // Remove bullet
            .replace(Regex("(?i)\\b(todo|task|action item):?\\s*"), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    
    private fun extractPriority(text: String): ActionPriority {
        val lowerText = text.lowercase()
        
        priorityIndicators.forEach { (priority, indicators) ->
            if (indicators.any { indicator -> lowerText.contains(indicator) }) {
                return priority
            }
        }
        
        // Default priority based on urgency words
        return when {
            lowerText.contains(Regex("\\b(urgent|asap|immediately|critical)\\b")) -> ActionPriority.URGENT
            lowerText.contains(Regex("\\b(important|high|crucial|vital)\\b")) -> ActionPriority.HIGH
            lowerText.contains(Regex("\\b(low|later|someday|eventually)\\b")) -> ActionPriority.LOW
            else -> ActionPriority.MEDIUM
        }
    }
    
    private fun extractDueDate(text: String): Long? {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Try each date pattern
        datePatterns.forEach { pattern ->
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val dateString = matcher.group().lowercase()
                
                return when {
                    dateString.contains("today") -> {
                        calendar.timeInMillis = now
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.timeInMillis
                    }
                    dateString.contains("tomorrow") -> {
                        calendar.timeInMillis = now + (24 * 60 * 60 * 1000)
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.timeInMillis
                    }
                    dateString.contains("next week") -> {
                        now + (7 * 24 * 60 * 60 * 1000)
                    }
                    dateString.contains("this week") -> {
                        calendar.timeInMillis = now
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        calendar.timeInMillis
                    }
                    dateString.matches(Regex("\\d+/\\d+/\\d+")) -> {
                        parseExplicitDate(dateString)
                    }
                    dateString.matches(Regex("in \\d+ (days?|weeks?|months?)")) -> {
                        parseRelativeDate(dateString, now)
                    }
                    else -> {
                        parseDayOfWeek(dateString, now)
                    }
                }
            }
        }
        
        return null
    }
    
    private fun parseExplicitDate(dateString: String): Long? {
        val formats = listOf(
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yy", Locale.getDefault()),
            SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()),
            SimpleDateFormat("MM-dd-yy", Locale.getDefault())
        )
        
        formats.forEach { format ->
            try {
                return format.parse(dateString)?.time
            } catch (e: Exception) {
                // Continue to next format
            }
        }
        
        return null
    }
    
    private fun parseRelativeDate(dateString: String, baseTime: Long): Long {
        val pattern = Pattern.compile("in (\\d+) (days?|weeks?|months?)")
        val matcher = pattern.matcher(dateString)
        
        if (matcher.find()) {
            val amount = matcher.group(1)?.toIntOrNull() ?: return baseTime
            val unit = matcher.group(2)?.lowercase() ?: return baseTime
            
            val multiplier = when {
                unit.startsWith("day") -> 24 * 60 * 60 * 1000L
                unit.startsWith("week") -> 7 * 24 * 60 * 60 * 1000L
                unit.startsWith("month") -> 30 * 24 * 60 * 60 * 1000L
                else -> 0L
            }
            
            return baseTime + (amount * multiplier)
        }
        
        return baseTime
    }
    
    private fun parseDayOfWeek(dateString: String, baseTime: Long): Long? {
        val daysOfWeek = mapOf(
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )
        
        daysOfWeek.forEach { (day, calendarDay) ->
            if (dateString.contains(day)) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = baseTime
                
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                val daysUntilTarget = (calendarDay - currentDay + 7) % 7
                
                if (daysUntilTarget == 0) {
                    calendar.add(Calendar.DAY_OF_YEAR, 7) // Next week
                } else {
                    calendar.add(Calendar.DAY_OF_YEAR, daysUntilTarget)
                }
                
                return calendar.timeInMillis
            }
        }
        
        return null
    }
    
    private fun calculateConfidence(
        text: String,
        isCheckbox: Boolean,
        isBullet: Boolean,
        priority: ActionPriority,
        dueDate: Long?
    ): Float {
        var confidence = 0.5f // Base confidence
        
        // Boost for explicit markers
        if (isCheckbox) confidence += 0.3f
        if (isBullet) confidence += 0.2f
        
        // Boost for action verbs
        val lowerText = text.lowercase()
        val actionVerbCount = actionVerbs.count { verb -> lowerText.contains(verb) }
        confidence += min(actionVerbCount * 0.1f, 0.3f)
        
        // Boost for task indicators
        val taskIndicatorCount = taskIndicators.count { indicator -> lowerText.contains(indicator) }
        confidence += min(taskIndicatorCount * 0.1f, 0.2f)
        
        // Boost for priority indicators
        if (priority != ActionPriority.MEDIUM) confidence += 0.1f
        
        // Boost for due dates
        if (dueDate != null) confidence += 0.2f
        
        // Boost for imperative form
        if (startsWithActionVerb(text)) confidence += 0.2f
        
        // Penalize very short or very long text
        when {
            text.length < 10 -> confidence *= 0.7f
            text.length > 200 -> confidence *= 0.8f
        }
        
        return min(confidence, 1f)
    }
    
    private fun startsWithActionVerb(text: String): Boolean {
        val firstWord = text.trim().split("\\s+".toRegex()).firstOrNull()?.lowercase()
        return firstWord != null && actionVerbs.contains(firstWord)
    }
    
    private fun extractContext(text: String): String {
        // Extract context clues like project names, people, locations
        val contextPattern = Pattern.compile("\\b(project|meeting|with|at|for|about)\\s+(\\w+(?:\\s+\\w+)?)", Pattern.CASE_INSENSITIVE)
        val matcher = contextPattern.matcher(text)
        val contexts = mutableListOf<String>()
        
        while (matcher.find()) {
            contexts.add(matcher.group())
        }
        
        return contexts.joinToString(", ")
    }
    
    /**
     * Analyze action item patterns and trends
     */
    fun analyzeActionPatterns(actionItems: List<Pair<Long, List<ActionItem>>>): ActionPatterns {
        val allItems = actionItems.flatMap { it.second }
        
        val priorityDistribution = allItems.groupingBy { it.priority }.eachCount()
        val completionRate = calculateCompletionRate(actionItems)
        val averageItemsPerNote = allItems.size.toFloat() / actionItems.size
        
        val timePatterns = analyzeTimePatterns(actionItems)
        val urgencyTrends = analyzeUrgencyTrends(actionItems)
        
        return ActionPatterns(
            priorityDistribution = priorityDistribution,
            completionRate = completionRate,
            averageItemsPerNote = averageItemsPerNote,
            timePatterns = timePatterns,
            urgencyTrends = urgencyTrends,
            commonActionTypes = findCommonActionTypes(allItems)
        )
    }
    
    private fun calculateCompletionRate(actionItems: List<Pair<Long, List<ActionItem>>>): Float {
        // This would require tracking completion status
        // For now, return estimated completion rate based on due dates
        val itemsWithDueDates = actionItems.flatMap { it.second }.count { it.dueDate != null }
        val totalItems = actionItems.flatMap { it.second }.size
        
        return if (totalItems > 0) itemsWithDueDates.toFloat() / totalItems else 0f
    }
    
    private fun analyzeTimePatterns(actionItems: List<Pair<Long, List<ActionItem>>>): Map<String, Int> {
        val patterns = mutableMapOf<String, Int>()
        
        actionItems.forEach { (timestamp, items) ->
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
            
            val timeSlot = when (hour) {
                in 6..11 -> "Morning"
                in 12..17 -> "Afternoon"
                in 18..21 -> "Evening"
                else -> "Night"
            }
            
            patterns[timeSlot] = patterns.getOrDefault(timeSlot, 0) + items.size
            patterns[dayOfWeek] = patterns.getOrDefault(dayOfWeek, 0) + items.size
        }
        
        return patterns
    }
    
    private fun analyzeUrgencyTrends(actionItems: List<Pair<Long, List<ActionItem>>>): List<UrgencyDataPoint> {
        return actionItems.map { (timestamp, items) ->
            val urgentCount = items.count { it.priority == ActionPriority.URGENT }
            val highCount = items.count { it.priority == ActionPriority.HIGH }
            val urgencyScore = (urgentCount * 2 + highCount).toFloat() / items.size
            
            UrgencyDataPoint(timestamp, urgencyScore, items.size)
        }.sortedBy { it.timestamp }
    }
    
    private fun findCommonActionTypes(actionItems: List<ActionItem>): List<ActionType> {
        val actionTypes = mutableMapOf<String, Int>()
        
        actionItems.forEach { item ->
            val firstWord = item.text.split(" ").firstOrNull()?.lowercase()
            if (firstWord != null && actionVerbs.contains(firstWord)) {
                actionTypes[firstWord] = actionTypes.getOrDefault(firstWord, 0) + 1
            }
        }
        
        return actionTypes.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { ActionType(it.key, it.value) }
    }
}

// Data classes for action item analysis
data class ActionPatterns(
    val priorityDistribution: Map<ActionPriority, Int>,
    val completionRate: Float,
    val averageItemsPerNote: Float,
    val timePatterns: Map<String, Int>,
    val urgencyTrends: List<UrgencyDataPoint>,
    val commonActionTypes: List<ActionType>
)

data class UrgencyDataPoint(
    val timestamp: Long,
    val urgencyScore: Float,
    val totalItems: Int
)

data class ActionType(
    val action: String,
    val frequency: Int
)