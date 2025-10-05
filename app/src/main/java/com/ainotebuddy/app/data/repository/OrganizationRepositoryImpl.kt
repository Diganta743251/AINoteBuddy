package com.ainotebuddy.app.data.repository
 
import com.ainotebuddy.app.data.local.dao.OrganizationDao
import com.ainotebuddy.app.data.mapper.toEntity
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.RecurringNote
import com.ainotebuddy.app.data.model.organization.SmartFolder
import com.ainotebuddy.app.data.model.organization.defaultNoteTemplates
import com.ainotebuddy.app.data.model.organization.defaultSmartFolders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrganizationRepositoryImpl @Inject constructor(
    private val organizationDao: OrganizationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OrganizationRepository {

    // Smart Folders
    override suspend fun getSmartFolders(): List<SmartFolder> = withContext(ioDispatcher) {
        organizationDao.getSmartFolders().map { it.toModel() }
    }

    override fun observeSmartFolders(): Flow<List<SmartFolder>> {
        return organizationDao.observeSmartFolders().map { folders ->
            folders.map { it.toModel() }
        }
    }

    override suspend fun getSmartFolderById(id: String): SmartFolder? = withContext(ioDispatcher) {
        organizationDao.getSmartFolderById(id)?.toModel()
    }

    override suspend fun createSmartFolder(folder: SmartFolder): Result<SmartFolder> = withContext(ioDispatcher) {
        return@withContext try {
            val id = organizationDao.insertSmartFolder(folder.toEntity())
            Result.success(folder.copy(id = id.toString()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSmartFolder(folder: SmartFolder): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            organizationDao.updateSmartFolder(folder.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSmartFolder(id: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            organizationDao.deleteSmartFolder(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotesForSmartFolder(folderId: String): List<String> = withContext(ioDispatcher) {
        // This would be implemented to query notes based on the folder's rules
        // For now, return an empty list
        emptyList()
    }

    // Note Templates
    override suspend fun getTemplates(): List<NoteTemplate> = withContext(ioDispatcher) {
        organizationDao.getTemplates().map { it.toModel() }
    }

    override fun observeTemplates(): Flow<List<NoteTemplate>> {
        return organizationDao.observeTemplates().map { templates ->
            templates.map { it.toModel() }
        }
    }

    override suspend fun getTemplateById(id: String): NoteTemplate? = withContext(ioDispatcher) {
        organizationDao.getTemplateById(id)?.toModel()
    }

    override suspend fun createTemplate(template: NoteTemplate): Result<NoteTemplate> = withContext(ioDispatcher) {
        return@withContext try {
            val id = organizationDao.insertTemplate(template.toEntity())
            Result.success(template.copy(id = id.toString()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTemplate(template: NoteTemplate): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            organizationDao.updateTemplate(template.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTemplate(id: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            organizationDao.deleteTemplate(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTemplatesByCategory(category: String): List<NoteTemplate> = withContext(ioDispatcher) {
        organizationDao.getTemplatesByCategory(category).map { it.toModel() }
    }

    fun observeTemplatesByCategory(category: String): kotlinx.coroutines.flow.Flow<List<NoteTemplate>> {
        return observeTemplates().map { list -> list.filter { it.category.equals(category, ignoreCase = true) } }
    }

    // Recurring Notes
    override suspend fun getRecurringNotes(): List<RecurringNote> = withContext(ioDispatcher) {
        organizationDao.getRecurringNotes().map { it.toModel() }
    }

    override fun observeRecurringNotes(): Flow<List<RecurringNote>> {
        return organizationDao.observeRecurringNotes().map { notes ->
            notes.map { it.toModel() }
        }
    }

    override suspend fun getRecurringNoteById(id: String): RecurringNote? = withContext(ioDispatcher) {
        organizationDao.getRecurringNoteById(id)?.toModel()
    }

    override suspend fun createRecurringNote(note: RecurringNote): Result<RecurringNote> = withContext(ioDispatcher) {
        return@withContext try {
            val id = organizationDao.insertRecurringNote(note.toEntity())
            Result.success(note.copy(id = id.toString()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecurringNote(note: RecurringNote): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            organizationDao.updateRecurringNote(note.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecurringNote(id: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            organizationDao.deleteRecurringNote(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDueRecurringNotes(currentTime: Long): List<RecurringNote> = withContext(ioDispatcher) {
        organizationDao.getDueRecurringNotes(currentTime).map { it.toModel() }
    }

    override suspend fun markRecurringNoteTriggered(id: String, nextTrigger: Long): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            organizationDao.markRecurringNoteTriggered(id, nextTrigger)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // AI-powered organization
    override suspend fun analyzeAndCategorizeNote(noteId: String): Result<Unit> = withContext(ioDispatcher) {
        // This would be implemented to analyze note content and update categories/tags
        // For now, return success
        Result.success(Unit)
    }

    override suspend fun generateSmartFolderSuggestions(): List<SmartFolder> = withContext(ioDispatcher) {
        // This would be implemented to generate smart folder suggestions based on user's notes
        // For now, return an empty list
        emptyList()
    }

    

    // Initialization
    override suspend fun initializeDefaultContent() = withContext(ioDispatcher) {
        // Insert default smart folders if they don't exist
        defaultSmartFolders.forEach { folder ->
            if (organizationDao.getSmartFolderById(folder.id) == null) {
                organizationDao.insertSmartFolder(folder.toEntity())
            }
        }

        // Insert default templates if they don't exist
        defaultNoteTemplates.forEach { template ->
            if (organizationDao.getTemplateById(template.id) == null) {
                organizationDao.insertTemplate(template.toEntity())
            }
        }
    }
}
