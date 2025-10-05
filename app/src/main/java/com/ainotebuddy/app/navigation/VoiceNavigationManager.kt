package com.ainotebuddy.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ainotebuddy.app.voice.model.VoiceCommand
import com.ainotebuddy.app.voice.model.VoiceNavigationState
import com.ainotebuddy.app.voice.model.VoiceNavigationTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages voice-controlled navigation and hands-free interactions
 */
@Stable
class VoiceNavigationManager {
    private val _navigationState = MutableStateFlow(VoiceNavigationState())
    val navigationState: StateFlow<VoiceNavigationState> = _navigationState.asStateFlow()
    
    private var currentScreen: String = ""
    private var currentNoteId: String? = null
    
    /**
     * Update the current screen for context-aware navigation
     */
    fun updateCurrentScreen(screen: String, noteId: String? = null) {
        currentScreen = screen
        currentNoteId = noteId
        _navigationState.update { it.copy(currentScreen = screen, currentNoteId = noteId) }
    }
    
    /**
     * Handle navigation commands from voice input
     */
    fun handleNavigationCommand(command: VoiceCommand): VoiceNavigationTarget? {
        return when (command) {
            is VoiceCommand.GoHome -> {
                navigateTo(VoiceNavigationTarget.Home)
                VoiceNavigationTarget.Home
            }
            is VoiceCommand.GoBack -> {
                navigateTo(VoiceNavigationTarget.Back)
                VoiceNavigationTarget.Back
            }
            is VoiceCommand.OpenSettings -> {
                navigateTo(VoiceNavigationTarget.Settings)
                VoiceNavigationTarget.Settings
            }
            is VoiceCommand.OpenNote -> {
                // In a real app, we would look up the note ID from the title
                val noteId = findNoteIdByTitle(command.noteTitle) ?: return null
                val target = VoiceNavigationTarget.NoteDetail(noteId)
                navigateTo(target)
                target
            }
            is VoiceCommand.CreateNote -> {
                val target = VoiceNavigationTarget.CreateNote(command.content)
                navigateTo(target)
                target
            }
            is VoiceCommand.Search -> {
                val target = VoiceNavigationTarget.Search(command.query)
                navigateTo(target)
                target
            }
            else -> null
        }
    }
    
    /**
     * Handle note editing commands
     */
    fun handleNoteEditCommand(command: VoiceCommand): VoiceNavigationTarget? {
        if (currentNoteId == null) return null
        
        return when (command) {
            is VoiceCommand.AddToNote -> {
                val target = VoiceNavigationTarget.EditNote(
                    noteId = currentNoteId!!,
                    contentToAdd = command.content
                )
                navigateTo(target)
                target
            }
            is VoiceCommand.DeleteNote -> {
                val target = VoiceNavigationTarget.DeleteNote(currentNoteId!!)
                navigateTo(target)
                target
            }
            is VoiceCommand.ArchiveNote -> {
                val target = VoiceNavigationTarget.ArchiveNote(currentNoteId!!)
                navigateTo(target)
                target
            }
            is VoiceCommand.SetReminder -> {
                val target = VoiceNavigationTarget.SetReminder(
                    noteId = currentNoteId!!,
                    reminderText = command.reminderText
                )
                navigateTo(target)
                target
            }
            is VoiceCommand.AddTag -> {
                val target = VoiceNavigationTarget.AddTag(
                    noteId = currentNoteId!!,
                    tagText = command.tagText
                )
                navigateTo(target)
                target
            }
            else -> null
        }
    }
    
    /**
     * Update navigation state with the new target
     */
    private fun navigateTo(target: VoiceNavigationTarget) {
        _navigationState.update { current ->
            current.copy(
                currentTarget = target,
                navigationHistory = current.navigationHistory + target,
                lastNavigationTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Clear the current navigation target after handling
     */
    fun clearNavigationTarget() {
        _navigationState.update { it.copy(currentTarget = null) }
    }
    
    /**
     * Find note ID by title (simplified - in a real app, this would query a repository)
     */
    private fun findNoteIdByTitle(title: String): String? {
        // In a real app, this would query your notes repository
        // For now, we'll return a dummy ID
        return if (title.isNotBlank()) "note_${title.hashCode()}" else null
    }
}

/**
 * Composable function to get the current voice navigation state
 */
@Composable
fun rememberVoiceNavigationManager(): VoiceNavigationManager {
    val context = LocalContext.current
    return remember { VoiceNavigationManager() }
}
