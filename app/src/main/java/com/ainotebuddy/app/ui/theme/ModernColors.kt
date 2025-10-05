package com.ainotebuddy.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Enhanced color palette for 2025 design standards
object ModernColors {
    
    // AI-themed colors
    val AIPrimary = Color(0xFF4285F4)
    val AISecondary = Color(0xFF34A853)
    val AIAccent = Color(0xFFEA4335)
    val AIWarning = Color(0xFFFBBC04)
    
    // Semantic colors for better UX
    val Success = Color(0xFF00C851)
    val Warning = Color(0xFFFF8800)
    val Error = Color(0xFFFF4444)
    val Info = Color(0xFF33B5E5)
    
    // Surface variations for depth
    val SurfaceElevated = Color(0xFFF8F9FA)
    val SurfaceContainer = Color(0xFFF1F3F4)
    val SurfaceContainerHigh = Color(0xFFE8EAED)
    val SurfaceContainerHighest = Color(0xFFDEE1E6)
    
    // Dark surface variations
    val DarkSurfaceElevated = Color(0xFF2D2D30)
    val DarkSurfaceContainer = Color(0xFF252526)
    val DarkSurfaceContainerHigh = Color(0xFF1E1E1E)
    val DarkSurfaceContainerHighest = Color(0xFF181818)
    
    // Accent colors for different note types
    val NotePersonal = Color(0xFF9C27B0)
    val NoteWork = Color(0xFF2196F3)
    val NoteIdea = Color(0xFFFF9800)
    val NoteTask = Color(0xFF4CAF50)
    val NoteArchive = Color(0xFF607D8B)
    
    // Collaboration colors
    val CollabUser1 = Color(0xFF1976D2)
    val CollabUser2 = Color(0xFF388E3C)
    val CollabUser3 = Color(0xFFE64A19)
    val CollabUser4 = Color(0xFF7B1FA2)
    val CollabUser5 = Color(0xFF00796B)
    
    // Glassmorphism colors
    val GlassLight = Color(0x1AFFFFFF)
    val GlassDark = Color(0x1A000000)
    val GlassBorder = Color(0x33FFFFFF)
    val GlassBorderDark = Color(0x33000000)
}

// Enhanced color schemes with better accessibility
val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    
    secondary = Color(0xFF388E3C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E8),
    onSecondaryContainer = Color(0xFF1B5E20),
    
    tertiary = Color(0xFF7B1FA2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF4A148C),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1A1C1E),
    
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE2E2E5),
    onSurfaceVariant = Color(0xFF44474E),
    
    surfaceContainer = ModernColors.SurfaceContainer,
    surfaceContainerHigh = ModernColors.SurfaceContainerHigh,
    surfaceContainerHighest = ModernColors.SurfaceContainerHighest,
    
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7CF),
    
    error = ModernColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFE8F5E8),
    
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color(0xFF4A148C),
    tertiaryContainer = Color(0xFF6A1B9A),
    onTertiaryContainer = Color(0xFFF3E5F5),
    
    background = Color(0xFF101214),
    onBackground = Color(0xFFE2E2E5),
    
    surface = Color(0xFF101214),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C7CF),
    
    surfaceContainer = ModernColors.DarkSurfaceContainer,
    surfaceContainerHigh = ModernColors.DarkSurfaceContainerHigh,
    surfaceContainerHighest = ModernColors.DarkSurfaceContainerHighest,
    
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

// Semantic color extensions
@Composable
fun ColorScheme.success(): Color = ModernColors.Success

@Composable
fun ColorScheme.warning(): Color = ModernColors.Warning

@Composable
fun ColorScheme.info(): Color = ModernColors.Info

@Composable
fun ColorScheme.aiPrimary(): Color = ModernColors.AIPrimary

@Composable
fun ColorScheme.aiSecondary(): Color = ModernColors.AISecondary

@Composable
fun ColorScheme.aiAccent(): Color = ModernColors.AIAccent

// Note type colors
@Composable
fun ColorScheme.notePersonal(): Color = ModernColors.NotePersonal

@Composable
fun ColorScheme.noteWork(): Color = ModernColors.NoteWork

@Composable
fun ColorScheme.noteIdea(): Color = ModernColors.NoteIdea

@Composable
fun ColorScheme.noteTask(): Color = ModernColors.NoteTask

@Composable
fun ColorScheme.noteArchive(): Color = ModernColors.NoteArchive
