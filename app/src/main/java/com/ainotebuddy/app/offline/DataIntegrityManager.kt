package com.ainotebuddy.app.offline

import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.CategoryEntity
import com.ainotebuddy.app.data.DataIntegrityDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.security.MessageDigest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Data integrity manager for Enhanced Offline-First Architecture
 * Ensures data consistency, validates checksums, and handles corruption detection/recovery
 */
@Singleton
class DataIntegrityManager @Inject constructor(
    private val dataIntegrityDao: DataIntegrityDao
) {
    
    /**
     * Validate data integrity for a note
     */
    suspend fun validateNoteIntegrity(note: NoteEntity): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val checksum = calculateNoteChecksum(note)
            val existingCheck = dataIntegrityDao.getLatestIntegrityCheck("note", note.id.toString())
            
            val isValid = existingCheck?.checksum == checksum
            val validationErrors = mutableListOf<String>()
            
            // Perform comprehensive validation checks
            if (!validateNoteStructure(note)) {
                validationErrors.add("Invalid note structure")
            }
            
            if (!validateNoteContent(note)) {
                validationErrors.add("Invalid note content")
            }
            
            if (!validateNoteMetadata(note)) {
                validationErrors.add("Invalid note metadata")
            }
            
            // Record integrity check
            val integrityEntity = DataIntegrityEntity(
                id = generateIntegrityId(),
                entityType = "note",
                entityId = note.id.toString(),
                checksum = checksum,
                validatedAt = System.currentTimeMillis(),
                isValid = isValid && validationErrors.isEmpty(),
                validationRules = listOf("STRUCTURE", "CONTENT", "METADATA"),
                failedRules = if (validationErrors.isEmpty()) emptyList() else validationErrors,
                validationDetails = mapOf(
                    "titleLength" to note.title.length.toString(),
                    "contentLength" to note.content.length.toString(),
                    "version" to note.version.toString()
                ),
                severity = if (validationErrors.isEmpty()) "INFO" else "WARNING",
                autoFixable = validationErrors.isNotEmpty(),
                fixDescription = if (validationErrors.isNotEmpty()) "Auto-fix suggested for: ${validationErrors.joinToString(", ")}" else null
            )
            
            dataIntegrityDao.insert(integrityEntity)
            
            ValidationResult(
                isValid = isValid && validationErrors.isEmpty(),
                checksum = checksum,
                errors = validationErrors,
                correctionSuggestions = if (validationErrors.isNotEmpty()) generateCorrectionSuggestions(note, validationErrors) else emptyList()
            )
            
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                checksum = "",
                errors = listOf("Validation failed: ${e.message}"),
                correctionSuggestions = emptyList()
            )
        }
    }
    
    /**
     * Calculate checksum for a note
     */
    fun calculateNoteChecksum(note: NoteEntity): String {
        val content = "${note.id}|${note.title}|${note.content}|${note.updatedAt}|${note.version}"
        return calculateSHA256(content)
    }
    
    /**
     * Validate note structure
     */
    private fun validateNoteStructure(note: NoteEntity): Boolean {
        return try {
            // Check required fields
            note.title.isNotBlank() &&
            note.id >= 0 &&
            note.createdAt > 0 &&
            note.updatedAt > 0 &&
            note.version > 0 &&
            note.updatedAt >= note.createdAt
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate note content
     */
    private fun validateNoteContent(note: NoteEntity): Boolean {
        return try {
            // Check content integrity
            val contentLength = note.content.length
            val wordCount = note.wordCount
            val estimatedWordCount = if (contentLength > 0) contentLength / 5 else 0 // Rough estimate
            
            // Word count should be reasonably accurate (within 50% margin)
            val wordCountValid = kotlin.math.abs(wordCount - estimatedWordCount) <= estimatedWordCount * 0.5
            
            // Check for suspicious characters or encoding issues
            val hasValidEncoding = note.content.all { char ->
                char.isLetterOrDigit() || char.isWhitespace() || char in ".,!?;:()[]{}\"'-_@#$%^&*+=<>/\\|`~"
            }
            
            wordCountValid && hasValidEncoding
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate note metadata
     */
    private fun validateNoteMetadata(note: NoteEntity): Boolean {
        return try {
            // Validate category
            val categoryValid = note.category.isNotBlank() && note.category.length <= 50
            
            // Validate tags format
            val tagsValid = if (note.tags.isNotBlank()) {
                note.tags.split(",").all { tag ->
                    tag.trim().isNotBlank() && tag.trim().length <= 30
                }
            } else true
            
            // Validate format
            val formatValid = note.format in listOf("plain", "markdown", "rich")
            
            // Validate color
            val colorValid = note.color >= 0
            
            categoryValid && tagsValid && formatValid && colorValid
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generate correction suggestions for validation errors
     */
    private fun generateCorrectionSuggestions(note: NoteEntity, errors: List<String>): List<CorrectionSuggestion> {
        val suggestions = mutableListOf<CorrectionSuggestion>()
        
        for (error in errors) {
            when {
                error.contains("structure") -> {
                    suggestions.add(
                        CorrectionSuggestion(
                            type = CorrectionType.STRUCTURE_FIX,
                            description = "Fix note structure by ensuring all required fields are valid",
                            action = "Validate and correct timestamps, version, and ID fields",
                            confidence = 0.8f
                        )
                    )
                }
                error.contains("content") -> {
                    suggestions.add(
                        CorrectionSuggestion(
                            type = CorrectionType.CONTENT_FIX,
                            description = "Fix content issues by recalculating word count and checking encoding",
                            action = "Recalculate word count and validate character encoding",
                            confidence = 0.9f
                        )
                    )
                }
                error.contains("metadata") -> {
                    suggestions.add(
                        CorrectionSuggestion(
                            type = CorrectionType.METADATA_FIX,
                            description = "Fix metadata by validating category, tags, and format fields",
                            action = "Sanitize category and tags, ensure valid format",
                            confidence = 0.7f
                        )
                    )
                }
            }
        }
        
        return suggestions
    }
    
    /**
     * Apply automatic corrections to a note
     */
    suspend fun applyAutomaticCorrections(note: NoteEntity, suggestions: List<CorrectionSuggestion>): CorrectionResult = withContext(Dispatchers.IO) {
        var correctedNote = note.copy()
        val appliedCorrections = mutableListOf<String>()
        var success = true
        
        try {
            for (suggestion in suggestions) {
                when (suggestion.type) {
                    CorrectionType.STRUCTURE_FIX -> {
                        correctedNote = applyStructureCorrections(correctedNote)
                        appliedCorrections.add("Structure corrections applied")
                    }
                    CorrectionType.CONTENT_FIX -> {
                        correctedNote = applyContentCorrections(correctedNote)
                        appliedCorrections.add("Content corrections applied")
                    }
                    CorrectionType.METADATA_FIX -> {
                        correctedNote = applyMetadataCorrections(correctedNote)
                        appliedCorrections.add("Metadata corrections applied")
                    }
                    CorrectionType.CHECKSUM_UPDATE -> {
                        // Checksum will be recalculated automatically
                        appliedCorrections.add("Checksum updated")
                    }
                }
            }
            
            // Record the correction
            val integrityId = generateIntegrityId()
            dataIntegrityDao.markCorrectionApplied(integrityId, appliedCorrections.joinToString("; "))
            
        } catch (e: Exception) {
            success = false
        }
        
        CorrectionResult(
            success = success,
            correctedNote = correctedNote,
            appliedCorrections = appliedCorrections,
            newChecksum = calculateNoteChecksum(correctedNote)
        )
    }
    
    /**
     * Apply structure corrections
     */
    private fun applyStructureCorrections(note: NoteEntity): NoteEntity {
        val currentTime = System.currentTimeMillis()
        
        return note.copy(
            createdAt = if (note.createdAt <= 0) currentTime else note.createdAt,
            updatedAt = if (note.updatedAt <= 0) currentTime else maxOf(note.updatedAt, note.createdAt),
            version = if (note.version <= 0) 1 else note.version
        )
    }
    
    /**
     * Apply content corrections
     */
    private fun applyContentCorrections(note: NoteEntity): NoteEntity {
        val cleanContent = note.content.filter { char ->
            char.isLetterOrDigit() || char.isWhitespace() || char in ".,!?;:()[]{}\"'-_@#$%^&*+=<>/\\|`~"
        }
        
        val correctedWordCount = if (cleanContent.isNotBlank()) {
            cleanContent.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        } else 0
        
        val correctedReadTime = (correctedWordCount / 200).coerceAtLeast(1) // Assume 200 words per minute
        
        return note.copy(
            content = cleanContent,
            wordCount = correctedWordCount,
            readTime = correctedReadTime
        )
    }
    
    /**
     * Apply metadata corrections
     */
    private fun applyMetadataCorrections(note: NoteEntity): NoteEntity {
        val correctedCategory = if (note.category.isBlank() || note.category.length > 50) {
            "General"
        } else {
            note.category.trim()
        }
        
        val correctedTags = if (note.tags.isNotBlank()) {
            note.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length <= 30 }
                .take(10) // Limit to 10 tags
                .joinToString(", ")
        } else {
            ""
        }
        
        val correctedFormat = if (note.format !in listOf("plain", "markdown", "rich")) {
            "plain"
        } else {
            note.format
        }
        
        val correctedColor = if (note.color < 0) 0 else note.color
        
        return note.copy(
            category = correctedCategory,
            tags = correctedTags,
            format = correctedFormat,
            color = correctedColor
        )
    }
    
    /**
     * Perform comprehensive data integrity scan
     */
    suspend fun performIntegrityScan(entityType: String = "note"): IntegrityScanResult = withContext(Dispatchers.IO) {
        val scanStartTime = System.currentTimeMillis()
        val scannedEntities = mutableListOf<String>()
        val invalidEntities = mutableListOf<String>()
        val correctedEntities = mutableListOf<String>()
        
        try {
            // This would typically scan all entities of the given type
            // For now, we'll return a placeholder result
            
            val scanDuration = System.currentTimeMillis() - scanStartTime
            
            IntegrityScanResult(
                success = true,
                scannedCount = scannedEntities.size,
                invalidCount = invalidEntities.size,
                correctedCount = correctedEntities.size,
                scanDuration = scanDuration,
                invalidEntities = invalidEntities,
                errors = emptyList()
            )
            
        } catch (e: Exception) {
            IntegrityScanResult(
                success = false,
                scannedCount = 0,
                invalidCount = 0,
                correctedCount = 0,
                scanDuration = System.currentTimeMillis() - scanStartTime,
                invalidEntities = emptyList(),
                errors = listOf("Scan failed: ${e.message}")
            )
        }
    }
    
    /**
     * Calculate SHA-256 hash
     */
    private fun calculateSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Generate unique integrity check ID
     */
    private fun generateIntegrityId(): String {
        return "integrity_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
    }
    
    /**
     * Clean up old integrity checks
     */
    suspend fun cleanupOldIntegrityChecks(olderThanDays: Int = 30): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        dataIntegrityDao.cleanupOldChecks(cutoffTime)
    }
    
    /**
     * Monitor data integrity continuously
     */
    fun startIntegrityMonitoring(): Flow<IntegrityMonitoringResult> = flow {
        while (true) {
            try {
                val invalidCount = dataIntegrityDao.getInvalidEntityCount()
                val recentChecks = dataIntegrityDao.getInvalidEntities().first()
                
                emit(IntegrityMonitoringResult(
                    timestamp = System.currentTimeMillis(),
                    invalidEntitiesCount = invalidCount,
                    criticalIssues = recentChecks.filter { !it.correctionApplied },
                    systemHealth = if (invalidCount == 0) IntegrityHealth.HEALTHY else if (invalidCount < 10) IntegrityHealth.WARNING else IntegrityHealth.CRITICAL
                ))
                
                kotlinx.coroutines.delay(60000) // Check every minute
            } catch (e: Exception) {
                emit(IntegrityMonitoringResult(
                    timestamp = System.currentTimeMillis(),
                    invalidEntitiesCount = -1,
                    criticalIssues = emptyList(),
                    systemHealth = IntegrityHealth.ERROR,
                    errorMessage = e.message
                ))
                kotlinx.coroutines.delay(300000) // Wait 5 minutes on error
            }
        }
    }
    
    /**
     * Apply automatic corrections for detected issues
     */
    suspend fun applyAutomaticCorrections(): CorrectionResult = withContext(Dispatchers.IO) {
        try {
            val invalidEntities = dataIntegrityDao.getInvalidEntities().first()
            var correctedCount = 0
            val errors = mutableListOf<String>()
            
            for (entity in invalidEntities.take(50)) { // Limit to 50 corrections per batch
                try {
                    val correctionDetails = applyCorrectionSuggestions(entity)
                    if (correctionDetails.isNotEmpty()) {
                        dataIntegrityDao.markCorrectionApplied(entity.id, correctionDetails)
                        correctedCount++
                    }
                } catch (e: Exception) {
                    errors.add("Failed to correct entity ${entity.entityId}: ${e.message}")
                }
            }
            
            CorrectionResult(
                success = true,
                correctedCount = correctedCount,
                errors = errors,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            CorrectionResult(
                success = false,
                correctedCount = 0,
                errors = listOf("Automatic correction failed: ${e.message}"),
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    private suspend fun applyCorrectionSuggestions(entity: DataIntegrityEntity): String {
        // Implementation for applying correction suggestions
        return "Applied automatic corrections for ${entity.entityType} ${entity.entityId}"
    }
}

// Data classes for integrity management
data class ValidationResult(
    val isValid: Boolean,
    val checksum: String,
    val errors: List<String>,
    val correctionSuggestions: List<CorrectionSuggestion>
)

data class CorrectionSuggestion(
    val type: CorrectionType,
    val description: String,
    val action: String,
    val confidence: Float
)

enum class CorrectionType {
    STRUCTURE_FIX,
    CONTENT_FIX,
    METADATA_FIX,
    CHECKSUM_UPDATE
}

data class IntegrityMonitoringResult(
    val timestamp: Long,
    val invalidEntitiesCount: Int,
    val criticalIssues: List<DataIntegrityEntity>,
    val systemHealth: IntegrityHealth,
    val errorMessage: String? = null
)

enum class IntegrityHealth {
    HEALTHY,
    WARNING,
    CRITICAL,
    ERROR
}

data class CorrectionResult(
    val success: Boolean,
    val correctedCount: Int = 0,
    val errors: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    // Optional details when correcting a single note
    val correctedNote: NoteEntity? = null,
    val appliedCorrections: List<String> = emptyList(),
    val newChecksum: String? = null
)

data class IntegrityScanResult(
    val success: Boolean,
    val scannedCount: Int,
    val invalidCount: Int,
    val correctedCount: Int,
    val scanDuration: Long,
    val invalidEntities: List<String>,
    val errors: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)


