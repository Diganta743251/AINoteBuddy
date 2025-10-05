package com.ainotebuddy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.ui.theme.ModernColors

// Accessibility preferences
data class AccessibilityPreferences(
    val highContrastMode: Boolean = false,
    val reducedMotion: Boolean = false,
    val largeText: Boolean = false,
    val screenReaderOptimized: Boolean = false,
    val hapticFeedback: Boolean = true,
    val voiceNavigation: Boolean = false
)

@Composable
fun AccessibilityAwareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    accessibilityPrefs: AccessibilityPreferences = AccessibilityPreferences()
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    val buttonColors = if (accessibilityPrefs.highContrastMode) {
        ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.White
        )
    } else {
        ButtonDefaults.buttonColors()
    }
    
    val focusBorderColor = if (accessibilityPrefs.highContrastMode) {
        Color.Yellow
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = buttonColors,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = 3.dp,
                        color = focusBorderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                role = Role.Button
                if (accessibilityPrefs.screenReaderOptimized) {
                    this.contentDescription = "$text button${if (!enabled) ", disabled" else ""}"
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(
                        if (accessibilityPrefs.largeText) 24.dp else 18.dp
                    )
                )
            }
            Text(
                text = text,
                fontSize = if (accessibilityPrefs.largeText) 18.sp else 14.sp,
                fontWeight = if (accessibilityPrefs.highContrastMode) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun AccessibilityAwareCard(
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accessibilityPrefs: AccessibilityPreferences = AccessibilityPreferences()
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    val cardColors = if (accessibilityPrefs.highContrastMode) {
        CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    } else {
        CardDefaults.cardColors()
    }
    
    val focusBorderColor = if (accessibilityPrefs.highContrastMode) {
        Color.Blue
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = 2.dp,
                        color = focusBorderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .semantics {
                contentDescription = title
                if (onClick != null) {
                    role = Role.Button
                }
            },
        colors = cardColors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (accessibilityPrefs.reducedMotion) 2.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = if (accessibilityPrefs.largeText) 20.sp else 16.sp,
                fontWeight = if (accessibilityPrefs.highContrastMode) FontWeight.Bold else FontWeight.Medium,
                color = if (accessibilityPrefs.highContrastMode) Color.Black else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

@Composable
fun HighContrastToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = enabled,
                onClick = { onToggle(!enabled) }
            )
            .padding(16.dp)
            .semantics {
                role = Role.Switch
                contentDescription = if (enabled) "High contrast mode enabled" else "High contrast mode disabled"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "High Contrast Mode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Improves visibility with stronger color contrast",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (enabled) Color.Yellow else MaterialTheme.colorScheme.primary,
                checkedTrackColor = if (enabled) Color.Black else MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun ReducedMotionToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = enabled,
                onClick = { onToggle(!enabled) }
            )
            .padding(16.dp)
            .semantics {
                role = Role.Switch
                contentDescription = if (enabled) "Reduced motion enabled" else "Reduced motion disabled"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Reduce Motion",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Minimizes animations and transitions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
fun LargeTextToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = enabled,
                onClick = { onToggle(!enabled) }
            )
            .padding(16.dp)
            .semantics {
                role = Role.Switch
                contentDescription = if (enabled) "Large text enabled" else "Large text disabled"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Large Text",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontSize = if (enabled) 18.sp else 16.sp
            )
            Text(
                text = "Increases text size throughout the app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (enabled) 14.sp else 12.sp
            )
        }
        
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
fun AccessibilitySettingsScreen(
    preferences: AccessibilityPreferences,
    onPreferencesChange: (AccessibilityPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Accessibility Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card {
            Column {
                HighContrastToggle(
                    enabled = preferences.highContrastMode,
                    onToggle = { onPreferencesChange(preferences.copy(highContrastMode = it)) }
                )
                
                Divider()
                
                ReducedMotionToggle(
                    enabled = preferences.reducedMotion,
                    onToggle = { onPreferencesChange(preferences.copy(reducedMotion = it)) }
                )
                
                Divider()
                
                LargeTextToggle(
                    enabled = preferences.largeText,
                    onToggle = { onPreferencesChange(preferences.copy(largeText = it)) }
                )
                
                Divider()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = preferences.hapticFeedback,
                            onClick = { 
                                onPreferencesChange(preferences.copy(hapticFeedback = !preferences.hapticFeedback))
                            }
                        )
                        .padding(16.dp)
                        .semantics {
                            role = Role.Switch
                            contentDescription = if (preferences.hapticFeedback) "Haptic feedback enabled" else "Haptic feedback disabled"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Vibration feedback for interactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = preferences.hapticFeedback,
                        onCheckedChange = { 
                            onPreferencesChange(preferences.copy(hapticFeedback = it))
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Voice & Navigation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = preferences.voiceNavigation,
                        onClick = { 
                            onPreferencesChange(preferences.copy(voiceNavigation = !preferences.voiceNavigation))
                        }
                    )
                    .padding(16.dp)
                    .semantics {
                        role = Role.Switch
                        contentDescription = if (preferences.voiceNavigation) "Voice navigation enabled" else "Voice navigation disabled"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Voice Navigation",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Navigate using voice commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = preferences.voiceNavigation,
                    onCheckedChange = { 
                        onPreferencesChange(preferences.copy(voiceNavigation = it))
                    }
                )
            }
        }
    }
}
