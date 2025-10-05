package com.ainotebuddy.app.ui.dashboard.presets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun PresetSuccessDialog(
    preset: DashboardPreset,
    onDismiss: () -> Unit,
    onCustomize: () -> Unit = {},
    onRatePreset: (Float) -> Unit = {}
) {
    var showContent by remember { mutableStateOf(false) }
    var showRating by remember { mutableStateOf(false) }
    var currentRating by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        delay(2000)
        showRating = true
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1a1a2e)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Animation
                AnimatedVisibility(
                    visible = showContent,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Success Icon with Animation
                        val scale by animateFloatAsState(
                            targetValue = if (showContent) 1f else 0f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "success_icon_scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(scale)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF4CAF50).copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        radius = 100f
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Preset Applied Successfully!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Your dashboard is now configured with the \"${preset.type.displayName}\" preset.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Configuration Summary
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(500, delayMillis = 300)
                    )
                ) {
                    PresetSummaryCard(preset = preset)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Rating Section
                AnimatedVisibility(
                    visible = showRating,
                    enter = fadeIn(animationSpec = tween(500)) + 
                           slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "How do you like this preset?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        StarRatingRow(
                            rating = currentRating,
                            onRatingChanged = { rating ->
                                currentRating = rating
                                onRatePreset(rating)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(500, delayMillis = 600)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCustomize,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                Color.White.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Customize")
                        }
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = preset.type.color,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetSummaryCard(preset: DashboardPreset) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    preset.type.icon,
                    contentDescription = null,
                    tint = preset.type.color,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = preset.type.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = preset.type.category.color
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    icon = Icons.Filled.Widgets,
                    label = "Widgets",
                    value = "${preset.widgetConfigs.count { it.isEnabled }}",
                    color = Color(0xFF6A82FB)
                )
                
                SummaryItem(
                    icon = Icons.Filled.TouchApp,
                    label = "Actions",
                    value = "${preset.fabConfig.actions.count { it.isEnabled }}",
                    color = Color(0xFFE91E63)
                )
                
                SummaryItem(
                    icon = Icons.Filled.Style,
                    label = "Style",
                    value = preset.fabConfig.style.name.take(3),
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun StarRatingRow(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    maxRating: Int = 5
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(maxRating) { index ->
            val starRating = index + 1f
            val isSelected = rating >= starRating
            val isHalfSelected = rating >= starRating - 0.5f && rating < starRating
            
            IconButton(
                onClick = { onRatingChanged(starRating) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isSelected) Icons.Filled.Star 
                    else if (isHalfSelected) Icons.Filled.StarHalf 
                    else Icons.Filled.StarBorder,
                    contentDescription = "Rate $starRating stars",
                    tint = if (isSelected || isHalfSelected) Color(0xFFFFD700) 
                           else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}