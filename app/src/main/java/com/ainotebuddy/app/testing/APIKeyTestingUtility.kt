package com.ainotebuddy.app.testing

import android.content.Context
import com.ainotebuddy.app.data.PreferencesManager
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for testing and validating API keys during development and user setup
 * This ensures users can properly configure their API keys and understand costs
 */
@Singleton
class APIKeyTestingUtility @Inject constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    /**
     * Test result for API key validation
     */
    data class APIKeyTestResult(
        val isValid: Boolean,
        val service: String,
        val errorMessage: String? = null,
        val usageInfo: String? = null,
        val costEstimate: String? = null
    )
    
    /**
     * Test OpenAI API key validity and get usage information
     */
    suspend fun testOpenAIKey(apiKey: String): APIKeyTestResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.openai.com/v1/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    APIKeyTestResult(
                        isValid = true,
                        service = "OpenAI",
                        usageInfo = "API key is valid and ready to use",
                        costEstimate = "Costs: ~$0.002 per 1K tokens (GPT-3.5) or ~$0.03 per 1K tokens (GPT-4)"
                    )
                }
                401 -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "OpenAI",
                        errorMessage = "Invalid API key. Please check your OpenAI API key."
                    )
                }
                429 -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "OpenAI",
                        errorMessage = "Rate limit exceeded. Your API key is valid but you've hit usage limits."
                    )
                }
                else -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "OpenAI",
                        errorMessage = "API error (${response.code}): ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            APIKeyTestResult(
                isValid = false,
                service = "OpenAI",
                errorMessage = "Connection error: ${e.message}. Check your internet connection."
            )
        }
    }
    
    /**
     * Test Gemini API key validity
     */
    suspend fun testGeminiKey(apiKey: String): APIKeyTestResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models?key=$apiKey")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    APIKeyTestResult(
                        isValid = true,
                        service = "Gemini",
                        usageInfo = "API key is valid and ready to use",
                        costEstimate = "Costs: Free tier available, then ~$0.0005 per 1K characters"
                    )
                }
                400 -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "Gemini",
                        errorMessage = "Invalid API key format. Please check your Gemini API key."
                    )
                }
                403 -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "Gemini",
                        errorMessage = "API key denied. Please verify your Gemini API key permissions."
                    )
                }
                else -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "Gemini",
                        errorMessage = "API error (${response.code}): ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            APIKeyTestResult(
                isValid = false,
                service = "Gemini",
                errorMessage = "Connection error: ${e.message}. Check your internet connection."
            )
        }
    }
    
    /**
     * Test Claude API key validity
     */
    suspend fun testClaudeKey(apiKey: String): APIKeyTestResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", "claude-3-haiku-20240307")
                put("max_tokens", 1)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Hi")
                    })
                })
            }
            
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    APIKeyTestResult(
                        isValid = true,
                        service = "Claude",
                        usageInfo = "API key is valid and ready to use",
                        costEstimate = "Costs: ~$0.25 per 1M tokens (Claude 3 Haiku) or ~$3 per 1M tokens (Claude 3 Opus)"
                    )
                }
                401 -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "Claude",
                        errorMessage = "Invalid API key. Please check your Claude API key."
                    )
                }
                429 -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "Claude",
                        errorMessage = "Rate limit exceeded. Your API key is valid but you've hit usage limits."
                    )
                }
                else -> {
                    APIKeyTestResult(
                        isValid = false,
                        service = "Claude",
                        errorMessage = "API error (${response.code}): ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            APIKeyTestResult(
                isValid = false,
                service = "Claude",
                errorMessage = "Connection error: ${e.message}. Check your internet connection."
            )
        }
    }
    
    /**
     * Test all configured API keys
     */
    suspend fun testAllConfiguredKeys(): List<APIKeyTestResult> {
        val results = mutableListOf<APIKeyTestResult>()
        
        // Test OpenAI key if configured
        preferencesManager.getOpenAIKey()?.let { key ->
            if (key.isNotBlank()) {
                results.add(testOpenAIKey(key))
            }
        }
        
        // Test Gemini key if configured
        preferencesManager.getGeminiKey()?.let { key ->
            if (key.isNotBlank()) {
                results.add(testGeminiKey(key))
            }
        }
        
        // Test Claude key if configured
        preferencesManager.getClaudeKey()?.let { key ->
            if (key.isNotBlank()) {
                results.add(testClaudeKey(key))
            }
        }
        
        return results
    }
    
    /**
     * Generate user-friendly setup instructions for API keys
     */
    fun getAPIKeySetupInstructions(): Map<String, String> {
        return mapOf(
            "OpenAI" to """
                1. Visit https://platform.openai.com/api-keys
                2. Sign in or create an account
                3. Click "Create new secret key"
                4. Copy the key and paste it in AINoteBuddy settings
                5. Note: You'll be charged based on usage (~$0.002 per 1K tokens for GPT-3.5)
            """.trimIndent(),
            
            "Gemini" to """
                1. Visit https://makersuite.google.com/app/apikey
                2. Sign in with your Google account
                3. Click "Create API key"
                4. Copy the key and paste it in AINoteBuddy settings
                5. Note: Free tier available, then ~$0.0005 per 1K characters
            """.trimIndent(),
            
            "Claude" to """
                1. Visit https://console.anthropic.com/
                2. Sign in or create an account
                3. Go to "API Keys" section
                4. Click "Create Key"
                5. Copy the key and paste it in AINoteBuddy settings
                6. Note: Costs ~$0.25 per 1M tokens (Haiku) to $3 per 1M tokens (Opus)
            """.trimIndent()
        )
    }
    
    /**
     * Get cost estimation for typical usage patterns
     */
    fun getCostEstimations(): Map<String, String> {
        return mapOf(
            "Light Usage (10 notes/day)" to """
                • OpenAI GPT-3.5: ~$1-3/month
                • Gemini: Free tier likely sufficient
                • Claude Haiku: ~$0.50-1.50/month
            """.trimIndent(),
            
            "Moderate Usage (50 notes/day)" to """
                • OpenAI GPT-3.5: ~$5-15/month
                • Gemini: ~$2-5/month
                • Claude Haiku: ~$2-8/month
            """.trimIndent(),
            
            "Heavy Usage (200 notes/day)" to """
                • OpenAI GPT-3.5: ~$20-60/month
                • Gemini: ~$8-20/month
                • Claude Haiku: ~$10-30/month
            """.trimIndent()
        )
    }
    
    /**
     * Check if user has at least one valid API key configured
     */
    suspend fun hasValidAPIKey(): Boolean {
        val results = testAllConfiguredKeys()
        return results.any { it.isValid }
    }
    
    /**
     * Get recommendations for API key setup based on user needs
     */
    fun getAPIKeyRecommendations(): Map<String, String> {
        return mapOf(
            "Budget-Conscious Users" to "Start with Gemini (free tier) or Claude Haiku (lowest cost)",
            "Quality-Focused Users" to "OpenAI GPT-4 or Claude Opus for best results",
            "Balanced Users" to "OpenAI GPT-3.5 or Gemini Pro for good quality at reasonable cost",
            "Privacy-Focused Users" to "All services process data on their servers - review their privacy policies"
        )
    }
}

/**
 * Extension functions for easy testing during development
 */
object APIKeyTestingHelper {
    
    /**
     * Quick test function for development/debugging
     */
    suspend fun quickTest(context: Context) {
        val prefsManager = PreferencesManager(context)
        val testingUtility = APIKeyTestingUtility(context, prefsManager)
        
        println("=== API Key Testing Results ===")
        val results = testingUtility.testAllConfiguredKeys()
        
        if (results.isEmpty()) {
            println("No API keys configured for testing")
        } else {
            results.forEach { result ->
                println("${result.service}: ${if (result.isValid) "✅ Valid" else "❌ Invalid"}")
                result.errorMessage?.let { println("  Error: $it") }
                result.usageInfo?.let { println("  Info: $it") }
                result.costEstimate?.let { println("  Cost: $it") }
                println()
            }
        }
    }
}
