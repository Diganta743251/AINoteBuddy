package com.ainotebuddy.app.ui.dashboard.fab

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun CustomizableFAB(
    onActionClick: (FABActionType) -> Unit,
    onCustomizeClick: () -> Unit,
    onExpansionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fabManager = remember { FABConfigurationManager(context) }
    val scope = rememberCoroutineScope()
    
    var isExpanded by remember { mutableStateOf(false) }
    var enabledActions by remember { mutableStateOf(emptyList<FABActionConfig>()) }
    var menuConfig by remember { mutableStateOf(FABConfigurationManager.getDefaultFABMenuConfig()) }
    
    // Notify parent about expansion changes
    LaunchedEffect(isExpanded) {
        onExpansionChange(isExpanded)
    }
    
    // Load configuration
    LaunchedEffect(Unit) {
        fabManager.configFlow.collect { config ->
            menuConfig = config
            enabledActions = fabManager.getEnabledActions()
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Action buttons
        if (isExpanded) {
            when (menuConfig.style) {
                FABMenuStyle.LINEAR -> {
                    LinearFABMenu(
                        actions = enabledActions,
                        config = menuConfig,
                        onActionClick = { action ->
                            scope.launch {
                                fabManager.recordActionUsage(action)
                                onActionClick(action)
                                isExpanded = false
                            }
                        },
                        onCustomizeClick = {
                            onCustomizeClick()
                            isExpanded = false
                        }
                    )
                }
                FABMenuStyle.CIRCULAR -> {
                    CircularFABMenu(
                        actions = enabledActions,
                        config = menuConfig,
                        onActionClick = { action ->
                            scope.launch {
                                fabManager.recordActionUsage(action)
                                onActionClick(action)
                                isExpanded = false
                            }
                        },
                        onCustomizeClick = {
                            onCustomizeClick()
                            isExpanded = false
                        }
                    )
                }
                FABMenuStyle.GRID -> {
                    GridFABMenu(
                        actions = enabledActions,
                        config = menuConfig,
                        onActionClick = { action ->
                            scope.launch {
                                fabManager.recordActionUsage(action)
                                onActionClick(action)
                                isExpanded = false
                            }
                        },
                        onCustomizeClick = {
                            onCustomizeClick()
                            isExpanded = false
                        }
                    )
                }
                FABMenuStyle.MINIMAL -> {
                    MinimalFABMenu(
                        actions = enabledActions.take(3),
                        config = menuConfig,
                        onActionClick = { action ->
                            scope.launch {
                                fabManager.recordActionUsage(action)
                                onActionClick(action)
                                isExpanded = false
                            }
                        },
                        onCustomizeClick = {
                            onCustomizeClick()
                            isExpanded = false
                        }
                    )
                }
            }
        }
        
        // Main FAB
        MainFAB(
            isExpanded = isExpanded,
            onClick = { isExpanded = !isExpanded },
            config = menuConfig
        )
        
        // Backdrop for closing menu
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isExpanded = false }
                    .zIndex(-1f)
            )
        }
    }
}

@Composable
fun MainFAB(
    isExpanded: Boolean,
    onClick: () -> Unit,
    config: FABMenuConfig
) {
    val haptic = LocalHapticFeedback.current
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(config.animationDuration),
        label = "fab_rotation"
    )
    
    FloatingActionButton(
        onClick = {
            if (config.hapticFeedback) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        containerColor = Color(0xFF6A82FB),
        contentColor = Color.White,
        modifier = Modifier.rotate(rotation)
    ) {
        Icon(
            if (isExpanded) Icons.Filled.Close else Icons.Filled.Add,
            contentDescription = if (isExpanded) "Close menu" else "Open menu"
        )
    }
}

@Composable
fun LinearFABMenu(
    actions: List<FABActionConfig>,
    config: FABMenuConfig,
    onActionClick: (FABActionType) -> Unit,
    onCustomizeClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(bottom = 80.dp)
    ) {
        // Customize button
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(config.animationDuration, delayMillis = 50)
            ) + fadeIn(animationSpec = tween(config.animationDuration))
        ) {
            FABActionButton(
                icon = Icons.Filled.Tune,
                label = "Customize",
                color = Color(0xFF9C27B0),
                showLabel = config.showLabels,
                onClick = onCustomizeClick
            )
        }
        
        // Action buttons
        actions.forEachIndexed { index, action ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(
                        durationMillis = config.animationDuration,
                        delayMillis = (index + 1) * 50
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = config.animationDuration,
                        delayMillis = (index + 1) * 50
                    )
                )
            ) {
                FABActionButton(
                    icon = action.type.icon,
                    label = action.customLabel ?: action.type.displayName,
                    color = if (action.customColor != null) {
                        Color(android.graphics.Color.parseColor(action.customColor))
                    } else {
                        action.type.color
                    },
                    showLabel = config.showLabels,
                    onClick = { onActionClick(action.type) }
                )
            }
        }
    }
}

