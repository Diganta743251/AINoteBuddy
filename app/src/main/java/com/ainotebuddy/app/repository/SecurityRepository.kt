package com.ainotebuddy.app.repository

import com.ainotebuddy.app.data.dao.SecuritySettingsDao
import com.ainotebuddy.app.data.model.security.SecuritySettings
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling security-related operations
 */
@Singleton
class SecurityRepository @Inject constructor(
    private val securitySettingsDao: SecuritySettingsDao
) {
    /**
     * Get security settings for a note
     */
    fun getSecuritySettings(noteId: Long): Flow<SecuritySettings?> {
        return securitySettingsDao.getSecuritySettings(noteId)
    }

    /**
     * Create or update security settings for a note
     */
    suspend fun setSecuritySettings(settings: SecuritySettings): Long {
        return securitySettingsDao.upsert(settings)
    }

    /**
     * Check if a note requires biometric authentication
     */
    suspend fun requiresBiometric(noteId: Long): Boolean {
        return securitySettingsDao.requiresBiometric(noteId)
    }

    /**
     * Check if a note is encrypted
     */
    suspend fun isEncrypted(noteId: Long): Boolean {
        return securitySettingsDao.isEncrypted(noteId)
    }

    /**
     * Enable or disable biometric lock for a note
     */
    suspend fun setBiometricLock(noteId: Long, enabled: Boolean) {
        securitySettingsDao.updateBiometricRequirement(noteId, enabled)
    }

    /**
     * Set encryption for a note
     */
    suspend fun setEncryption(noteId: Long, enabled: Boolean, keyAlias: String? = null) {
        securitySettingsDao.updateEncryptionStatus(noteId, enabled, keyAlias)
    }

    /**
     * Set self-destruct time for a note
     */
    suspend fun setSelfDestructTime(noteId: Long, timestamp: Date?) {
        securitySettingsDao.updateSelfDestructTime(noteId, timestamp)
    }

    /**
     * Set share password for a note
     */
    suspend fun setSharePassword(noteId: Long, passwordHash: String?) {
        securitySettingsDao.updateSharePassword(noteId, passwordHash)
    }

    /**
     * Get all notes that have expired (self-destruct time has passed)
     */
    suspend fun getExpiredNoteIds(): List<Long> {
        return securitySettingsDao.getExpiredNoteIds()
    }

    /**
     * Delete security settings for a note
     */
    suspend fun deleteSecuritySettings(noteId: Long) {
        securitySettingsDao.deleteByNoteId(noteId)
    }
}
