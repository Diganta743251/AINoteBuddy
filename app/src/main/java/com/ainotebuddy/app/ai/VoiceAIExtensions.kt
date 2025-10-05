package com.ainotebuddy.app.ai

import com.ainotebuddy.app.voice.*
import kotlinx.coroutines.*
import java.io.File

/**
 * Extensions to the AI Analysis Engine for voice-specific processing
 * Integrates voice capabilities with existing AI systems
 */

/**
 * Analyze voice note with comprehensive AI processing
 */
suspend fun AIAnalysisEngine.analyzeVoiceNote(
    transcription: String,
    audioFile: File?,
    context: Any? // simplified placeholder
): String = withContext(Dispatchers.IO) {
    // Simplified implementation to avoid referencing missing models
    val topics: List<TopicResult> = extractTopicsFromQuery(transcription)
    val sentiment = SentimentAnalyzer().analyzeSentiment(transcription)
    val actions = VoiceHeuristics.performQuickActionItemDetection(transcription)
    val summary = transcription.lines().firstOrNull()?.take(120) ?: transcription.take(120)
    // Return a simple JSON-like string with key info for now
    "title=" + (transcription.take(30)) + "...; topics=" + topics.joinToString() + 
        "; sentiment=" + sentiment.sentiment.name + "; actions=" + actions.size + 
        "; summary=" + summary
}

/**
 * Analyze voice command with AI understanding
 */
suspend fun AIAnalysisEngine.analyzeVoiceCommand(command: String): String = withContext(Dispatchers.IO) {
    
    // Extract intent using AI
    val intent = extractVoiceCommandIntent(command)
    
    // Extract parameters
    val parameters = extractCommandParameters(command, intent)
    
    // Extract main content
    val extractedContent = extractCommandContent(command, intent)
    
    // Analyze confidence
    val confidence = calculateCommandConfidence(command, intent, parameters)
    
    // Create context
    // Simple string result for now
    "intent=" + intent + "; confidence=" + confidence + "; params=" + parameters.entries.joinToString { it.key + ":" + it.value } + 
        "; content=" + extractedContent
}

/**
 * Correct transcription using AI language models
 */
suspend fun AIAnalysisEngine.correctTranscription(transcription: String): String = withContext(Dispatchers.IO) {
    
    // Apply grammar correction
    val grammarCorrected = applyGrammarCorrection(transcription)
    
    // Apply context-aware corrections
    val contextCorrected = applyContextualCorrections(grammarCorrected)
    
    // Apply domain-specific corrections
    val domainCorrected = applyDomainCorrections(contextCorrected)
    
    // Apply personalized corrections based on user patterns
    val personalizedCorrected = applyPersonalizedCorrections(domainCorrected)
    
    personalizedCorrected
}

/**
 * Perform quick analysis for real-time processing
 */
suspend fun AIAnalysisEngine.performQuickAnalysis(partialTranscription: String): com.ainotebuddy.app.voice.VoiceQuickInsight = withContext(Dispatchers.IO) {
    
    // Quick topic detection
    val quickTopics: List<TopicResult> = VoiceHeuristics.performQuickTopicDetection(partialTranscription)
    if (quickTopics.isNotEmpty()) {
        val firstTopic = quickTopics[0]
        return@withContext com.ainotebuddy.app.voice.VoiceQuickInsight(
            type = com.ainotebuddy.app.voice.VoiceQuickInsightType.TOPIC_DETECTED,
            content = "Topic detected: ${firstTopic.topic}",
            confidence = firstTopic.confidence,
            timestamp = System.currentTimeMillis()
        )
    }
    
    // Quick action item detection
    val quickActionItems = VoiceHeuristics.performQuickActionItemDetection(partialTranscription)
    if (quickActionItems.isNotEmpty()) {
        val firstAction = quickActionItems[0]
        return@withContext com.ainotebuddy.app.voice.VoiceQuickInsight(
            type = com.ainotebuddy.app.voice.VoiceQuickInsightType.ACTION_ITEM_FOUND,
            content = "Action item: ${firstAction.text}",
            confidence = 0.8f,
            timestamp = System.currentTimeMillis()
        )
    }
    
    // Quick sentiment analysis
    val quickSentiment = performQuickSentimentAnalysis(partialTranscription)
    if (quickSentiment.sentiment != Sentiment.NEUTRAL) {
        return@withContext com.ainotebuddy.app.voice.VoiceQuickInsight(
            type = com.ainotebuddy.app.voice.VoiceQuickInsightType.SENTIMENT_CHANGE,
            content = "Sentiment: ${if (quickSentiment.sentiment == Sentiment.POSITIVE) "positive" else "negative"}",
            confidence = quickSentiment.confidence,
            timestamp = System.currentTimeMillis()
        )
    }
    
    // Default result
    com.ainotebuddy.app.voice.VoiceQuickInsight(
        type = com.ainotebuddy.app.voice.VoiceQuickInsightType.IMPORTANT_POINT,
        content = "Processing...",
        confidence = 0.5f,
        timestamp = System.currentTimeMillis()
    )
}

