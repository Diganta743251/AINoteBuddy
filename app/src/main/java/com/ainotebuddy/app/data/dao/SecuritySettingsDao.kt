package com.ainotebuddy.app.data.dao

import androidx.room.*
import com.ainotebuddy.app.data.model.security.SecuritySettings
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Data Access Object for security settings related operations
 */
@Dao
interface SecuritySettingsDao {
    /**
     * Get security settings for a specific note
     */
    @Query("SELECT * FROM security_settings WHERE noteId = :noteId")
    fun getSecuritySettings(noteId: Long): Flow<SecuritySettings?>

    /**
     * Get security settings for multiple notes
     */
    @Query("SELECT * FROM security_settings WHERE noteId IN (:noteIds)")
    fun getSecuritySettingsForNotes(noteIds: List<Long>): Flow<List<SecuritySettings>>

    /**
     * Insert or update security settings
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SecuritySettings): Long

    /**
     * Delete security settings for a note
     */
    @Query("DELETE FROM security_settings WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    /**
     * Check if a note requires biometric authentication
     */
    @Query("SELECT requiresBiometric FROM security_settings WHERE noteId = :noteId")
    suspend fun requiresBiometric(noteId: Long): Boolean

    /**
     * Check if a note is encrypted
     */
    @Query("SELECT isEncrypted FROM security_settings WHERE noteId = :noteId")
    suspend fun isEncrypted(noteId: Long): Boolean

    /**
     * Get all notes that have expired (self-destruct time has passed)
     */
    @Query("SELECT noteId FROM security_settings WHERE selfDestructTimestamp IS NOT NULL AND selfDestructTimestamp <= :currentTime")
    suspend fun getExpiredNoteIds(currentTime: Date = Date()): List<Long>

    /**
     * Update biometric requirement for a note
     */
    @Query("UPDATE security_settings SET requiresBiometric = :requiresBiometric, updatedAt = :updatedAt WHERE noteId = :noteId")
    suspend fun updateBiometricRequirement(noteId: Long, requiresBiometric: Boolean, updatedAt: Date = Date())

    /**
     * Update encryption status for a note
     */
    @Query("UPDATE security_settings SET isEncrypted = :isEncrypted, encryptionKeyAlias = :keyAlias, updatedAt = :updatedAt WHERE noteId = :noteId")
    suspend fun updateEncryptionStatus(noteId: Long, isEncrypted: Boolean, keyAlias: String?, updatedAt: Date = Date())

    /**
     * Update self-destruct timestamp for a note
     */
    @Query("UPDATE security_settings SET selfDestructTimestamp = :timestamp, updatedAt = :updatedAt WHERE noteId = :noteId")
    suspend fun updateSelfDestructTime(noteId: Long, timestamp: Date?, updatedAt: Date = Date())

    /**
     * Update share password for a note
     */
    @Query("UPDATE security_settings SET sharePasswordHash = :passwordHash, updatedAt = :updatedAt WHERE noteId = :noteId")
    suspend fun updateSharePassword(noteId: Long, passwordHash: String?, updatedAt: Date = Date())
}
