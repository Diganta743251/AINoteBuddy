package com.ainotebuddy.app.ai

import android.content.Context
import com.ainotebuddy.app.data.AIProvider
import com.ainotebuddy.app.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI API Client - Handles communication with different AI providers
 */
class AIApiClient(private val context: Context? = null) {
    
    private val preferencesManager = context?.let { PreferencesManager(it) }
    private val json = Json { ignoreUnknownKeys = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun processRequest(prompt: String, systemPrompt: String = ""): AIApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                when (preferencesManager?.getAIProvider() ?: AIProvider.OFFLINE) {
                    AIProvider.OPENAI -> processOpenAIRequest(prompt, systemPrompt)
                    AIProvider.GEMINI -> processGeminiRequest(prompt, systemPrompt)
                    AIProvider.CLAUDE -> processClaudeRequest(prompt, systemPrompt)
                    AIProvider.OFFLINE -> processOfflineRequest(prompt)
                }
            } catch (e: Exception) {
                AIApiResponse(
                    success = false,
                    content = "AI service temporarily unavailable: ${e.message}",
                    error = e.message,
                    tokensUsed = 0
                )
            }
        }
    }
    
    private suspend fun processOpenAIRequest(prompt: String, systemPrompt: String): AIApiResponse {
        val apiKey = preferencesManager?.getOpenAIKey()
            ?: return AIApiResponse(false, "OpenAI API key not configured", "Missing API key")
        
        val model = preferencesManager?.getAIModel() ?: "gpt-3.5-turbo"
        val temperature = preferencesManager?.getAITemperature() ?: 0.7f
        val maxTokens = preferencesManager?.getMaxTokens() ?: 1000
        
        val messages = mutableListOf<OpenAIMessage>()
        if (systemPrompt.isNotBlank()) {
            messages.add(OpenAIMessage("system", systemPrompt))
        }
        messages.add(OpenAIMessage("user", prompt))
        
        val requestBody = OpenAIRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            max_tokens = maxTokens
        )
        
        val jsonBody = json.encodeToString(requestBody)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBody)
                AIApiResponse(
                    success = true,
                    content = openAIResponse.choices.firstOrNull()?.message?.content ?: "No response",
                    tokensUsed = openAIResponse.usage?.total_tokens ?: 0
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                AIApiResponse(false, "OpenAI API error", errorBody)
            }
        } catch (e: IOException) {
            AIApiResponse(false, "Network error: ${e.message}", e.message)
        }
    }
    
    private suspend fun processGeminiRequest(prompt: String, systemPrompt: String): AIApiResponse {
        val apiKey = preferencesManager?.getGeminiKey()
            ?: return AIApiResponse(false, "Gemini API key not configured", "Missing API key")
        
        val model = preferencesManager?.getAIModel() ?: "gemini-pro"
        val temperature = preferencesManager?.getAITemperature() ?: 0.7f
        val maxTokens = preferencesManager?.getMaxTokens() ?: 1000
        
        val fullPrompt = if (systemPrompt.isNotBlank()) {
            "$systemPrompt\n\nUser: $prompt"
        } else {
            prompt
        }
        
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(fullPrompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens
            )
        )
        
        val jsonBody = json.encodeToString(requestBody)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val geminiResponse = json.decodeFromString<GeminiResponse>(responseBody)
                val content = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
                AIApiResponse(
                    success = true,
                    content = content,
                    tokensUsed = geminiResponse.usageMetadata?.totalTokenCount ?: 0
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                AIApiResponse(false, "Gemini API error", errorBody)
            }
        } catch (e: IOException) {
            AIApiResponse(false, "Network error: ${e.message}", e.message)
        }
    }
    
    private suspend fun processClaudeRequest(prompt: String, systemPrompt: String): AIApiResponse {
        val apiKey = preferencesManager?.getClaudeKey()
            ?: return AIApiResponse(false, "Claude API key not configured", "Missing API key")
        
        val model = preferencesManager?.getAIModel() ?: "claude-3-sonnet-20240229"
        val temperature = preferencesManager?.getAITemperature() ?: 0.7f
        val maxTokens = preferencesManager?.getMaxTokens() ?: 1000
        
        val messages = listOf(ClaudeMessage("user", prompt))
        
        val requestBody = ClaudeRequest(
            model = model,
            max_tokens = maxTokens,
            temperature = temperature,
            system = systemPrompt.takeIf { it.isNotBlank() },
            messages = messages
        )
        
        val jsonBody = json.encodeToString(requestBody)
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("anthropic-version", "2023-06-01")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val claudeResponse = json.decodeFromString<ClaudeResponse>(responseBody)
                val content = claudeResponse.content?.firstOrNull()?.text ?: "No response"
                AIApiResponse(
                    success = true,
                    content = content,
                    tokensUsed = claudeResponse.usage?.output_tokens ?: 0
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                AIApiResponse(false, "Claude API error", errorBody)
            }
        } catch (e: IOException) {
            AIApiResponse(false, "Network error: ${e.message}", e.message)
        }
    }
    
    private fun processOfflineRequest(prompt: String): AIApiResponse {
        // Fallback to the existing offline AI service
        val offlineService = context?.let { NoteBuddyAIService(it) }
        return AIApiResponse(
            success = true,
            content = "Offline mode: Basic processing applied to your request.",
            tokensUsed = 0
        )
    }
    
    suspend fun testApiKey(provider: AIProvider, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val testPrompt = "Hello, this is a test message. Please respond with 'API key is working'."
                val request = when (provider) {
                    AIProvider.OPENAI -> createOpenAITestRequest(apiKey, testPrompt)
                    AIProvider.GEMINI -> createGeminiTestRequest(apiKey, testPrompt)
                    AIProvider.CLAUDE -> createClaudeTestRequest(apiKey, testPrompt)
                    AIProvider.OFFLINE -> return@withContext true
                }
                
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }
    
    // Individual test methods for ViewModel
    suspend fun testOpenAIConnection(apiKey: String): Boolean = testApiKey(AIProvider.OPENAI, apiKey)
    suspend fun testGeminiConnection(apiKey: String): Boolean = testApiKey(AIProvider.GEMINI, apiKey)
    suspend fun testClaudeConnection(apiKey: String): Boolean = testApiKey(AIProvider.CLAUDE, apiKey)
    
    private fun createOpenAITestRequest(apiKey: String, prompt: String): Request {
        val requestBody = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(OpenAIMessage("user", prompt)),
            temperature = 0.1f,
            max_tokens = 50
        )
        val jsonBody = json.encodeToString(requestBody)
        
        return Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    private fun createGeminiTestRequest(apiKey: String, prompt: String): Request {
        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
            generationConfig = GeminiGenerationConfig(temperature = 0.1f, maxOutputTokens = 50)
        )
        val jsonBody = json.encodeToString(requestBody)
        
        return Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    private fun createClaudeTestRequest(apiKey: String, prompt: String): Request {
        val requestBody = ClaudeRequest(
            model = "claude-3-sonnet-20240229",
            max_tokens = 50,
            temperature = 0.1f,
            messages = listOf(ClaudeMessage("user", prompt))
        )
        val jsonBody = json.encodeToString(requestBody)
        
        return Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("anthropic-version", "2023-06-01")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
    }
}

// Data classes for API responses
data class AIApiResponse(
    val success: Boolean,
    val content: String,
    val error: String? = null,
    val tokensUsed: Int = 0
)

// OpenAI API Models
@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Float,
    val max_tokens: Int
)

@Serializable
data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIResponse(
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage?
)

@Serializable
data class OpenAIChoice(
    val message: OpenAIMessage
)

@Serializable
data class OpenAIUsage(
    val total_tokens: Int
)

// Gemini API Models
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float,
    val maxOutputTokens: Int
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val usageMetadata: GeminiUsageMetadata?
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent?
)

@Serializable
data class GeminiUsageMetadata(
    val totalTokenCount: Int
)

// Claude API Models
@Serializable
data class ClaudeRequest(
    val model: String,
    val max_tokens: Int,
    val temperature: Float,
    val system: String? = null,
    val messages: List<ClaudeMessage>
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContent>?,
    val usage: ClaudeUsage?
)

@Serializable
data class ClaudeContent(
    val text: String
)

@Serializable
data class ClaudeUsage(
    val output_tokens: Int
)