/**
 * Analyze audio content with AI
 */
// Minimal models moved to VoiceModelsMinimal.kt

suspend fun AIAnalysisEngine.analyzeAudioContent(
    transcription: String,
    recording: ByteArray?
): BasicAudioAnalysis = withContext(Dispatchers.IO) {
    // Analyze transcription content with simple helpers
    val topics: List<TopicResult> = extractTopicsFromQuery(transcription)
    val entities = extractEntitiesFromQuery(transcription) // currently unused in BasicAudioAnalysis but useful for future
    val sentiment = SentimentAnalyzer().analyzeSentiment(transcription)
    val actionItems = VoiceHeuristics.performQuickActionItemDetection(transcription)

    val quickInsight = com.ainotebuddy.app.voice.VoiceQuickInsight(
        type = com.ainotebuddy.app.voice.VoiceQuickInsightType.IMPORTANT_POINT,
        content = "Processed ${transcription.length.coerceAtMost(60)} chars",
        confidence = 0.6f
    )

    val confidence = (0.4f + (topics.size * 0.1f) + (actionItems.size * 0.1f)).coerceIn(0f, 1f)

    BasicAudioAnalysis(
        summary = if (transcription.isBlank()) "Empty audio transcription" else "Audio processed successfully.",
        quickInsight = quickInsight,
        topics = topics,
        actionItems = actionItems,
        sentiment = sentiment,
        confidence = confidence
    )
}

/**
 * Analyze voice patterns for comprehensive insights
 */
// Minimal replacement for pattern analysis

suspend fun AIAnalysisEngine.analyzeVoicePatterns(
    transcriptions: List<String>,
    recordings: List<ByteArray?>,
    timeframeMs: Long
): BasicVoicePattern = withContext(Dispatchers.IO) {
    // Simple heuristics as placeholders
    val avgLength = transcriptions.map { it.length }.average()
    val pace = when {
        avgLength < 20 -> "Fast"
        avgLength < 80 -> "Moderate"
        else -> "Slow"
    }
    val clarity = if (transcriptions.any { it.contains("um", ignoreCase = true) || it.contains("uh", ignoreCase = true) }) "Fair" else "Clear"
    val fillerCount = transcriptions.sumOf { text ->
        Regex("\\b(um|uh)\\b", RegexOption.IGNORE_CASE).findAll(text).count()
    }
    BasicVoicePattern(pace = pace, clarity = clarity, fillerWordCount = fillerCount)
}

/**
 * Extract topics from query for voice search
 */
suspend fun AIAnalysisEngine.extractTopicsFromQuery(query: String): List<TopicResult> = withContext(Dispatchers.IO) {
    // Simple heuristic topic modeler: top repeated words > 4 chars
    query
        .split("\\s+".toRegex())
        .filter { it.length > 4 }
        .groupBy { it.lowercase() }
        .entries
        .sortedByDescending { it.value.size }
        .take(3)
        .map { TopicResult(it.key, (it.value.size.toFloat() / 10f).coerceIn(0f, 1f)) }
}

/**
 * Extract entities from query for voice search
 */
