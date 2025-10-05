package com.ainotebuddy.app.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

/**
 * Text scaling utilities for accessibility and user preferences
 */

class TextScaler(initialScale: Float = 1.0f) {
    private var scaleState: MutableState<Float> = mutableStateOf(initialScale)

    fun getFontScale(): Float = scaleState.value

    fun setFontScale(scale: Float) {
        scaleState.value = scale.coerceIn(0.8f, 2.0f)
    }

    fun increaseFontScale(step: Float = 0.1f) {
        setFontScale(getFontScale() + step)
    }

    fun decreaseFontScale(step: Float = 0.1f) {
        setFontScale(getFontScale() - step)
    }

    fun resetFontScale() {
        setFontScale(1.0f)
    }
}

@Composable
fun rememberTextScaler(initialScale: Float = 1.0f): TextScaler {
    val scaler = remember { TextScaler(initialScale) }
    return scaler
}

@Composable
fun ScalableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    scalePreference: Float = 1.0f,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val scaledStyle = style.copy(
        fontSize = style.fontSize * scalePreference,
        color = color
    )
    
    Text(
        text = text,
        modifier = modifier,
        style = scaledStyle
    )
}

@Composable
fun AdaptiveText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    baseStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    minFontSize: TextUnit = 8.sp,
    maxFontSize: TextUnit = 32.sp
) {
    val density = LocalDensity.current
    var currentFontSize by remember { mutableStateOf(baseStyle.fontSize) }
    
    Text(
        text = text,
        modifier = modifier,
        style = baseStyle.copy(fontSize = currentFontSize),
        maxLines = maxLines,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                val newSize = with(density) {
                    (currentFontSize.value * 0.9f).sp
                }
                if (newSize >= minFontSize) {
                    currentFontSize = newSize
                }
            }
        }
    )
}

@Composable
fun TextSizeSlider(
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0.8f..2.0f
) {
    Column(modifier = modifier) {
        Text(
            text = "Text Size: ${(currentScale * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium
        )
        
        Slider(
            value = currentScale,
            onValueChange = onScaleChange,
            valueRange = range,
            steps = 7,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Smaller",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Larger",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun TextScalingDemo(
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Text Scaling Preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ScalableText(
            text = "Heading Text",
            style = MaterialTheme.typography.headlineSmall,
            scalePreference = scale
        )
        
        ScalableText(
            text = "This is body text that shows how the scaling affects readability. " +
                    "The text should be clear and comfortable to read at any scale.",
            style = MaterialTheme.typography.bodyMedium,
            scalePreference = scale
        )
        
        ScalableText(
            text = "Small caption text",
            style = MaterialTheme.typography.bodySmall,
            scalePreference = scale
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { }) {
                ScalableText(
                    text = "Button",
                    style = MaterialTheme.typography.labelMedium,
                    scalePreference = scale,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            OutlinedButton(onClick = { }) {
                ScalableText(
                    text = "Outlined",
                    style = MaterialTheme.typography.labelMedium,
                    scalePreference = scale
                )
            }
            
            TextButton(onClick = { }) {
                ScalableText(
                    text = "Text",
                    style = MaterialTheme.typography.labelMedium,
                    scalePreference = scale
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ScalableText(
            text = "Sample Note Content",
            style = MaterialTheme.typography.titleSmall,
            scalePreference = scale,
            color = MaterialTheme.colorScheme.primary
        )
        
        ScalableText(
            text = "This is how your notes will appear with the selected text size. " +
                    "Make sure it's comfortable for you to read and write.",
            style = MaterialTheme.typography.bodyMedium,
            scalePreference = scale
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ScalableText(
            text = "• Bulleted list item one",
            style = MaterialTheme.typography.bodyMedium,
            scalePreference = scale
        )
        
        ScalableText(
            text = "• Bulleted list item two",
            style = MaterialTheme.typography.bodyMedium,
            scalePreference = scale
        )
        
        ScalableText(
            text = "• Bulleted list item three",
            style = MaterialTheme.typography.bodyMedium,
            scalePreference = scale
        )
    }
}

/**
 * Text scaling utilities for different screen sizes
 */
object TextScalingUtils {
    
    fun getScaleForScreenWidth(screenWidthDp: Dp): Float {
        return when {
            screenWidthDp < 360.dp -> 0.9f
            screenWidthDp > 800.dp -> 1.2f
            else -> 1.0f
        }
    }
    
    fun getOptimalFontSize(
        baseSize: TextUnit,
        screenDensity: Float,
        userPreference: Float = 1.0f
    ): TextUnit {
        val densityFactor = when {
            screenDensity < 1.5f -> 1.1f
            screenDensity > 3.0f -> 0.9f
            else -> 1.0f
        }
        
        return baseSize * densityFactor * userPreference
    }
    
    fun calculateLineHeight(fontSize: TextUnit, scale: Float): TextUnit {
        return fontSize * 1.4f * scale
    }
}

/**
 * Accessibility text scaling presets
 */
enum class TextSizePreset(val scale: Float, val displayName: String) {
    EXTRA_SMALL(0.8f, "Extra Small"),
    SMALL(0.9f, "Small"),
    NORMAL(1.0f, "Normal"),
    LARGE(1.2f, "Large"),
    EXTRA_LARGE(1.5f, "Extra Large"),
    ACCESSIBILITY(2.0f, "Accessibility")
}

@Composable
fun TextSizePresetSelector(
    currentPreset: TextSizePreset,
    onPresetSelected: (TextSizePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Text Size Preset",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextSizePreset.values().forEach { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentPreset == preset,
                    onClick = { onPresetSelected(preset) }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                ScalableText(
                    text = preset.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    scalePreference = preset.scale
                )
            }
        }
    }
}

/**
 * Dynamic font scaling based on content length
 */
@Composable
fun DynamicScaleText(
    text: String,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    maxScale: Float = 1.5f,
    minScale: Float = 0.8f
) {
    val scale = remember(text) {
        when {
            text.length < 50 -> maxScale
            text.length > 500 -> minScale
            else -> 1.0f - ((text.length - 50) / 450f) * (maxScale - minScale)
        }
    }
    
    ScalableText(
        text = text,
        modifier = modifier,
        style = baseStyle,
        scalePreference = scale
    )
}