package com.ainotebuddy.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import kotlin.math.min
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced named entity recognition system
 */
@Singleton
class EntityRecognizer @Inject constructor() {
    
    // Entity patterns
    private val personPatterns = listOf(
        Pattern.compile("\\b[A-Z][a-z]+\\s+[A-Z][a-z]+\\b"), // First Last
        Pattern.compile("\\b(Mr|Mrs|Ms|Dr|Prof)\\s+[A-Z][a-z]+\\b"), // Title Name
        Pattern.compile("\\b[A-Z][a-z]+\\s+(said|told|mentioned|asked|replied)\\b"), // Name + speech verb
    )
    
    private val organizationPatterns = listOf(
        Pattern.compile("\\b[A-Z][a-z]*\\s+(Inc|LLC|Corp|Company|Corporation|Ltd|Limited)\\b"),
        Pattern.compile("\\b(Google|Microsoft|Apple|Amazon|Facebook|Meta|Netflix|Tesla|Uber|Airbnb)\\b"),
        Pattern.compile("\\b[A-Z]{2,}\\b"), // Acronyms
        Pattern.compile("\\b(University|College|School)\\s+of\\s+[A-Z][a-z]+\\b"),
    )
    
    private val locationPatterns = listOf(
        Pattern.compile("\\b[A-Z][a-z]+,\\s*[A-Z]{2}\\b"), // City, State
        Pattern.compile("\\b(New York|Los Angeles|Chicago|Houston|Phoenix|Philadelphia|San Antonio|San Diego|Dallas|San Jose|Austin|Jacksonville|Fort Worth|Columbus|Charlotte|San Francisco|Indianapolis|Seattle|Denver|Washington|Boston|El Paso|Nashville|Detroit|Oklahoma City|Portland|Las Vegas|Memphis|Louisville|Baltimore|Milwaukee|Albuquerque|Tucson|Fresno|Sacramento|Kansas City|Long Beach|Mesa|Atlanta|Colorado Springs|Virginia Beach|Raleigh|Omaha|Miami|Oakland|Minneapolis|Tulsa|Wichita|New Orleans|Arlington)\\b"),
        Pattern.compile("\\b(at|in|from|to)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)\\b"),
    )
    
    private val datePatterns = listOf(
        Pattern.compile("\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},?\\s+\\d{4}\\b"),
        Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b"),
        Pattern.compile("\\b\\d{1,2}-\\d{1,2}-\\d{2,4}\\b"),
        Pattern.compile("\\b(today|tomorrow|yesterday|next week|last week|this week)\\b", Pattern.CASE_INSENSITIVE),
    )
    
    private val timePatterns = listOf(
        Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?\\b"),
        Pattern.compile("\\b\\d{1,2}\\s*(AM|PM|am|pm)\\b"),
        Pattern.compile("\\b(morning|afternoon|evening|night|noon|midnight)\\b", Pattern.CASE_INSENSITIVE),
    )
    
    private val moneyPatterns = listOf(
        Pattern.compile("\\$\\d+(?:,\\d{3})*(?:\\.\\d{2})?\\b"),
        Pattern.compile("\\b\\d+(?:,\\d{3})*(?:\\.\\d{2})?\\s*dollars?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(USD|EUR|GBP|JPY|CAD|AUD)\\s*\\d+(?:,\\d{3})*(?:\\.\\d{2})?\\b"),
    )
    
    private val phonePatterns = listOf(
        Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b"),
        Pattern.compile("\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b"),
        Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{4}\\b"),
        Pattern.compile("\\b\\d{10}\\b"),
    )
    
    private val emailPatterns = listOf(
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
    )
    
    private val urlPatterns = listOf(
        Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"),
        Pattern.compile("www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"),
    )
    
