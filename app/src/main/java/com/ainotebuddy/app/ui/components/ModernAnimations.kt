package com.ainotebuddy.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.ui.theme.ModernColors
import kotlin.math.sin

// Enhanced spring animations for modern feel
val modernSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

val quickSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium
)

// Modern slide transitions
fun slideInFromBottom() = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(300, easing = FastOutSlowInEasing)
)

fun slideOutToBottom() = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(300, easing = FastOutSlowInEasing)
)

fun slideInFromRight() = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = tween(300, easing = FastOutSlowInEasing)
)

fun slideOutToRight() = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(300, easing = FastOutSlowInEasing)
)

// Staggered list animations
@Composable
fun StaggeredAnimatedVisibility(
    visible: Boolean,
    index: Int,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 50,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 50,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(200)
        ) + slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(200)
        ),
        content = content
    )
}

// Floating action button with modern animations
@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    icon: @Composable () -> Unit,
    text: @Composable (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1.1f else 1f,
        animationSpec = modernSpring,
        label = "fabScale"
    )
    
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        expanded = expanded,
        icon = icon,
        text = text ?: {}
    )
}

// Shimmer loading effect
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
    )
}

// Pulsing dot indicator
@Composable
fun PulsingDot(
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = modifier
            .size(8.dp)
            .scale(scale)
            .background(color, CircleShape)
    )
}

// Breathing animation for AI elements
@Composable
fun BreathingAnimation(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    
    Box(
        modifier = Modifier.scale(scale)
    ) {
        content()
    }
}

// Morphing background for dynamic themes
@Composable
fun MorphingBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "morphing")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morphProgress"
    )
    
    val currentColors = remember(colors, animatedProgress) {
        val index = (animatedProgress * (colors.size - 1)).toInt()
        val nextIndex = (index + 1) % colors.size
        val localProgress = (animatedProgress * (colors.size - 1)) % 1f
        
        listOf(
            lerp(colors[index], colors[nextIndex], localProgress),
            lerp(colors[nextIndex], colors[(nextIndex + 1) % colors.size], localProgress)
        )
    }
    
    Box(
        modifier = modifier.background(
            Brush.linearGradient(currentColors)
        )
    )
}

// Particle system for celebrations
@Composable
fun ParticleSystem(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val particles = remember { List(20) { ParticleState() } }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            particles.forEach { it.reset() }
        }
    }
    
    if (isActive) {
        Box(modifier = modifier) {
            particles.forEachIndexed { index, particle ->
                val infiniteTransition = rememberInfiniteTransition(label = "particle$index")
                val progress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000 + index * 100, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "particleProgress$index"
                )
                
                val x = particle.startX + (particle.velocityX * progress)
                val y = particle.startY + (particle.velocityY * progress) + (0.5f * 500f * progress * progress)
                val alpha = 1f - progress
                
                Box(
                    modifier = Modifier
                        .offset(x.dp, y.dp)
                        .size(4.dp)
                        .background(
                            particle.color.copy(alpha = alpha),
                            CircleShape
                        )
                )
            }
        }
    }
}

private data class ParticleState(
    var startX: Float = 0f,
    var startY: Float = 0f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var color: Color = Color.Blue
) {
    fun reset() {
        startX = (-50..50).random().toFloat()
        startY = 0f
        velocityX = (-100..100).random().toFloat()
        velocityY = (-200..-100).random().toFloat()
        color = listOf(
            ModernColors.AIPrimary,
            ModernColors.AISecondary,
            ModernColors.Success,
            ModernColors.Warning
        ).random()
    }
}

// Wave animation for voice input
@Composable
fun VoiceWaveAnimation(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceWave")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = if (isActive) 20f else 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600 + index * 100,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100)
                ),
                label = "waveHeight$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// Color interpolation helper
private fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + fraction * (stop.red - start.red),
        green = start.green + fraction * (stop.green - start.green),
        blue = start.blue + fraction * (stop.blue - start.blue),
        alpha = start.alpha + fraction * (stop.alpha - start.alpha)
    )
}