suspend fun AIAnalysisEngine.extractEntitiesFromQuery(query: String): List<EntityResult> = withContext(Dispatchers.IO) {
    // Minimal entity recognizer: detect simple patterns
    val emailRegex = "[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+".toRegex()
    val urlRegex = "https?://\\S+".toRegex()
    val phoneRegex = "\\b(\\+?\\d[\\d -]{7,}\\d)\\b".toRegex()

    val emails = emailRegex.findAll(query).map {
        EntityResult(it.value, EntityType.EMAIL, 0.9f, 0.5f, query)
    }
    val urls = urlRegex.findAll(query).map {
        EntityResult(it.value, EntityType.URL, 0.9f, 0.4f, query)
    }
    val phones = phoneRegex.findAll(query).map {
        EntityResult(it.value, EntityType.PHONE, 0.85f, 0.4f, query)
    }

    (emails + urls + phones).toList()
}

/**
 * Enhance note content from voice input
 */
suspend fun AIAnalysisEngine.enhanceNoteContent(content: String): EnhancedNoteContent = withContext(Dispatchers.IO) {
    val topics: List<TopicResult> = extractTopicsFromQuery(content)
    val title = (topics.firstOrNull()?.topic ?: content.lines().firstOrNull()?.take(40) ?: "Voice Note")
    val enhancedContent = enhanceTranscription(content, null)
    val tags = topics.map { it.topic }.take(5)
    val category = tags.firstOrNull() ?: "General"
    EnhancedNoteContent(
        title = title,
        content = enhancedContent,
        tags = tags,
        category = category
    )
}

// Helper functions for voice AI processing

private fun createNoteFromTranscription(transcription: String): com.ainotebuddy.app.data.Note {
    return com.ainotebuddy.app.data.Note(
        id = 0L,
        title = "Voice Note",
        content = transcription,
        category = "Voice",
        tags = emptyList(),
        dateCreated = System.currentTimeMillis(),
        dateModified = System.currentTimeMillis(),
        isPinned = false
    )
}

private suspend fun adjustSentimentForVoice(
    sentiment: SentimentResult,
    audioFile: File?
): SentimentResult {
    // Voice often has different sentiment patterns than text
    // Adjust based on audio characteristics if available
    return sentiment.copy(
        confidence = sentiment.confidence * 0.9f // Slightly lower confidence for voice
    )
}

private fun adjustActionItemsForUrgency(
    actionItems: List<ActionItem>,
    urgency: VoiceUrgency?
): List<ActionItem> {
    return when (urgency) {
        VoiceUrgency.HIGH -> actionItems.map { it.copy(priority = ActionPriority.HIGH) }
        VoiceUrgency.MEDIUM -> actionItems.map { it.copy(priority = ActionPriority.MEDIUM) }
        VoiceUrgency.LOW, null -> actionItems
    }
}

private suspend fun generateStructuredVoiceContent(
    transcription: String,
    topics: List<TopicResult>,
    entities: List<EntityResult>,
    actionItems: List<ActionItem>
): String {
    
    var structured = transcription
    
    // Add topic headers if multiple topics detected
    if (topics.size > 1) {
        val topicSections = organizeContentByTopics(transcription, topics)
        structured = topicSections.joinToString("\n\n") { section ->
            "## ${section.topic}\n${section.content}"
        }
    }
    
    // Add action items section if any found
    if (actionItems.isNotEmpty()) {
        structured += "\n\n## Action Items\n"
        structured += actionItems.joinToString("\n") { "- ${it.text}" }
    }
    
    // Add people mentioned section if entities found
    val people = entities.filter { it.type == EntityType.PERSON }
    if (people.isNotEmpty()) {
        structured += "\n\n## People Mentioned\n"
        structured += people.joinToString(", ") { it.text }
    }
    
    return structured
}

private suspend fun generateVoiceNoteTitle(
    transcription: String,
    topics: List<TopicResult>,
    context: VoiceContext?
): String {
    
    // Use context topic if available
    context?.topic?.let { return it }
    
    // Use dominant topic
    topics.firstOrNull()?.let { return it.topic }
    
    // Generate from first sentence
    val firstSentence = transcription.split(".").firstOrNull()?.trim()
    if (!firstSentence.isNullOrEmpty() && firstSentence.length < 50) {
        return firstSentence
    }
    
    // Generate from first few words
    val words = transcription.split(" ").take(5)
    return words.joinToString(" ") + if (transcription.split(" ").size > 5) "..." else ""
}

