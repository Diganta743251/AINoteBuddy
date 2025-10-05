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
 * Enhanced NoteBuddy AI Service - Smart note assistant with real AI API integration
 * Supports OpenAI, Gemini, Claude with offline fallback
 */
class EnhancedNoteBuddyAIService(private val context: Context) {
    
    private val apiClient = AIApiClient(context)
    private val preferencesManager = PreferencesManager(context)
    private val offlineService = NoteBuddyAIService(context)

    /**
     * Main AI processing function with real API integration
     */
    suspend fun processAIRequest(
        request: String,
        noteContent: String,
        noteTitle: String = ""
    ): AIResponse {
        return withContext(Dispatchers.Default) {
            // Track usage
            preferencesManager.incrementAIUsage()
            preferencesManager.incrementAIUsage()
            
            // Check if we have a valid API key for online processing
            val hasValidApiKey = when (preferencesManager.getAIProvider()) {
                AIProvider.OPENAI -> !preferencesManager.getOpenAIKey().isNullOrBlank()
                AIProvider.GEMINI -> !preferencesManager.getGeminiKey().isNullOrBlank()
                AIProvider.CLAUDE -> !preferencesManager.getClaudeKey().isNullOrBlank()
                AIProvider.OFFLINE -> false
            }
            
            if (hasValidApiKey && preferencesManager.getAIProvider() != AIProvider.OFFLINE) {
                try {
                    // Use real AI API
                    processWithAIAPI(request, noteContent, noteTitle)
                } catch (e: Exception) {
                    // Fallback to offline if API fails
                    processOffline(request, noteContent, noteTitle)
                }
            } else {
                // Use offline processing
                processOffline(request, noteContent, noteTitle)
            }
        }
    }
    
    /**
     * Process request using real AI APIs
     */
    private suspend fun processWithAIAPI(
        request: String,
        noteContent: String,
        noteTitle: String
    ): AIResponse {
        val systemPrompt = createSystemPrompt(request)
        val userPrompt = createUserPrompt(request, noteContent, noteTitle)
        
        val apiResponse = apiClient.processRequest(userPrompt, systemPrompt)
        
        return if (apiResponse.success) {
            AIResponse(
                type = determineResponseType(request),
                content = apiResponse.content,
                success = true,
                metadata = mapOf(
                    "provider" to preferencesManager.getAIProvider().displayName,
                    "model" to preferencesManager.getAIModel(),
                    "tokens_used" to apiResponse.tokensUsed.toString(),
                    "processing_type" to "api",
                    "timestamp" to System.currentTimeMillis().toString()
                )
            )
        } else {
            throw Exception("API processing failed: ${apiResponse.error}")
        }
    }
    
    /**
     * Offline processing fallback
     */
    private suspend fun processOffline(
        request: String,
        noteContent: String,
        noteTitle: String
    ): AIResponse {
        val response = offlineService.processAIRequest(request, noteContent, noteTitle)
        return response.copy(
            metadata = response.metadata + mapOf(
                "processing_type" to "offline",
                "fallback_reason" to "api_unavailable_or_disabled"
            )
        )
    }
    
