package com.ainotebuddy.app.offline

import com.ainotebuddy.app.data.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced conflict resolution engine for Enhanced Offline-First Architecture
 * Provides intelligent merge strategies and conflict detection algorithms
 */
@Singleton
class ConflictResolutionEngine @Inject constructor() {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Resolve conflicts when creating a new note
     */
    suspend fun resolveCreateNoteConflict(
        newNote: NoteEntity,
        existingNotes: List<NoteEntity>
    ): ConflictResolution = withContext(Dispatchers.Default) {
        
        // Find the most similar existing note
        val mostSimilar = findMostSimilarNote(newNote, existingNotes)
        
        if (mostSimilar != null) {
            val similarity = calculateContentSimilarity(newNote.content, mostSimilar.content)
            
            when {
                similarity > 0.9 -> {
                    // Very similar content - suggest merge
                    val mergedNote = mergeNotes(newNote, mostSimilar)
                    ConflictResolution(
                        strategy = ConflictResolutionStrategy.AUTO_MERGE,
                        confidence = similarity,
                        resolvedNote = mergedNote,
                        conflictData = ConflictData(
                            conflictType = ConflictType.CONTENT,
                            localVersion = serializeNote(newNote),
                            remoteVersion = serializeNote(mostSimilar),
                            suggestedResolution = ConflictResolutionStrategy.AUTO_MERGE,
                            confidence = similarity
                        )
                    )
                }
                similarity > 0.7 -> {
                    // Moderately similar - require user choice
                    ConflictResolution(
                        strategy = ConflictResolutionStrategy.USER_CHOICE,
                        confidence = similarity,
                        conflictData = ConflictData(
                            conflictType = ConflictType.CONTENT,
                            localVersion = serializeNote(newNote),
                            remoteVersion = serializeNote(mostSimilar),
                            suggestedResolution = ConflictResolutionStrategy.SIDE_BY_SIDE,
                            confidence = similarity
                        )
                    )
                }
                else -> {
                    // Different enough - allow creation
                    ConflictResolution(
                        strategy = ConflictResolutionStrategy.ACCEPT_LOCAL,
                        confidence = 1.0f,
                        resolvedNote = newNote
                    )
                }
            }
        } else {
            // No similar notes found
            ConflictResolution(
                strategy = ConflictResolutionStrategy.ACCEPT_LOCAL,
                confidence = 1.0f,
                resolvedNote = newNote
            )
        }
    }
    
    /**
     * Resolve conflicts when updating a note
     */
    suspend fun resolveUpdateConflict(
        currentNote: NoteEntity,
        proposedChanges: Map<String, String>,
        conflictData: ConflictData
    ): ConflictResolution = withContext(Dispatchers.Default) {
        
        when (conflictData.conflictType) {
            ConflictType.VERSION -> resolveVersionConflict(currentNote, proposedChanges, conflictData)
            ConflictType.CONTENT -> resolveContentConflict(currentNote, proposedChanges, conflictData)
            ConflictType.METADATA -> resolveMetadataConflict(currentNote, proposedChanges, conflictData)
            ConflictType.STRUCTURAL -> resolveStructuralConflict(currentNote, proposedChanges, conflictData)
            ConflictType.COLLABORATIVE -> resolveCollaborativeConflict(currentNote, proposedChanges, conflictData)
        }
    }
    
