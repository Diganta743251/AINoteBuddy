package com.ainotebuddy.app.data.model.security

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * Represents the security settings for a note
 * @property id Unique identifier for the security settings
 * @property noteId The ID of the note these settings belong to
 * @property isEncrypted Whether the note is encrypted
 * @property encryptionKeyAlias The alias for the encryption key in Android Keystore
 * @property requiresBiometric Whether the note requires biometric authentication
 * @property selfDestructTimestamp When the note should self-destruct (null for no self-destruction)
 * @property sharePasswordHash Hashed password for secure sharing (null if no password set)
 */
@Entity(tableName = "security_settings")
data class SecuritySettings(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val isEncrypted: Boolean = false,
    val encryptionKeyAlias: String? = null,
    val requiresBiometric: Boolean = false,
    val selfDestructTimestamp: Date? = null,
    val sharePasswordHash: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