    /**
     * Create system prompt for AI APIs
     */
    private fun createSystemPrompt(request: String): String {
        val requestType = determineResponseType(request)
        
        return when (requestType) {
            AIResponseType.ENHANCEMENT -> """
                You are NoteBuddy, an expert writing assistant. Your task is to improve and enhance notes.
                
                Guidelines:
                - Fix grammar, spelling, and punctuation errors
                - Improve clarity and readability
                - Maintain the original meaning and tone
                - Use proper markdown formatting
                - Keep the content concise but comprehensive
                - Preserve all important information
                
                Return only the enhanced content without explanations.
            """.trimIndent()
            
            AIResponseType.SUMMARY -> """
                You are NoteBuddy, a summarization expert. Create concise, actionable summaries.
                
                Guidelines:
                - Extract 2-4 key points maximum
                - Identify action items and deadlines
                - Use bullet points for clarity
                - Include important dates and names
                - Format with markdown for readability
                
                Structure your response as:
                **Summary:**
                • Key point 1
                • Key point 2
                
                **📝 Action Items:** (if any)
                ✅ Action item with owner and deadline
                
                **⏰ Important Dates:** (if any)
                📅 Date and event
            """.trimIndent()
            
            AIResponseType.TASKS -> """
                You are NoteBuddy, a task extraction specialist. Find and organize actionable items.
                
                Guidelines:
                - Identify all actionable items from the content
                - Create clear, specific task descriptions
                - Use checkbox format for tasks
                - Include deadlines and assignees when mentioned
                - Prioritize tasks if urgency is indicated
                
                Format as:
                **📋 Extracted Tasks:**
                - [ ] Task description with details
                - [ ] Another task with deadline if mentioned
            """.trimIndent()
            
            AIResponseType.GENERATION -> """
                You are NoteBuddy, a content generation expert. Create well-structured notes.
                
                Guidelines:
                - Generate comprehensive, organized content
                - Use appropriate headings and structure
                - Include relevant sections based on the topic
                - Use markdown formatting for clarity
                - Make content actionable and useful
                
                Adapt the structure based on the content type (meeting, study, project, etc.).
            """.trimIndent()
            
            AIResponseType.FLASHCARDS -> """
                You are NoteBuddy, an educational content specialist. Create effective study flashcards.
                
                Guidelines:
                - Extract key concepts and definitions
                - Create clear questions and answers
                - Focus on important information
                - Use simple, direct language
                - Format for easy studying
                
                Format as:
                **🎯 Study Flashcards**
                
                **Card 1:**
                **Q:** Question here
                **A:** Answer here
            """.trimIndent()
            
            else -> """
                You are NoteBuddy, an intelligent note-taking assistant. Help users with their notes effectively.
                
                Guidelines:
                - Be helpful and professional
                - Use markdown formatting
                - Focus on the user's specific request
                - Provide actionable, useful responses
                - Keep responses concise but comprehensive
            """.trimIndent()
        }
    }
    
    /**
     * Create user prompt for AI APIs
     */
    private fun createUserPrompt(request: String, noteContent: String, noteTitle: String): String {
        return buildString {
            appendLine("Request: $request")
            appendLine()
            
            if (noteTitle.isNotBlank()) {
                appendLine("Note Title: $noteTitle")
                appendLine()
            }
            
            if (noteContent.isNotBlank()) {
                appendLine("Note Content:")
                appendLine("---")
                appendLine(noteContent)
                appendLine("---")
            } else {
                appendLine("Note Content: [Empty - generate new content based on the request]")
            }
        }
    }
    
    /**
     * Determine response type based on request
     */
    private fun determineResponseType(request: String): AIResponseType {
        val requestLower = request.lowercase().trim()
        return when {
            requestLower.contains("improve") || requestLower.contains("enhance") || 
            requestLower.contains("rewrite") || requestLower.contains("fix") || 
            requestLower.contains("grammar") || requestLower.contains("better") -> AIResponseType.ENHANCEMENT
            
            requestLower.contains("summarize") || requestLower.contains("summary") ||
            requestLower.contains("tldr") || requestLower.contains("brief") ||
            requestLower.contains("key points") -> AIResponseType.SUMMARY
            
            requestLower.contains("task") || requestLower.contains("todo") ||
            requestLower.contains("action") || requestLower.contains("checklist") ||
            requestLower.contains("extract") && requestLower.contains("task") -> AIResponseType.TASKS
            
            requestLower.contains("create") || requestLower.contains("generate") ||
            requestLower.contains("write") || requestLower.contains("draft") ||
            requestLower.contains("make") -> AIResponseType.GENERATION
            
            requestLower.contains("flashcard") || requestLower.contains("quiz") ||
            requestLower.contains("study") || requestLower.contains("q&a") ||
            requestLower.contains("questions") -> AIResponseType.FLASHCARDS
            
            requestLower.contains("expand") || requestLower.contains("elaborate") ||
            requestLower.contains("detail") || requestLower.contains("more") -> AIResponseType.EXPANSION
            
            requestLower.contains("shorten") || requestLower.contains("condense") ||
            requestLower.contains("brief") || requestLower.contains("compact") -> AIResponseType.SHORTENING
            
            requestLower.contains("format") || requestLower.contains("organize") ||
            requestLower.contains("structure") || requestLower.contains("clean") -> AIResponseType.FORMATTING
            
            else -> AIResponseType.GENERAL
        }
    }
    
