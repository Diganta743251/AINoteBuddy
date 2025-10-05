package com.ainotebuddy.app.ai

import android.content.Context
import com.ainotebuddy.app.data.NoteEntity
import java.util.Locale

/**
 * Simple local AI service that provides basic AI-like features using local algorithms
 * instead of external AI services.
 */
class SimpleAIService(private val context: Context) {

    /**
     * Generate a simple summary of the note content
     */
    fun summarizeNote(note: NoteEntity): String {
        val content = note.content
        if (content.isBlank()) return "Empty note"
        
        val sentences = content.split(". ", "! ", "? ").filter { it.isNotBlank() }
        
        return when {
            sentences.isEmpty() -> "Empty note"
            sentences.size <= 2 -> content.take(100) + if (content.length > 100) "..." else ""
            else -> {
                // Take first and last sentence, or first two if very short
                val summary = if (sentences.size >= 3) {
                    "${sentences.first().trim()}. ${sentences.last().trim()}"
                } else {
                    sentences.take(2).joinToString(". ") + "."
                }
                summary.take(150) + if (summary.length > 150) "..." else ""
            }
        }
    }

    /**
     * Generate suggested tags based on note content
     */
    fun generateTags(note: NoteEntity): List<String> {
        val content = (note.title + " " + note.content).lowercase(Locale.getDefault())
        val tags = mutableSetOf<String>()
        
        // Common keywords for different categories
        val tagKeywords = mapOf(
            "work" to listOf("work", "job", "office", "meeting", "project", "task", "deadline", "business"),
            "personal" to listOf("personal", "family", "friend", "birthday", "vacation", "home"),
            "health" to listOf("health", "doctor", "medicine", "exercise", "fitness", "diet"),
            "finance" to listOf("money", "budget", "bank", "finance", "payment", "expense", "income"),
            "shopping" to listOf("buy", "purchase", "shop", "store", "amazon", "order"),
            "travel" to listOf("travel", "trip", "flight", "hotel", "vacation", "visit"),
            "education" to listOf("learn", "study", "course", "school", "university", "book", "exam"),
            "ideas" to listOf("idea", "think", "concept", "plan", "strategy", "solution"),
            "important" to listOf("important", "urgent", "critical", "asap", "priority"),
            "todo" to listOf("todo", "task", "do", "complete", "finish", "remember")
        )
        
        // Check for keyword matches
        tagKeywords.forEach { (tag, keywords) ->
            if (keywords.any { keyword -> content.contains(keyword) }) {
                tags.add(tag)
            }
        }
        
        // Add length-based tags
        when {
            note.content.length > 1000 -> tags.add("long")
            note.content.length < 50 -> tags.add("short")
            note.content.contains("?") -> tags.add("question")
            note.content.contains("!") -> tags.add("note")
        }
        
        return tags.take(5).toList() // Limit to 5 tags
    }

    /**
     * Analyze sentiment of the note (simple keyword-based approach)
     */
    fun analyzeSentiment(note: NoteEntity): String {
        val content = (note.title + " " + note.content).lowercase(Locale.getDefault())
        
        val positiveWords = listOf(
            "great", "good", "excellent", "amazing", "wonderful", "fantastic", "love",
            "happy", "joy", "success", "win", "achieve", "accomplished", "proud"
        )
        
        val negativeWords = listOf(
            "bad", "terrible", "awful", "hate", "sad", "angry", "frustrated", "problem",
            "issue", "wrong", "failed", "disappointed", "worried", "stress"
        )
        
        val positiveCount = positiveWords.count { content.contains(it) }
        val negativeCount = negativeWords.count { content.contains(it) }
        
        return when {
            positiveCount > negativeCount -> "Positive"
            negativeCount > positiveCount -> "Negative"
            else -> "Neutral"
        }
    }

    /**
     * Extract key phrases from note content
     */
    fun extractKeyPhrases(note: NoteEntity): List<String> {
        val content = note.content
        if (content.isBlank()) return emptyList()
        
        val phrases = mutableListOf<String>()
        
        // Extract sentences that seem important (questions, exclamations, or contain certain keywords)
        val sentences = content.split(". ", "! ", "? ").filter { it.isNotBlank() }
        
        sentences.forEach { sentence ->
            val trimmed = sentence.trim()
            when {
                trimmed.contains("?") -> phrases.add(trimmed.take(80))
                trimmed.contains("!") -> phrases.add(trimmed.take(80))
                trimmed.lowercase().contains("important") -> phrases.add(trimmed.take(80))
                trimmed.lowercase().contains("remember") -> phrases.add(trimmed.take(80))
                trimmed.lowercase().contains("note:") -> phrases.add(trimmed.take(80))
            }
        }
        
        return phrases.take(3) // Limit to top 3 phrases
    }

    /**
     * Generate writing suggestions
     */
    fun generateWritingSuggestions(note: NoteEntity): List<String> {
        val suggestions = mutableListOf<String>()
        val content = note.content
        
        if (content.isBlank()) {
            suggestions.add("Start by writing your main idea")
            suggestions.add("Add a clear title to your note")
            return suggestions
        }
        
        // Length-based suggestions
        when {
            content.length < 20 -> suggestions.add("Consider expanding with more details")
            content.length > 2000 -> suggestions.add("Consider breaking this into multiple notes")
        }
        
        // Structure suggestions
        if (!content.contains(".") && content.length > 50) {
            suggestions.add("Consider adding proper punctuation")
        }
        
        if (note.title.isBlank()) {
            suggestions.add("Add a descriptive title")
        }
        
        // Content suggestions
        if (content.lowercase().contains("todo") || content.lowercase().contains("task")) {
            suggestions.add("Consider creating a checklist format")
        }
        
        if (content.contains("?")) {
            suggestions.add("Consider researching answers to your questions")
        }
        
        return suggestions.take(3)
    }

    /**
     * Suggest related categories based on content
     */
    fun suggestCategory(note: NoteEntity): String {
        val content = (note.title + " " + note.content).lowercase(Locale.getDefault())
        
        val categoryKeywords = mapOf(
            "Work" to listOf("work", "job", "office", "meeting", "project", "business", "client"),
            "Personal" to listOf("personal", "family", "friend", "home", "diary", "journal"),
            "Health" to listOf("health", "doctor", "medicine", "exercise", "fitness", "diet"),
            "Finance" to listOf("money", "budget", "bank", "finance", "payment", "expense"),
            "Travel" to listOf("travel", "trip", "flight", "hotel", "vacation", "journey"),
            "Education" to listOf("learn", "study", "course", "school", "university", "research"),
            "Shopping" to listOf("buy", "purchase", "shop", "store", "shopping", "order"),
            "Ideas" to listOf("idea", "think", "concept", "plan", "brainstorm", "creative")
        )
        
        categoryKeywords.forEach { (category, keywords) ->
            if (keywords.any { keyword -> content.contains(keyword) }) {
                return category
            }
        }
        
        return "General" // Default category
    }

    /**
     * Simple spell check using basic patterns
     */
    fun basicSpellCheck(text: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Check for common mistakes
        val commonMistakes = mapOf(
            "teh" to "the",
            "adn" to "and", 
            "taht" to "that",
            "thier" to "their",
            "recieve" to "receive",
            "seperate" to "separate",
            "occurence" to "occurrence"
        )
        
        commonMistakes.forEach { (wrong, correct) ->
            if (text.lowercase().contains(wrong)) {
                suggestions.add("Consider changing '$wrong' to '$correct'")
            }
        }
        
        return suggestions
    }
}