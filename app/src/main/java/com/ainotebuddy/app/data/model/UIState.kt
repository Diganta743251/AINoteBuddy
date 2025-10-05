package com.ainotebuddy.app.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing the UI state that needs to be persisted
 */
@Serializable
data class UIState(
    // Map of note IDs to their expanded state (true = expanded, false = collapsed)
    val expandedStates: Map<String, Boolean> = emptyMap(),
    
    // Set of selected note IDs
    val selectedNoteIds: Set<String> = emptySet(),
    
    // Current view mode (list or grid)
    val viewMode: String = "grid"
)

/**
 * Default UI state
 */
val DefaultUIState = UIState()
