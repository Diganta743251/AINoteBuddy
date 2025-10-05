package com.ainotebuddy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.ui.components.PreferenceItem
import com.ainotebuddy.app.ui.components.SectionHeader
import com.ainotebuddy.app.ui.components.dialogs.TextSizeDialog
import com.ainotebuddy.app.ui.theme.LocalSpacing
import com.ainotebuddy.app.utils.TextScaler
import com.ainotebuddy.app.utils.rememberTextScaler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState

/**
 * Screen for managing accessibility settings
 */
@Composable
fun AccessibilitySettingsScreen(
    onBackClick: () -> Unit,
    viewModel: AccessibilityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val textScaler = rememberTextScaler()
    var showTextSizeDialog by remember { mutableStateOf(false) }
    
    // Text size dialog
    if (showTextSizeDialog) {
        TextSizeDialog(
            onDismiss = { showTextSizeDialog = false },
            onConfirm = { 
                viewModel.setFontScale(textScaler.getFontScale())
                showTextSizeDialog = false 
            },
            textScaler = textScaler
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accessibility_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Display Settings
            SectionHeader(
                title = stringResource(R.string.display),
                icon = Icons.Default.Visibility
            )
            
            // High Contrast Mode
            PreferenceItem(
                title = stringResource(R.string.high_contrast_mode),
                description = stringResource(R.string.high_contrast_mode_description),
                icon = Icons.Default.InvertColors,
                endContent = {
                    Switch(
                        checked = uiState.highContrastMode,
                        onCheckedChange = { viewModel.setHighContrastMode(it) }
                    )
                },
                onClick = { viewModel.setHighContrastMode(!uiState.highContrastMode) }
            )
            
            // Text Size
            PreferenceItem(
                title = stringResource(R.string.text_size),
                description = stringResource(R.string.text_size_description),
                icon = Icons.Default.TextFields,
                endContent = {
                    Text(
                        "${(uiState.fontScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { 
                    // Reset the text scaler to the current preference
                    textScaler.setFontScale(uiState.fontScale)
                    showTextSizeDialog = true 
                }
            )
            
            // Text-to-Speech Settings
            SectionHeader(
                title = stringResource(R.string.text_to_speech),
                icon = Icons.Default.VolumeUp
            )
            
            PreferenceItem(
                title = stringResource(R.string.enable_tts),
                description = stringResource(R.string.enable_tts_description),
                icon = Icons.Default.Headphones,
                endContent = {
                    Switch(
                        checked = uiState.ttsEnabled,
                        onCheckedChange = { viewModel.setTtsEnabled(it) }
                    )
                },
                onClick = { viewModel.setTtsEnabled(!uiState.ttsEnabled) }
            )
            
            // Keyboard Navigation
            SectionHeader(
                title = stringResource(R.string.keyboard_navigation),
                icon = Icons.Default.Keyboard
            )
            
            PreferenceItem(
                title = stringResource(R.string.enable_keyboard_nav),
                description = stringResource(R.string.enable_keyboard_nav_description),
                icon = Icons.Default.KeyboardArrowRight,
                endContent = {
                    Switch(
                        checked = uiState.keyboardNavEnabled,
                        onCheckedChange = { viewModel.setKeyboardNavEnabled(it) }
                    )
                },
                onClick = { viewModel.setKeyboardNavEnabled(!uiState.keyboardNavEnabled) }
            )
            
            // Spacer at the bottom
            Spacer(modifier = Modifier.height(spacing.extraLarge * 2))
        }
    }
}

/**
 * ViewModel for managing accessibility settings
 */
@HiltViewModel
class AccessibilityViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : androidx.lifecycle.ViewModel() {
    
    data class AccessibilityUiState(
        val highContrastMode: Boolean = false,
        val fontScale: Float = 1f,
        val ttsEnabled: Boolean = false,
        val keyboardNavEnabled: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            highContrastMode = preferencesManager.getHighContrastMode(),
            fontScale = preferencesManager.getFontScale(),
            ttsEnabled = preferencesManager.getTtsEnabled(),
            keyboardNavEnabled = preferencesManager.getKeyboardNavEnabled()
        )
    }
    
    fun setHighContrastMode(enabled: Boolean) {
        preferencesManager.setHighContrastMode(enabled)
        _uiState.value = _uiState.value.copy(highContrastMode = enabled)
    }
    
    fun setFontScale(scale: Float) {
        preferencesManager.setFontScale(scale)
        _uiState.value = _uiState.value.copy(fontScale = scale)
    }
    
    fun setTtsEnabled(enabled: Boolean) {
        preferencesManager.setTtsEnabled(enabled)
        _uiState.value = _uiState.value.copy(ttsEnabled = enabled)
    }
    
    fun setKeyboardNavEnabled(enabled: Boolean) {
        preferencesManager.setKeyboardNavEnabled(enabled)
        _uiState.value = _uiState.value.copy(keyboardNavEnabled = enabled)
    }
}
