package com.ainotebuddy.app.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.CategoryEntity
import com.ainotebuddy.app.data.model.Task
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.repository.TaskRepository
import com.ainotebuddy.app.repository.TaskRepositoryImpl
import com.ainotebuddy.app.repository.TaskCount
// TODO: Re-enable when Hilt compatibility is resolved
// import dagger.hilt.android.lifecycle.HiltViewModel
// import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import com.ainotebuddy.app.StickyNoteWidgetProvider
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import com.ainotebuddy.app.AINoteBuddyApplication
import com.ainotebuddy.app.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    // Create task repository using injected context (temporary until it's properly injectable)
    private val taskRepository by lazy { TaskRepositoryImpl(context) }

    // Combine notes with their task counts
    val notes = repository.getAllNotes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Remove categories for now since method doesn't exist
    // val categories = repository.getAllCategories().stateIn(
    //     viewModelScope,
    //     SharingStarted.WhileSubscribed(5000),
    //     emptyList()
    // )

    val tasksWithCounts = taskRepository.getTaskCounts().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

    fun insertNote(note: NoteEntity, context: Context) {
        viewModelScope.launch {
            repository.insertNote(note)
            notifyWidgetUpdate(context)
        }
    }
    
    fun updateNote(note: NoteEntity, context: Context) {
        viewModelScope.launch {
            repository.updateNote(note)
            notifyWidgetUpdate(context)
        }
    }
    
    fun deleteNote(noteId: Long, context: Context) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            note?.let { 
                repository.deleteNote(it)
                notifyWidgetUpdate(context)
            }
        }
    }
    
    suspend fun getNoteById(id: Long): NoteEntity? {
        return repository.getNoteById(id)
    }
    
    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return repository.getSearchResults(query)
    }
    
    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> {
        return repository.getNotesByCategory(category)
    }
    
    // Simplified category management - remove for now since methods don't exist
    // fun insertCategory(category: CategoryEntity) {
    //     viewModelScope.launch {
    //         repository.insertCategory(category)
    //     }
    // }
    
    // fun updateCategory(category: CategoryEntity) {
    //     viewModelScope.launch {
    //         repository.updateCategory(category)
    //     }
    // }
    
    // fun deleteCategory(categoryId: Long) {
    //     viewModelScope.launch {
    //         repository.deleteCategory(categoryId)
    //     }
    // }
    
    // Task management
    fun insertTask(task: Task) {
        viewModelScope.launch {
            taskRepository.addTask(task)
        }
    }
    
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
    
    fun getTasksForNote(noteId: Long): Flow<List<Task>> {
        return taskRepository.getTasksForNote(noteId)
    }
    
    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
        }
    }
    
    // Add missing pin/favorite functionality that MainActivity needs
    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            note?.let {
                // Simple toggle - would need to add isPinned field to NoteEntity
                // For now, just mark as favorite
                repository.markAsFavorite(noteId, true)
            }
        }
    }
    
    fun toggleFavorite(noteId: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            note?.let {
                // Toggle favorite status
                repository.markAsFavorite(noteId, !it.isFavorite)
            }
        }
    }
    
    // Widget update functionality
    private fun notifyWidgetUpdate(context: Context) {
        val intent = Intent(context, StickyNoteWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, StickyNoteWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }
    
    // Auto-sync check
    private suspend fun isAutoSyncEnabled(context: Context): Boolean {
        val autoSyncKey = booleanPreferencesKey("auto_sync_enabled")
        return context.dataStore.data.first()[autoSyncKey] ?: false
    }
    
    // Simplified sync placeholder
    private fun triggerAutoSync(context: Context) {
        // Placeholder for sync functionality
        viewModelScope.launch {
            // Would trigger sync if available
        }
    }
    
    // Bulk operations
    fun createQuickNote(title: String, content: String, context: Context) {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertNote(note)
            notifyWidgetUpdate(context)
        }
    }
    
    fun deleteMultipleNotes(noteIds: List<Long>, context: Context) {
        viewModelScope.launch {
            noteIds.forEach { noteId ->
                val note = repository.getNoteById(noteId)
                note?.let { repository.deleteNote(it) }
            }
            notifyWidgetUpdate(context)
        }
    }
}