private fun generateVoiceNoteTags(
    topics: List<TopicResult>,
    entities: List<EntityResult>,
    context: VoiceContext?
): List<String> {
    
    val tags = mutableListOf<String>()
    
    // Add topic tags
    tags.addAll(topics.take(3).map { it.topic })
    
    // Add entity tags
    tags.addAll(entities.take(2).map { it.text })
    
    // Add context tags
    context?.let { ctx ->
        ctx.location?.let { tags.add("location:$it") }
        if (ctx.participants.isNotEmpty()) {
            tags.add("meeting")
        }
        tags.add("urgency:${ctx.urgency.name.lowercase()}")
    }
    
    // Add voice-specific tag
    tags.add("voice-note")
    
    return tags.distinct().take(5)
}

private fun determineVoiceNoteCategory(
    topics: List<TopicResult>,
    context: VoiceContext?
): String {
    
    // Use context-based category
    context?.let { ctx ->
        if (ctx.participants.isNotEmpty()) return "Meetings"
        ctx.topic?.let { topic ->
            return when {
                topic.contains("work", ignoreCase = true) -> "Work"
                topic.contains("personal", ignoreCase = true) -> "Personal"
                topic.contains("idea", ignoreCase = true) -> "Ideas"
                else -> "General"
            }
        }
    }
    
    // Use topic-based category
    topics.firstOrNull()?.let { topic ->
        return when {
            topic.topic.contains("meeting", ignoreCase = true) -> "Meetings"
            topic.topic.contains("idea", ignoreCase = true) -> "Ideas"
            topic.topic.contains("task", ignoreCase = true) -> "Tasks"
            topic.topic.contains("work", ignoreCase = true) -> "Work"
            else -> "General"
        }
    }
    
    return "Voice Notes"
}

private suspend fun enhanceTranscription(transcription: String, audioFile: File?): String {
    // Apply various enhancements to improve transcription quality
    var enhanced = transcription
    
    // Fix common transcription errors
    enhanced = fixCommonTranscriptionErrors(enhanced)
    
    // Add proper punctuation
    enhanced = addProperPunctuation(enhanced)
    
    // Fix capitalization
    enhanced = fixCapitalization(enhanced)
    
    // Remove filler words if excessive
    enhanced = removeExcessiveFillerWords(enhanced)
    
    return enhanced
}

// Using SimpleInsight from VoiceModelsMinimal.kt

private fun generateVoiceSpecificInsights(
    transcription: String,
    analysisInsights: List<String>,
    context: VoiceContext?
): List<SimpleInsight> {
    val insights = mutableListOf<SimpleInsight>()

    // Voice-specific insight
    insights.add(
        SimpleInsight(
            title = "Voice Communication Style",
            description = "This note was created through voice input, showing natural speech patterns",
            confidence = 0.9f,
            actionable = false
        )
    )

    // Context insights
    context?.let { ctx ->
        if (ctx.participants.isNotEmpty()) {
            insights.add(
                SimpleInsight(
                    title = "Collaborative Discussion",
                    description = "This voice note involved ${ctx.participants.size} participants",
                    confidence = 1.0f,
                    actionable = true
                )
            )
        }
        if (ctx.urgency == VoiceUrgency.HIGH) {
            insights.add(
                SimpleInsight(
                    title = "Urgent Content",
                    description = "This voice note was marked as urgent",
                    confidence = 1.0f,
                    actionable = true
                )
            )
        }
    }

    // Add existing insights as simple items
    insights.addAll(analysisInsights.map {
        SimpleInsight(
            title = "Insight",
            description = it,
            confidence = 0.7f,
            actionable = false
        )
    })

    return insights
}