    // Common names for better person recognition
    private val commonFirstNames = setOf(
        "james", "robert", "john", "michael", "david", "william", "richard", "charles",
        "joseph", "thomas", "christopher", "daniel", "paul", "mark", "donald", "steven",
        "mary", "patricia", "jennifer", "linda", "elizabeth", "barbara", "susan", "jessica",
        "sarah", "karen", "lisa", "nancy", "betty", "helen", "sandra", "donna", "carol"
    )
    
    private val commonLastNames = setOf(
        "smith", "johnson", "williams", "brown", "jones", "garcia", "miller", "davis",
        "rodriguez", "martinez", "hernandez", "lopez", "gonzalez", "wilson", "anderson",
        "thomas", "taylor", "moore", "jackson", "martin", "lee", "perez", "thompson"
    )
    
    /**
     * Recognize entities in text content
     */
    suspend fun recognizeEntities(content: String): List<EntityResult> = withContext(Dispatchers.IO) {
        if (content.isBlank()) {
            return@withContext emptyList()
        }
        
        val entities = mutableListOf<EntityResult>()
        
        // Extract different types of entities
        entities.addAll(extractPersons(content))
        entities.addAll(extractOrganizations(content))
        entities.addAll(extractLocations(content))
        entities.addAll(extractDates(content))
        entities.addAll(extractTimes(content))
        entities.addAll(extractMoney(content))
        entities.addAll(extractPhones(content))
        entities.addAll(extractEmails(content))
        entities.addAll(extractUrls(content))
        
        // Remove duplicates and sort by importance
        entities.distinctBy { "${it.type}_${it.text.lowercase()}" }
            .sortedByDescending { it.importance * it.confidence }
    }
    
    /**
     * Extract entities from multiple texts in batch
     */
    suspend fun batchRecognizeEntities(contents: List<String>): List<List<EntityResult>> = withContext(Dispatchers.IO) {
        contents.map { content ->
            recognizeEntities(content)
        }
    }
    
