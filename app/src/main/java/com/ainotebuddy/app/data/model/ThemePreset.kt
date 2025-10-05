package com.ainotebuddy.app.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Data class representing a theme preset that can be exported/imported
 */
@Serializable
data class ThemePreset(
    val name: String,
    val description: String = "",
    val isDarkTheme: Boolean,
    val primaryColor: String,
    val secondaryColor: String,
    val tertiaryColor: String,
    val errorColor: String,
    val backgroundColor: String,
    val surfaceColor: String,
    val onPrimary: String,
    val onSecondary: String,
    val onTertiary: String,
    val onError: String,
    val onBackground: String,
    val onSurface: String,
    val version: Int = 1
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
    
    /**
     * Convert color string back to Color
     */
    fun colorFromString(colorString: String): Color {
        return Color(android.graphics.Color.parseColor(colorString))
    }
    
    /**
     * Convert Color to string representation
     */
    fun colorToString(color: Color): String {
        return String.format("#%08X", color.value.toLong() and 0xFFFFFFFF)
    }
}