private fun extractVoiceCommandIntent(command: String): VoiceCommandIntent {
    val lowerCommand = command.lowercase()
    
    return when {
        lowerCommand.contains("create") || lowerCommand.contains("new") || lowerCommand.contains("make") -> VoiceCommandIntent.CREATE_NOTE
        lowerCommand.contains("search") || lowerCommand.contains("find") || lowerCommand.contains("look for") -> VoiceCommandIntent.SEARCH_NOTES
        lowerCommand.contains("organize") || lowerCommand.contains("sort") || lowerCommand.contains("arrange") -> VoiceCommandIntent.ORGANIZE_NOTES
        lowerCommand.contains("insight") || lowerCommand.contains("analyze") || lowerCommand.contains("summary") -> VoiceCommandIntent.GET_INSIGHTS
        lowerCommand.contains("dashboard") || lowerCommand.contains("customize") || lowerCommand.contains("settings") -> VoiceCommandIntent.CUSTOMIZE_DASHBOARD
        lowerCommand.contains("read") || lowerCommand.contains("show") || lowerCommand.contains("display") -> VoiceCommandIntent.READ_NOTE
        lowerCommand.contains("summarize") || lowerCommand.contains("sum up") -> VoiceCommandIntent.SUMMARIZE_CONTENT
        lowerCommand.contains("remind") || lowerCommand.contains("reminder") -> VoiceCommandIntent.SET_REMINDER
        lowerCommand.contains("delete") || lowerCommand.contains("remove") -> VoiceCommandIntent.DELETE_NOTE
        lowerCommand.contains("share") || lowerCommand.contains("send") -> VoiceCommandIntent.SHARE_NOTE
        lowerCommand.contains("export") || lowerCommand.contains("backup") -> VoiceCommandIntent.EXPORT_DATA
        lowerCommand.contains("help") || lowerCommand.contains("how") -> VoiceCommandIntent.HELP
        else -> VoiceCommandIntent.UNKNOWN
    }
}

private fun extractCommandParameters(command: String, intent: VoiceCommandIntent): Map<String, Any> {
    val parameters = mutableMapOf<String, Any>()
    
    when (intent) {
        VoiceCommandIntent.CREATE_NOTE -> {
            // Extract note type
            when {
                command.contains("meeting", ignoreCase = true) -> parameters["type"] = "meeting"
                command.contains("idea", ignoreCase = true) -> parameters["type"] = "idea"
                command.contains("task", ignoreCase = true) -> parameters["type"] = "task"
                else -> parameters["type"] = "general"
            }
        }
        VoiceCommandIntent.SEARCH_NOTES -> {
            // Extract search terms
            val searchTerms = extractSearchTerms(command)
            parameters["terms"] = searchTerms
        }
        VoiceCommandIntent.SET_REMINDER -> {
            // Extract time and content
            val reminderTime = extractReminderTime(command)
            val reminderContent = extractReminderContent(command)
            parameters["time"] = reminderTime
            parameters["content"] = reminderContent
        }
        else -> {
            // Default parameters
        }
    }
    
    return parameters
}

private fun extractCommandContent(command: String, intent: VoiceCommandIntent): String {
    return when (intent) {
        VoiceCommandIntent.CREATE_NOTE -> extractNoteContent(command)
        VoiceCommandIntent.SEARCH_NOTES -> extractSearchQuery(command)
        VoiceCommandIntent.SET_REMINDER -> extractReminderContent(command)
        else -> command
    }
}

private fun calculateCommandConfidence(
    command: String,
    intent: VoiceCommandIntent,
    parameters: Map<String, Any>
): Float {
    var confidence = 0.5f
    
    // Boost confidence for clear intent indicators
    val intentKeywords = getIntentKeywords(intent)
    val matchingKeywords = intentKeywords.count { command.contains(it, ignoreCase = true) }
    confidence += (matchingKeywords.toFloat() / intentKeywords.size) * 0.3f
    
    // Boost confidence for extracted parameters
    confidence += parameters.size * 0.1f
    
    // Boost confidence for command length (not too short, not too long)
    val wordCount = command.split(" ").size
    confidence += when {
        wordCount in 3..10 -> 0.2f
        wordCount in 2..15 -> 0.1f
        else -> 0f
    }
    
    return minOf(confidence, 1.0f)
}

private fun calculateVoiceAnalysisConfidence(
    transcriptionQuality: Float,
    topicConfidence: Float,
    entityConfidence: Float,
    sentimentConfidence: Float
): Float {
    return (transcriptionQuality + topicConfidence + entityConfidence + sentimentConfidence) / 4f
}