    /**
     * Resolve version conflicts (when note was modified by another client)
     */
    private suspend fun resolveVersionConflict(
        currentNote: NoteEntity,
        proposedChanges: Map<String, String>,
        conflictData: ConflictData
    ): ConflictResolution {
        
        // Apply three-way merge strategy
        val baseContent = conflictData.baseVersion ?: currentNote.content
        val localContent = proposedChanges["content"] ?: currentNote.content
        val remoteContent = currentNote.content
        
        val mergeResult = performThreeWayMerge(baseContent, localContent, remoteContent)
        
        return when {
            mergeResult.hasConflicts -> {
                // Manual resolution required
                ConflictResolution(
                    strategy = ConflictResolutionStrategy.USER_CHOICE,
                    confidence = 0.5f,
                    conflictData = conflictData.copy(
                        conflictMarkers = mergeResult.conflictMarkers,
                        suggestedResolution = ConflictResolutionStrategy.THREE_WAY_MERGE
                    )
                )
            }
            mergeResult.confidence > 0.8f -> {
                // High confidence automatic merge
                val resolvedChanges = proposedChanges.toMutableMap()
                resolvedChanges["content"] = mergeResult.mergedContent
                
                ConflictResolution(
                    strategy = ConflictResolutionStrategy.AUTO_MERGE,
                    confidence = mergeResult.confidence,
                    resolvedChanges = resolvedChanges
                )
            }
            else -> {
                // Low confidence - suggest AI assistance
                ConflictResolution(
                    strategy = ConflictResolutionStrategy.AI_ASSISTED,
                    confidence = mergeResult.confidence,
                    conflictData = conflictData.copy(
                        suggestedResolution = ConflictResolutionStrategy.AI_ASSISTED
                    )
                )
            }
        }
    }
    
    /**
     * Resolve content conflicts
     */
    private suspend fun resolveContentConflict(
        currentNote: NoteEntity,
        proposedChanges: Map<String, String>,
        conflictData: ConflictData
    ): ConflictResolution {
        
        val localContent = proposedChanges["content"] ?: currentNote.content
        val remoteContent = currentNote.content
        
        // Analyze the nature of content changes
        val localChanges = analyzeContentChanges(currentNote.content, localContent)
        val remoteChanges = analyzeContentChanges(currentNote.content, remoteContent)
        
        return when {
            localChanges.changeType == ContentChangeType.ADDITION && remoteChanges.changeType == ContentChangeType.ADDITION -> {
                // Both added content - try to merge
                val mergedContent = mergeContentAdditions(currentNote.content, localContent, remoteContent)
                val resolvedChanges = proposedChanges.toMutableMap()
                resolvedChanges["content"] = mergedContent
                
                ConflictResolution(
                    strategy = ConflictResolutionStrategy.AUTO_MERGE,
                    confidence = 0.7f,
                    resolvedChanges = resolvedChanges
                )
            }
            localChanges.changeType == ContentChangeType.DELETION && remoteChanges.changeType == ContentChangeType.MODIFICATION -> {
                // Local deletion vs remote modification - require user choice
                ConflictResolution(
                    strategy = ConflictResolutionStrategy.USER_CHOICE,
                    confidence = 0.3f,
                    conflictData = conflictData.copy(
                        suggestedResolution = ConflictResolutionStrategy.SIDE_BY_SIDE
                    )
                )
            }
            else -> {
                // Default to user choice for complex conflicts
                ConflictResolution(
                    strategy = ConflictResolutionStrategy.USER_CHOICE,
                    confidence = 0.5f,
                    conflictData = conflictData
                )
            }
        }
    }
    
    /**
     * Resolve metadata conflicts (title, tags, category, etc.)
     */
    private suspend fun resolveMetadataConflict(
        currentNote: NoteEntity,
        proposedChanges: Map<String, String>,
        conflictData: ConflictData
    ): ConflictResolution {
        
        val resolvedChanges = mutableMapOf<String, String>()
        var totalConfidence = 0.0f
        var conflictCount = 0
        
        // Resolve each metadata field individually
        for ((field, proposedValue) in proposedChanges) {
            when (field) {
                "title" -> {
                    val resolution = resolveTitleConflict(currentNote.title, proposedValue)
                    resolvedChanges[field] = resolution.resolvedValue
                    totalConfidence += resolution.confidence
                    conflictCount++
                }
                "category" -> {
                    val resolution = resolveCategoryConflict(currentNote.category, proposedValue)
                    resolvedChanges[field] = resolution.resolvedValue
                    totalConfidence += resolution.confidence
                    conflictCount++
                }
                "tags" -> {
                    val resolution = resolveTagsConflict(currentNote.tags, proposedValue)
                    resolvedChanges[field] = resolution.resolvedValue
                    totalConfidence += resolution.confidence
                    conflictCount++
                }
                else -> {
                    // For other fields, use last-write-wins
                    resolvedChanges[field] = proposedValue
                    totalConfidence += 0.8f
                    conflictCount++
                }
            }
        }
        
        val averageConfidence = if (conflictCount > 0) totalConfidence / conflictCount else 0.5f
        
        return ConflictResolution(
            strategy = if (averageConfidence > 0.7f) ConflictResolutionStrategy.AUTO_MERGE else ConflictResolutionStrategy.USER_CHOICE,
            confidence = averageConfidence,
            resolvedChanges = resolvedChanges
        )
    }
    