@Composable
fun CircularFABMenu(
    actions: List<FABActionConfig>,
    config: FABMenuConfig,
    onActionClick: (FABActionType) -> Unit,
    onCustomizeClick: () -> Unit
) {
    val radius = 120.dp
    val angleStep = 360f / (actions.size + 1) // +1 for customize button
    Box(
        modifier = Modifier.size(radius * 2),
        contentAlignment = Alignment.Center
    ) {
        // Customize button
        val customizeAngle = 0f
        val customizeX = (radius.value * cos(Math.toRadians(customizeAngle.toDouble()))).dp
        val customizeY = (radius.value * sin(Math.toRadians(customizeAngle.toDouble()))).dp
        
        Box(
            modifier = Modifier.offset(customizeX, customizeY)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = tween(config.animationDuration, delayMillis = 50)
                ) + fadeIn()
            ) {
                FABActionButton(
                    icon = Icons.Filled.Tune,
                    label = "Customize",
                    color = Color(0xFF9C27B0),
                    showLabel = config.showLabels,
                    onClick = onCustomizeClick,
                    size = 48.dp
                )
            }
        }
        
        // Action buttons in circle
        actions.forEachIndexed { index, action ->
            val angle = angleStep * (index + 1)
            val x = (radius.value * cos(Math.toRadians(angle.toDouble()))).dp
            val y = (radius.value * sin(Math.toRadians(angle.toDouble()))).dp
            
            Box(
                modifier = Modifier.offset(x, y)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        animationSpec = tween(
                            durationMillis = config.animationDuration,
                            delayMillis = (index + 1) * 50
                        )
                    ) + fadeIn(animationSpec = tween(config.animationDuration, delayMillis = (index + 1) * 50))
                ) {
                    FABActionButton(
                        icon = action.type.icon,
                        label = action.customLabel ?: action.type.displayName,
                        color = if (action.customColor != null) {
                            Color(android.graphics.Color.parseColor(action.customColor))
                        } else {
                            action.type.color
                        },
                        showLabel = config.showLabels,
                        onClick = { onActionClick(action.type) },
                        size = 48.dp
                    )
                }
            }
        }
    }
}

@Composable
fun GridFABMenu(
    actions: List<FABActionConfig>,
    config: FABMenuConfig,
    onActionClick: (FABActionType) -> Unit,
    onCustomizeClick: () -> Unit
) {
    val columns = 2
    val rows = ceil((actions.size + 1) / columns.toFloat()).toInt()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 80.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    
                    if (index == 0) {
                        // Customize button
                        AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(
                                animationSpec = tween(config.animationDuration, delayMillis = 50)
                            )
                        ) {
                            FABActionButton(
                                icon = Icons.Filled.Tune,
                                label = "Customize",
                                color = Color(0xFF9C27B0),
                                showLabel = config.showLabels,
                                onClick = onCustomizeClick,
                                size = 48.dp
                            )
                        }
                    } else if (index - 1 < actions.size) {
                        val action = actions[index - 1]
                        AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(
                                animationSpec = tween(
                                    durationMillis = config.animationDuration,
                                    delayMillis = index * 50
                                )
                            )
                        ) {
                            FABActionButton(
                                icon = action.type.icon,
                                label = action.customLabel ?: action.type.displayName,
                                color = if (action.customColor != null) {
                                    Color(android.graphics.Color.parseColor(action.customColor))
                                } else {
                                    action.type.color
                                },
                                showLabel = config.showLabels,
                                onClick = { onActionClick(action.type) },
                                size = 48.dp
                            )
                        }
                    } else {
                        // Empty space
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalFABMenu(
    actions: List<FABActionConfig>,
    config: FABMenuConfig,
    onActionClick: (FABActionType) -> Unit,
    onCustomizeClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 80.dp)
    ) {
        // Customize button
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(config.animationDuration, delayMillis = 50)
            )
        ) {
            FABActionButton(
                icon = Icons.Filled.Tune,
                label = "Customize",
                color = Color(0xFF9C27B0),
                showLabel = false,
                onClick = onCustomizeClick,
                size = 40.dp
            )
        }
        
        // Top 3 actions
        actions.take(3).forEachIndexed { index, action ->
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(
                        durationMillis = config.animationDuration,
                        delayMillis = (index + 1) * 50
                    )
                )
            ) {
                FABActionButton(
                    icon = action.type.icon,
                    label = action.customLabel ?: action.type.displayName,
                    color = if (action.customColor != null) {
                        Color(android.graphics.Color.parseColor(action.customColor))
                    } else {
                        action.type.color
                    },
                    showLabel = false,
                    onClick = { onActionClick(action.type) },
                    size = 40.dp
                )
            }
        }
    }
}

@Composable
fun FABActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    showLabel: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            containerColor = color,
            contentColor = Color.White,
            modifier = Modifier.size(size)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size((size.value * 0.4f).dp)
            )
        }
        
        if (showLabel) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}