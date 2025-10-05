package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.ainotebuddy.app.R
import java.time.LocalDate

/**
 * A dialog for exporting analytics reports in different formats.
 *
 * @param onDismissRequest Callback when the dialog should be dismissed
 * @param onConfirm Callback when the export is confirmed with the selected options
 * @param modifier Modifier to be applied to the dialog
 * @param selectedFormat The currently selected export format
 * @param dateRange The selected date range as a pair of start and end dates
 * @param availableFormats List of available export formats (defaults to common formats)
 */
@Composable
fun ExportReportDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (ExportFormat, LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    selectedFormat: ExportFormat = ExportFormat.PDF,
    dateRange: ClosedRange<LocalDate> = LocalDate.now().minusDays(30)..LocalDate.now(),
    availableFormats: List<ExportFormat> = ExportFormat.values().toList()
) {
    var currentFormat by remember { mutableStateOf(selectedFormat) }
    var startDate by remember { mutableStateOf(dateRange.start) }
    var endDate by remember { mutableStateOf(dateRange.endInclusive) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { 
            Text(
                text = "Export Report",
                style = MaterialTheme.typography.headlineSmall
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Format selection
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Format options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(bottom = 16.dp)
                ) {
                    availableFormats.forEach { format ->
                        FormatOption(
                            format = format,
                            isSelected = currentFormat == format,
                            onSelected = { currentFormat = format },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Date range selection
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Start date
                    DatePickerButton(
                        label = "Start Date",
                        date = startDate,
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // End date
                    DatePickerButton(
                        label = "End Date",
                        date = endDate,
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Date validation error
                if (startDate > endDate) {
                    Text(
                        text = "Start date must be before end date",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentFormat, startDate, endDate) },
                enabled = startDate <= endDate
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
    
    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { startDate = it }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { endDate = it },
            initialDate = endDate
        )
    }
}

@Composable
private fun FormatOption(
    format: ExportFormat,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Format icon
        Icon(
            imageVector = format.icon,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Format name and description
        Column {
            Text(
                text = format.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = format.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DatePickerButton(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = date.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate = LocalDate.now()
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Date") },
        text = {
            // In a real app, you would use a proper date picker here
            // This is a simplified version for demonstration
            Column {
                Text("Date picker would appear here")
                Text("Selected: $selectedDate")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(selectedDate)
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Represents the available export formats for analytics reports.
 */
enum class ExportFormat(
    val displayName: String,
    val description: String,
    val fileExtension: String,
    val mimeType: String,
    val icon: ImageVector
) {
    PDF(
        displayName = "PDF",
        description = "Portable Document Format",
        fileExtension = "pdf",
        mimeType = "application/pdf",
        icon = Icons.Default.PictureAsPdf
    ),
    CSV(
        displayName = "CSV",
        description = "Comma Separated Values",
        fileExtension = "csv",
        mimeType = "text/csv",
        icon = Icons.Default.TableChart
    ),
    EXCEL(
        displayName = "Excel",
        description = "Microsoft Excel Spreadsheet",
        fileExtension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        icon = Icons.Default.TableChart
    )
}