    /**
     * Resolve structural conflicts (note format, structure changes)
     */
    private suspend fun resolveStructuralConflict(
        currentNote: NoteEntity,
        proposedChanges: Map<String, String>,
        conflictData: ConflictData
    ): ConflictResolution {
        
        // For structural conflicts, typically require user intervention
        return ConflictResolution(
            strategy = ConflictResolutionStrategy.USER_CHOICE,
            confidence = 0.3f,
            conflictData = conflictData.copy(
                suggestedResolution = ConflictResolutionStrategy.SIDE_BY_SIDE
            )
        )
    }
    
    /**
     * Resolve collaborative conflicts (real-time editing conflicts)
     */
    private suspend fun resolveCollaborativeConflict(
        currentNote: NoteEntity,
        proposedChanges: Map<String, String>,
        conflictData: ConflictData
    ): ConflictResolution {
        
        // Collaborative conflicts should use operational transform
        // This is handled by the existing collaborative editing system
        return ConflictResolution(
            strategy = ConflictResolutionStrategy.AUTO_MERGE,
            confidence = 0.9f,
            resolvedChanges = proposedChanges // OT should have already resolved conflicts
        )
    }
    
    /**
     * Perform three-way merge algorithm
     */
    private fun performThreeWayMerge(base: String, local: String, remote: String): MergeResult {
        if (base == local && base == remote) {
            return MergeResult(local, true, 1.0f, emptyList())
        }
        
        if (base == local) {
            // Only remote changed
            return MergeResult(remote, true, 0.9f, emptyList())
        }
        
        if (base == remote) {
            // Only local changed
            return MergeResult(local, true, 0.9f, emptyList())
        }
        
        if (local == remote) {
            // Both made same changes
            return MergeResult(local, true, 1.0f, emptyList())
        }
        
        // Both changed differently - attempt line-by-line merge
        val baseLines = base.lines()
        val localLines = local.lines()
        val remoteLines = remote.lines()
        
        val mergedLines = mutableListOf<String>()
        val conflictMarkers = mutableListOf<ConflictMarker>()
        
        val maxLines = maxOf(baseLines.size, localLines.size, remoteLines.size)
        var conflictCount = 0
        
        for (i in 0 until maxLines) {
            val baseLine = baseLines.getOrNull(i) ?: ""
            val localLine = localLines.getOrNull(i) ?: ""
            val remoteLine = remoteLines.getOrNull(i) ?: ""
            
            when {
                baseLine == localLine && baseLine == remoteLine -> {
                    mergedLines.add(baseLine)
                }
                baseLine == localLine -> {
                    mergedLines.add(remoteLine)
                }
                baseLine == remoteLine -> {
                    mergedLines.add(localLine)
                }
                localLine == remoteLine -> {
                    mergedLines.add(localLine)
                }
                else -> {
                    // Conflict - add both versions with markers
                    val conflictStart = mergedLines.size
                    mergedLines.add("<<<<<<< LOCAL")
                    mergedLines.add(localLine)
                    mergedLines.add("=======")
                    mergedLines.add(remoteLine)
                    mergedLines.add(">>>>>>> REMOTE")
                    
                    conflictMarkers.add(
                        ConflictMarker(
                            startIndex = conflictStart,
                            endIndex = mergedLines.size - 1,
                            conflictType = "LINE_CONFLICT",
                            localContent = localLine,
                            remoteContent = remoteLine
                        )
                    )
                    conflictCount++
                }
            }
        }
        
        val hasConflicts = conflictCount > 0
        val confidence = if (hasConflicts) 0.3f else 0.8f
        
        return MergeResult(
            mergedContent = mergedLines.joinToString("\n"),
            hasConflicts = hasConflicts,
            confidence = confidence,
            conflictMarkers = conflictMarkers
        )
    }
    
