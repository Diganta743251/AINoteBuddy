package com.ainotebuddy.app.features.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.viewmodel.NoteEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorAiSheet(
    noteId: Long,
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: NoteEditorViewModel,
) {
    if (!show) return

    val summary by viewModel.aiSummary.collectAsState(initial = null)
    val tags by viewModel.aiTags.collectAsState(initial = emptyList())
    val summarizeLoading by viewModel.summarizeLoading.collectAsState(initial = false)
    val autoTagLoading by viewModel.autoTagLoading.collectAsState(initial = false)

    val retryTs = remember { mutableStateOf(0L) }
    var retryDisabledUntil by remember { mutableStateOf(0L) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Used to show the "Updated just now" label; declared before first usage
    var showUpdatedLabel by remember { mutableStateOf(false) }

    // Animate expand/collapse based on `show`
    LaunchedEffect(show) {
        if (show) sheetState.expand() else sheetState.hide()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AI Tools", style = MaterialTheme.typography.titleMedium)

            Text("AI Actions", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))

            // Action buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.requestSummary(noteId) },
                    enabled = !summarizeLoading
                ) {
                    if (summarizeLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Summarize")
                    }
                }
                Button(
                    onClick = { viewModel.requestAutoTags(noteId) },
                    enabled = !autoTagLoading
                ) {
                    if (autoTagLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Auto-Tag")
                    }
                }
            }

            Divider(Modifier.padding(vertical = 8.dp))

            // Error + Retry centralized below buttons
            val error by viewModel.aiError.collectAsState(initial = null)
            error?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - retryTs.value > 1000L) {
                                viewModel.retryAiAction()
                                retryTs.value = now
                            }
                        },
                        enabled = !summarizeLoading && !autoTagLoading && System.currentTimeMillis() > retryDisabledUntil
                    ) { Text("Retry") }
                }
            }

            // Results section header
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Results", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))

            // Smart Reminder suggestion chip
            val reminderCandidate by viewModel.reminderCandidate.collectAsState(initial = null)
            reminderCandidate?.let { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    var lastClickTs by remember { mutableStateOf(0L) }

                    TextButton(onClick = {
                        val now = System.currentTimeMillis()
                        if (com.ainotebuddy.app.util.ClickGuard.allow(lastClickTs)) {
                            lastClickTs = now
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.confirmReminder()
                        }
                    }) {
                        val pretty = com.ainotebuddy.app.util.TimeFormatter.formatRelativeTime(c.timeMillis)
                        // Show natural synonyms that were parsed (noon/tonight/midnight) when applicable by relying on TimeFormatter
                        Text("\uD83D\uDCC5 Add Reminder: $pretty")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val now = System.currentTimeMillis()
                        if (com.ainotebuddy.app.util.ClickGuard.allow(lastClickTs)) {
                            lastClickTs = now
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.dismissReminderSuggestion()
                        }
                    }) {
                        Text("Dismiss ❌")
                    }
                }
            }

            if (summarizeLoading && summary == null) {
                CircularProgressIndicator()
            }

            summary?.let { s ->
                Text("Summary", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp))
                val summaryAlpha by animateFloatAsState(if (s.isNotBlank()) 1f else 0f, label = "summary_alpha")
                Text(
                    s,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .graphicsLayer(alpha = summaryAlpha)
                )
                Button(onClick = { viewModel.applySummaryToContent() }, enabled = s.isNotBlank()) { Text("Apply Summary") }
                // Briefly disable Retry after new summary appears
                LaunchedEffect(s) { retryDisabledUntil = System.currentTimeMillis() + 500L }

                // If only Summary is present, show the updated label here too
                if (tags.isEmpty()) {
                    AnimatedVisibility(visible = showUpdatedLabel) {
                        Text(
                            "Updated just now",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Updated just now fading label under results
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            LaunchedEffect(summary, tags) {
                if ((summary?.isNotBlank() == true) || tags.isNotEmpty()) {
                    showUpdatedLabel = true
                    retryDisabledUntil = System.currentTimeMillis() + 500L
                    // Platform-adaptive haptic on update
                    if (Build.VERSION.SDK_INT >= 34) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    } else {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    }
                    delay(1500L)
                    showUpdatedLabel = false
                }
            }




            if (tags.isNotEmpty()) {
                Text(
                    "Generated by AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                Text("Tags", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp))
                val tagsAlpha by animateFloatAsState(if (tags.isNotEmpty()) 1f else 0f, label = "tags_alpha")
                Text(
                    tags.joinToString(", "),
                    modifier = Modifier
                        .animateContentSize()
                        .graphicsLayer(alpha = tagsAlpha)
                )
                Button(onClick = { viewModel.applyTagsToNote() }, enabled = tags.isNotEmpty()) { Text("Apply Tags") }
                // Briefly disable Retry after new tags appear
                LaunchedEffect(tags) { retryDisabledUntil = System.currentTimeMillis() + 500L }

                // Updated just now label right under results
                AnimatedVisibility(visible = showUpdatedLabel) {
                    Text(
                        "Updated just now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Final end-cap divider when there is content
                if ((summary?.isNotBlank() == true) || tags.isNotEmpty()) {
                    Divider(Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}