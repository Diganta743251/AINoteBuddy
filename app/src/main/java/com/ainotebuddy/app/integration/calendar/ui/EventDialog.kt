package com.ainotebuddy.app.integration.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ainotebuddy.app.integration.calendar.model.CalendarEvent
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    event: CalendarEvent? = null,
    calendars: List<Pair<Long, String>>,
    selectedCalendarId: Long,
    onCalendarSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        allDay: Boolean,
        location: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var startTime by remember { mutableStateOf(event?.startTime ?: LocalDateTime.now()) }
    var endTime by remember { mutableStateOf(event?.endTime ?: LocalDateTime.now().plusHours(1)) }
    var allDay by remember { mutableStateOf(event?.allDay ?: false) }
    var location by remember { mutableStateOf(event?.location ?: "") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (event == null) "Add to Calendar" else "Edit Event",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Calendar selection (simplified)
                if (calendars.isNotEmpty()) {
                    OutlinedTextField(
                        value = calendars.firstOrNull { it.first == selectedCalendarId }?.second ?: "",
                        onValueChange = {},
                        label = { Text("Calendar") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // All day switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("All day")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = allDay,
                        onCheckedChange = { allDay = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Start time (display only for now)
                Text("Start time")
                OutlinedTextField(
                    value = startTime.toString(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // End time (display only for now)
                Text("End time")
                OutlinedTextField(
                    value = endTime.toString(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                title,
                                description,
                                startTime,
                                endTime,
                                allDay,
                                location
                            )
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (event == null) "ADD" else "SAVE")
                    }
                }
            }
        }
    }
}

// Simplified helpers removed to avoid missing components
