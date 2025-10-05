package com.ainotebuddy.app.collaboration

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user presence and awareness in collaborative sessions
 */
@Singleton
class PresenceManager @Inject constructor(
    private val firebaseService: FirebaseCollaborationService
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Current user's presence state
    private val _currentPresence = MutableStateFlow<PresenceInfo?>(null)
    val currentPresence: StateFlow<PresenceInfo?> = _currentPresence.asStateFlow()
    
    // Other users' presence in the current session
    private val _otherUsersPresence = MutableStateFlow<List<PresenceInfo>>(emptyList())
    val otherUsersPresence: StateFlow<List<PresenceInfo>> = _otherUsersPresence.asStateFlow()
    
    // Combined presence information
    val allPresence: StateFlow<List<PresenceInfo>> = combine(
        currentPresence,
        otherUsersPresence
    ) { current, others ->
        listOfNotNull(current) + others
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Active users count
    val activeUsersCount: StateFlow<Int> = allPresence.map { presenceList ->
        presenceList.count { it.isActive }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
    
    // Typing indicators
    val typingUsers: StateFlow<List<PresenceInfo>> = allPresence.map { presenceList ->
        presenceList.filter { it.isTyping && it.userId != _currentPresence.value?.userId }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private var currentSessionId: String? = null
    private var presenceUpdateJob: Job? = null
    
    /**
     * Start presence tracking for a collaborative session
     */
    fun startPresenceTracking(
        sessionId: String,
        userId: String,
        initialPosition: CursorPosition? = null
    ) {
        currentSessionId = sessionId
        
        // Initialize current user's presence; convert CursorPosition to CursorSelection
        val initialPresence = PresenceInfo(
            sessionId = sessionId,
            userId = userId,
            isActive = true,
            lastActivity = System.currentTimeMillis(),
            cursorPosition = initialPosition?.let { CursorSelection(userId = userId, startIndex = it.offset, endIndex = it.offset) }
        )
        
        _currentPresence.value = initialPresence
        
        // Start observing other users' presence
        startObservingPresence(sessionId, userId)
        
        // Start periodic presence updates
        startPresenceUpdates()
    }
    
    /**
     * Stop presence tracking
     */
    fun stopPresenceTracking() {
        presenceUpdateJob?.cancel()
        
        // Mark user as offline
        _currentPresence.value?.let { presence ->
            val offlinePresence = presence.copy(
                isActive = false,
                isTyping = false,
                lastActivity = System.currentTimeMillis()
            )
            
            scope.launch {
                firebaseService.updatePresence(presence.sessionId, offlinePresence)
            }
        }
        
        _currentPresence.value = null
        _otherUsersPresence.value = emptyList()
        currentSessionId = null
    }
    
    /**
     * Update cursor position
     */
    fun updateCursorPosition(position: CursorPosition) {
        _currentPresence.value?.let { presence ->
            val selection = CursorSelection(userId = presence.userId, startIndex = position.offset, endIndex = position.offset)
            val updatedPresence = presence.copy(
                cursorPosition = selection,
                lastActivity = System.currentTimeMillis()
            )
            
            _currentPresence.value = updatedPresence
            
            scope.launch {
                firebaseService.updatePresence(presence.sessionId, updatedPresence)
            }
        }
    }
    
    /**
     * Update typing status
     */
    fun updateTypingStatus(isTyping: Boolean) {
        _currentPresence.value?.let { presence ->
            val updatedPresence = presence.copy(
                isTyping = isTyping,
                lastActivity = System.currentTimeMillis()
            )
            
            _currentPresence.value = updatedPresence
            
            scope.launch {
                firebaseService.updatePresence(presence.sessionId, updatedPresence)
            }
        }
    }
    
    /**
     * Update current section being viewed
     */
    fun updateCurrentSection(section: String?) {
        _currentPresence.value?.let { presence ->
            val updatedPresence = presence.copy(
                currentSection = section,
                lastActivity = System.currentTimeMillis()
            )
            
            _currentPresence.value = updatedPresence
            
            scope.launch {
                firebaseService.updatePresence(presence.sessionId, updatedPresence)
            }
        }
    }
    
    /**
     * Update selected text
     */
    fun updateSelectedText(selectedText: String?) {
        _currentPresence.value?.let { presence ->
            val updatedPresence = presence.copy(
                selectedText = selectedText,
                lastActivity = System.currentTimeMillis()
            )
            
            _currentPresence.value = updatedPresence
            
            scope.launch {
                firebaseService.updatePresence(presence.sessionId, updatedPresence)
            }
        }
    }
    
    /**
     * Get presence information for a specific user
     */
    fun getPresenceForUser(userId: String): PresenceInfo? {
        return allPresence.value.find { it.userId == userId }
    }
    
    /**
     * Check if a user is currently active
     */
    fun isUserActive(userId: String): Boolean {
        return getPresenceForUser(userId)?.isActive == true
    }
    
    /**
     * Check if a user is currently typing
     */
    fun isUserTyping(userId: String): Boolean {
        return getPresenceForUser(userId)?.isTyping == true
    }
    
    /**
     * Get cursor position for a specific user
     */
    fun getUserCursorPosition(userId: String): CursorPosition? {
        val sel = getPresenceForUser(userId)?.cursorPosition ?: return null
        return CursorPosition(offset = sel.startIndex, line = 0, column = sel.startIndex)
    }
    
    /**
     * Get all active cursor positions (excluding current user)
     */
    fun getActiveCursorPositions(): List<CursorPosition> {
        val currentUserId = _currentPresence.value?.userId
        return allPresence.value
            .filter { it.userId != currentUserId && it.isActive && it.cursorPosition != null }
            .mapNotNull { it.cursorPosition?.let { sel -> CursorPosition(offset = sel.startIndex, line = 0, column = sel.startIndex) } }
    }
    
    /**
     * Get typing indicator text
     */
    fun getTypingIndicatorText(): String? {
        val typingUsersList = typingUsers.value
        
        return when (typingUsersList.size) {
            0 -> null
            1 -> "${getUserDisplayName(typingUsersList[0].userId)} is typing..."
            2 -> "${getUserDisplayName(typingUsersList[0].userId)} and ${getUserDisplayName(typingUsersList[1].userId)} are typing..."
            else -> "${getUserDisplayName(typingUsersList[0].userId)} and ${typingUsersList.size - 1} others are typing..."
        }
    }
    
    /**
     * Get user display name from presence info
     */
    private fun getUserDisplayName(userId: String): String {
        // This would typically fetch from user cache or database
        // For now, return a simplified version
        return "User ${userId.take(8)}"
    }
    
    /**
     * Start observing presence of other users
     */
    private fun startObservingPresence(sessionId: String, currentUserId: String) {
        scope.launch {
            firebaseService.observePresence(sessionId)
                .collect { presenceList ->
                    // Filter out current user's presence
                    val otherUsers = presenceList.filter { it.userId != currentUserId }
                    _otherUsersPresence.value = otherUsers
                }
        }
    }
    
    /**
     * Start periodic presence updates
     */
    private fun startPresenceUpdates() {
        presenceUpdateJob = scope.launch {
            while (isActive) {
                delay(PRESENCE_UPDATE_INTERVAL)
                
                _currentPresence.value?.let { presence ->
                    // Update last activity timestamp
                    val updatedPresence = presence.copy(
                        lastActivity = System.currentTimeMillis()
                    )
                    
                    firebaseService.updatePresence(presence.sessionId, updatedPresence)
                }
            }
        }
    }
    
    /**
     * Clean up inactive presence entries
     */
    fun cleanupInactivePresence() {
        scope.launch {
            val currentTime = System.currentTimeMillis()
            val updatedPresence = _otherUsersPresence.value.filter { presence ->
                currentTime - presence.lastActivity < PRESENCE_TIMEOUT
            }
            
            if (updatedPresence.size != _otherUsersPresence.value.size) {
                _otherUsersPresence.value = updatedPresence
            }
        }
    }
    
    companion object {
        private const val PRESENCE_UPDATE_INTERVAL = 30_000L // 30 seconds
        private const val PRESENCE_TIMEOUT = 120_000L // 2 minutes
    }
}

/**
 * Extension functions for presence-related UI helpers
 */

/**
 * Get a user-friendly description of presence status
 */
fun PresenceInfo.getStatusDescription(): String {
    val timeSinceActivity = System.currentTimeMillis() - lastActivity
    
    return when {
        !isActive -> "Offline"
        isTyping -> "Typing..."
        timeSinceActivity < 60_000 -> "Active now"
        timeSinceActivity < 300_000 -> "Active recently"
        else -> "Away"
    }
}

/**
 * Check if presence is considered stale
 */
fun PresenceInfo.isStale(): Boolean {
    val timeSinceActivity = System.currentTimeMillis() - lastActivity
    return timeSinceActivity > 120_000 // 2 minutes
}

/**
 * Get presence priority for sorting (active users first, then by last activity)
 */
fun PresenceInfo.getPriority(): Long {
    return when {
        !isActive -> Long.MAX_VALUE
        isTyping -> 0
        else -> System.currentTimeMillis() - lastActivity
    }
}
