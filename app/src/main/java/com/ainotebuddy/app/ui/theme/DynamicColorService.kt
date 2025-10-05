package com.ainotebuddy.app.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Service for handling dynamic color theming based on the device's wallpaper
 */
class DynamicColorService(private val context: Context) {

    /**
     * Extract dominant colors from the wallpaper
     */
    suspend fun extractWallpaperColors(): WallpaperColors? = withContext(Dispatchers.IO) {
        return@withContext try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperDrawable = wallpaperManager.drawable as? android.graphics.drawable.BitmapDrawable
            val originalBitmap = wallpaperDrawable?.bitmap ?: return@withContext null
            val bitmap = Bitmap.createScaledBitmap(originalBitmap, 128, 128, false)
            
            val palette = Palette.from(bitmap).generate()
            
            // Extract prominent colors from the palette
            val dominantSwatch = palette.dominantSwatch
            val mutedSwatch = palette.mutedSwatch
            val vibrantSwatch = palette.vibrantSwatch
            val lightVibrantSwatch = palette.lightVibrantSwatch
            val darkVibrantSwatch = palette.darkVibrantSwatch
            
            WallpaperColors(
                primary = dominantSwatch?.rgb?.toComposeColor() ?: ComposeColor(0xFF6750A4),
                onPrimary = dominantSwatch?.bodyTextColor?.toComposeColor() ?: ComposeColor.White,
                primaryContainer = vibrantSwatch?.rgb?.toComposeColor() ?: ComposeColor(0xFFEADDFF),
                onPrimaryContainer = vibrantSwatch?.titleTextColor?.toComposeColor() ?: ComposeColor(0xFF21005D),
                secondary = mutedSwatch?.rgb?.toComposeColor() ?: ComposeColor(0xFF625B71),
                onSecondary = mutedSwatch?.bodyTextColor?.toComposeColor() ?: ComposeColor.White,
                secondaryContainer = lightVibrantSwatch?.rgb?.toComposeColor() ?: ComposeColor(0xFFE8DEF8),
                onSecondaryContainer = lightVibrantSwatch?.titleTextColor?.toComposeColor() ?: ComposeColor(0xFF1D1B20),
                tertiary = darkVibrantSwatch?.rgb?.toComposeColor() ?: ComposeColor(0xFF7D5260),
                onTertiary = darkVibrantSwatch?.bodyTextColor?.toComposeColor() ?: ComposeColor.White,
                background = ComposeColor(0xFFFFFBFE),
                onBackground = ComposeColor(0xFF1C1B1F),
                surface = ComposeColor(0xFFFFFBFE),
                onSurface = ComposeColor(0xFF1C1B1F)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate a dynamic color scheme based on the wallpaper
     */
    suspend fun generateDynamicColorScheme(isDark: Boolean): androidx.compose.material3.ColorScheme {
        return if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }
    
    /**
     * Check if dynamic color is available on the current device
     */
    fun isDynamicColorAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    }
    
    private fun Int.toComposeColor(): ComposeColor = ComposeColor(this)
    
    private fun Int.isLightColor(): Boolean = ColorUtils.calculateLuminance(this) > 0.5
    
    private fun Int.toReadableTextColor(): ComposeColor = if (isLightColor()) ComposeColor.Black else ComposeColor.White
}

/**
 * Data class to hold extracted wallpaper colors
 */
data class WallpaperColors(
    val primary: ComposeColor,
    val onPrimary: ComposeColor,
    val primaryContainer: ComposeColor,
    val onPrimaryContainer: ComposeColor,
    val secondary: ComposeColor,
    val onSecondary: ComposeColor,
    val secondaryContainer: ComposeColor,
    val onSecondaryContainer: ComposeColor,
    val tertiary: ComposeColor,
    val onTertiary: ComposeColor,
    val background: ComposeColor,
    val onBackground: ComposeColor,
    val surface: ComposeColor,
    val onSurface: ComposeColor
)

/**
 * Composable function to remember the DynamicColorService
 */
@Composable
fun rememberDynamicColorService(): DynamicColorService {
    val context = LocalContext.current
    return remember { DynamicColorService(context) }
}