    /**
     * Process voice input with AI enhancement
     */
    suspend fun processVoiceInput(rawVoiceText: String): AIResponse {
        return withContext(Dispatchers.Default) {
            val hasValidApiKey = when (preferencesManager.getAIProvider()) {
                AIProvider.OPENAI -> !preferencesManager.getOpenAIKey().isNullOrBlank()
                AIProvider.GEMINI -> !preferencesManager.getGeminiKey().isNullOrBlank()
                AIProvider.CLAUDE -> !preferencesManager.getClaudeKey().isNullOrBlank()
                AIProvider.OFFLINE -> false
            }
            
            if (hasValidApiKey && preferencesManager.isVoiceProcessingEnabled()) {
                try {
                    val systemPrompt = """
                        You are NoteBuddy's voice processing assistant. Clean up voice transcriptions.
                        
                        Guidelines:
                        - Remove filler words (um, uh, er, like)
                        - Fix grammar and sentence structure
                        - Add proper punctuation
                        - Organize rambling speech into clear points
                        - Preserve the original meaning
                        - Format as a clean note with timestamp
                        
                        Format the output as a proper voice note with timestamp.
                    """.trimIndent()
                    
                    val userPrompt = "Clean up this voice transcription: $rawVoiceText"
                    
                    val apiResponse = apiClient.processRequest(userPrompt, systemPrompt)
                    
                    if (apiResponse.success) {
                        AIResponse(
                            type = AIResponseType.VOICE_PROCESSING,
                            content = apiResponse.content,
                            success = true,
                            metadata = mapOf(
                                "provider" to preferencesManager.getAIProvider().displayName,
                                "processing_type" to "api_voice",
                                "original_length" to rawVoiceText.length.toString(),
                                "processed_length" to apiResponse.content.length.toString()
                            )
                        )
                    } else {
                        // Fallback to offline processing
                        offlineService.processVoiceInput(rawVoiceText)
                    }
                } catch (e: Exception) {
                    // Fallback to offline processing
                    offlineService.processVoiceInput(rawVoiceText)
                }
            } else {
                // Use offline processing
                offlineService.processVoiceInput(rawVoiceText)
            }
        }
    }
    
    /**
     * Test API connection
     */
    suspend fun testConnection(): Boolean {
        return try {
            val provider = preferencesManager.getAIProvider()
            val apiKey = when (provider) {
                AIProvider.OPENAI -> preferencesManager.getOpenAIKey()
                AIProvider.GEMINI -> preferencesManager.getGeminiKey()
                AIProvider.CLAUDE -> preferencesManager.getClaudeKey()
                AIProvider.OFFLINE -> return true
            }
            
            if (apiKey.isNullOrBlank()) return false
            
            apiClient.testApiKey(provider, apiKey)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get usage statistics
     */
    fun getUsageStats(): Map<String, Any> {
        return mapOf(
            "total_requests" to preferencesManager.getAIUsageCount(),
            "last_used" to preferencesManager.getLastAIUsage(),
            "current_provider" to preferencesManager.getAIProvider().displayName,
            "has_valid_key" to when (preferencesManager.getAIProvider()) {
                AIProvider.OPENAI -> !preferencesManager.getOpenAIKey().isNullOrBlank()
                AIProvider.GEMINI -> !preferencesManager.getGeminiKey().isNullOrBlank()
                AIProvider.CLAUDE -> !preferencesManager.getClaudeKey().isNullOrBlank()
                AIProvider.OFFLINE -> true
            }
        )
    }
}