    private fun extractPersons(content: String): List<EntityResult> {
        val persons = mutableListOf<EntityResult>()
        
        personPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val personName = matcher.group()?.trim() ?: continue
                val confidence = calculatePersonConfidence(personName, content)
                val importance = calculatePersonImportance(personName, content)
                
                if (confidence > 0.3f) {
                    persons.add(
                        EntityResult(
                            text = personName,
                            type = EntityType.PERSON,
                            confidence = confidence,
                            importance = importance,
                            context = extractEntityContext(content, matcher.start(), matcher.end())
                        )
                    )
                }
            }
        }
        
        return persons
    }
    
    private fun extractOrganizations(content: String): List<EntityResult> {
        val organizations = mutableListOf<EntityResult>()
        
        organizationPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val orgName = matcher.group().trim()
                val confidence = calculateOrganizationConfidence(orgName, content)
                val importance = calculateOrganizationImportance(orgName, content)
                
                if (confidence > 0.4f) {
                    organizations.add(
                        EntityResult(
                            text = orgName,
                            type = EntityType.ORGANIZATION,
                            confidence = confidence,
                            importance = importance,
                            context = extractEntityContext(content, matcher.start(), matcher.end())
                        )
                    )
                }
            }
        }
        
        return organizations
    }
    
    private fun extractLocations(content: String): List<EntityResult> {
        val locations = mutableListOf<EntityResult>()
        
        locationPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val locationName = matcher.group().trim()
                val confidence = calculateLocationConfidence(locationName, content)
                val importance = calculateLocationImportance(locationName, content)
                
                if (confidence > 0.4f) {
                    locations.add(
                        EntityResult(
                            text = locationName,
                            type = EntityType.LOCATION,
                            confidence = confidence,
                            importance = importance,
                            context = extractEntityContext(content, matcher.start(), matcher.end())
                        )
                    )
                }
            }
        }
        
        return locations
    }
    
    private fun extractDates(content: String): List<EntityResult> {
        val dates = mutableListOf<EntityResult>()
        
        datePatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val dateText = matcher.group().trim()
                val confidence = 0.9f // Dates are usually high confidence
                val importance = calculateDateImportance(dateText, content)
                
                dates.add(
                    EntityResult(
                        text = dateText,
                        type = EntityType.DATE,
                        confidence = confidence,
                        importance = importance,
                        context = extractEntityContext(content, matcher.start(), matcher.end())
                    )
                )
            }
        }
        
        return dates
    }
    
    private fun extractTimes(content: String): List<EntityResult> {
        val times = mutableListOf<EntityResult>()
        
        timePatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val timeText = matcher.group().trim()
                val confidence = 0.8f // Times are usually high confidence
                val importance = calculateTimeImportance(timeText, content)
                
                times.add(
                    EntityResult(
                        text = timeText,
                        type = EntityType.TIME,
                        confidence = confidence,
                        importance = importance,
                        context = extractEntityContext(content, matcher.start(), matcher.end())
                    )
                )
            }
        }
        
        return times
    }
    
    private fun extractMoney(content: String): List<EntityResult> {
        val money = mutableListOf<EntityResult>()
        
        moneyPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val moneyText = matcher.group().trim()
                val confidence = 0.95f // Money patterns are very specific
                val importance = calculateMoneyImportance(moneyText, content)
                
                money.add(
                    EntityResult(
                        text = moneyText,
                        type = EntityType.MONEY,
                        confidence = confidence,
                        importance = importance,
                        context = extractEntityContext(content, matcher.start(), matcher.end())
                    )
                )
            }
        }
        
        return money
    }
    
    private fun extractPhones(content: String): List<EntityResult> {
        val phones = mutableListOf<EntityResult>()
        
        phonePatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val phoneText = matcher.group().trim()
                val confidence = 0.9f // Phone patterns are specific
                val importance = 0.7f // Phones are generally important
                
                phones.add(
                    EntityResult(
                        text = phoneText,
                        type = EntityType.PHONE,
                        confidence = confidence,
                        importance = importance,
                        context = extractEntityContext(content, matcher.start(), matcher.end())
                    )
                )
            }
        }
        
        return phones
    }
    
    private fun extractEmails(content: String): List<EntityResult> {
        val emails = mutableListOf<EntityResult>()
        
        emailPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val emailText = matcher.group().trim()
                val confidence = 0.95f // Email patterns are very specific
                val importance = 0.8f // Emails are generally important
                
                emails.add(
                    EntityResult(
                        text = emailText,
                        type = EntityType.EMAIL,
                        confidence = confidence,
                        importance = importance,
                        context = extractEntityContext(content, matcher.start(), matcher.end())
                    )
                )
            }
        }
        
        return emails
    }
    
    private fun extractUrls(content: String): List<EntityResult> {
        val urls = mutableListOf<EntityResult>()
        
        urlPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val urlText = matcher.group().trim()
                val confidence = 0.9f // URL patterns are specific
                val importance = 0.6f // URLs are moderately important
                
                urls.add(
                    EntityResult(
                        text = urlText,
                        type = EntityType.URL,
                        confidence = confidence,
                        importance = importance,
                        context = extractEntityContext(content, matcher.start(), matcher.end())
                    )
                )
            }
        }
        
        return urls
    }
    
    private fun calculatePersonConfidence(name: String, content: String): Float {
        var confidence = 0.5f
        
        val nameParts = name.split(" ")
        if (nameParts.size >= 2) {
            val firstName = nameParts[0].lowercase()
            val lastName = nameParts.last().lowercase()
            
            // Boost for common names
            if (firstName in commonFirstNames) confidence += 0.2f
            if (lastName in commonLastNames) confidence += 0.2f
            
            // Boost for proper capitalization
            if (nameParts.all { it[0].isUpperCase() && it.substring(1).all { c -> c.isLowerCase() } }) {
                confidence += 0.2f
            }
            
            // Boost for context clues
            val lowerContent = content.lowercase()
            if (lowerContent.contains("$firstName said") || 
                lowerContent.contains("$firstName told") ||
                lowerContent.contains("meeting with $firstName")) {
                confidence += 0.3f
            }
        }
        
        return min(confidence, 1f)
    }
    
    private fun calculateOrganizationConfidence(orgName: String, content: String): Float {
        var confidence = 0.6f
        
        // Boost for known companies
        val knownCompanies = setOf("google", "microsoft", "apple", "amazon", "facebook", "meta")
        if (orgName.lowercase() in knownCompanies) {
            confidence += 0.3f
        }
        
        // Boost for company suffixes
        if (orgName.contains(Regex("\\b(Inc|LLC|Corp|Company|Corporation|Ltd|Limited)\\b"))) {
            confidence += 0.2f
        }
        
        // Boost for context
        val lowerContent = content.lowercase()
        if (lowerContent.contains("work at") || lowerContent.contains("company") || lowerContent.contains("business")) {
            confidence += 0.1f
        }
        
        return min(confidence, 1f)
    }
    
    private fun calculateLocationConfidence(location: String, content: String): Float {
        var confidence = 0.5f
        
        // Boost for known cities
        val majorCities = setOf("new york", "los angeles", "chicago", "houston", "phoenix", "philadelphia")
        if (location.lowercase() in majorCities) {
            confidence += 0.3f
        }
        
        // Boost for location prepositions
        val lowerContent = content.lowercase()
        if (lowerContent.contains("in ${location.lowercase()}") || 
            lowerContent.contains("at ${location.lowercase()}") ||
            lowerContent.contains("from ${location.lowercase()}")) {
            confidence += 0.2f
        }
        
        return min(confidence, 1f)
    }
    
    private fun calculatePersonImportance(name: String, content: String): Float {
        var importance = 0.5f
        
        // Count mentions
        val mentions = content.split(Regex("\\s+")).count { it.contains(name.split(" ")[0], ignoreCase = true) }
        importance += min(mentions * 0.1f, 0.3f)
        
        // Boost for context
        val lowerContent = content.lowercase()
        val nameFirst = name.split(" ")[0].lowercase()
        
        if (lowerContent.contains("meeting with $nameFirst") || 
            lowerContent.contains("call $nameFirst") ||
            lowerContent.contains("$nameFirst said")) {
            importance += 0.3f
        }
        
        return min(importance, 1f)
    }
    
    private fun calculateOrganizationImportance(orgName: String, content: String): Float {
        var importance = 0.6f
        
        // Count mentions
        val mentions = content.split(Regex("\\s+")).count { it.contains(orgName, ignoreCase = true) }
        importance += min(mentions * 0.1f, 0.2f)
        
        // Boost for work context
        val lowerContent = content.lowercase()
        if (lowerContent.contains("work") || lowerContent.contains("job") || lowerContent.contains("career")) {
            importance += 0.2f
        }
        
        return min(importance, 1f)
    }
    
    private fun calculateLocationImportance(location: String, content: String): Float {
        var importance = 0.4f
        
        // Boost for travel context
        val lowerContent = content.lowercase()
        if (lowerContent.contains("travel") || lowerContent.contains("trip") || lowerContent.contains("visit")) {
            importance += 0.3f
        }
        
        // Boost for meeting context
        if (lowerContent.contains("meeting") || lowerContent.contains("conference")) {
            importance += 0.2f
        }
        
        return min(importance, 1f)
    }
    
    private fun calculateDateImportance(date: String, content: String): Float {
        var importance = 0.7f
        
        // Boost for deadline context
        val lowerContent = content.lowercase()
        if (lowerContent.contains("deadline") || lowerContent.contains("due") || lowerContent.contains("by")) {
            importance += 0.2f
        }
        
        // Boost for event context
        if (lowerContent.contains("meeting") || lowerContent.contains("appointment") || lowerContent.contains("event")) {
            importance += 0.1f
        }
        
        return min(importance, 1f)
    }
    
    private fun calculateTimeImportance(time: String, content: String): Float {
        var importance = 0.6f
        
        // Boost for scheduling context
        val lowerContent = content.lowercase()
        if (lowerContent.contains("meeting") || lowerContent.contains("call") || lowerContent.contains("appointment")) {
            importance += 0.3f
        }
        
        return min(importance, 1f)
    }
    
    private fun calculateMoneyImportance(money: String, content: String): Float {
        var importance = 0.8f
        
        // Extract amount for importance calculation
        val amount = money.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
        
        // Higher amounts are more important
        when {
            amount > 10000 -> importance += 0.2f
            amount > 1000 -> importance += 0.1f
        }
        
        // Boost for financial context
        val lowerContent = content.lowercase()
        if (lowerContent.contains("budget") || lowerContent.contains("cost") || lowerContent.contains("price")) {
            importance += 0.1f
        }
        
        return min(importance, 1f)
    }
    
    private fun extractEntityContext(content: String, start: Int, end: Int): String {
        val contextRadius = 50
        val contextStart = maxOf(0, start - contextRadius)
        val contextEnd = minOf(content.length, end + contextRadius)
        
        return content.substring(contextStart, contextEnd).trim()
    }
    
    /**
     * Analyze entity patterns and relationships
     */
    fun analyzeEntityPatterns(entities: List<List<EntityResult>>): EntityPatterns {
        val allEntities = entities.flatten()
        
        val typeDistribution = allEntities.groupingBy { it.type }.eachCount()
        val topEntities = allEntities.groupingBy { it.text }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(20)
            .map { (text, count) -> EntityFrequency(text = text, type = EntityType.MISC, count = count, confidence = 0.5f) }
        
        val relationships = findEntityRelationships(entities)
        
        return EntityPatterns(
            typeDistribution = typeDistribution,
            topEntities = topEntities,
            relationships = relationships,
            averageEntitiesPerDocument = allEntities.size.toFloat() / entities.size
        )
    }
    
    private fun findEntityRelationships(entities: List<List<EntityResult>>): List<EntityRelationship> {
        val relationships = mutableListOf<EntityRelationship>()
        
        entities.forEach { documentEntities ->
            // Find co-occurring entities
            for (i in documentEntities.indices) {
                for (j in i + 1 until documentEntities.size) {
                    val entity1 = documentEntities[i]
                    val entity2 = documentEntities[j]
                    
                    if (entity1.type != entity2.type) {
                        relationships.add(
                            EntityRelationship(
                                entity1 = entity1.text,
                                entity2 = entity2.text,
                                type1 = entity1.type,
                                type2 = entity2.type,
                                strength = calculateRelationshipStrength(entity1, entity2)
                            )
                        )
                    }
                }
            }
        }
        
        return relationships.groupBy { "${it.entity1}_${it.entity2}" }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(10)
            .map { entry ->
                val parts = entry.key.split("_")
                EntityRelationship(parts[0], parts[1], EntityType.MISC, EntityType.MISC, entry.value.toFloat())
            }
    }
    
    private fun calculateRelationshipStrength(entity1: EntityResult, entity2: EntityResult): Float {
        // Simple proximity-based relationship strength
        return (entity1.importance + entity2.importance) / 2f
    }
}

// Data classes for entity analysis
data class EntityFrequency(
    val text: String,
    val type: EntityType,
    val count: Int,
    val confidence: Float
)

data class EntityPatterns(
    val typeDistribution: Map<EntityType, Int>,
    val topEntities: List<EntityFrequency>,
    val relationships: List<EntityRelationship>,
    val averageEntitiesPerDocument: Float
)

data class EntityRelationship(
    val entity1: String,
    val entity2: String,
    val type1: EntityType,
    val type2: EntityType,
    val strength: Float
)