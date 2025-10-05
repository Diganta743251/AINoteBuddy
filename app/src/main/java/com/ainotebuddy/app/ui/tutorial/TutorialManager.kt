package com.ainotebuddy.app.ui.tutorial

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.ainotebuddy.app.data.TutorialPreferences
import com.ainotebuddy.app.data.TutorialPreferences.Companion.getInstance
import com.ainotebuddy.app.ui.components.TutorialStep
import com.ainotebuddy.app.ui.components.TutorialState
import com.ainotebuddy.app.ui.components.TutorialPosition
import com.ainotebuddy.app.ui.components.rememberTutorialState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * Manages tutorials for different features in the app
 */
class TutorialManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val tutorialState = TutorialState()
    
    // Define all tutorial steps
    private val onboardingTutorial = listOf(
        TutorialStep(
            title = "Welcome to AI NoteBuddy",
            description = "Let's take a quick tour of the app's main features.",
            targetId = "welcome",
            position = TutorialPosition.CENTER
        ),
        TutorialStep(
            title = "Navigation",
            description = "Swipe left or right to navigate between different sections of the app.",
            targetId = "bottom_navigation",
            position = TutorialPosition.BOTTOM
        ),
        TutorialStep(
            title = "Create Notes",
            description = "Tap the + button to create a new note. You can add text, images, and more!",
            targetId = "create_note",
            position = TutorialPosition.TOP
        ),
        TutorialStep(
            title = "Search",
            description = "Use the search bar to quickly find your notes by title or content.",
            targetId = "search",
            position = TutorialPosition.TOP
        ),
        TutorialStep(
            title = "Customize",
            description = "Tap the menu to access settings and customize your experience.",
            targetId = "menu",
            position = TutorialPosition.TOP
        )
    )
    
    private val noteTakingTutorial = listOf(
        TutorialStep(
            title = "Rich Text Editing",
            description = "Format your text with different styles, lists, and headings.",
            targetId = "editor_toolbar",
            position = TutorialPosition.TOP
        ),
        TutorialStep(
            title = "Attachments",
            description = "Add images, files, and links to your notes.",
            targetId = "attachment_button",
            position = TutorialPosition.BOTTOM
        ),
        TutorialStep(
            title = "AI Assistance",
            description = "Get smart suggestions and auto-formatting as you type.",
            targetId = "ai_assist",
            position = TutorialPosition.BOTTOM
        )
    )
    
    private val organizationTutorial = listOf(
        TutorialStep(
            title = "Tags & Categories",
            description = "Organize your notes with tags and categories for easy access.",
            targetId = "tags",
            position = TutorialPosition.RIGHT
        ),
        TutorialStep(
            title = "Smart Folders",
            description = "Create smart folders that automatically organize notes based on rules.",
            targetId = "smart_folders",
            position = TutorialPosition.LEFT
        )
    )
    
    private val collaborationTutorial = listOf(
        TutorialStep(
            title = "Real-time Collaboration",
            description = "Share notes and collaborate with others in real-time.",
            targetId = "collaborate_button",
            position = TutorialPosition.TOP
        ),
        TutorialStep(
            title = "Comments & Mentions",
            description = "Leave comments and mention team members for feedback.",
            targetId = "comments",
            position = TutorialPosition.RIGHT
        )
    )
    
    private val tutorialPreferences = getInstance(context)
    
    // Start a specific tutorial if not completed
    suspend fun startTutorialIfNeeded(type: TutorialType): Boolean {
        val tutorialId = "tutorial_" + type.name.lowercase()
        val completedTutorials = tutorialPreferences.completedTutorials.first()
        
        return if (completedTutorials.contains(tutorialId)) {
            false
        } else {
            val steps = when (type) {
                TutorialType.ONBOARDING -> onboardingTutorial
                TutorialType.NOTE_TAKING -> noteTakingTutorial
                TutorialType.ORGANIZATION -> organizationTutorial
                TutorialType.COLLABORATION -> collaborationTutorial
            }
            tutorialState.start(steps)
            true
        }
    }
    
    // Start a specific tutorial (force start)
    fun startTutorial(type: TutorialType) {
        val steps = when (type) {
            TutorialType.ONBOARDING -> onboardingTutorial
            TutorialType.NOTE_TAKING -> noteTakingTutorial
            TutorialType.ORGANIZATION -> organizationTutorial
            TutorialType.COLLABORATION -> collaborationTutorial
        }
        tutorialState.start(steps)
    }
    
    // Mark tutorial as completed
    fun completeTutorial(type: TutorialType) {
        coroutineScope.launch {
            tutorialPreferences.markTutorialCompleted("tutorial_" + type.name.lowercase())
        }
    }
    
    // Get the current tutorial state
    fun getTutorialState(): TutorialState = tutorialState
    
    companion object {
        @Composable
        fun rememberTutorialManager(
            coroutineScope: CoroutineScope = rememberCoroutineScope()
        ): TutorialManager {
            val context = LocalContext.current
            return remember { TutorialManager(context, coroutineScope) }
        }
    }
}

/**
 * Different types of tutorials available in the app
 */
enum class TutorialType {
    ONBOARDING,
    NOTE_TAKING,
    ORGANIZATION,
    COLLABORATION
}

/**
 * Helper function to get a preview of tutorial steps for previews
 */
@Composable
fun getPreviewTutorialSteps(): List<TutorialStep> {
    return listOf(
        TutorialStep(
            title = "Welcome",
            description = "This is a preview of what the tutorial will look like.",
            targetId = "preview",
            position = TutorialPosition.CENTER
        ),
        TutorialStep(
            title = "Feature Highlight",
            description = "Different elements will be highlighted as you go through the tutorial.",
            targetId = "preview_feature",
            position = TutorialPosition.BOTTOM
        )
    )
}
