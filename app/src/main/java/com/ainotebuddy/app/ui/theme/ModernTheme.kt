package com.ainotebuddy.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ainotebuddy.app.ui.components.AccessibilityPreferences

// Enhanced theme state for modern UI/UX
@Stable
data class ModernThemeState(
    val isDarkTheme: Boolean = false,
    val useDynamicColor: Boolean = true,
    val accessibilityPrefs: AccessibilityPreferences = AccessibilityPreferences(),
    val useGlassmorphism: Boolean = true,
    val animationsEnabled: Boolean = true
)

@Composable
fun ModernAINoteBuddyTheme(
    themeState: ModernThemeState = ModernThemeState(),
    content: @Composable () -> Unit
) {
    val darkTheme = themeState.isDarkTheme || isSystemInDarkTheme()
    
    // Dynamic color is available on Android 12+
    val dynamicColor = themeState.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    val colorScheme = when {
        dynamicColor && darkTheme -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        dynamicColor && !darkTheme -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        themeState.accessibilityPrefs.highContrastMode && darkTheme -> {
            // High contrast dark theme
            darkColorScheme(
                primary = androidx.compose.ui.graphics.Color.White,
                onPrimary = androidx.compose.ui.graphics.Color.Black,
                primaryContainer = androidx.compose.ui.graphics.Color.Yellow,
                onPrimaryContainer = androidx.compose.ui.graphics.Color.Black,
                background = androidx.compose.ui.graphics.Color.Black,
                onBackground = androidx.compose.ui.graphics.Color.White,
                surface = androidx.compose.ui.graphics.Color.Black,
                onSurface = androidx.compose.ui.graphics.Color.White
            )
        }
        themeState.accessibilityPrefs.highContrastMode && !darkTheme -> {
            // High contrast light theme
            lightColorScheme(
                primary = androidx.compose.ui.graphics.Color.Black,
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = androidx.compose.ui.graphics.Color.Yellow,
                onPrimaryContainer = androidx.compose.ui.graphics.Color.Black,
                background = androidx.compose.ui.graphics.Color.White,
                onBackground = androidx.compose.ui.graphics.Color.Black,
                surface = androidx.compose.ui.graphics.Color.White,
                onSurface = androidx.compose.ui.graphics.Color.Black
            )
        }
        darkTheme -> ModernDarkColorScheme
        else -> ModernLightColorScheme
    }
    
    val typography = if (themeState.accessibilityPrefs.largeText) {
        // Scale up typography for accessibility
        ModernTypography.copy(
            displayLarge = ModernTypography.displayLarge.copy(fontSize = ModernTypography.displayLarge.fontSize * 1.2f),
            displayMedium = ModernTypography.displayMedium.copy(fontSize = ModernTypography.displayMedium.fontSize * 1.2f),
            displaySmall = ModernTypography.displaySmall.copy(fontSize = ModernTypography.displaySmall.fontSize * 1.2f),
            headlineLarge = ModernTypography.headlineLarge.copy(fontSize = ModernTypography.headlineLarge.fontSize * 1.2f),
            headlineMedium = ModernTypography.headlineMedium.copy(fontSize = ModernTypography.headlineMedium.fontSize * 1.2f),
            headlineSmall = ModernTypography.headlineSmall.copy(fontSize = ModernTypography.headlineSmall.fontSize * 1.2f),
            titleLarge = ModernTypography.titleLarge.copy(fontSize = ModernTypography.titleLarge.fontSize * 1.2f),
            titleMedium = ModernTypography.titleMedium.copy(fontSize = ModernTypography.titleMedium.fontSize * 1.2f),
            titleSmall = ModernTypography.titleSmall.copy(fontSize = ModernTypography.titleSmall.fontSize * 1.2f),
            bodyLarge = ModernTypography.bodyLarge.copy(fontSize = ModernTypography.bodyLarge.fontSize * 1.2f),
            bodyMedium = ModernTypography.bodyMedium.copy(fontSize = ModernTypography.bodyMedium.fontSize * 1.2f),
            bodySmall = ModernTypography.bodySmall.copy(fontSize = ModernTypography.bodySmall.fontSize * 1.2f),
            labelLarge = ModernTypography.labelLarge.copy(fontSize = ModernTypography.labelLarge.fontSize * 1.2f),
            labelMedium = ModernTypography.labelMedium.copy(fontSize = ModernTypography.labelMedium.fontSize * 1.2f),
            labelSmall = ModernTypography.labelSmall.copy(fontSize = ModernTypography.labelSmall.fontSize * 1.2f)
        )
    } else {
        ModernTypography
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    // Provide theme state through composition local
    CompositionLocalProvider(
        LocalModernThemeState provides themeState
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

// Composition local for theme state
val LocalModernThemeState = compositionLocalOf { ModernThemeState() }

// Theme extensions for easy access to modern features
@Composable
fun MaterialTheme.isGlassmorphismEnabled(): Boolean {
    return LocalModernThemeState.current.useGlassmorphism
}

@Composable
fun MaterialTheme.areAnimationsEnabled(): Boolean {
    val themeState = LocalModernThemeState.current
    return themeState.animationsEnabled && !themeState.accessibilityPrefs.reducedMotion
}

@Composable
fun MaterialTheme.accessibilityPrefs(): AccessibilityPreferences {
    return LocalModernThemeState.current.accessibilityPrefs
}

// Semantic color extensions
@Composable
fun MaterialTheme.semanticColors(): SemanticColors {
    return SemanticColors(
        success = ModernColors.Success,
        warning = ModernColors.Warning,
        error = colorScheme.error,
        info = ModernColors.Info,
        aiPrimary = ModernColors.AIPrimary,
        aiSecondary = ModernColors.AISecondary,
        aiAccent = ModernColors.AIAccent
    )
}

data class SemanticColors(
    val success: androidx.compose.ui.graphics.Color,
    val warning: androidx.compose.ui.graphics.Color,
    val error: androidx.compose.ui.graphics.Color,
    val info: androidx.compose.ui.graphics.Color,
    val aiPrimary: androidx.compose.ui.graphics.Color,
    val aiSecondary: androidx.compose.ui.graphics.Color,
    val aiAccent: androidx.compose.ui.graphics.Color
)

// Note type colors extension
@Composable
fun MaterialTheme.noteTypeColors(): NoteTypeColors {
    return NoteTypeColors(
        personal = ModernColors.NotePersonal,
        work = ModernColors.NoteWork,
        idea = ModernColors.NoteIdea,
        task = ModernColors.NoteTask,
        archive = ModernColors.NoteArchive
    )
}

data class NoteTypeColors(
    val personal: androidx.compose.ui.graphics.Color,
    val work: androidx.compose.ui.graphics.Color,
    val idea: androidx.compose.ui.graphics.Color,
    val task: androidx.compose.ui.graphics.Color,
    val archive: androidx.compose.ui.graphics.Color
)
