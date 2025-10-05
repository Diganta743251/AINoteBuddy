package com.ainotebuddy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.ui.components.*
import com.ainotebuddy.app.ui.theme.ModernColors
import kotlinx.coroutines.delay

@Composable
fun ModernSplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(SplashPhase.LOGO_APPEAR) }
    
    LaunchedEffect(Unit) {
        delay(500)
        animationPhase = SplashPhase.LOGO_SCALE
        delay(800)
        animationPhase = SplashPhase.TEXT_APPEAR
        delay(1000)
        animationPhase = SplashPhase.AI_ANIMATION
        delay(1500)
        animationPhase = SplashPhase.COMPLETE
        delay(500)
        onSplashComplete()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background particles
        ParticleSystem(
            isActive = animationPhase >= SplashPhase.AI_ANIMATION,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Animation
            AnimatedVisibility(
                visible = animationPhase >= SplashPhase.LOGO_APPEAR,
                enter = fadeIn(tween(500)) + scaleIn(tween(500, easing = FastOutSlowInEasing))
            ) {
                val logoScale by animateFloatAsState(
                    targetValue = if (animationPhase >= SplashPhase.LOGO_SCALE) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "logoScale"
                )
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing background circle
                    if (animationPhase >= SplashPhase.AI_ANIMATION) {
                        BreathingAnimation {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        ModernColors.AIPrimary.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                    
                    // Main logo icon
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI NoteBuddy Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name Animation
            AnimatedVisibility(
                visible = animationPhase >= SplashPhase.TEXT_APPEAR,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AI NoteBuddy",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your Intelligent Note Companion",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // AI Loading Animation
            AnimatedVisibility(
                visible = animationPhase >= SplashPhase.AI_ANIMATION,
                enter = fadeIn(tween(400))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // AI thinking dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { index ->
                            PulsingDot(
                                color = ModernColors.AIPrimary,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Initializing AI...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Version info at bottom
        AnimatedVisibility(
            visible = animationPhase >= SplashPhase.TEXT_APPEAR,
            enter = fadeIn(tween(800, delayMillis = 400)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = "Version 2.0.0 • Modern UI Edition",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

private enum class SplashPhase {
    LOGO_APPEAR,
    LOGO_SCALE,
    TEXT_APPEAR,
    AI_ANIMATION,
    COMPLETE
}
