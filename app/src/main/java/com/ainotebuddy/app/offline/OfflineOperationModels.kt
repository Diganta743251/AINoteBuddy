package com.ainotebuddy.app.offline

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ainotebuddy.app.data.NoteEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Comprehensive offline operation models for Enhanced Offline-First Architecture
 */



@Entity(
    tableName = "sync_state",
    primaryKeys = ["entityType", "entityId"]
)
data class SyncStateEntity(
    val entityType: String, // "note", "category", "tag", etc.
    val entityId: String,
    val localVersion: Long,
    val remoteVersion: Long,
    val lastSyncedAt: Long,
    val syncStatus: String, // SYNCED, PENDING, CONFLICT, ERROR
    val conflictData: String? = null, // JSON serialized conflict information
    val checksum: String, // Data integrity checksum
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null
)




// Sealed class hierarchy for offline operations
    @Serializable
    sealed class OfflineOperation {
        abstract val id: String
        abstract val timestamp: Long
        abstract val priority: Priority
        abstract val retryCount: Int
        abstract val maxRetries: Int
        abstract val networkRequirement: NetworkRequirement
        abstract val estimatedSize: Long
    
    @Serializable
        data class CreateNote(
        override val id: String,
        val noteData: String, // JSON payload for the note (avoid serializing Room entity)
        val tempId: String, // Temporary ID for offline creation
        override val timestamp: Long,
        override val priority: Priority = Priority.HIGH,
        override val retryCount: Int = 0,
        override val maxRetries: Int = 5,
        override val networkRequirement: NetworkRequirement = NetworkRequirement.ANY,
        override val estimatedSize: Long = noteData.length.toLong()
    ) : OfflineOperation()
    
    @Serializable
    data class UpdateNote(
        override val id: String,
        val noteId: Long,
        val changes: Map<String, String>, // Field name to new value
        val previousVersion: Long,
        val conflictResolution: ConflictResolutionStrategy? = null,
        override val timestamp: Long,
        override val priority: Priority = Priority.HIGH,
        override val retryCount: Int = 0,
        override val maxRetries: Int = 5,
        override val networkRequirement: NetworkRequirement = NetworkRequirement.ANY,
        override val estimatedSize: Long = changes.values.sumOf { it.length }.toLong()
    ) : OfflineOperation()
    
    @Serializable
    data class DeleteNote(
        override val id: String,
        val noteId: Long,
        val softDelete: Boolean = true,
        override val timestamp: Long,
        override val priority: Priority = Priority.HIGH,
        override val retryCount: Int = 0,
        override val maxRetries: Int = 3,
        override val networkRequirement: NetworkRequirement = NetworkRequirement.ANY,
        override val estimatedSize: Long = 0
    ) : OfflineOperation()
    
    @Serializable
    data class CreateCategory(
        override val id: String,
        val name: String,
        val description: String = "",
        val color: Int = 0,
        override val timestamp: Long,
        override val priority: Priority = Priority.MEDIUM,
        override val retryCount: Int = 0,
        override val maxRetries: Int = 3,
        override val networkRequirement: NetworkRequirement = NetworkRequirement.ANY,
        override val estimatedSize: Long = name.length + description.length.toLong()
    ) : OfflineOperation()
    
    @Serializable
    data class AIAnalysis(
        override val id: String,
        val noteId: Long,
        val analysisType: String, // CATEGORY, TAGS, SENTIMENT, SUMMARY
        val content: String,
        val useLocalAI: Boolean = true,
        override val timestamp: Long,
        override val priority: Priority = Priority.LOW,
        override val retryCount: Int = 0,
        override val maxRetries: Int = 2,
        override val networkRequirement: NetworkRequirement = NetworkRequirement.WIFI_ONLY,
        override val estimatedSize: Long = content.length.toLong()
    ) : OfflineOperation()
    
    @Serializable
    data class SyncCollaborativeSession(
        override val id: String,
        val sessionId: String,
        val operations: List<String>, // Serialized collaborative operations
        override val timestamp: Long,
        override val priority: Priority = Priority.HIGH,
        override val retryCount: Int = 0,
        override val maxRetries: Int = 10,
        override val networkRequirement: NetworkRequirement = NetworkRequirement.ANY,
        override val estimatedSize: Long = operations.sumOf { it.length }.toLong()
    ) : OfflineOperation()
}

@Serializable
enum class Priority(val value: Int) {
    HIGH(1),
    MEDIUM(2),
    LOW(3)
}

@Serializable
enum class NetworkRequirement {
    ANY,
    WIFI_ONLY,
    MOBILE_DATA_OK
}

@Serializable
enum class OperationStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    RETRYING
}

@Serializable
enum class SyncStatus {
    SYNCED,
    PENDING,
    CONFLICT,
    ERROR,
    OFFLINE_ONLY
}

@Serializable
enum class ConflictType {
    CONTENT,      // Different content in same note
    METADATA,     // Different metadata (title, tags, etc.)
    STRUCTURAL,   // Different note structure
    COLLABORATIVE, // Real-time collaborative conflicts
    VERSION       // Version mismatch conflicts
}

