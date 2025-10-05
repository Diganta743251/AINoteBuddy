package com.ainotebuddy.app.search

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.collections.set

private val Context.visualSearchCache: DataStore<Preferences> by preferencesDataStore(name = "visual_search_cache")

/**
 * Manages caching of extracted text from visual content to improve performance
 */
class VisualSearchCache(private val context: Context) {
    private val inMemoryCache = mutableMapOf<String, String>()
    private val cacheLifetime = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
    
    /**
     * Get cached text for a URI if it exists and is fresh
     */
    suspend fun getCachedText(uri: Uri): String? {
        val cacheKey = getCacheKey(uri)
        
        // Check in-memory cache first
        inMemoryCache[cacheKey]?.let { return it }
        
        // Check disk cache
        val prefs = context.visualSearchCache.data.firstOrNull() ?: return null
        val cachedValue = prefs[stringPreferencesKey(cacheKey)] ?: return null
        
        // Parse the cached value (format: timestamp|text)
        val parts = cachedValue.split("|", limit = 2)
        if (parts.size != 2) return null
        
        val timestamp = parts[0].toLongOrNull() ?: return null
        val currentTime = System.currentTimeMillis()
        
        // Check if cache is still valid
        return if (currentTime - timestamp < cacheLifetime) {
            // Cache hit, update in-memory cache
            inMemoryCache[cacheKey] = parts[1]
            parts[1]
        } else {
            // Cache expired, remove it
            removeFromCache(uri)
            null
        }
    }
    
    /**
     * Cache extracted text for a URI
     */
    suspend fun cacheText(uri: Uri, text: String) {
        val cacheKey = getCacheKey(uri)
        val timestamp = System.currentTimeMillis()
        val cacheValue = "$timestamp|$text"
        
        // Update in-memory cache
        inMemoryCache[cacheKey] = text
        
        // Update disk cache
        context.visualSearchCache.edit { prefs ->
            prefs[stringPreferencesKey(cacheKey)] = cacheValue
        }
    }
    
    /**
     * Remove an entry from the cache
     */
    suspend fun removeFromCache(uri: Uri) {
        val cacheKey = getCacheKey(uri)
        inMemoryCache.remove(cacheKey)
        
        context.visualSearchCache.edit { prefs ->
            prefs.remove(stringPreferencesKey(cacheKey))
        }
    }
    
    /**
     * Clear all cached data
     */
    suspend fun clearCache() {
        inMemoryCache.clear()
        context.visualSearchCache.edit { prefs ->
            prefs.clear()
        }
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats {
        val prefs = context.visualSearchCache.data.firstOrNull() ?: return CacheStats()
        val currentTime = System.currentTimeMillis()
        var totalSize = 0L
        var expiredCount = 0
        var validCount = 0
        
        prefs.asMap().values.forEach { pref ->
            if (pref is String) {
                val parts = pref.split("|", limit = 2)
                if (parts.size == 2) {
                    val timestamp = parts[0].toLongOrNull()
                    if (timestamp != null) {
                        totalSize += pref.length.toLong()
                        if (currentTime - timestamp < cacheLifetime) {
                            validCount++
                        } else {
                            expiredCount++
                        }
                    }
                }
            }
        }
        
        return CacheStats(
            inMemorySize = inMemoryCache.size,
            diskSize = totalSize,
            validEntries = validCount,
            expiredEntries = expiredCount
        )
    }
    
    private fun getCacheKey(uri: Uri): String {
        // Use a combination of URI and last modified time as cache key
        val lastModified = try {
            val file = context.contentResolver.openFileDescriptor(uri, "r")?.use {
                // ParcelFileDescriptor doesn't expose last modified reliably; use size + current time as a salt
                it.statSize.toString() + "_" + System.currentTimeMillis().toString()
            } ?: ""
        } catch (e: Exception) {
            ""
        }
        return "${uri.toString().hashCode()}_$lastModified"
    }
    
    data class CacheStats(
        val inMemorySize: Int = 0,
        val diskSize: Long = 0,
        val validEntries: Int = 0,
        val expiredEntries: Int = 0
    )
}

/**
 * Extension function to get cache stats in a blocking way (for Java interop)
 */
fun VisualSearchCache.getCacheStatsBlocking(): VisualSearchCache.CacheStats {
    return runBlocking {
        getCacheStats()
    }
}
