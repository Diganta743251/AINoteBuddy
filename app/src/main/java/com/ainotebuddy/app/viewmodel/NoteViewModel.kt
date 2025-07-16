package com.ainotebuddy.app.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.CategoryEntity
import com.ainotebuddy.app.repository.AdvancedNoteRepository
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.sync.GoogleDriveSyncService
import com.ainotebuddy.app.auth.GoogleAuthService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.drive.Drive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import com.ainotebuddy.app.StickyNoteWidgetProvider
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import com.ainotebuddy.app.AINoteBuddyApplication

class NoteViewModel(
    private val repository: NoteRepository?,
    private val advancedRepository: AdvancedNoteRepository,
    private val googleAuthService: GoogleAuthService?
) : ViewModel() {

    val notes = advancedRepository.allNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val favoriteNotes = advancedRepository.favoriteNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val pinnedNotes = advancedRepository.pinnedNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val categories = advancedRepository.allCategories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val tags = advancedRepository.allTags.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val AUTO_SYNC_KEY = booleanPreferencesKey("auto_sync_enabled")

    private suspend fun isAutoSyncEnabled(): Boolean {
        // return AINoteBuddyApplication.dataStore.data.first()[AUTO_SYNC_KEY] == true
        return false // Commented out dataStore
    }

    fun addNote(note: NoteEntity, context: Context) {
        viewModelScope.launch {
            advancedRepository.insert(note)
            notifyWidgetUpdate(context)
            if (isAutoSyncEnabled()) {
                triggerAutoSync(context)
            }
        }
    }
    
    fun updateNote(note: NoteEntity, context: Context) {
        viewModelScope.launch {
            advancedRepository.update(note)
            notifyWidgetUpdate(context)
            if (isAutoSyncEnabled()) {
                triggerAutoSync(context)
            }
        }
    }
    fun deleteNote(noteId: Long, context: Context) {
        viewModelScope.launch {
            val note = advancedRepository.getNoteById(noteId)
            note?.let { advancedRepository.delete(it) }
            notifyWidgetUpdate(context)
            if (isAutoSyncEnabled()) {
                triggerAutoSync(context)
            }
        }
    }
    
    fun syncToDrive(account: GoogleSignInAccount, syncService: GoogleDriveSyncService) {
        viewModelScope.launch {
            try {
                val driveService = googleAuthService?.createDriveService(account)
                val currentNotes = notes.value
                // TODO: Implement sync logic
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun syncFromDrive(account: GoogleSignInAccount, syncService: GoogleDriveSyncService) {
        viewModelScope.launch {
            try {
                val driveService = googleAuthService?.createDriveService(account)
                // TODO: Implement sync logic
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun triggerAutoSync(context: Context) {
        val app = context.applicationContext as AINoteBuddyApplication
        // val account = app.googleAuthService.getCurrentAccount()
        // if (account != null) {
        //     syncToDrive(account, app.googleDriveSyncService)
        // }
    }

    // Nested folder flows/actions
    fun getRootCategories(): Flow<List<CategoryEntity>> = advancedRepository.getRootCategories()
    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>> = advancedRepository.getSubcategories(parentId)
    fun moveCategory(categoryId: Long, newParentId: Long?) {
        viewModelScope.launch {
            advancedRepository.moveCategory(categoryId, newParentId)
        }
    }

    // Vault flows/actions - TODO: Implement when vault functionality is available
    val vaultNotes = kotlinx.coroutines.flow.flowOf(emptyList<NoteEntity>()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun moveNoteToVault(noteId: Long) {
        viewModelScope.launch { 
            // TODO: Implement vault functionality
        }
    }
    fun moveNoteOutOfVault(noteId: Long) {
        viewModelScope.launch { 
            // TODO: Implement vault functionality
        }
    }

    fun setCategoryLocked(categoryId: Long, locked: Boolean) {
        viewModelScope.launch {
            advancedRepository.setCategoryLocked(categoryId, locked)
        }
    }

    suspend fun setReminder(noteId: Long, reminderTime: Long?) {
        // TODO: Implement reminder logic
    }

    private fun notifyWidgetUpdate(context: Context) {
        val intent = Intent(context, StickyNoteWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        context.sendBroadcast(intent)
    }

    // Overload for addNote to support (title, content) usage in NoteScreen
    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = NoteEntity(title = title, content = content)
            advancedRepository.insert(note)
        }
    }

    // Stub for pinNote to support DashboardScreen (implement actual logic as needed)
    fun pinNote(noteId: Long) {
        viewModelScope.launch {
            // TODO: Implement pin/unpin logic in repository
        }
    }
    
    fun toggleFavorite(noteId: Long) {
        viewModelScope.launch {
            val note = advancedRepository.getNoteById(noteId)
            note?.let {
                advancedRepository.toggleFavorite(noteId, !it.isFavorite)
            }
        }
    }
    
    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            val note = advancedRepository.getNoteById(noteId)
            note?.let {
                advancedRepository.togglePin(noteId, !it.isPinned)
            }
        }
    }
}

class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val advancedRepository: AdvancedNoteRepository,
    private val googleAuthService: GoogleAuthService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            return NoteViewModel(repository, advancedRepository, googleAuthService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
