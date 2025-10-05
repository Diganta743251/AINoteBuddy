package com.ainotebuddy.app.data.ai

import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.SmartFolder

import com.ainotebuddy.app.data.repository.OrganizationRepository
import com.ainotebuddy.app.repository.NoteRepository as EntityNoteRepository
import com.ainotebuddy.app.data.repository.NoteRepository as DomainNoteRepository
// No custom Result type; we will return kotlin.Result from public methods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that provides AI-powered smart organization features
 */
@Singleton
class AISmartOrganizationService @Inject constructor(
    private val noteRepository: DomainNoteRepository,
    private val organizationRepository: OrganizationRepository,
    private val aiServiceProvider: com.ainotebuddy.app.ai.AIServiceProvider
) {
    /**
     * Analyzes a note and suggests smart folders it should belong to
     */
    suspend fun suggestFoldersForNote(noteId: String): kotlin.Result<List<Pair<SmartFolder, Double>>> {
        return withContext(Dispatchers.IO) {
            try {
                val note = noteRepository.getNoteById(noteId) ?: return@withContext kotlin.Result.failure(IllegalArgumentException("Note not found"))
                val folders = organizationRepository.getSmartFolders()
                
                // If no folders exist, suggest creating some
                if (folders.isEmpty()) {
                    return@withContext kotlin.Result.success(emptyList())
                }
                
                // Analyze note content and title to determine relevance to each folder
                val folderScores = folders.map { folder ->
                    val relevance = calculateFolderRelevance(note, folder)
                    folder to relevance
                }.filter { (_, score) -> score > 0.5 } // Only include relevant folders
                
                kotlin.Result.success(folderScores.sortedByDescending { it.second })
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
    
    /**
     * Analyzes a note and suggests related notes
     */
    suspend fun findRelatedNotes(noteId: String, limit: Int = 5): kotlin.Result<List<Pair<com.ainotebuddy.app.data.Note, Double>>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentNote = noteRepository.getNoteById(noteId) ?: 
                    return@withContext kotlin.Result.failure(IllegalArgumentException("Note not found"))
                
                // Get all notes except the current one
                val allNotes = noteRepository.getAllNotes().first()
                    .filter { it.id != noteId.toLongOrNull() }
                
                // Calculate similarity scores with other notes
                val relatedNotes = allNotes.map { note ->
                    val similarity = calculateNoteSimilarity(currentNote, note)
                    note to similarity
                }.filter { (_, score) -> score > 0.3 } // Only include somewhat similar notes
                .sortedByDescending { it.second }
                .take(limit)
                
                kotlin.Result.success(relatedNotes)
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
    
    /**
     * Generates a template from an existing note
     */
    suspend fun generateTemplateFromNote(noteId: String): kotlin.Result<NoteTemplate> {
        return withContext(Dispatchers.IO) {
            try {
                val note = noteRepository.getNoteById(noteId) ?: 
                    return@withContext kotlin.Result.failure(IllegalArgumentException("Note not found"))
                
                val prompt = """
                    Analyze the following note and create a reusable template from it.
                    Extract the structure, key elements, and variables that could be parameterized.
                    
                    Note Title: ${note.title}
                    Note Content: ${note.content}
                    
                    Return the response in the following JSON format:
                    {
                        "name": "Template name based on note content",
                        "description": "Brief description of when to use this template",
                        "category": "Category name (e.g., Work, Personal, Meeting)",
                        "content": "The template content with variables in {{variable_name}} format",
                        "variables": [
                            {
                                "name": "variable_name",
                                "defaultValue": "default value",
                                "description": "Description of what this variable represents"
                            }
                        ]
                    }
                """.trimIndent()
                
                val response = aiServiceProvider.processNaturalLanguageQuery(prompt).rawResponse ?: ""
                val template = parseTemplateFromAIResponse(response)
                
                kotlin.Result.success(template)
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
    
    /**
     * Suggests smart folder rules based on note content
     */
    suspend fun suggestFolderRules(noteId: String): kotlin.Result<List<com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule>> {
        return withContext(Dispatchers.IO) {
            try {
                val note = noteRepository.getNoteById(noteId) ?: 
                    return@withContext kotlin.Result.failure(IllegalArgumentException("Note not found"))
                
                val prompt = """
                    Analyze the following note and suggest rules for automatically categorizing similar notes.
                    Consider the content, title, and any identifiable patterns or categories.
                    
                    Note Title: ${note.title}
                    Note Content: ${note.content}
                    
                    Return the response as a JSON array of rules with the following format:
                    [
                        {
                            "field": "title|content|tags",
                            "operator": "contains|equals|startsWith|endsWith|matches",
                            "value": "value to match",
                            "conditionType": "AND|OR"
                        }
                    ]
                """.trimIndent()
                
                val response = aiServiceProvider.processNaturalLanguageQuery(prompt).rawResponse ?: ""
                val rules = parseRulesFromAIResponse(response)
                
                kotlin.Result.success(rules)
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
    
    /**
     * Analyzes notes and suggests smart folders to create
     */
    suspend fun suggestSmartFolders(): kotlin.Result<List<Pair<String, List<com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule>>>> {
        return withContext(Dispatchers.IO) {
            try {
                val notes = noteRepository.getAllNotes().first()
                if (notes.isEmpty()) {
                    return@withContext kotlin.Result.success(emptyList())
                }
                
                // Sample a subset of notes for analysis if there are too many
                val sampleNotes = if (notes.size > 50) {
                    notes.shuffled().take(50)
                } else {
                    notes
                }
                
                val prompt = """
                    Analyze the following notes and suggest categories or smart folders that could be created 
                    to better organize them. For each suggested folder, provide a set of rules that would 
                    automatically categorize similar notes.
                    
                    Notes Sample:
                    ${sampleNotes.joinToString("\n---\n") { "Title: ${it.title}\nContent: ${it.content.take(200)}..." }}
                    
                    Return the response as a JSON array of objects with the following format:
                    [
                        {
                            "name": "Folder Name",
                            "description": "Brief description of what this folder contains",
                            "rules": [
                                {
                                    "field": "title|content|tags",
                                    "operator": "contains|equals|startsWith|endsWith|matches",
                                    "value": "value to match",
                                    "conditionType": "AND|OR"
                                }
                            ]
                        }
                    ]
                """.trimIndent()
                
                val response = aiServiceProvider.processNaturalLanguageQuery(prompt).rawResponse ?: ""
                val suggestions = parseFolderSuggestionsFromAIResponse(response)
                
                kotlin.Result.success(suggestions)
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
    
    // Helper methods for calculating relevance and similarity
    
    private suspend fun calculateFolderRelevance(note: com.ainotebuddy.app.data.Note, folder: SmartFolder): Double {
        // If folder has no rules, use AI to determine relevance
        if (folder.rules.isEmpty()) {
            return calculateAIRelatedness(note, folder.name, folder.description)
        }
        
        // Otherwise, evaluate the folder's rules against the note
        return evaluateFolderRules(note, folder.rules)
    }
    
    private fun evaluateFolderRules(note: Note, rules: List<com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule>): Double {
        // Simple implementation: count how many rules match
        val matchedRules = rules.count { rule ->
            when (rule.field.lowercase()) {
                "title" -> evaluateRule(note.title, rule)
                "content" -> evaluateRule(note.content, rule)
                "tags" -> note.tags.any { tag -> evaluateRule(tag, rule) }
                else -> false
            }
        }
        
        return matchedRules.toDouble() / rules.size.coerceAtLeast(1)
    }
    
    private fun evaluateRule(fieldValue: String, rule: com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule): Boolean {
        return when (rule.operator.lowercase()) {
            "contains" -> fieldValue.contains(rule.value, ignoreCase = true)
            "equals" -> fieldValue.equals(rule.value, ignoreCase = true)
            "startswith" -> fieldValue.startsWith(rule.value, ignoreCase = true)
            "endswith" -> fieldValue.endsWith(rule.value, ignoreCase = true)
            "matches" -> fieldValue.matches(Regex(rule.value, RegexOption.IGNORE_CASE))
            else -> false
        }
    }
    
    private suspend fun calculateNoteSimilarity(note1: Note, note2: Note): Double {
        // Simple content-based similarity (can be enhanced with embeddings)
        val contentSimilarity = calculateTextSimilarity(note1.content, note2.content)
        val titleSimilarity = calculateTextSimilarity(note1.title, note2.title)
        
        // Weighted average (title is more important than content)
        return (titleSimilarity * 0.6) + (contentSimilarity * 0.4)
    }
    
    private suspend fun calculateTextSimilarity(text1: String, text2: String): Double {
        // Simple implementation using Jaccard similarity of word sets
        val words1 = text1.split("\\s+").toSet()
        val words2 = text2.split("\\s+").toSet()
        
        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val intersection = words1.intersect(words2).size.toDouble()
        val union = words1.union(words2).size.toDouble()
        
        return intersection / union
    }
    
    private suspend fun calculateAIRelatedness(note: Note, name: String, description: String): Double {
        val prompt = """
            On a scale from 0.0 to 1.0, how relevant is this note to the folder "$name"?
            Folder description: $description
            
            Note Title: ${note.title}
            Note Content: ${note.content.take(500)}
            
            Respond with only a number between 0.0 and 1.0, where 0.0 is not relevant at all and 1.0 is highly relevant.
        """.trimIndent()
        
        return try {
            val response = aiServiceProvider.processNaturalLanguageQuery(prompt).rawResponse?.trim()
            response?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    // Helper methods for parsing AI responses
    
    private fun parseTemplateFromAIResponse(response: String): NoteTemplate {
        // Parse JSON response and create a NoteTemplate
        // This is a simplified implementation - in a real app, use a proper JSON parser
        return try {
            // Extract values using regex (simplified)
            val name = """"name"\s*:\s*"([^"]*)"""".toRegex()
                .find(response)?.groupValues?.get(1) ?: "New Template"
                
            val description = """"description"\s*:\s*"([^"]*)"""".toRegex()
                .find(response)?.groupValues?.get(1) ?: ""
                
            val category = """"category"\s*:\s*"([^"]*)"""".toRegex()
                .find(response)?.groupValues?.get(1) ?: "General"
                
            val content = """"content"\s*:\s*"([^"]*)"""".toRegex()
                .find(response)?.groupValues?.get(1) ?: ""
            
            // Create a default template if parsing fails
            NoteTemplate(
                name = name,
                description = description,
                icon = "📝",
                category = category,
                content = content,
                variables = listOf(
                    NoteTemplate.TemplateVariable("title", "", "Note title"),
                    NoteTemplate.TemplateVariable("content", "", "Note content")
                )
            )
        } catch (e: Exception) {
            // Return a default template if parsing fails
            NoteTemplate(
                name = "New Template",
                description = "Generated from note",
                icon = "📝",
                category = "General",
                content = "# {{title}}\n\n{{content}}",
                variables = listOf(
                    NoteTemplate.TemplateVariable("title", "", "Note title"),
                    NoteTemplate.TemplateVariable("content", "", "Note content")
                )
            )
        }
    }
    
    private fun parseRulesFromAIResponse(response: String): List<com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule> {
        // Parse JSON response and create a list of SmartFolderRule
        // This is a simplified implementation - in a real app, use a proper JSON parser
        return try {
            // Extract rules using regex (simplified)
            val ruleRegex = """\{\s*"field"\s*:\s*"([^"]*)"\s*,\s*"operator"\s*:\s*"([^"]*)"\s*,\s*"value"\s*:\s*"([^"]*)"\s*,\s*"conditionType"\s*:\s*"([^"]*)"\s*\}""".toRegex()
            
            ruleRegex.findAll(response).map { match ->
                val (field, operator, value, conditionType) = match.destructured
                com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule(
                    field = field,
                    operator = operator,
                    value = value
                )
            }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseFolderSuggestionsFromAIResponse(response: String): List<Pair<String, List<com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule>>> {
        // Parse JSON response and create a list of folder suggestions with rules
        // This is a simplified implementation - in a real app, use a proper JSON parser
        return try {
            // Extract folder suggestions using regex (simplified)
            val folderRegex = """\{\s*"name"\s*:\s*"([^"]*)"\s*,\s*"description"\s*:\s*"([^"]*)"\s*,\s*"rules"\s*:\s*\[(.*?)\]\s*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            
            folderRegex.findAll(response).map { match ->
                val (name, _, rulesJson) = match.destructured
                val rules = parseRulesFromAIResponse("[$rulesJson]")
                name to rules
            }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