    /**
     * Find the most similar note from a list
     */
    private fun findMostSimilarNote(targetNote: NoteEntity, candidates: List<NoteEntity>): NoteEntity? {
        if (candidates.isEmpty()) return null
        
        return candidates.maxByOrNull { candidate ->
            val titleSimilarity = calculateStringSimilarity(targetNote.title, candidate.title)
            val contentSimilarity = calculateContentSimilarity(targetNote.content, candidate.content)
            val categorySimilarity = if (targetNote.category == candidate.category) 1.0f else 0.0f
            
            // Weighted similarity score
            (titleSimilarity * 0.3f + contentSimilarity * 0.6f + categorySimilarity * 0.1f)
        }
    }
    
    /**
     * Calculate content similarity between two strings
     */
    private fun calculateContentSimilarity(content1: String, content2: String): Float {
        if (content1 == content2) return 1.0f
        if (content1.isEmpty() || content2.isEmpty()) return 0.0f
        
        // Use Jaccard similarity on word sets
        val words1 = content1.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        val words2 = content2.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) intersection.toFloat() / union.toFloat() else 0.0f
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateStringSimilarity(str1: String, str2: String): Float {
        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f
        
        val distance = levenshteinDistance(str1, str2)
        val maxLength = maxOf(str1.length, str2.length)
        
        return 1.0f - (distance.toFloat() / maxLength.toFloat())
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) dp[i][0] = i
        for (j in 0..str2.length) dp[0][j] = j
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                dp[i][j] = if (str1[i - 1] == str2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[str1.length][str2.length]
    }
    
    /**
     * Merge two notes intelligently
     */
    private fun mergeNotes(note1: NoteEntity, note2: NoteEntity): NoteEntity {
        return note1.copy(
            title = if (note1.title.length > note2.title.length) note1.title else note2.title,
            content = mergeContent(note1.content, note2.content),
            tags = mergeTags(note1.tags, note2.tags),
            category = if (note1.category != "General") note1.category else note2.category,
            updatedAt = maxOf(note1.updatedAt, note2.updatedAt),
            version = maxOf(note1.version, note2.version) + 1
        )
    }
    
    /**
     * Merge content from two sources
     */
    private fun mergeContent(content1: String, content2: String): String {
        if (content1.contains(content2)) return content1
        if (content2.contains(content1)) return content2
        
        // Simple merge - combine unique sentences
        val sentences1 = content1.split(Regex("[.!?]+")).map { it.trim() }.filter { it.isNotBlank() }
        val sentences2 = content2.split(Regex("[.!?]+")).map { it.trim() }.filter { it.isNotBlank() }
        
        val mergedSentences = (sentences1 + sentences2).distinct()
        return mergedSentences.joinToString(". ") + if (mergedSentences.isNotEmpty()) "." else ""
    }
    
    /**
     * Merge tags from two sources
     */
    private fun mergeTags(tags1: String, tags2: String): String {
        val tagSet1 = tags1.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val tagSet2 = tags2.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        
        return (tagSet1 + tagSet2).joinToString(", ")
    }
    
