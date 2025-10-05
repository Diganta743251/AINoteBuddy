package com.ainotebuddy.app.ai

import android.content.Context
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.data.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * NoteBuddy AI Service - Smart note assistant with advanced AI capabilities
 * Handles note enhancement, summarization, task extraction, and content generation
 * Now supports real AI APIs (OpenAI, Gemini, Claude) with offline fallback
 */
class NoteBuddyAIService(private val context: Context) {
    
    private val apiClient = AIApiClient(context)
    private val preferencesManager = PreferencesManager(context)
    


    /**
     * Main AI processing function - determines the best action based on user input
     */
    suspend fun processAIRequest(
        request: String,
        noteContent: String,
        noteTitle: String = ""
    ): AIResponse {
        return withContext(Dispatchers.Default) {
            try {
                if (preferencesManager.getAIProvider() != AIProvider.OFFLINE && preferencesManager.hasValidApiKey()) {
                    processWithOpenAI(request, noteContent, noteTitle)
                } else {
                    processWithLocalAI(request, noteContent, noteTitle)
                }
            } catch (e: Exception) {
                AIResponse(
                    type = AIResponseType.GENERAL,
                    content = "I encountered an error while processing your request. Please try again.",
                    success = false,
                    metadata = mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
    
    private suspend fun processWithOpenAI(request: String, noteContent: String, noteTitle: String): AIResponse {
        val apiKey = preferencesManager.getOpenAIKey() ?: return AIResponse(
            type = AIResponseType.GENERAL,
            content = "Please set your OpenAI API key in settings to use AI features.",
            success = false
        )
        
        val systemPrompt = getSystemPrompt(request)
        val userPrompt = buildUserPrompt(request, noteContent, noteTitle)
        
        val chatRequest = ChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            maxTokens = 1000,
            temperature = 0.7
        )
        
        return try {
            // Use AIApiClient instead
            val apiClient = AIApiClient(context)
            val response = apiClient.processRequest(userPrompt, systemPrompt)
            
            return if (response.success) {
                AIResponse(
                    type = AIResponseType.GENERAL,
                    content = response.content,
                    success = true
                )
            } else {
                AIResponse(
                    type = AIResponseType.GENERAL,
                    content = response.error ?: "Failed to process request",
                    success = false
                )
            }
        } catch (e: Exception) {
            AIResponse(
                type = AIResponseType.GENERAL,
                content = "Network error occurred. Please check your connection and try again.",
                success = false,
                metadata = mapOf("error" to e.message.orEmpty())
            )
        }
    }
    
    private suspend fun processWithLocalAI(request: String, noteContent: String, noteTitle: String): AIResponse {
        val requestLower = request.lowercase().trim()
        
        return when {
            // Note Enhancement
            requestLower.contains("improve") || requestLower.contains("enhance") || 
            requestLower.contains("rewrite") || requestLower.contains("fix") -> {
                enhanceNote(noteContent, noteTitle)
            }
            
            // Summarization
            requestLower.contains("summarize") || requestLower.contains("summary") ||
            requestLower.contains("tldr") || requestLower.contains("brief") -> {
                summarizeNote(noteContent, noteTitle)
            }
            
            // Task Extraction
            requestLower.contains("task") || requestLower.contains("todo") ||
            requestLower.contains("action") || requestLower.contains("checklist") -> {
                extractTasks(noteContent)
            }
            
            // Note Generation
            requestLower.contains("create") || requestLower.contains("generate") ||
            requestLower.contains("write") || requestLower.contains("draft") -> {
                generateNote(request)
            }
            
            // Flashcards
            requestLower.contains("flashcard") || requestLower.contains("quiz") ||
            requestLower.contains("study") || requestLower.contains("q&a") -> {
                createFlashcards(noteContent)
            }
            
            // Expand Content
            requestLower.contains("expand") || requestLower.contains("elaborate") ||
            requestLower.contains("detail") || requestLower.contains("more") -> {
                expandContent(noteContent, noteTitle)
            }
            
            // Shorten Content
            requestLower.contains("shorten") || requestLower.contains("condense") ||
            requestLower.contains("brief") || requestLower.contains("compact") -> {
                shortenContent(noteContent)
            }
            
            // Format Content
            requestLower.contains("format") || requestLower.contains("organize") ||
            requestLower.contains("structure") || requestLower.contains("clean") -> {
                formatContent(noteContent)
            }
            
            else -> {
                // Default: Try to understand the request and provide appropriate response
                handleGeneralRequest(request, noteContent, noteTitle)
            }
        }
    }
    
    private fun getSystemPrompt(request: String): String {
        return when {
            request.lowercase().contains("enhance") || request.lowercase().contains("improve") -> {
                "You are a helpful writing assistant. Enhance and improve the given text while maintaining its original meaning and structure. Make it more clear, engaging, and well-organized."
            }
            request.lowercase().contains("summarize") || request.lowercase().contains("summary") -> {
                "You are a summarization expert. Create a concise, well-structured summary of the given text that captures all key points and main ideas."
            }
            request.lowercase().contains("task") || request.lowercase().contains("todo") -> {
                "You are a task extraction specialist. Identify and extract actionable tasks, to-dos, and action items from the given text. Format them as a clear, organized list."
            }
            request.lowercase().contains("flashcard") -> {
                "You are an educational content creator. Create study flashcards from the given text. Format as Q: [Question] A: [Answer] pairs."
            }
            else -> {
                "You are a helpful AI assistant specialized in note-taking and content organization. Help the user with their request in a clear and useful way."
            }
        }
    }
    
    private fun buildUserPrompt(request: String, noteContent: String, noteTitle: String): String {
        return buildString {
            append("Request: $request\n\n")
            if (noteTitle.isNotBlank()) {
                append("Note Title: $noteTitle\n\n")
            }
            append("Content to process:\n$noteContent")
        }
    }
    
    private fun determineResponseType(request: String): AIResponseType {
        return when {
            request.lowercase().contains("enhance") || request.lowercase().contains("improve") -> AIResponseType.ENHANCEMENT
            request.lowercase().contains("summarize") || request.lowercase().contains("summary") -> AIResponseType.SUMMARY
            request.lowercase().contains("task") || request.lowercase().contains("todo") -> AIResponseType.TASKS
            request.lowercase().contains("flashcard") -> AIResponseType.FLASHCARDS
            request.lowercase().contains("generate") || request.lowercase().contains("create") -> AIResponseType.GENERATION
            else -> AIResponseType.GENERAL
        }
    }

    /**
     * Enhance note content - improve grammar, clarity, and structure
     */
    private suspend fun enhanceNote(content: String, title: String): AIResponse {
        if (content.isBlank()) {
            return AIResponse(
                type = AIResponseType.ENHANCEMENT,
                content = "Please provide some content to enhance.",
                success = false
            )
        }

        val enhanced = buildString {
            // Improve title if provided
            if (title.isNotBlank()) {
                appendLine("**${improveTitle(title)}**")
                appendLine()
            }

            // Process content by paragraphs
            val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
            paragraphs.forEachIndexed { index, paragraph ->
                val enhancedParagraph = enhanceParagraph(paragraph.trim())
                append(enhancedParagraph)
                
                if (index < paragraphs.size - 1) {
                    appendLine()
                    appendLine()
                }
            }
        }

        return AIResponse(
            type = AIResponseType.ENHANCEMENT,
            content = enhanced,
            success = true,
            metadata = mapOf(
                "original_length" to content.length.toString(),
                "enhanced_length" to enhanced.length.toString(),
                "improvement_type" to "grammar_and_clarity"
            )
        )
    }

    /**
     * Generate comprehensive summary with action points
     */
    private suspend fun summarizeNote(content: String, title: String): AIResponse {
        if (content.isBlank()) {
            return AIResponse(
                type = AIResponseType.SUMMARY,
                content = "No content to summarize.",
                success = false
            )
        }

        // Extract data first
        val keyPoints = extractKeyPoints(content)
        val actionItems = extractActionItems(content)
        val deadlines = extractDeadlines(content)
        
        val summary = buildString {
            if (title.isNotBlank()) {
                appendLine("**Summary: $title**")
                appendLine()
            }

            // Main summary (2-4 key points)
            keyPoints.take(4).forEach { point ->
                appendLine("• $point")
            }

            // Action items if any
            if (actionItems.isNotEmpty()) {
                appendLine()
                appendLine("**📝 Action Items:**")
                actionItems.forEach { action ->
                    appendLine("✅ $action")
                }
            }

            // Deadlines if any
            if (deadlines.isNotEmpty()) {
                appendLine()
                appendLine("**⏰ Important Dates:**")
                deadlines.forEach { deadline ->
                    appendLine("📅 $deadline")
                }
            }
        }

        return AIResponse(
            type = AIResponseType.SUMMARY,
            content = summary,
            success = true,
            metadata = mapOf(
                "word_count" to content.split("\\s+".toRegex()).size.toString(),
                "key_points" to keyPoints.size.toString(),
                "action_items" to actionItems.size.toString()
            )
        )
    }

    /**
     * Extract actionable tasks from note content
     */
    private suspend fun extractTasks(content: String): AIResponse {
        if (content.isBlank()) {
            return AIResponse(
                type = AIResponseType.TASKS,
                content = "No content to extract tasks from.",
                success = false
            )
        }

        val tasks = mutableListOf<String>()
        
        // Extract existing checkboxes
        val checkboxPattern = Pattern.compile("- \\[[ x]\\] (.+)")
        val checkboxMatcher = checkboxPattern.matcher(content)
        while (checkboxMatcher.find()) {
            tasks.add(checkboxMatcher.group(1))
        }

        // Extract action verbs and create tasks
        val actionVerbs = listOf("need to", "should", "must", "have to", "will", "plan to", "going to")
        val sentences = content.split("[.!?]".toRegex())
        
        sentences.forEach { sentence ->
            val trimmed = sentence.trim()
            actionVerbs.forEach { verb ->
                if (trimmed.lowercase().contains(verb) && trimmed.length > 10) {
                    val task = extractTaskFromSentence(trimmed)
                    if (task.isNotBlank() && !tasks.contains(task)) {
                        tasks.add(task)
                    }
                }
            }
        }

        // Look for numbered lists that might be tasks
        val numberedPattern = Pattern.compile("\\d+\\. (.+)")
        val numberedMatcher = numberedPattern.matcher(content)
        while (numberedMatcher.find()) {
            val potentialTask = numberedMatcher.group(1).trim()
            if (potentialTask.length > 5 && !tasks.contains(potentialTask)) {
                tasks.add(potentialTask)
            }
        }

        val taskList = buildString {
            appendLine("**📋 Extracted Tasks:**")
            appendLine()
            if (tasks.isEmpty()) {
                appendLine("No specific tasks found. Here are some suggested actions:")
                appendLine("- [ ] Review and organize this note")
                appendLine("- [ ] Follow up on key points mentioned")
                appendLine("- [ ] Share with relevant team members if needed")
            } else {
                tasks.forEach { task ->
                    appendLine("- [ ] $task")
                }
            }
        }

        return AIResponse(
            type = AIResponseType.TASKS,
            content = taskList,
            success = true,
            metadata = mapOf(
                "tasks_found" to tasks.size.toString(),
                "extraction_method" to "pattern_matching_and_nlp"
            )
        )
    }

    /**
     * Generate structured note from prompt
     */
    private suspend fun generateNote(prompt: String): AIResponse {
        val cleanPrompt = prompt.replace(Regex("(create|generate|write|draft)\\s+", RegexOption.IGNORE_CASE), "").trim()
        
        if (cleanPrompt.isBlank()) {
            return AIResponse(
                type = AIResponseType.GENERATION,
                content = "Please provide a topic or prompt for note generation.",
                success = false
            )
        }

        val generatedNote = when {
            cleanPrompt.lowercase().contains("meeting") -> generateMeetingNote(cleanPrompt)
            cleanPrompt.lowercase().contains("study") || cleanPrompt.lowercase().contains("chapter") -> generateStudyNote(cleanPrompt)
            cleanPrompt.lowercase().contains("project") -> generateProjectNote(cleanPrompt)
            cleanPrompt.lowercase().contains("idea") || cleanPrompt.lowercase().contains("brainstorm") -> generateIdeaNote(cleanPrompt)
            else -> generateGeneralNote(cleanPrompt)
        }

        return AIResponse(
            type = AIResponseType.GENERATION,
            content = generatedNote,
            success = true,
            metadata = mapOf(
                "prompt" to cleanPrompt,
                "note_type" to detectNoteType(cleanPrompt),
                "generated_at" to SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
        )
    }

    /**
     * Create flashcards from note content
     */
    private suspend fun createFlashcards(content: String): AIResponse {
        if (content.isBlank()) {
            return AIResponse(
                type = AIResponseType.FLASHCARDS,
                content = "No content available to create flashcards.",
                success = false
            )
        }

        val flashcards = mutableListOf<Pair<String, String>>()
        
        // Extract Q&A patterns
        val qaPattern = Pattern.compile("Q:\\s*(.+?)\\s*A:\\s*(.+?)(?=Q:|$)", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val qaMatcher = qaPattern.matcher(content)
        while (qaMatcher.find()) {
            flashcards.add(Pair(qaMatcher.group(1).trim(), qaMatcher.group(2).trim()))
        }

        // Extract definitions (term: definition)
        val defPattern = Pattern.compile("(.+?):\\s*(.+?)(?=\\n|$)")
        val defMatcher = defPattern.matcher(content)
        while (defMatcher.find()) {
            val term = defMatcher.group(1).trim()
            val definition = defMatcher.group(2).trim()
            if (term.length < 50 && definition.length > 10) {
                flashcards.add(Pair("What is $term?", definition))
            }
        }

        // Generate flashcards from key concepts
        if (flashcards.isEmpty()) {
            val keyPoints = extractKeyPoints(content)
            keyPoints.take(5).forEach { point ->
                val question = generateQuestionFromPoint(point)
                flashcards.add(Pair(question, point))
            }
        }

        val flashcardContent = buildString {
            appendLine("**🎯 Study Flashcards**")
            appendLine()
            if (flashcards.isEmpty()) {
                appendLine("No suitable content found for flashcards. Try adding more structured information with definitions or key concepts.")
            } else {
                flashcards.forEachIndexed { index, (question, answer) ->
                    appendLine("**Card ${index + 1}:**")
                    appendLine("**Q:** $question")
                    appendLine("**A:** $answer")
                    appendLine()
                }
            }
        }

        return AIResponse(
            type = AIResponseType.FLASHCARDS,
            content = flashcardContent,
            success = true,
            metadata = mapOf(
                "cards_created" to flashcards.size.toString(),
                "extraction_method" to "pattern_matching"
            )
        )
    }

    /**
     * Expand content with more details
     */
    private suspend fun expandContent(content: String, title: String): AIResponse {
        if (content.isBlank()) {
            return AIResponse(
                type = AIResponseType.EXPANSION,
                content = "Please provide content to expand.",
                success = false
            )
        }

        val expanded = buildString {
            if (title.isNotBlank()) {
                appendLine("**$title**")
                appendLine()
            }

            val sentences = content.split("[.!?]".toRegex()).filter { it.trim().isNotBlank() }
            sentences.forEach { sentence ->
                val trimmed = sentence.trim()
                appendLine("$trimmed.")
                
                // Add elaboration based on content type
                val elaboration = generateElaboration(trimmed)
                if (elaboration.isNotBlank()) {
                    appendLine(elaboration)
                }
                appendLine()
            }

            // Add suggested next steps
            appendLine("**💡 Additional Considerations:**")
            val suggestions = generateExpansionSuggestions(content)
            suggestions.forEach { suggestion ->
                appendLine("• $suggestion")
            }
        }

        return AIResponse(
            type = AIResponseType.EXPANSION,
            content = expanded,
            success = true,
            metadata = mapOf(
                "original_sentences" to content.split("[.!?]".toRegex()).filter { it.trim().isNotBlank() }.size.toString(),
                "expansion_ratio" to (expanded.length.toFloat() / content.length).toString()
            )
        )
    }

    /**
     * Shorten content while preserving key information
     */
    private suspend fun shortenContent(content: String): AIResponse {
        if (content.isBlank()) {
            return AIResponse(
                type = AIResponseType.SHORTENING,
                content = "No content to shorten.",
                success = false
            )
        }

        val keyPoints = extractKeyPoints(content)
        val shortened = buildString {
            appendLine("**📝 Condensed Version:**")
            appendLine()
            keyPoints.take(3).forEach { point ->
                appendLine("• $point")
            }
        }

        return AIResponse(
            type = AIResponseType.SHORTENING,
            content = shortened,
            success = true,
            metadata = mapOf(
                "original_length" to content.length.toString(),
                "shortened_length" to shortened.length.toString(),
                "compression_ratio" to (shortened.length.toFloat() / content.length).toString()
            )
        )
    }

    /**
     * Format and organize content structure
     */
    private suspend fun formatContent(content: String): AIResponse {
        if (content.isBlank()) {
            return AIResponse(
                type = AIResponseType.FORMATTING,
                content = "No content to format.",
                success = false
            )
        }

        val formatted = buildString {
            val lines = content.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            var currentSection = ""
            
            lines.forEach { line ->
                when {
                    // Headers
                    line.length < 50 && line.endsWith(":") -> {
                        if (currentSection.isNotBlank()) appendLine()
                        appendLine("## ${line.removeSuffix(":")}")
                        appendLine()
                        currentSection = line
                    }
                    // Bullet points
                    line.startsWith("-") || line.startsWith("•") || line.startsWith("*") -> {
                        appendLine("• ${line.removePrefix("-").removePrefix("•").removePrefix("*").trim()}")
                    }
                    // Numbers
                    line.matches(Regex("\\d+\\..*")) -> {
                        appendLine(line)
                    }
                    // Regular content
                    else -> {
                        if (line.length > 100) {
                            // Break long lines into paragraphs
                            val sentences = line.split("[.!?]".toRegex()).filter { it.trim().isNotBlank() }
                            sentences.forEach { sentence ->
                                appendLine("${sentence.trim()}.")
                            }
                            appendLine()
                        } else {
                            appendLine(line)
                        }
                    }
                }
            }
        }

        return AIResponse(
            type = AIResponseType.FORMATTING,
            content = formatted,
            success = true,
            metadata = mapOf(
                "formatting_applied" to "markdown_structure",
                "lines_processed" to content.split("\n").size.toString()
            )
        )
    }

    /**
     * Handle general AI requests
     */
    private suspend fun handleGeneralRequest(request: String, content: String, title: String): AIResponse {
        val response = buildString {
            appendLine("**🤖 AI Assistant Response**")
            appendLine()
            appendLine("I understand you want help with: \"$request\"")
            appendLine()
            
            if (content.isNotBlank()) {
                appendLine("Based on your note content, here are some suggestions:")
                appendLine()
                
                val suggestions = generateGeneralSuggestions(request, content)
                suggestions.forEach { suggestion ->
                    appendLine("• $suggestion")
                }
            } else {
                appendLine("To better assist you, please provide some note content or try one of these commands:")
                appendLine("• \"Summarize this note\"")
                appendLine("• \"Extract tasks from this\"")
                appendLine("• \"Improve this content\"")
                appendLine("• \"Create flashcards\"")
                appendLine("• \"Generate a meeting note\"")
            }
        }

        return AIResponse(
            type = AIResponseType.GENERAL,
            content = response,
            success = true,
            metadata = mapOf(
                "request_type" to "general_assistance",
                "has_content" to content.isNotBlank().toString()
            )
        )
    }

    // Helper methods for content processing
    private fun improveTitle(title: String): String {
        return title.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun enhanceParagraph(paragraph: String): String {
        var enhanced = paragraph
        
        // Fix common grammar issues
        enhanced = enhanced.replace(Regex("\\bi\\b"), "I")
        enhanced = enhanced.replace(Regex("\\s+"), " ")
        enhanced = enhanced.trim()
        
        // Ensure proper sentence ending
        if (!enhanced.endsWith(".") && !enhanced.endsWith("!") && !enhanced.endsWith("?")) {
            enhanced += "."
        }
        
        return enhanced
    }

    private fun extractKeyPoints(content: String): List<String> {
        val points = mutableListOf<String>()
        
        // Extract bullet points
        val bulletPattern = Pattern.compile("[-•*]\\s*(.+)")
        val bulletMatcher = bulletPattern.matcher(content)
        while (bulletMatcher.find()) {
            points.add(bulletMatcher.group(1).trim())
        }
        
        // Extract sentences with important keywords
        val sentences = content.split("[.!?]".toRegex()).filter { it.trim().isNotBlank() }
        val importantKeywords = listOf("important", "key", "main", "primary", "essential", "critical", "note that", "remember")
        
        sentences.forEach { sentence ->
            val trimmed = sentence.trim()
            if (importantKeywords.any { keyword -> trimmed.lowercase().contains(keyword) }) {
                points.add(trimmed)
            }
        }
        
        // If no specific points found, use first few sentences
        if (points.isEmpty()) {
            points.addAll(sentences.take(3).map { it.trim() })
        }
        
        return points.distinct()
    }

    private fun extractActionItems(content: String): List<String> {
        val actions = mutableListOf<String>()
        val actionKeywords = listOf("need to", "should", "must", "have to", "will", "action:", "todo:", "task:")
        
        val sentences = content.split("[.!?]".toRegex())
        sentences.forEach { sentence ->
            val trimmed = sentence.trim()
            actionKeywords.forEach { keyword ->
                if (trimmed.lowercase().contains(keyword)) {
                    val action = extractTaskFromSentence(trimmed)
                    if (action.isNotBlank()) {
                        actions.add(action)
                    }
                }
            }
        }
        
        return actions.distinct()
    }

    private fun extractDeadlines(content: String): List<String> {
        val deadlines = mutableListOf<String>()
        val datePattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\w*\\s+\\d{1,2}|\\b(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday))", Pattern.CASE_INSENSITIVE)
        val dateMatcher = datePattern.matcher(content)
        
        while (dateMatcher.find()) {
            val context = getContextAroundMatch(content, dateMatcher.start(), dateMatcher.end())
            deadlines.add(context)
        }
        
        return deadlines.distinct()
    }

    private fun extractTaskFromSentence(sentence: String): String {
        // Remove common prefixes and clean up
        var task = sentence
            .replace(Regex("(need to|should|must|have to|will|going to)\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(i|we|you|they)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .replaceFirstChar { it.uppercase() }
        
        if (task.length > 100) {
            task = task.substring(0, 97) + "..."
        }
        
        return task
    }

    private fun getContextAroundMatch(text: String, start: Int, end: Int): String {
        val contextStart = maxOf(0, start - 20)
        val contextEnd = minOf(text.length, end + 20)
        return text.substring(contextStart, contextEnd).trim()
    }

    private fun generateMeetingNote(prompt: String): String {
        return buildString {
            appendLine("**Meeting Note - ${extractTopicFromPrompt(prompt)}**")
            appendLine()
            appendLine("**📅 Date:** ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())}")
            appendLine("**⏰ Time:** [To be filled]")
            appendLine("**👥 Attendees:** [To be added]")
            appendLine()
            appendLine("**📋 Agenda:**")
            appendLine("- [ ] [Topic 1]")
            appendLine("- [ ] [Topic 2]")
            appendLine("- [ ] [Topic 3]")
            appendLine()
            appendLine("**📝 Discussion Points:**")
            appendLine("• ")
            appendLine()
            appendLine("**✅ Action Items:**")
            appendLine("- [ ] [Action 1] - [Assignee] - [Due Date]")
            appendLine("- [ ] [Action 2] - [Assignee] - [Due Date]")
            appendLine()
            appendLine("**📌 Next Steps:**")
            appendLine("• ")
        }
    }

    private fun generateStudyNote(prompt: String): String {
        val topic = extractTopicFromPrompt(prompt)
        return buildString {
            appendLine("**Study Note: $topic**")
            appendLine()
            appendLine("**📚 Key Concepts:**")
            appendLine("1. **[Concept 1]:** [Definition/Explanation]")
            appendLine("2. **[Concept 2]:** [Definition/Explanation]")
            appendLine("3. **[Concept 3]:** [Definition/Explanation]")
            appendLine()
            appendLine("**💡 Important Points:**")
            appendLine("• [Key point 1]")
            appendLine("• [Key point 2]")
            appendLine("• [Key point 3]")
            appendLine()
            appendLine("**📝 Examples:**")
            appendLine("• [Example 1]")
            appendLine("• [Example 2]")
            appendLine()
            appendLine("**🎯 Study Tips:**")
            appendLine("• Review key concepts regularly")
            appendLine("• Practice with examples")
            appendLine("• Create flashcards for definitions")
        }
    }

    private fun generateProjectNote(prompt: String): String {
        val project = extractTopicFromPrompt(prompt)
        return buildString {
            appendLine("**Project: $project**")
            appendLine()
            appendLine("**🎯 Objective:**")
            appendLine("[Project goal and purpose]")
            appendLine()
            appendLine("**📋 Tasks:**")
            appendLine("- [ ] [Task 1]")
            appendLine("- [ ] [Task 2]")
            appendLine("- [ ] [Task 3]")
            appendLine()
            appendLine("**📅 Timeline:**")
            appendLine("• **Start Date:** [Date]")
            appendLine("• **Milestones:** [Key dates]")
            appendLine("• **Deadline:** [End date]")
            appendLine()
            appendLine("**👥 Team Members:**")
            appendLine("• [Name 1] - [Role]")
            appendLine("• [Name 2] - [Role]")
            appendLine()
            appendLine("**📊 Progress:**")
            appendLine("• [Current status]")
            appendLine("• [Completed items]")
            appendLine("• [Next steps]")
        }
    }

    private fun generateIdeaNote(prompt: String): String {
        val topic = extractTopicFromPrompt(prompt)
        return buildString {
            appendLine("**💡 Idea: $topic**")
            appendLine()
            appendLine("**🎯 Core Concept:**")
            appendLine("[Main idea description]")
            appendLine()
            appendLine("**✨ Key Features:**")
            appendLine("• [Feature 1]")
            appendLine("• [Feature 2]")
            appendLine("• [Feature 3]")
            appendLine()
            appendLine("**🔍 Research Needed:**")
            appendLine("- [ ] [Research topic 1]")
            appendLine("- [ ] [Research topic 2]")
            appendLine()
            appendLine("**📈 Potential Benefits:**")
            appendLine("• [Benefit 1]")
            appendLine("• [Benefit 2]")
            appendLine()
            appendLine("**⚠️ Challenges:**")
            appendLine("• [Challenge 1]")
            appendLine("• [Challenge 2]")
            appendLine()
            appendLine("**🚀 Next Steps:**")
            appendLine("- [ ] [Action 1]")
            appendLine("- [ ] [Action 2]")
        }
    }

    private fun generateGeneralNote(prompt: String): String {
        val topic = extractTopicFromPrompt(prompt)
        return buildString {
            appendLine("**📝 Note: $topic**")
            appendLine()
            appendLine("**Overview:**")
            appendLine("[Brief description of the topic]")
            appendLine()
            appendLine("**Key Points:**")
            appendLine("• [Point 1]")
            appendLine("• [Point 2]")
            appendLine("• [Point 3]")
            appendLine()
            appendLine("**Details:**")
            appendLine("[Additional information and context]")
            appendLine()
            appendLine("**References:**")
            appendLine("• [Source 1]")
            appendLine("• [Source 2]")
        }
    }

    private fun extractTopicFromPrompt(prompt: String): String {
        val cleaned = prompt
            .replace(Regex("(create|generate|write|draft|note|about|for)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .replaceFirstChar { it.uppercase() }
        
        return if (cleaned.isBlank()) "New Topic" else cleaned
    }

    private fun detectNoteType(prompt: String): String {
        val promptLower = prompt.lowercase()
        return when {
            promptLower.contains("meeting") -> "meeting"
            promptLower.contains("study") || promptLower.contains("chapter") -> "study"
            promptLower.contains("project") -> "project"
            promptLower.contains("idea") || promptLower.contains("brainstorm") -> "idea"
            else -> "general"
        }
    }

    private fun generateQuestionFromPoint(point: String): String {
        return when {
            point.lowercase().contains("what") -> point
            point.lowercase().contains("how") -> point
            point.lowercase().contains("why") -> point
            point.lowercase().contains("when") -> point
            point.lowercase().contains("where") -> point
            else -> "What is the key point about: ${point.take(50)}${if (point.length > 50) "..." else ""}?"
        }
    }

    private fun generateElaboration(sentence: String): String {
        // Simple elaboration based on content type
        return when {
            sentence.lowercase().contains("important") -> "This point requires special attention and should be prioritized."
            sentence.lowercase().contains("meeting") -> "Consider preparing an agenda and inviting relevant stakeholders."
            sentence.lowercase().contains("deadline") -> "Make sure to set reminders and track progress regularly."
            sentence.lowercase().contains("project") -> "Break this down into smaller, manageable tasks with clear timelines."
            else -> ""
        }
    }

    private fun generateExpansionSuggestions(content: String): List<String> {
        val suggestions = mutableListOf<String>()
        val contentLower = content.lowercase()
        
        when {
            contentLower.contains("meeting") -> {
                suggestions.add("Add attendee list and their roles")
                suggestions.add("Include agenda items with time allocations")
                suggestions.add("Specify action items with owners and deadlines")
            }
            contentLower.contains("project") -> {
                suggestions.add("Define project scope and objectives")
                suggestions.add("Create timeline with milestones")
                suggestions.add("Identify required resources and team members")
            }
            contentLower.contains("idea") -> {
                suggestions.add("Research similar concepts or solutions")
                suggestions.add("Consider potential challenges and solutions")
                suggestions.add("Outline implementation steps")
            }
            else -> {
                suggestions.add("Add more specific details and examples")
                suggestions.add("Include relevant dates and deadlines")
                suggestions.add("Consider adding visual elements or diagrams")
            }
        }
        
        return suggestions
    }

    private fun generateGeneralSuggestions(request: String, content: String): List<String> {
        val suggestions = mutableListOf<String>()
        val requestLower = request.lowercase()
        
        when {
            requestLower.contains("help") -> {
                suggestions.add("Try asking me to summarize, enhance, or extract tasks")
                suggestions.add("I can also create flashcards or generate new notes")
                suggestions.add("Use specific commands like 'improve this' or 'create tasks'")
            }
            requestLower.contains("organize") -> {
                suggestions.add("Format your content with headers and bullet points")
                suggestions.add("Group related information together")
                suggestions.add("Add categories or tags for better organization")
            }
            else -> {
                suggestions.add("Consider breaking down complex topics into smaller sections")
                suggestions.add("Add action items or next steps if applicable")
                suggestions.add("Include relevant dates, deadlines, or reminders")
            }
        }
        
        return suggestions
    }

    /**
     * Process voice input and clean it up
     */
    suspend fun processVoiceInput(rawVoiceText: String): AIResponse {
        return withContext(Dispatchers.Default) {
            val cleaned = cleanVoiceText(rawVoiceText)
            val formatted = formatVoiceNote(cleaned)
            
            AIResponse(
                type = AIResponseType.VOICE_PROCESSING,
                content = formatted,
                success = true,
                metadata = mapOf(
                    "original_text" to rawVoiceText,
                    "processing_type" to "voice_cleanup_and_formatting"
                )
            )
        }
    }

    private fun cleanVoiceText(text: String): String {
        return text
            .replace(Regex("\\bum\\b|\\buh\\b|\\ber\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatVoiceNote(text: String): String {
        return buildString {
            appendLine("**🎤 Voice Note**")
            appendLine("*Recorded: ${SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date())}*")
            appendLine()
            
            val sentences = text.split("[.!?]".toRegex()).filter { it.trim().isNotBlank() }
            sentences.forEach { sentence ->
                appendLine("• ${sentence.trim().replaceFirstChar { it.uppercase() }}.")
            }
        }
    }
}

/**
 * AI Response data class
 */
data class AIResponse(
    val type: AIResponseType,
    val content: String,
    val success: Boolean,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of AI responses
 */
enum class AIResponseType {
    ENHANCEMENT,
    SUMMARY,
    TASKS,
    GENERATION,
    FLASHCARDS,
    EXPANSION,
    SHORTENING,
    FORMATTING,
    VOICE_PROCESSING,
    GENERAL
}