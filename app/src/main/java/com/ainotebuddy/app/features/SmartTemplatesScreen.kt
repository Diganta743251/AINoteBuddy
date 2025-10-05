package com.ainotebuddy.app.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

data class NoteTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: TemplateCategory,
    val content: String,
    val tags: List<String> = emptyList()
)

enum class TemplateCategory {
    WORK, PERSONAL, EDUCATION, CREATIVE, HEALTH, TRAVEL
}

object TemplateRepository {
    val templates = listOf(
        // Work Templates
        NoteTemplate(
            id = "meeting_notes",
            name = "Meeting Notes",
            description = "Structure for meeting notes with agenda and action items",
            icon = Icons.Filled.BusinessCenter,
            category = TemplateCategory.WORK,
            content = """# Meeting Notes - ${getCurrentDate()}

## Attendees
- 

## Agenda
1. 
2. 
3. 

## Discussion Points
- 

## Action Items
- [ ] 
- [ ] 
- [ ] 

## Next Meeting
Date: 
Time: 
Location: """,
            tags = listOf("meeting", "work", "agenda")
        ),
        
        NoteTemplate(
            id = "project_planning",
            name = "Project Planning",
            description = "Template for project planning and tracking",
            icon = Icons.Filled.Assignment,
            category = TemplateCategory.WORK,
            content = """# Project: [Project Name]

## Overview
**Start Date:** ${getCurrentDate()}
**End Date:** 
**Status:** Planning
**Priority:** 

## Objectives
- 
- 
- 

## Milestones
- [ ] Milestone 1 - 
- [ ] Milestone 2 - 
- [ ] Milestone 3 - 

## Resources Needed
- 
- 

## Risks & Mitigation
| Risk | Impact | Mitigation |
|------|--------|------------|
|      |        |            |

## Notes
""",
            tags = listOf("project", "planning", "work")
        ),
        
        // Personal Templates
        NoteTemplate(
            id = "daily_journal",
            name = "Daily Journal",
            description = "Daily reflection and planning template",
            icon = Icons.Filled.Book,
            category = TemplateCategory.PERSONAL,
            content = """# Daily Journal - ${getCurrentDate()}

## Morning Reflection
**Mood:** 
**Energy Level:** 
**Today's Focus:** 

## Goals for Today
- [ ] 
- [ ] 
- [ ] 

## Gratitude
1. 
2. 
3. 

## Evening Reflection
**What went well:** 

**What could be improved:** 

**Tomorrow's priority:** """,
            tags = listOf("journal", "daily", "reflection")
        ),
        
        NoteTemplate(
            id = "goal_setting",
            name = "Goal Setting",
            description = "SMART goals template",
            icon = Icons.Filled.Flag,
            category = TemplateCategory.PERSONAL,
            content = """# Goal: [Goal Name]

## SMART Criteria
**Specific:** 
**Measurable:** 
**Achievable:** 
**Relevant:** 
**Time-bound:** 

## Action Steps
- [ ] Step 1: 
- [ ] Step 2: 
- [ ] Step 3: 

## Resources Needed
- 
- 

## Potential Obstacles
- 
- 

## Success Metrics
- 
- 

## Review Date:** ${getDatePlusWeeks(4)}""",
            tags = listOf("goals", "planning", "smart")
        ),
        
        // Education Templates
        NoteTemplate(
            id = "lecture_notes",
            name = "Lecture Notes",
            description = "Structured template for lecture notes",
            icon = Icons.Filled.School,
            category = TemplateCategory.EDUCATION,
            content = """# Lecture Notes - ${getCurrentDate()}

**Course:** 
**Topic:** 
**Professor:** 
**Date:** ${getCurrentDate()}

## Key Concepts
- 
- 
- 

## Detailed Notes
### Section 1
- 

### Section 2
- 

## Questions
- 
- 

## Action Items
- [ ] Review chapter 
- [ ] Complete assignment 
- [ ] Prepare for next class

## Summary
""",
            tags = listOf("lecture", "education", "notes")
        ),
        
        NoteTemplate(
            id = "study_plan",
            name = "Study Plan",
            description = "Template for organizing study sessions",
            icon = Icons.Filled.MenuBook,
            category = TemplateCategory.EDUCATION,
            content = """# Study Plan - [Subject]

**Exam Date:** 
**Study Period:** ${getCurrentDate()} to 
**Total Study Hours:** 

## Topics to Cover
- [ ] Topic 1 - Estimated time: 
- [ ] Topic 2 - Estimated time: 
- [ ] Topic 3 - Estimated time: 

## Study Schedule
| Date | Time | Topic | Resources |
|------|------|-------|-----------|
|      |      |       |           |

## Resources
- Textbook: 
- Online: 
- Notes: 

## Practice Tests
- [ ] Practice test 1 - Date: 
- [ ] Practice test 2 - Date: 

## Review Sessions
- [ ] Final review - Date: """,
            tags = listOf("study", "education", "planning")
        ),
        
        // Creative Templates
        NoteTemplate(
            id = "creative_brief",
            name = "Creative Brief",
            description = "Template for creative projects",
            icon = Icons.Filled.Palette,
            category = TemplateCategory.CREATIVE,
            content = """# Creative Brief - [Project Name]

**Date:** ${getCurrentDate()}
**Client/Project:** 
**Deadline:** 

## Objective
What are we trying to achieve?

## Target Audience
Who are we creating this for?

## Key Message
What's the main message?

## Tone & Style
- Tone: 
- Style: 
- Colors: 
- Typography: 

## Deliverables
- [ ] 
- [ ] 
- [ ] 

## Inspiration & References
- 
- 

## Success Metrics
How will we measure success?

## Notes & Ideas
""",
            tags = listOf("creative", "brief", "design")
        ),
        
        // Health Templates
        NoteTemplate(
            id = "workout_log",
            name = "Workout Log",
            description = "Track your fitness routine",
            icon = Icons.Filled.FitnessCenter,
            category = TemplateCategory.HEALTH,
            content = """# Workout Log - ${getCurrentDate()}

**Workout Type:** 
**Duration:** 
**Location:** 

## Warm-up (5-10 min)
- 

## Main Workout
### Exercise 1: 
- Sets: 
- Reps: 
- Weight: 
- Notes: 

### Exercise 2: 
- Sets: 
- Reps: 
- Weight: 
- Notes: 

### Exercise 3: 
- Sets: 
- Reps: 
- Weight: 
- Notes: 

## Cool-down (5-10 min)
- 

## Overall Rating: /10
**Energy Level:** 
**Difficulty:** 
**Notes:** """,
            tags = listOf("workout", "fitness", "health")
        ),
        
        // Travel Templates
        NoteTemplate(
            id = "travel_itinerary",
            name = "Travel Itinerary",
            description = "Plan your trip details",
            icon = Icons.Filled.Flight,
            category = TemplateCategory.TRAVEL,
            content = """# Travel Itinerary - [Destination]

**Travel Dates:** ${getCurrentDate()} to 
**Travelers:** 
**Budget:** 

## Flight Details
**Departure:** 
- Date: 
- Time: 
- Flight: 
- Confirmation: 

**Return:** 
- Date: 
- Time: 
- Flight: 
- Confirmation: 

## Accommodation
**Hotel:** 
**Address:** 
**Check-in:** 
**Check-out:** 
**Confirmation:** 

## Daily Itinerary
### Day 1 - ${getCurrentDate()}
- Morning: 
- Afternoon: 
- Evening: 

### Day 2
- Morning: 
- Afternoon: 
- Evening: 

## Packing List
- [ ] 
- [ ] 
- [ ] 

## Important Contacts
- Hotel: 
- Emergency: 
- Local contact: 

## Notes & Tips
""",
            tags = listOf("travel", "itinerary", "planning")
        )
    )
    