    /**
     * Analyze content changes
     */
    private fun analyzeContentChanges(original: String, modified: String): ContentChangeAnalysis {
        val originalLength = original.length
        val modifiedLength = modified.length
        
        val changeType = when {
            modifiedLength > originalLength * 1.2 -> ContentChangeType.ADDITION
            modifiedLength < originalLength * 0.8 -> ContentChangeType.DELETION
            original != modified -> ContentChangeType.MODIFICATION
            else -> ContentChangeType.NONE
        }
        
        val similarity = calculateContentSimilarity(original, modified)
        
        return ContentChangeAnalysis(changeType, similarity)
    }
    
    /**
     * Merge content additions from multiple sources
     */
    private fun mergeContentAdditions(base: String, addition1: String, addition2: String): String {
        // Simple strategy: append both additions if they don't overlap
        val baseWords = base.split(Regex("\\s+")).toSet()
        val words1 = addition1.split(Regex("\\s+")).filter { it !in baseWords }
        val words2 = addition2.split(Regex("\\s+")).filter { it !in baseWords }
        
        val uniqueAdditions = (words1 + words2).distinct()
        
        return if (uniqueAdditions.isNotEmpty()) {
            "$base ${uniqueAdditions.joinToString(" ")}"
        } else {
            base
        }
    }
    
    /**
     * Resolve title conflicts
     */
    private fun resolveTitleConflict(currentTitle: String, proposedTitle: String): FieldResolution {
        return when {
            currentTitle == proposedTitle -> FieldResolution(currentTitle, 1.0f)
            currentTitle.isBlank() -> FieldResolution(proposedTitle, 0.9f)
            proposedTitle.isBlank() -> FieldResolution(currentTitle, 0.9f)
            proposedTitle.length > currentTitle.length -> FieldResolution(proposedTitle, 0.7f)
            else -> FieldResolution(currentTitle, 0.7f)
        }
    }
    
    /**
     * Resolve category conflicts
     */
    private fun resolveCategoryConflict(currentCategory: String, proposedCategory: String): FieldResolution {
        return when {
            currentCategory == proposedCategory -> FieldResolution(currentCategory, 1.0f)
            currentCategory == "General" -> FieldResolution(proposedCategory, 0.9f)
            proposedCategory == "General" -> FieldResolution(currentCategory, 0.9f)
            else -> FieldResolution(proposedCategory, 0.6f) // Prefer newer category
        }
    }
    
    /**
     * Resolve tags conflicts
     */
    private fun resolveTagsConflict(currentTags: String, proposedTags: String): FieldResolution {
        val mergedTags = mergeTags(currentTags, proposedTags)
        val confidence = if (currentTags == proposedTags) 1.0f else 0.8f
        return FieldResolution(mergedTags, confidence)
    }
    
    /**
     * Serialize note for conflict data without requiring @Serializable on NoteEntity
     */
    private fun serializeNote(note: NoteEntity): String {
        fun esc(s: String): String = s.replace("\"", "\\\"")
        return "{" +
            "\"id\":${note.id}," +
            "\"title\":\"${esc(note.title)}\"," +
            "\"category\":\"${esc(note.category)}\"," +
            "\"version\":${note.version}" +
        "}"
    }
}

// Data classes for conflict resolution
data class ConflictResolution(
    val strategy: ConflictResolutionStrategy,
    val confidence: Float,
    val resolvedNote: NoteEntity? = null,
    val resolvedChanges: Map<String, String>? = null,
    val conflictData: ConflictData? = null,
    val requiresUserIntervention: Boolean = strategy == ConflictResolutionStrategy.USER_CHOICE
)

data class MergeResult(
    val mergedContent: String,
    val hasConflicts: Boolean,
    val confidence: Float,
    val conflictMarkers: List<ConflictMarker>
)

data class ContentChangeAnalysis(
    val changeType: ContentChangeType,
    val similarity: Float
)

enum class ContentChangeType {
    NONE,
    ADDITION,
    DELETION,
    MODIFICATION
}

data class FieldResolution(
    val resolvedValue: String,
    val confidence: Float
)
