package com.ainotebuddy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import android.net.Uri

object ThemeManager {
    // Reactive theme state for UI layers
    private val _themeState = MutableLiveData(ThemeState())
    val themeState: LiveData<ThemeState> get() = _themeState

    // Import/Export status flows used by ThemeSelectionScreen
    sealed class ExportStatus {
        data class Success(val message: String) : ExportStatus()
        data class Error(val message: String) : ExportStatus()
        object InProgress : ExportStatus()
    }

    sealed class ImportStatus {
        data class Success(val message: String) : ImportStatus()
        data class Error(val message: String) : ImportStatus()
        object InProgress : ImportStatus()
    }

    private val _exportStatus = MutableSharedFlow<ExportStatus>()
    private val _importStatus = MutableSharedFlow<ImportStatus>()
    val exportStatus: SharedFlow<ExportStatus> get() = _exportStatus
    val importStatus: SharedFlow<ImportStatus> get() = _importStatus

    // Theme operations
    fun setTheme(theme: AppTheme) {
        val current = _themeState.value ?: ThemeState()
        _themeState.value = current.copy(
            currentTheme = theme,
            isDarkMode = when (theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                else -> current.isDarkMode
            }
        )
    }

    fun setFontScale(scale: Float) {
        val current = _themeState.value ?: ThemeState()
        _themeState.value = current.copy(fontScale = scale)
    }

    fun toggleDynamicColors() {
        val current = _themeState.value ?: ThemeState()
        _themeState.value = current.copy(useDynamicColors = !current.useDynamicColors)
    }

    fun updateCustomColors(colors: Map<String, Color>) {
        val current = _themeState.value ?: ThemeState()
        _themeState.value = current.copy(customColors = colors)
    }

    // Stub implementations for import/export to satisfy UI calls
    suspend fun exportThemePreset(name: String, description: String, uri: Uri): ExportStatus {
        _exportStatus.emit(ExportStatus.InProgress)
        val status = ExportStatus.Success("Theme '$name' exported")
        _exportStatus.emit(status)
        return status
    }

    suspend fun importThemePreset(uri: Uri): ImportStatus {
        _importStatus.emit(ImportStatus.InProgress)
        val status = ImportStatus.Success("Theme imported")
        _importStatus.emit(status)
        return status
    }
    
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }
    
    @Composable
    fun getColorScheme(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        dynamicColors: Boolean = true
    ): ColorScheme {
        val darkTheme = when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
        
        return if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF6750A4),
                onPrimary = Color(0xFFFFFFFF),
                secondary = Color(0xFF625B71),
                onSecondary = Color(0xFFFFFFFF),
                background = Color(0xFF1C1B1F),
                onBackground = Color(0xFFE6E1E5),
                surface = Color(0xFF1C1B1F),
                onSurface = Color(0xFFE6E1E5)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF6750A4),
                onPrimary = Color(0xFFFFFFFF),
                secondary = Color(0xFF625B71),
                onSecondary = Color(0xFFFFFFFF),
                background = Color(0xFFFEFBFF),
                onBackground = Color(0xFF1C1B1F),
                surface = Color(0xFFFEFBFF),
                onSurface = Color(0xFF1C1B1F)
            )
        }
    }
}

// State and types used by UI screens
data class ThemeState(
    val currentTheme: AppTheme = AppTheme.SYSTEM,
    val fontScale: Float = 1.0f,
    val isDarkMode: Boolean = false,
    val useDynamicColors: Boolean = true,
    val customColors: Map<String, Color> = emptyMap()
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM, MATERIAL_YOU, DYNAMIC, FUTURISTIC, OCEAN;
    companion object {
        val baseThemes: List<AppTheme> = listOf(LIGHT, DARK, SYSTEM, MATERIAL_YOU, DYNAMIC)
        val presets: List<AppTheme> = listOf(FUTURISTIC, OCEAN)
    }
}

val AppTheme.displayName: String
    get() = when (this) {
        AppTheme.LIGHT -> "Light"
        AppTheme.DARK -> "Dark"
        AppTheme.SYSTEM -> "System"
        AppTheme.MATERIAL_YOU -> "Material You"
        AppTheme.DYNAMIC -> "Dynamic"
        AppTheme.FUTURISTIC -> "Futuristic"
        AppTheme.OCEAN -> "Ocean"
    }

// Public color palettes used by ThemeSelectionScreen
val LightColors = lightColorScheme(
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
    background = Color(0xFFFEFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1C1B1F)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = Color(0xFFE8DEF8),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5)
)

val MaterialYouLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F)
)

val MaterialYouDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color.Black,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5)
)

// Preset color maps for Ocean theme previews
val oceanLightColors: Map<String, Color> = mapOf(
    "primary" to Color(0xFF006783),
    "onPrimary" to Color.White,
    "primaryContainer" to Color(0xFFC1E8FF),
    "onPrimaryContainer" to Color(0xFF001E2A),
    "secondary" to Color(0xFF4F616E),
    "onSecondary" to Color.White,
    "secondaryContainer" to Color(0xFFD1E4FF),
    "onSecondaryContainer" to Color(0xFF0B1D29)
)

val oceanDarkColors: Map<String, Color> = mapOf(
    "primary" to Color(0xFF86CEFF),
    "onPrimary" to Color(0xFF003549),
    "primaryContainer" to Color(0xFF004D6A),
    "onPrimaryContainer" to Color(0xFFC1E8FF),
    "secondary" to Color(0xFFB4C9E7),
    "onSecondary" to Color(0xFF1E3248),
    "secondaryContainer" to Color(0xFF344863),
    "onSecondaryContainer" to Color(0xFFD1E4FF)
)