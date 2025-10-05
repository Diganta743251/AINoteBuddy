package com.ainotebuddy.app.integration.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.integration.calendar.model.CalendarEvent
import com.ainotebuddy.app.integration.calendar.viewmodel.CalendarIntegrationViewModel
import com.ainotebuddy.app.ui.components.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CalendarIntegration(
    viewModel: CalendarIntegrationViewModel,
    noteId: Long,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEventDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    // Load calendars and events when the note changes
    LaunchedEffect(noteId) {
        viewModel.loadCalendars(noteId)
    }

    // Show error if any (local banner)
    uiState.error?.let { error ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with add button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Calendar Events",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            Button(
                onClick = { showAddEventDialog = true },
                enabled = uiState.calendars.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Event")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Events list
        if (uiState.events.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Event,
                title = "No Events",
                description = "Add an event to link this note to your calendar"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.events) { event ->
                    EventCard(
                        event = event,
                        onEditClick = {
                            selectedEvent = event
                            showAddEventDialog = true
                        },
                        onDeleteClick = {
                            viewModel.deleteEvent(event.id)
                        }
                    )
                }
            }
        }
    }

    // Add/Edit Event Dialog
    if (showAddEventDialog) {
        EventDialog(
            event = selectedEvent,
            calendars = uiState.calendars,
            selectedCalendarId = uiState.selectedCalendarId,
            onCalendarSelected = { viewModel.selectCalendar(it) },
            onDismiss = {
                showAddEventDialog = false
                selectedEvent = null
            },
            onConfirm = { title, description, startTime, endTime, allDay, location ->
                if (selectedEvent == null) {
                    viewModel.createEvent(
                        title = title,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        allDay = allDay,
                        location = location
                    )
                } else {
                    viewModel.updateEvent(
                        selectedEvent!!.copy(
                            title = title,
                            description = description,
                            startTime = startTime,
                            endTime = endTime,
                            allDay = allDay,
                            location = location
                        )
                    )
                }
                showAddEventDialog = false
                selectedEvent = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCard(
    event: CalendarEvent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Event title
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Event time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (event.allDay) {
                        "${event.startTime.format(dateFormatter)} (All Day)"
                    } else {
                        "${event.startTime.format(dateFormatter)} • ${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Location
            if (event.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onEditClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = onDeleteClick,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
