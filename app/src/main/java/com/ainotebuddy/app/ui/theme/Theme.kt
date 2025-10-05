package com.ainotebuddy.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// High Contrast Light Colors
private val HighContrastLightColors = lightColorScheme(
    primary = Color(0xFF0000FF), // Brighter, more saturated colors
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCCCFF),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFFFF0000), // High contrast red
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFCCCC),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF006400), // Dark green for high contrast
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFFCC),
    onTertiaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color.Black,
    error = Color(0xFFB00020),
    onError = Color.White,
    outline = Color.Black,
    outlineVariant = Color(0xFF444444)
)

// Light Colors (unique name to avoid collisions)
private val BaseLightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8),
    onPrimaryContainer = Color(0xFF1D1B20),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFCCE5FF),
    onSecondaryContainer = Color(0xFF001E2F),
    tertiary = Color(0xFF6750A4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21005D),
    background = Color(0xFFFEFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

// Dark Colors (unique name to avoid collisions)
private val BaseDarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = Color(0xFFE8DEF8),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D65),
    onSecondaryContainer = Color(0xFFCCE5FF),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// Material You Light Colors (unique name to avoid collisions)
private val BaseMaterialYouLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D1B20),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

// Material You Dark Colors (unique name to avoid collisions)
private val BaseMaterialYouDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// Futuristic accent colors
val FuturisticPrimary = Color(0xFF6A82FB)
val FuturisticSecondary = Color(0xFFFC5C7D)

/**
 * Main theme composable that applies the selected theme, colors, and typography
 */
@Composable
fun AINoteBuddyTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    highContrast: Boolean = false,
    customColors: Map<String, Any?> = emptyMap(),
    content: @Composable () -> Unit
) {
    // Ensure customColors is Map<String, Color>. If it's typed differently, coerce safely.
    fun toColor(value: Any?): Color? = when (value) {
        is Color -> value
        is Long -> Color(value)
        is Int -> Color(value)
        is String -> runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrNull()
        else -> null
    }

    val colorScheme = when {
        highContrast && darkTheme -> BaseDarkColors
        highContrast -> HighContrastLightColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        customColors.isNotEmpty() -> {
            // Apply custom color scheme from provided map, coerced to Map<String, Color>
            val colors: Map<String, Color> = customColors
                .mapValues { (_, v) -> toColor(v) }
                .filterValues { it != null }
                .mapValues { it.value!! }
            if (darkTheme) {
                darkColorScheme(
                    primary = colors["primary"] ?: BaseDarkColors.primary,
                    onPrimary = colors["onPrimary"] ?: BaseDarkColors.onPrimary,
                    primaryContainer = colors["primaryContainer"] ?: BaseDarkColors.primaryContainer,
                    onPrimaryContainer = colors["onPrimaryContainer"] ?: BaseDarkColors.onPrimaryContainer,
                    secondary = colors["secondary"] ?: BaseDarkColors.secondary,
                    onSecondary = colors["onSecondary"] ?: BaseDarkColors.onSecondary,
                    secondaryContainer = colors["secondaryContainer"] ?: BaseDarkColors.secondaryContainer,
                    onSecondaryContainer = colors["onSecondaryContainer"] ?: BaseDarkColors.onSecondaryContainer,
                    tertiary = colors["tertiary"] ?: BaseDarkColors.tertiary,
                    onTertiary = colors["onTertiary"] ?: BaseDarkColors.onTertiary,
                    background = colors["background"] ?: BaseDarkColors.background,
                    onBackground = colors["onBackground"] ?: BaseDarkColors.onBackground,
                    surface = colors["surface"] ?: BaseDarkColors.surface,
                    onSurface = colors["onSurface"] ?: BaseDarkColors.onSurface,
                    error = colors["error"] ?: BaseDarkColors.error,
                    onError = colors["onError"] ?: BaseDarkColors.onError
                )
            } else {
                lightColorScheme(
                    primary = colors["primary"] ?: BaseLightColors.primary,
                    onPrimary = colors["onPrimary"] ?: BaseLightColors.onPrimary,
                    primaryContainer = colors["primaryContainer"] ?: BaseLightColors.primaryContainer,
                    onPrimaryContainer = colors["onPrimaryContainer"] ?: BaseLightColors.onPrimaryContainer,
                    secondary = colors["secondary"] ?: BaseLightColors.secondary,
                    onSecondary = colors["onSecondary"] ?: BaseLightColors.onSecondary,
                    secondaryContainer = colors["secondaryContainer"] ?: BaseLightColors.secondaryContainer,
                    onSecondaryContainer = colors["onSecondaryContainer"] ?: BaseLightColors.onSecondaryContainer,
                    tertiary = colors["tertiary"] ?: BaseLightColors.tertiary,
                    onTertiary = colors["onTertiary"] ?: BaseLightColors.onTertiary,
                    background = colors["background"] ?: BaseLightColors.background,
                    onBackground = colors["onBackground"] ?: BaseLightColors.onBackground,
                    surface = colors["surface"] ?: BaseLightColors.surface,
                    onSurface = colors["onSurface"] ?: BaseLightColors.onSurface,
                    error = colors["error"] ?: BaseLightColors.error,
                    onError = colors["onError"] ?: BaseLightColors.onError
                )
            }
        }
        darkTheme -> BaseDarkColors
        else -> BaseLightColors
    }

    // Update window insets controller for edge-to-edge theming
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Apply the theme with animations
    AnimatedContent(
        targetState = colorScheme,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
            fadeOut(animationSpec = tween(100))
        },
        label = "ThemeAnimation"
    ) { currentColorScheme ->
        MaterialTheme(
            colorScheme = currentColorScheme,
            typography = ModernTypography,
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blur: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = shape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e).copy(alpha = 0.9f),
                        Color(0xFF16213e).copy(alpha = 0.9f),
                        Color(0xFF0f3460).copy(alpha = 0.9f),
                        Color(0xFF533483).copy(alpha = 0.9f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, offset * 0.5f),
                    end = androidx.compose.ui.geometry.Offset(offset * 0.5f, 1000f)
                )
            )
    )
} 