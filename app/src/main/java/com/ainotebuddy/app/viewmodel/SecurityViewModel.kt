package com.ainotebuddy.app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.security.SecuritySettings
import com.ainotebuddy.app.repository.SecurityRepository
import com.ainotebuddy.app.security.BiometricManager
import com.ainotebuddy.app.security.EncryptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * UI state for security-related screens
 */
data class SecurityUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val securitySettings: SecuritySettings? = null,
    val isBiometricAvailable: Boolean = false,
    val isEncrypted: Boolean = false,
    val requiresBiometric: Boolean = false,
    val selfDestructTime: Date? = null,
    val hasSharePassword: Boolean = false
)

/**
 * ViewModel for handling security-related operations
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val biometricManager: BiometricManager,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private var currentNoteId: Long = -1L

    /**
     * Load security settings for a note
     */
    fun loadSecuritySettings(noteId: Long) {
        currentNoteId = noteId
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Check biometric availability
                val (isBiometricAvailable, _) = biometricManager.isBiometricAvailable()
                
                // Load security settings
                securityRepository.getSecuritySettings(noteId).collectLatest { settings ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        securitySettings = settings,
                        isBiometricAvailable = isBiometricAvailable,
                        isEncrypted = settings?.isEncrypted ?: false,
                        requiresBiometric = settings?.requiresBiometric ?: false,
                        selfDestructTime = settings?.selfDestructTimestamp,
                        hasSharePassword = !settings?.sharePasswordHash.isNullOrBlank()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load security settings"
                )
            }
        }
    }

    /**
     * Toggle biometric lock for the current note
     */
    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            try {
                securityRepository.setBiometricLock(currentNoteId, enabled)
                _uiState.value = _uiState.value.copy(
                    requiresBiometric = enabled
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update biometric lock"
                )
            }
        }
    }

    /**
     * Toggle encryption for the current note
     * @param enabled Whether to enable or disable encryption
     * @param keyAlias Optional key alias to use for encryption
     */
    fun toggleEncryption(enabled: Boolean, keyAlias: String? = null) {
        viewModelScope.launch {
            try {
                securityRepository.setEncryption(currentNoteId, enabled, keyAlias)
                _uiState.value = _uiState.value.copy(
                    isEncrypted = enabled
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update encryption"
                )
            }
        }
    }

    /**
     * Set self-destruct time for the current note
     * @param timestamp When the note should self-destruct (null to disable)
     */
    fun setSelfDestructTime(timestamp: Date?) {
        viewModelScope.launch {
            try {
                securityRepository.setSelfDestructTime(currentNoteId, timestamp)
                _uiState.value = _uiState.value.copy(
                    selfDestructTime = timestamp
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to set self-destruct time"
                )
            }
        }
    }

    /**
     * Set a password for secure sharing of the current note
     * @param password The password to set (null to remove password)
     */
    fun setSharePassword(password: String?) {
        viewModelScope.launch {
            try {
                // In a real app, you would hash the password before storing it
                val passwordHash = password?.let { "hashed_${it.hashCode()}" }
                securityRepository.setSharePassword(currentNoteId, passwordHash)
                _uiState.value = _uiState.value.copy(
                    hasSharePassword = passwordHash != null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to set share password"
                )
            }
        }
    }

    /**
     * Check if a password is valid for accessing a shared note
     * @param password The password to check
     * @return true if the password is valid, false otherwise
     */
    fun validateSharePassword(password: String): Boolean {
        val currentHash = _uiState.value.securitySettings?.sharePasswordHash ?: return false
        // In a real app, you would use a proper password hashing algorithm
        return currentHash == "hashed_${password.hashCode()}"
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Check if the self-destruct time has passed
     */
    fun isNoteExpired(): Boolean {
        val selfDestructTime = _uiState.value.selfDestructTime ?: return false
        return Date().after(selfDestructTime)
    }
}