    fun getTemplatesByCategory(category: TemplateCategory): List<NoteTemplate> {
        return templates.filter { it.category == category }
    }
    
    fun searchTemplates(query: String): List<NoteTemplate> {
        return templates.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.description.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTemplatesScreen(
    onBack: () -> Unit,
    onTemplateSelected: (NoteTemplate) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val displayedTemplates = when {
        searchQuery.isNotBlank() -> TemplateRepository.searchTemplates(searchQuery)
        selectedCategory != null -> TemplateRepository.getTemplatesByCategory(selectedCategory!!)
        else -> TemplateRepository.templates
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Smart Templates") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search templates") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category Filter
            if (searchQuery.isBlank()) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(TemplateCategory.values()) { category ->
                        CategoryCard(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = { 
                                selectedCategory = if (selectedCategory == category) null else category
                            }
                        )
                    }
                }
            }
            
            // Templates List
            Text(
                text = when {
                    searchQuery.isNotBlank() -> "Search Results (${displayedTemplates.size})"
                    selectedCategory != null -> "${selectedCategory!!.name} Templates"
                    else -> "All Templates"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedTemplates) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onTemplateSelected(template) }
                    )
                }
                
                if (displayedTemplates.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No templates found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: TemplateCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (category) {
        TemplateCategory.WORK -> Icons.Filled.BusinessCenter
        TemplateCategory.PERSONAL -> Icons.Filled.Person
        TemplateCategory.EDUCATION -> Icons.Filled.School
        TemplateCategory.CREATIVE -> Icons.Filled.Palette
        TemplateCategory.HEALTH -> Icons.Filled.FitnessCenter
        TemplateCategory.TRAVEL -> Icons.Filled.Flight
    }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TemplateCard(
    template: NoteTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                template.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (template.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        template.tags.take(3).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
            
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions
private fun getCurrentDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

private fun getDatePlusWeeks(weeks: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.WEEK_OF_YEAR, weeks)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}

// Extension to create note from template
fun NoteTemplate.toNoteEntity(): NoteEntity {
    return NoteEntity(
        title = this.name,
        content = this.content,
        tags = this.tags.joinToString(","),
        category = this.category.name,
        format = "markdown"
    )
}