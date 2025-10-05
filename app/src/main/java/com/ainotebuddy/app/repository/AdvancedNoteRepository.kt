package com.ainotebuddy.app.repository

import com.ainotebuddy.app.ai.AIService
import com.ainotebuddy.app.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AdvancedNoteRepository(
    private val noteDao: NoteDao?,
    private val categoryDao: CategoryDao?,
    private val tagDao: TagDao?,
    private val templateDao: TemplateDao?, // placeholder DAO
    private val checklistItemDao: ChecklistItemDao?,
    private val aiService: AIService?
) {
    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao?.getAllNotes() ?: flowOf(emptyList())
    val favoriteNotes: Flow<List<NoteEntity>> = noteDao?.getFavoriteNotes() ?: flowOf(emptyList())
    val pinnedNotes: Flow<List<NoteEntity>> = noteDao?.getPinnedNotes() ?: flowOf(emptyList())
    val archivedNotes: Flow<List<NoteEntity>> = noteDao?.getArchivedNotes() ?: flowOf(emptyList())
    val vaultNotes: Flow<List<NoteEntity>> = noteDao?.getVaultNotes() ?: flowOf(emptyList())

    // Categories
    val noteCategories: Flow<List<String>> = noteDao?.getAllCategories() ?: flowOf(emptyList())

    // Tags
    val allTags: Flow<List<TagEntity>> = tagDao?.getAllTags() ?: flowOf(emptyList())

    // Templates (not implemented)
    val allTemplates: Flow<List<TemplateEntity>> = flowOf(emptyList())

    // Checklist items
    fun getChecklistItems(noteId: Long): Flow<List<ChecklistItemEntity>> =
        checklistItemDao?.getItemsForNote(noteId) ?: flowOf(emptyList())

    suspend fun addChecklistItem(item: ChecklistItemEntity): Long =
        checklistItemDao?.insert(item) ?: 0L

    suspend fun updateChecklistItem(item: ChecklistItemEntity) {
        checklistItemDao?.update(item)
    }

    suspend fun updateChecklistItems(items: List<ChecklistItemEntity>) {
        checklistItemDao?.updateItems(items)
    }

    suspend fun deleteChecklistItem(item: ChecklistItemEntity) {
        checklistItemDao?.delete(item)
    }

    suspend fun deleteAllChecklistItemsForNote(noteId: Long) {
        checklistItemDao?.deleteAllForNote(noteId)
    }

    // Note CRUD
    suspend fun insert(note: NoteEntity): Long {
        val id = noteDao?.insert(note) ?: 0L

        // Update tags table heuristically
        if (note.tags.isNotEmpty()) {
            note.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tagName ->
                    val existing = tagDao?.getTagByName(tagName)
                    if (existing == null) {
                        tagDao?.insertTag(TagEntity(name = tagName))
                    } else {
                        tagDao?.incrementUsageCount(tagName)
                    }
                }
        }
        return id
    }

    suspend fun insertNote(note: NoteEntity): Long = insert(note)

    suspend fun insertWithAI(title: String, content: String): Long {
        val category = aiService?.categorizeNote(content, title) ?: "General"
        val tags = aiService?.suggestTags(content) ?: emptyList()
        val wc = content.split(Regex("\\W+")).count { it.isNotBlank() }
        val readTime = (wc / 200).coerceAtLeast(1)

        val note = NoteEntity(
            title = title,
            content = content,
            category = category,
            tags = tags.joinToString(","),
            wordCount = wc,
            readTime = readTime,
            updatedAt = System.currentTimeMillis()
        )
        return insert(note)
    }

    suspend fun update(note: NoteEntity) {
        val wc = note.content.split(Regex("\\W+")).count { it.isNotBlank() }
        val updated = note.copy(
            updatedAt = System.currentTimeMillis(),
            wordCount = wc,
            readTime = (wc / 200).coerceAtLeast(1)
        )
        noteDao?.update(updated)

        if (updated.tags.isNotEmpty()) {
            updated.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tagName ->
                    val existing = tagDao?.getTagByName(tagName)
                    if (existing == null) {
                        tagDao?.insertTag(TagEntity(name = tagName))
                    } else {
                        tagDao?.incrementUsageCount(tagName)
                    }
                }
        }
    }

    suspend fun delete(note: NoteEntity) {
        noteDao?.softDelete(note.id)
    }

    suspend fun getNoteById(noteId: Long): NoteEntity? = noteDao?.getNoteById(noteId)

    // Queries
    fun searchNotes(query: String): Flow<List<NoteEntity>> =
        noteDao?.searchNotes(query) ?: flowOf(emptyList())

    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> =
        noteDao?.getNotesByCategory(category) ?: flowOf(emptyList())

    fun getNotesByTag(tag: String): Flow<List<NoteEntity>> =
        noteDao?.getNotesByTag(tag) ?: flowOf(emptyList())

    // Note actions
    suspend fun togglePin(noteId: Long, isPinned: Boolean) { noteDao?.togglePin(noteId, isPinned) }
    suspend fun toggleFavorite(noteId: Long, isFavorite: Boolean) { noteDao?.toggleFavorite(noteId, isFavorite) }
    suspend fun toggleArchive(noteId: Long, isArchived: Boolean) { noteDao?.toggleArchive(noteId, isArchived) }
    suspend fun updateCategory(noteId: Long, category: String) { noteDao?.updateCategory(noteId, category) }
    suspend fun updateTags(noteId: Long, tags: String) { noteDao?.updateTags(noteId, tags) }
    suspend fun setReminder(noteId: Long, reminderTime: Long?) { noteDao?.setReminder(noteId, reminderTime) }
    suspend fun setNoteInVault(noteId: Long, inVault: Boolean) { noteDao?.setNoteInVault(noteId, inVault) }

    // Category ops
    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao?.insert(category) ?: 0L
    suspend fun updateCategory(category: CategoryEntity) { categoryDao?.update(category) }
    suspend fun deleteCategory(category: CategoryEntity) { categoryDao?.delete(category) }
    suspend fun getDefaultCategory(): CategoryEntity? = categoryDao?.getDefaultCategory()
    suspend fun setCategoryLocked(categoryId: Long, locked: Boolean) { categoryDao?.setCategoryLocked(categoryId, locked) }
    fun getRootCategories(): Flow<List<CategoryEntity>> = categoryDao?.getRootCategories() ?: flowOf(emptyList())
    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>> = categoryDao?.getSubcategories(parentId) ?: flowOf(emptyList())
    suspend fun moveCategory(categoryId: Long, newParentId: Long?) { categoryDao?.moveCategory(categoryId, newParentId) }

    // Tag ops
    suspend fun searchTags(query: String): List<TagEntity> = tagDao?.searchTags(query) ?: emptyList()
    suspend fun insertTag(tag: TagEntity): Long = tagDao?.insertTag(tag) ?: 0L
    suspend fun updateTag(tag: TagEntity) { tagDao?.updateTag(tag) }
    suspend fun deleteTag(tag: TagEntity) { tagDao?.deleteTag(tag) }

    // Templates - stubs
    suspend fun insertTemplate(template: TemplateEntity): Long = 0L
    suspend fun updateTemplate(template: TemplateEntity) {}
    suspend fun deleteTemplate(template: TemplateEntity) {}
    suspend fun getTemplateById(templateId: Long): TemplateEntity? = null
    fun getTemplatesByCategory(category: String): Flow<List<TemplateEntity>> = flowOf(emptyList())
    fun getPopularTemplates(): Flow<List<TemplateEntity>> = flowOf(emptyList())
    fun getRecentTemplates(): Flow<List<TemplateEntity>> = flowOf(emptyList())

    // AI-enhanced
    suspend fun generateSummary(noteId: Long): String {
        val note = getNoteById(noteId)
        val content = note?.content ?: ""
        return if (content.length <= 120) content else content.take(120) + "..."
    }

    suspend fun suggestImprovements(noteId: Long): List<String> {
        val note = getNoteById(noteId)
        val content = note?.content ?: ""
        val suggestions = mutableListOf<String>()
        if (content.length < 80) suggestions.add("Consider adding more details to the note")
        if (!content.contains(Regex("[.!?]"))) suggestions.add("Add punctuation for readability")
        return suggestions
    }

    suspend fun translateNote(noteId: Long, targetLanguage: String): String {
        val note = getNoteById(noteId)
        return note?.content ?: ""
    }

    suspend fun generateTags(noteId: Long): List<String> {
        val note = getNoteById(noteId)
        return aiService?.suggestTags(note?.content ?: "") ?: emptyList()
    }

    suspend fun categorizeNote(noteId: Long): String {
        val note = getNoteById(noteId)
        return aiService?.categorizeNote(note?.content ?: "", note?.title ?: "") ?: "General"
    }

    suspend fun getNoteStats(noteId: Long): NoteStats {
        val note = getNoteById(noteId)
        val tags = note?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        return NoteStats(
            wordCount = note?.wordCount ?: 0,
            readTime = note?.readTime ?: 0,
            lastModified = note?.updatedAt ?: 0L,
            category = note?.category ?: "General",
            tags = tags
        )
    }

    suspend fun getWritingInsights(noteId: Long): WritingInsights {
        val note = getNoteById(noteId)
        val content = note?.content ?: ""
        val words = content.split(Regex("\\W+")).filter { it.isNotBlank() }
        val longWords = words.count { it.length > 6 }
        val readability = if (words.isNotEmpty()) 1.0 - (longWords.toDouble() / words.size) else 0.0
        val sentiment = when {
            content.contains(Regex("(great|excellent|amazing|wonderful|fantastic)", RegexOption.IGNORE_CASE)) -> "positive"
            content.contains(Regex("(bad|terrible|awful|horrible|disappointing)", RegexOption.IGNORE_CASE)) -> "negative"
            else -> "neutral"
        }
        return WritingInsights(
            readabilityScore = readability,
            sentiment = sentiment,
            keyTopics = emptyList(),
            suggestions = emptyList()
        )
    }
}

data class NoteStats(
    val wordCount: Int,
    val readTime: Int,
    val lastModified: Long,
    val category: String,
    val tags: List<String>
)

data class WritingInsights(
    val readabilityScore: Double,
    val sentiment: String,
    val keyTopics: List<String>,
    val suggestions: List<String>
)