@Serializable
enum class ConflictResolutionStrategy {
    AUTO_MERGE,           // Automatic merge using algorithms
    ACCEPT_LOCAL,         // Keep local version
    ACCEPT_REMOTE,        // Keep remote version
    USER_CHOICE,          // User manually chooses
    AI_ASSISTED,          // AI suggests best merge
    THREE_WAY_MERGE,      // Git-style three-way merge
    SIDE_BY_SIDE         // Show both versions for comparison
}

@Serializable
data class ConflictData(
    val conflictType: ConflictType,
    val localVersion: String,
    val remoteVersion: String,
    val baseVersion: String? = null, // For three-way merge
    val conflictMarkers: List<ConflictMarker> = emptyList(),
    val suggestedResolution: ConflictResolutionStrategy? = null,
    val confidence: Float = 0.0f
)

@Serializable
data class ConflictMarker(
    val startIndex: Int,
    val endIndex: Int,
    val conflictType: String,
    val localContent: String,
    val remoteContent: String,
    val suggestion: String? = null
)

@Serializable
data class SyncResult(
    val success: Boolean,
    val operationId: String,
    val syncedAt: Long,
    val conflicts: List<ConflictData> = emptyList(),
    val errorMessage: String? = null,
    val dataTransferred: Long = 0,
    val syncDuration: Long = 0
)

@Serializable
data class NetworkState(
    val isConnected: Boolean,
    val connectionType: ConnectionType,
    val bandwidth: Long = 0, // Estimated bandwidth in bytes/second
    val isMetered: Boolean = false,
    val signalStrength: Int = 0, // 0-100
    val latency: Long = 0 // Ping time in milliseconds
)

@Serializable
enum class ConnectionType {
    NONE,
    WIFI,
    MOBILE_DATA,
    ETHERNET,
    UNKNOWN
}

@Serializable
data class OfflineCapabilities(
    val canCreateNotes: Boolean = true,
    val canEditNotes: Boolean = true,
    val canDeleteNotes: Boolean = true,
    val canUseAI: Boolean = false, // Depends on local AI availability
    val canSync: Boolean = false,
    val canCollaborate: Boolean = false,
    val availableFeatures: List<String> = emptyList()
)

// Utility functions for serialization
object OfflineOperationSerializer {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    fun serialize(operation: OfflineOperation): String {
        return json.encodeToString(OfflineOperation.serializer(), operation)
    }
    
    fun deserialize(data: String, type: String): OfflineOperation? {
        return try {
            when (type) {
                "CREATE_NOTE" -> json.decodeFromString<OfflineOperation.CreateNote>(data)
                "UPDATE_NOTE" -> json.decodeFromString<OfflineOperation.UpdateNote>(data)
                "DELETE_NOTE" -> json.decodeFromString<OfflineOperation.DeleteNote>(data)
                "CREATE_CATEGORY" -> json.decodeFromString<OfflineOperation.CreateCategory>(data)
                "AI_ANALYSIS" -> json.decodeFromString<OfflineOperation.AIAnalysis>(data)
                "SYNC_COLLABORATIVE_SESSION" -> json.decodeFromString<OfflineOperation.SyncCollaborativeSession>(data)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun serializeConflictData(conflictData: ConflictData): String {
        return json.encodeToString(ConflictData.serializer(), conflictData)
    }
    
    fun deserializeConflictData(data: String): ConflictData? {
        return try {
            json.decodeFromString<ConflictData>(data)
        } catch (e: Exception) {
            null
        }
    }
}

// Extension functions for easy conversion
fun OfflineOperation.toEntity(): OfflineOperationEntity {
    return OfflineOperationEntity(
        id = this.id,
        type = when (this) {
            is OfflineOperation.CreateNote -> "CREATE_NOTE"
            is OfflineOperation.UpdateNote -> "UPDATE_NOTE"
            is OfflineOperation.DeleteNote -> "DELETE_NOTE"
            is OfflineOperation.CreateCategory -> "CREATE_CATEGORY"
            is OfflineOperation.AIAnalysis -> "AI_ANALYSIS"
            is OfflineOperation.SyncCollaborativeSession -> "SYNC_COLLABORATIVE_SESSION"
        },
        entityType = when (this) {
            is OfflineOperation.CreateNote -> "note"
            is OfflineOperation.UpdateNote -> "note"
            is OfflineOperation.DeleteNote -> "note"
            is OfflineOperation.CreateCategory -> "category"
            is OfflineOperation.AIAnalysis -> "note"
            is OfflineOperation.SyncCollaborativeSession -> "collaborative_session"
        },
        entityId = when (this) {
            is OfflineOperation.CreateNote -> this.tempId
            is OfflineOperation.UpdateNote -> this.noteId.toString()
            is OfflineOperation.DeleteNote -> this.noteId.toString()
            is OfflineOperation.CreateCategory -> this.name
            is OfflineOperation.AIAnalysis -> this.noteId.toString()
            is OfflineOperation.SyncCollaborativeSession -> this.sessionId
        },
        status = "PENDING",
        data = OfflineOperationSerializer.serialize(this),
        timestamp = this.timestamp,
        scheduledAt = this.timestamp,
        priority = this.priority.value,
        retryCount = this.retryCount,
        maxRetries = this.maxRetries,
        networkRequirement = this.networkRequirement.name,
        estimatedSize = this.estimatedSize
    )
}

fun OfflineOperationEntity.toOperation(): OfflineOperation? {
    val d = this.data ?: return null
    return OfflineOperationSerializer.deserialize(d, this.type)
}
