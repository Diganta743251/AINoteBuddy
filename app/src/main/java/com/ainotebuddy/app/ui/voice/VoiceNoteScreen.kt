package com.ainotebuddy.app.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ainotebuddy.app.voice.*
import com.ainotebuddy.app.ui.theme.*

/**
 * Voice Note Screen with AI-enhanced recording and real-time transcription
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNoteScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceNoteViewModel = hiltViewModel()
) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val realTimeTranscription by viewModel.realTimeTranscription.collectAsStateWithLifecycle()
    val voiceInsights by viewModel.voiceInsights.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Voice Note",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Voice settings
                    IconButton(onClick = { viewModel.showVoiceSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Voice Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Voice State Indicator
            VoiceStateIndicator(
                voiceState = voiceState,
                recordingDuration = recordingDuration,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Real-time Transcription
            RealTimeTranscriptionCard(
                transcription = realTimeTranscription,
                voiceState = voiceState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
            )
            
            // AI Insights Panel
            if (voiceInsights.isNotEmpty()) {
                AIInsightsPanel(
                    insights = voiceInsights,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            
            // Voice Controls
            VoiceControlsPanel(
                voiceState = voiceState,
                onStartRecording = { viewModel.startVoiceNote() },
                onStopRecording = { viewModel.stopVoiceNote() },
                onPauseRecording = { viewModel.pauseVoiceNote() },
                onResumeRecording = { viewModel.resumeVoiceNote() },
                onCancelRecording = { viewModel.cancelVoiceNote() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Voice state indicator with animated visual feedback
 */
@Composable
private fun VoiceStateIndicator(
    voiceState: VoiceState,
    recordingDuration: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // Animated voice visualization
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    when (voiceState) {
                        is VoiceState.Recording -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        is VoiceState.Processing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        is VoiceState.Listening -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .border(
                    width = 3.dp,
                    color = when (voiceState) {
                        is VoiceState.Recording -> MaterialTheme.colorScheme.error
                        is VoiceState.Processing -> MaterialTheme.colorScheme.primary
                        is VoiceState.Listening -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            
            // Pulsing animation for recording
            if (voiceState is VoiceState.Recording) {
                val scale = remember { mutableFloatStateOf(1f) }
                // Simple manual pulse without animation APIs to avoid missing imports
                LaunchedEffect(Unit) {
                    while (true) {
                        scale.floatValue = 1.1f
                        kotlinx.coroutines.delay(500)
                        scale.floatValue = 1f
                        kotlinx.coroutines.delay(500)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale.floatValue)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
            
            // State icon
            Icon(
                imageVector = when (voiceState) {
                    is VoiceState.Recording -> Icons.Default.Mic
                    is VoiceState.Processing -> Icons.Default.Psychology
                    is VoiceState.Listening -> Icons.Default.Hearing
                    is VoiceState.Error -> Icons.Default.Error
                    else -> Icons.Default.MicNone
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = when (voiceState) {
                    is VoiceState.Recording -> Color.White
                    is VoiceState.Processing -> MaterialTheme.colorScheme.primary
                    is VoiceState.Listening -> MaterialTheme.colorScheme.secondary
                    is VoiceState.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // State text and duration
        Text(
            text = when (voiceState) {
                is VoiceState.Recording -> "Recording..."
                is VoiceState.Processing -> "Processing with AI..."
                is VoiceState.Listening -> "Listening for commands..."
                is VoiceState.Error -> "Error: ${voiceState.message}"
                else -> "Ready to record"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when (voiceState) {
                is VoiceState.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        
        if (voiceState is VoiceState.Recording && recordingDuration > 0) {
            Text(
                text = formatDuration(recordingDuration),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Real-time transcription display with AI enhancements
 */
@Composable
private fun RealTimeTranscriptionCard(
    transcription: String,
    voiceState: VoiceState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Transcription",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // AI enhancement indicator
                if (voiceState is VoiceState.Recording || voiceState is VoiceState.Processing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Enhanced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Transcription content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (transcription.isNotEmpty()) {
                    item {
                        Text(
                            text = transcription,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (voiceState) {
                                    is VoiceState.Recording -> "Speak now... AI is listening and transcribing in real-time"
                                    is VoiceState.Processing -> "Processing your voice note with AI..."
                                    else -> "Tap the microphone to start recording"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Typing indicator for real-time processing
                if (voiceState is VoiceState.Recording) {
                    item {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}

/**
 * AI insights panel showing real-time analysis
 */
@Composable
private fun AIInsightsPanel(
    insights: List<VoiceQuickInsight>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(insights) { insight ->
                    InsightChip(insight = insight)
                }
            }
        }
    }
}

/**
 * Individual insight chip
 */
@Composable
private fun InsightChip(
    insight: VoiceQuickInsight,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { /* Handle insight click */ },
        label = {
            Text(
                text = insight.content,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = when (insight.type) {
                    VoiceQuickInsightType.TOPIC_DETECTED -> Icons.Default.Topic
                    VoiceQuickInsightType.ACTION_ITEM_FOUND -> Icons.Default.Task
                    VoiceQuickInsightType.IMPORTANT_POINT -> Icons.Default.Star
                    VoiceQuickInsightType.SENTIMENT_CHANGE -> Icons.Default.Mood
                    VoiceQuickInsightType.ENTITY_MENTIONED -> Icons.Default.Person
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier
    )
}

/**
 * Voice controls panel with recording buttons
 */
@Composable
private fun VoiceControlsPanel(
    voiceState: VoiceState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            when (voiceState) {
                is VoiceState.Idle -> {
                    // Start recording button
                    FloatingActionButton(
                        onClick = onStartRecording,
                        modifier = Modifier.size(64.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start Recording",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
                
                is VoiceState.Recording -> {
                    // Pause button
                    IconButton(
                        onClick = onPauseRecording,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Stop button
                    FloatingActionButton(
                        onClick = onStopRecording,
                        modifier = Modifier.size(64.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Recording",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    
                    // Cancel button
                    IconButton(
                        onClick = onCancelRecording,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                is VoiceState.Processing -> {
                    // Processing indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "AI is processing your voice note...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                is VoiceState.Error -> {
                    // Retry button
                    OutlinedButton(
                        onClick = onStartRecording
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
                
                else -> {
                    // Default state
                    FloatingActionButton(
                        onClick = onStartRecording,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start Recording",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Typing indicator for real-time transcription
 */
@Composable
private fun TypingIndicator() {
    // Inline simple blink without compose animation dependency issues
    var alpha by remember { mutableStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            alpha = 0.3f; delay(400)
            alpha = 1f; delay(400)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (index == 0) alpha else alpha * 0.7f
                        )
                    )
            )
            if (index < 2) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "AI is analyzing...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
    }
}

/**
 * Format duration in MM:SS format
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}