private fun getTranscriptionQuality(transcription: String, audioFile: File?): Float {
    // Estimate transcription quality based on various factors
    var quality = 0.7f // Base quality
    
    // Check for common transcription issues
    val hasRepeatedWords = hasRepeatedWords(transcription)
    val hasIncompleteWords = hasIncompleteWords(transcription)
    val hasProperPunctuation = hasProperPunctuation(transcription)
    
    if (!hasRepeatedWords) quality += 0.1f
    if (!hasIncompleteWords) quality += 0.1f
    if (hasProperPunctuation) quality += 0.1f
    
    return minOf(quality, 1.0f)
}

// Removed analyzeSpeakingPatterns for non-existent voice models

private fun calculateAudioAnalysisConfidence(
    transcription: String,
    topics: List<TopicResult>,
    entities: List<EntityResult>,
    sentiment: SentimentResult
): Float {
    var confidence = 0.5f

    // Crude proxy for audio quality: length and punctuation
    val lengthFactor = (transcription.length.coerceAtMost(200) / 200f) * 0.2f
    val punctuationFactor = if (transcription.any { it == '.' || it == ',' }) 0.1f else 0f
    confidence += lengthFactor + punctuationFactor

    // Factor in analysis results
    confidence += topics.map { it.confidence }.average().toFloat() * 0.2f
    confidence += entities.map { it.confidence }.average().toFloat() * 0.1f
    confidence += sentiment.confidence * 0.1f

    return minOf(confidence, 1.0f)
}

// Additional helper functions would be implemented here
// Removed phantom voice-model helpers; will be reintroduced with real models later

// Placeholder implementations for various helper functions
private fun applyGrammarCorrection(text: String): String = text
private fun applyContextualCorrections(text: String): String = text
private fun applyDomainCorrections(text: String): String = text
private fun applyPersonalizedCorrections(text: String): String = text
private fun performQuickTopicDetection(text: String): List<TopicResult> {
    // Delegate to centralized heuristics to avoid duplication
    return VoiceHeuristics.performQuickTopicDetection(text)
}

private fun performQuickActionItemDetection(text: String): List<ActionItem> {
    // Delegate to centralized heuristics to avoid duplication
    return VoiceHeuristics.performQuickActionItemDetection(text)
}

private fun performQuickSentimentAnalysis(text: String): SentimentResult {
    // Use the existing analyzer for quick pass
    // Note: this is synchronous wrapper for simplicity here
    val analyzer = SentimentAnalyzer()
    // We cannot call suspend directly; use a simplified heuristic inline to avoid coroutine
    val words = text.lowercase().split(" ")
    val positiveWords = listOf("good", "great", "excellent", "amazing", "love", "happy")
    val negativeWords = listOf("bad", "terrible", "awful", "hate", "sad", "angry")
    val pos = words.count { it in positiveWords }
    val neg = words.count { it in negativeWords }
    val total = words.size.coerceAtLeast(1)
    val positiveScore = pos.toFloat() / total
    val negativeScore = neg.toFloat() / total
    val neutralScore = 1f - positiveScore - negativeScore
    val sentiment = when {
        positiveScore > negativeScore -> Sentiment.POSITIVE
        negativeScore > positiveScore -> Sentiment.NEGATIVE
        else -> Sentiment.NEUTRAL
    }
    val confidence = maxOf(positiveScore, negativeScore, neutralScore)
    return SentimentResult(sentiment, confidence, positiveScore, negativeScore, neutralScore)
}
private fun organizeContentByTopics(content: String, topics: List<TopicResult>): List<TopicSection> = emptyList()
private fun fixCommonTranscriptionErrors(text: String): String = text
private fun addProperPunctuation(text: String): String = text
private fun fixCapitalization(text: String): String = text
private fun removeExcessiveFillerWords(text: String): String = text
private fun extractSearchTerms(command: String): List<String> = emptyList()
private fun extractReminderTime(command: String): String = ""
private fun extractReminderContent(command: String): String = ""
private fun extractNoteContent(command: String): String = ""
private fun extractSearchQuery(command: String): String = ""
private fun getIntentKeywords(intent: VoiceCommandIntent): List<String> = emptyList()
private fun hasRepeatedWords(text: String): Boolean = false
private fun hasIncompleteWords(text: String): Boolean = false
private fun hasProperPunctuation(text: String): Boolean = true

data class TopicSection(val topic: String, val content: String)
