package com.ainotebuddy.app.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SimpleSettingsViewModel(private val context: Context) : ViewModel() {
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val noteRepository = NoteRepository(context)
    
    private val _uiState = MutableStateFlow(
        SimpleSettingsUiState(
            darkModeEnabled = sharedPrefs.getBoolean("dark_mode", false),
            autoSaveEnabled = sharedPrefs.getBoolean("auto_save", true),
            aiSuggestionsEnabled = sharedPrefs.getBoolean("ai_suggestions", true),
            appLockEnabled = sharedPrefs.getBoolean("app_lock", false)
        )
    )
    val uiState: StateFlow<SimpleSettingsUiState> = _uiState.asStateFlow()
    
    fun toggleDarkMode() {
        val newValue = !_uiState.value.darkModeEnabled
        _uiState.value = _uiState.value.copy(darkModeEnabled = newValue)
        sharedPrefs.edit().putBoolean("dark_mode", newValue).apply()
        
        Toast.makeText(context, "Dark mode ${if (newValue) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
    }
    
    fun toggleAutoSave() {
        val newValue = !_uiState.value.autoSaveEnabled
        _uiState.value = _uiState.value.copy(autoSaveEnabled = newValue)
        sharedPrefs.edit().putBoolean("auto_save", newValue).apply()
        
        Toast.makeText(context, "Auto-save ${if (newValue) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
    }
    
    fun toggleAISuggestions() {
        val newValue = !_uiState.value.aiSuggestionsEnabled
        _uiState.value = _uiState.value.copy(aiSuggestionsEnabled = newValue)
        sharedPrefs.edit().putBoolean("ai_suggestions", newValue).apply()
        
        Toast.makeText(context, "AI suggestions ${if (newValue) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
    }
    
    fun toggleAppLock() {
        val newValue = !_uiState.value.appLockEnabled
        _uiState.value = _uiState.value.copy(appLockEnabled = newValue)
        sharedPrefs.edit().putBoolean("app_lock", newValue).apply()
        
        Toast.makeText(context, "App lock ${if (newValue) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
    }
    
    fun exportNotes() {
        viewModelScope.launch {
            try {
                // Simple export functionality - get all notes and create a basic text export
                noteRepository.getAllNotes().first().let { notes ->
                    if (notes.isEmpty()) {
                        Toast.makeText(context, "No notes to export", Toast.LENGTH_SHORT).show()
                        return@let
                    }
                    
                    val exportText = buildString {
                        appendLine("=== AINoteBuddy Notes Export ===")
                        appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                        appendLine()
                        
                        notes.forEachIndexed { index, note ->
                            appendLine("--- Note ${index + 1} ---")
                            appendLine("Title: ${note.title}")
                            appendLine("Category: ${note.category}")
                            appendLine("Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.createdAt))}")
                            appendLine("Content:")
                            appendLine(note.content)
                            appendLine()
                        }
                    }
                    
                    // For now, just show a toast. In a real app, we'd save to file or share
                    Toast.makeText(context, "Export ready! ${notes.size} notes prepared", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun importNotes() {
        // Placeholder for import functionality
        Toast.makeText(context, "Import feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            try {
                // This is a destructive operation, so we'd normally show a confirmation dialog
                Toast.makeText(context, "This would clear all data (feature disabled for safety)", Toast.LENGTH_LONG).show()
                
                // To actually implement:
                // 1. Show confirmation dialog
                // 2. Clear database: database.clearAllTables()
                // 3. Clear shared preferences
                // 4. Clear file storage
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to clear data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Get setting values for other parts of the app
    fun isDarkModeEnabled(): Boolean = _uiState.value.darkModeEnabled
    fun isAutoSaveEnabled(): Boolean = _uiState.value.autoSaveEnabled
    fun areAISuggestionsEnabled(): Boolean = _uiState.value.aiSuggestionsEnabled
    fun isAppLockEnabled(): Boolean = _uiState.value.appLockEnabled
}

data class SimpleSettingsUiState(
    val darkModeEnabled: Boolean = false,
    val autoSaveEnabled: Boolean = true,
    val aiSuggestionsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)