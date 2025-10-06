package com.ainotebuddy.app.ui.screens


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ainotebuddy.app.R
import com.ainotebuddy.app.navigation.VoiceNavigationManager
import com.ainotebuddy.app.ui.components.voice.HandsFreeEditor
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.ainotebuddy.app.ui.viewmodel.VoiceNoteEditorViewModel
import com.ainotebuddy.app.voice.VoiceCommandService
import com.ainotebuddy.app.voice.model.VoiceNavigationTarget
import com.ainotebuddy.app.voice.model.VoiceNavigationFeedback
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Screen for hands-free note editing with voice commands
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNoteEditorScreen(
    noteId: String? = null,
    initialContent: String = "",
    onBackPressed: () -> Unit = {},
    onNoteSaved: (String) -> Unit = {},
    viewModel: VoiceNoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Navigation manager for voice commands
    val navigationManager = remember { VoiceNavigationManager() }
    
    // Update current screen in navigation manager
    LaunchedEffect(noteId) {
        navigationManager.updateCurrentScreen("voice_note_editor", noteId)
    }
    
    // Handle voice command results when viewModel changes
    LaunchedEffect(viewModel) {
        viewModel.commandResult.collectLatest { _ ->
            // Minimal handling path while voice parsing is stubbed
            // Consider mapping transcripts to actions in VoiceCommandService when ready
        }
    }
    
    // Handle voice recognition errors
    LaunchedEffect(uiState) {
        uiState.error?.let { err ->
            viewModel.showFeedback(VoiceNavigationFeedback.Error(err))
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(
                            if (noteId != null) R.string.edit_note 
                            else R.string.new_voice_note
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Add any additional actions here
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Hands-free editor
                HandsFreeEditor(
                    currentContent = uiState.content,
                    onContentUpdate = { viewModel.updateContent(it) },
                    onSave = { viewModel.saveNote() },
                    onCancel = onBackPressed,
                    isListening = uiState.isListening,
                    isProcessing = uiState.isProcessing,
                    feedback = uiState.feedback,
                    onStartListening = { viewModel.startListening() },
                    onStopListening = { viewModel.stopListening() },
                    onCommandRecognized = { recognized ->
                        viewModel.processVoiceCommand(recognized)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Show loading indicator if processing
            if (uiState.isProcessing) {
                // Loading indicator would go here
            }
        }
    }
    
    // Handle navigation events
    LaunchedEffect(uiState.navigationTarget) {
        uiState.navigationTarget?.let { target ->
            when (target) {
                is VoiceNavigationTarget.NoteDetail -> {
                    // Navigate to note detail
                    onNoteSaved(target.noteId)
                }
                // Handle other navigation targets as needed
                else -> { /* No-op */ }
            }
            // Clear the navigation target after handling
            viewModel.clearNavigationTarget()
        }
    }
}

// Activity-based launcher removed per design; navigation handled by NavHost callback.

@Preview(showBackground = true)
@Composable
private fun VoiceNoteEditorPreview() {
    AINoteBuddyTheme {
        VoiceNoteEditorScreen(
            noteId = "note_123",
            initialContent = "This is a sample note created with voice commands.",
            onBackPressed = {}
        )
    }
}
