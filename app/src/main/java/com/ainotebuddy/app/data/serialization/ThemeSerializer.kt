package com.ainotebuddy.app.data.serialization

import android.content.Context
import android.net.Uri
import com.ainotebuddy.app.data.model.ThemePreset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Utility class for serializing and deserializing theme presets
 */
class ThemeSerializer(private val context: Context) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Serialize a theme preset to a JSON string
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun serializeThemePreset(preset: ThemePreset): String {
        return json.encodeToString(preset)
    }

    /**
     * Deserialize a theme preset from a JSON string
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun deserializeThemePreset(jsonString: String): Result<ThemePreset> {
        return try {
            Result.success(json.decodeFromString<ThemePreset>(jsonString))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export a theme preset to a file
     */
    fun exportThemePreset(preset: ThemePreset, uri: Uri): Result<Unit> {
        return try {
            val jsonString = serializeThemePreset(preset)
            context.contentResolver.openOutputStream(uri)?.use { outputStream -> 
                outputStream.write(jsonString.toByteArray())
                Result.success(Unit)
            } ?: Result.failure(Exception("Failed to open output stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import a theme preset from a file
     */
    fun importThemePreset(uri: Uri): Result<ThemePreset> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                deserializeThemePreset(jsonString)
            } ?: Result.failure(Exception("Failed to open input stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert an input stream to a string
     */
    private fun inputStreamToString(inputStream: InputStream): String {
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } != -1) {
            outputStream.write(buffer, 0, length)
        }
        return outputStream.toString("UTF-8")
    }
}
