package com.ainotebuddy.app.ui.components.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "${uiState.completedTasks}/${uiState.totalTasks}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            )
        },
        bottomBar = {
            AddTaskInput(
                onAddTask = { title ->
                    viewModel.addTask(title)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            } else {
                AnimatedVisibility(
                    visible = !uiState.isLoading,
                    enter = fadeIn() + expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut() + shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.tasks,
                            key = { _, task -> task.id }
                        ) { index, task ->
                            TaskItem(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                            )

                            // Only show divider if not the last item
                            if (index < uiState.tasks.lastIndex) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    )
                                ) {
                                    Divider(
                                        modifier = Modifier.padding(start = 48.dp, end = 16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                    
                    // Animated empty state
                    AnimatedVisibility(
                        visible = uiState.tasks.isEmpty(),
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f)
                    ) {
                        val kb = LocalSoftwareKeyboardController.current
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { 
                                        // Dismiss keyboard when tapping empty space
                                        kb?.hide()
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.no_tasks_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.alpha(0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_tasks_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Show error message if any
            uiState.error?.let { error ->
                var showError by remember { mutableStateOf(true) }
                
                if (showError) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            IconButton(onClick = { showError = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    ) {
                        Text(text = error)
                    }
                    
                    // Auto-dismiss after 3 seconds
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(3000)
                        showError = false
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskItem(
    task: com.ainotebuddy.app.data.model.Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "taskItemScale"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onToggleComplete() }
                )
            },
        tonalElevation = if (isPressed) 0.dp else 1.dp,
        shadowElevation = if (isPressed) 0.dp else 1.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .animateContentSize()
        ) {
            // Animated checkbox
            val transition = updateTransition(
                targetState = task.isCompleted,
                label = "checkboxTransition"
            )
            
            val borderColor by transition.animateColor(label = "borderColor") { isCompleted ->
                if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            }
            
            val backgroundColor by transition.animateColor(label = "backgroundColor") { isCompleted ->
                if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            }
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToggleComplete() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val checkmarkScale by transition.animateFloat(
                    transitionSpec = { tween(durationMillis = 200) },
                    label = "checkmarkScale"
                ) { isCompleted ->
                    if (isCompleted) 1f else 0f
                }
                
                if (checkmarkScale > 0.1f) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.mark_task_incomplete),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .scale(checkmarkScale)
                    )
                }
            }
            
            // Task title with animation
            val checkmarkAlpha by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 200) },
                label = "checkmarkAlpha"
            ) { isCompleted ->
                if (isCompleted) 0.7f else 1f
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = checkmarkAlpha
                    }
            )
            
            // Delete button with animation
            var showDeleteButton by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                showDeleteButton = true
            }
            
            AnimatedVisibility(
                visible = showDeleteButton,
                enter = fadeIn(animationSpec = tween(200, delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.delete_task),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AddTaskInput(
    onAddTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Plus icon
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        // Text field
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Add a task") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onAddTask(text)
                        text = ""
                    }
                    keyboardController?.hide()
                }
            ),
            colors = TextFieldDefaults.colors(),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        
        // Clear text button
        AnimatedVisibility(
            visible = text.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = { text = "" },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear text",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Request focus when this composable is first composed
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
