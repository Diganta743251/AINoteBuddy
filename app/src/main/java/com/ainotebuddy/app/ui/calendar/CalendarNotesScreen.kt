package com.ainotebuddy.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarNotesScreen(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    onBack: () -> Unit,
    onNewNote: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Filter notes for selected date
    val selectedDateString = fullDateFormat.format(selectedDate.time)
    val notesForSelectedDate = notes.filter { note ->
        val noteDate = fullDateFormat.format(Date(note.createdAt))
        noteDate == selectedDateString
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        TopAppBar(
            title = { Text("Calendar Notes") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = onNewNote) {
                    Icon(Icons.Filled.Add, "New Note")
                }
            }
        )
        
        // Month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonth.add(Calendar.MONTH, -1)
                currentMonth = currentMonth.clone() as Calendar
            }) {
                Icon(Icons.Filled.ChevronLeft, "Previous Month")
            }
            
            Text(
                text = dateFormat.format(currentMonth.time),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = {
                currentMonth.add(Calendar.MONTH, 1)
                currentMonth = currentMonth.clone() as Calendar
            }) {
                Icon(Icons.Filled.ChevronRight, "Next Month")
            }
        }
        
        // Calendar grid
        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            notes = notes,
            onDateSelected = { date ->
                selectedDate = date
            }
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Notes for selected date
        Text(
            text = "Notes for ${SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(selectedDate.time)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (notesForSelectedDate.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No notes for this date",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onNewNote) {
                                Text("Create Note")
                            }
                        }
                    }
                }
            } else {
                items(notesForSelectedDate) { note ->
                    NoteCalendarCard(
                        note = note,
                        onClick = { onNoteClick(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    notes: List<NoteEntity>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = Calendar.getInstance().apply {
        time = currentMonth.time
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Create note count map for each day
    val noteCountByDate = notes.groupBy { note ->
        dateFormat.format(Date(note.createdAt))
    }.mapValues { it.value.size }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            dayHeaders.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar days
        val totalCells = 42 // 6 weeks * 7 days
        val days = (1..totalCells).map { index ->
            val dayNumber = index - startDayOfWeek
            if (dayNumber in 1..daysInMonth) {
                val dayCalendar = Calendar.getInstance().apply {
                    time = currentMonth.time
                    set(Calendar.DAY_OF_MONTH, dayNumber)
                }
                val dateString = dateFormat.format(dayCalendar.time)
                val noteCount = noteCountByDate[dateString] ?: 0
                CalendarDay(dayNumber, dayCalendar, noteCount, true)
            } else {
                CalendarDay(0, Calendar.getInstance(), 0, false)
            }
        }
        
        // Arrange in rows of 7
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        isSelected = day.isValid && isSameDay(day.calendar, selectedDate),
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

data class CalendarDay(
    val dayNumber: Int,
    val calendar: Calendar,
    val noteCount: Int,
    val isValid: Boolean
)

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onDateSelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    day.noteCount > 0 -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = day.isValid) {
                if (day.isValid) {
                    onDateSelected(day.calendar)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (day.isValid) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.dayNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        day.noteCount > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (day.noteCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCalendarCard(
    note: NoteEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (note.color != 0) Color(note.color) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (note.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(note.tags.split(",").filter { it.isNotBlank() }) { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag.trim(), fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}