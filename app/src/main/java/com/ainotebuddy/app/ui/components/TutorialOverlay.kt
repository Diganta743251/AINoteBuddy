package com.ainotebuddy.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ainotebuddy.app.ui.theme.ModernColors
import com.ainotebuddy.app.ui.tutorial.TutorialManager
import com.ainotebuddy.app.ui.tutorial.TutorialType

/**
 * Data class representing a single step in the tutorial
 */
data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector = Icons.Default.Help,
    val targetId: String = "",
    val position: TutorialPosition = TutorialPosition.BOTTOM, // Position relative to target
    val shape: TutorialShape = TutorialShape.ROUNDED_RECT
)

/**
 * Position of the tooltip relative to the target
 */
enum class TutorialPosition {
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}

/**
 * Shape of the highlight overlay
 */
enum class TutorialShape {
    RECT, ROUNDED_RECT, CIRCLE
}

/**
 * State holder for the tutorial overlay
 */
class TutorialState {
    private var _currentStep by mutableStateOf(0)
    private var _isShowing by mutableStateOf(false)
    private var _steps = listOf<TutorialStep>()
    
    val currentStep: Int get() = _currentStep
    val isShowing: Boolean get() = _isShowing
    val steps: List<TutorialStep> get() = _steps
    
    fun start(steps: List<TutorialStep>) {
        _steps = steps
        _currentStep = 0
        _isShowing = true
    }
    
    fun next() {
        if (_currentStep < _steps.size - 1) {
            _currentStep++
        } else {
            _isShowing = false
        }
    }
    
    fun previous() {
        if (_currentStep > 0) {
            _currentStep--
        }
    }
    
    fun skip() {
        _isShowing = false
    }
    
    fun getCurrentStep(): TutorialStep? {
        return _steps.getOrNull(_currentStep)
    }
}

/**
 * Remember the tutorial state across recompositions
 */
@Composable
fun rememberTutorialState(): TutorialState {
    return remember { TutorialState() }
}

/**
 * Composable that shows a tutorial overlay with highlighted elements and tooltips
 * 
 * @param state The tutorial state
 * @param tutorialType The type of tutorial being shown
 * @param tutorialManager The tutorial manager for handling completion
 * @param modifier Modifier for the overlay
 * @param onDismiss Callback when the tutorial is dismissed
 * @param onComplete Callback when the tutorial is completed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialOverlay(
    state: TutorialState,
    tutorialType: TutorialType? = null,
    tutorialManager: TutorialManager? = null,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    // Handle tutorial completion
    val onTutorialComplete = {
        tutorialType?.let { type ->
            tutorialManager?.completeTutorial(type)
        }
        onComplete()
    }
    val currentStep = state.getCurrentStep() ?: return
    
    // Track the position of the target element
    var targetRect by remember { mutableStateOf(Rect.Zero) }
    
    // Calculate tooltip position based on target
    val tooltipOffset = with(LocalDensity.current) {
        val x = when (currentStep.position) {
            TutorialPosition.LEFT -> targetRect.left - 16.dp.toPx()
            TutorialPosition.RIGHT -> targetRect.right + 16.dp.toPx()
            else -> targetRect.center.x
        }
        val y = when (currentStep.position) {
            TutorialPosition.TOP -> targetRect.top - 16.dp.toPx()
            TutorialPosition.BOTTOM -> targetRect.bottom + 16.dp.toPx()
            else -> targetRect.center.y
        }
        x to y
    }
    
    // Show the overlay
    if (state.isShowing) {
        Dialog(
            onDismissRequest = { state.skip(); onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // Highlight the target element
                if (targetRect != Rect.Zero) {
                    val shape = when (currentStep.shape) {
                        TutorialShape.CIRCLE -> CircleShape
                        TutorialShape.ROUNDED_RECT -> RoundedCornerShape(8.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    
                    Box(
                        modifier = Modifier
                            .offset(
                                x = targetRect.left.dp,
                                y = targetRect.top.dp
                            )
                            .size(
                                width = targetRect.width.dp,
                                height = targetRect.height.dp
                            )
                            .clip(shape)
                            .border(
                                width = 2.dp,
                                color = ModernColors.AIPrimary,
                                shape = shape
                            )
                    )
                }
                
                // Tooltip
                Surface(
                    modifier = Modifier
                        .offset(
                            x = tooltipOffset.first.dp,
                            y = tooltipOffset.second.dp
                        )
                        .padding(16.dp)
                        .widthIn(min = 200.dp, max = 300.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Step indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${state.currentStep + 1}/${state.steps.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            IconButton(
                                onClick = { state.skip(); onDismiss() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Skip tutorial"
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Content
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            tint = ModernColors.AIPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = currentStep.title,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = currentStep.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Navigation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back button
                            if (state.currentStep > 0) {
                                TextButton(
                                    onClick = { state.previous() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Back")
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            
                            // Next/Done button
                            val buttonText = if (state.currentStep == state.steps.size - 1) "Done" else "Next"
                            Button(
                                onClick = {
                                    if (state.currentStep == state.steps.size - 1) {
                                        state.skip()
                                        onTutorialComplete()
                                    } else {
                                        state.next()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ModernColors.AIPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(buttonText)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to make an element targetable by the tutorial
 */
fun Modifier.tutorialTarget(
    tutorialState: TutorialState,
    stepId: String,
    onPositioned: (Rect) -> Unit = {}
): Modifier = this.then(
    Modifier.onGloballyPositioned { layoutCoordinates ->
        if (tutorialState.isShowing && tutorialState.getCurrentStep()?.targetId == stepId) {
            val rect = layoutCoordinates.boundsInRoot()
            onPositioned(rect)
        }
    }
)
