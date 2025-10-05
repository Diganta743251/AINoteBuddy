package com.ainotebuddy.app.ui.components.accessibility

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ainotebuddy.app.utils.TextToSpeechHelper
import com.ainotebuddy.app.utils.rememberTextToSpeechHelper

/**
 * A composable that displays text with read-aloud functionality
 * @param text The text to display and read aloud
 * @param modifier Modifier for the container
 * @param textStyle The style for the text
 * @param highlightColor The color to highlight the currently spoken word
 * @param highlightTextColor The text color for the highlighted word
 * @param showControls Whether to show the playback controls
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReadAloudText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    highlightColor: Color = MaterialTheme.colorScheme.primaryContainer,
    highlightTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    showControls: Boolean = true
) {
    val textToSpeech = rememberTextToSpeechHelper()
    val isSpeaking by textToSpeech.speakingState.collectAsState()
    
    Column(modifier = modifier) {
        // Display the text with highlighting
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        // Playback controls
        if (showControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button
                IconButton(
                    onClick = {
                        if (isSpeaking) {
                            textToSpeech.stop()
                        } else {
                            textToSpeech.speak(text)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    AnimatedContent(
                        targetState = isSpeaking,
                        label = "playPauseIcon"
                    ) { speaking ->
                        Icon(
                            imageVector = if (speaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (speaking) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Stop button
                if (isSpeaking) {
                    IconButton(
                        onClick = { textToSpeech.stop() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    // Clean up when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            textToSpeech.stop()
        }
    }
}

/**
 * A preview composable for ReadAloudText
 */
@Preview
@Composable
fun ReadAloudTextPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Read Aloud Demo",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ReadAloudText(
                    text = "This is a sample text that can be read aloud with word highlighting. " +
                            "Tap the play button to hear it read out loud.",
                    modifier = Modifier.fillMaxWidth(),
                    showControls = true
                )
            }
        }
    }
}
