package com.ainotebuddy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.TagUsage
import com.ainotebuddy.app.ui.components.analytics.*
import com.ainotebuddy.app.ui.components.analytics.TimeRange
import com.ainotebuddy.app.ui.viewmodel.AnalyticsViewModel
import com.ainotebuddy.app.ui.viewmodel.AnalyticsUiState
import com.ainotebuddy.app.ui.components.LoadingIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.ainotebuddy.app.data.repository.ExportFormat as RepoExportFormat
import com.ainotebuddy.app.ui.components.analytics.ExportFormat as UIExportFormat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBackClick: () -> Unit,
    onNavigateToTagAnalytics: () -> Unit = {},
    onNavigateToActivityHeatmap: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    val tagUsage by viewModel.tagUsage.collectAsStateWithLifecycle(emptyList<TagUsage>())
    val activityHeatmap by viewModel.activityHeatmap.collectAsStateWithLifecycle(emptyMap<LocalDate, Int>())
    val noteStats by viewModel.noteStats.collectAsStateWithLifecycle(emptyMap<ActivityType, Int>())
    
    // State for export dialog
    var showExportDialog by remember { mutableStateOf(false) }
    
    // Handle side effects - load data when needed
    LaunchedEffect(viewModel, isRefreshing) {
        if (!isRefreshing && uiState !is AnalyticsUiState.Success) {
            viewModel.loadAnalyticsData()
        }
    }
    
    // Track note view for analytics - once per screen composition
    LaunchedEffect(viewModel) {
        viewModel.trackNoteView()
    }
    
    // Handle export results
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AnalyticsUiState.ExportSuccess -> {
                Toast.makeText(context, "Report exported successfully", Toast.LENGTH_SHORT).show()
            }
            is AnalyticsUiState.ExportError -> {
                Toast.makeText(context, "Export failed: ${state.message}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            AnalyticsTopBar(
                onBackClick = onBackClick,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                selectedTimeRange = selectedTimeRange,
                onTimeRangeSelected = { viewModel.loadAnalyticsData(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExportDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export Report"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    if (!isRefreshing) {
                        LoadingIndicator()
                    }
                }
                is AnalyticsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadAnalyticsData() }) {
                            Text("Retry")
                        }
                    }
                }
                is AnalyticsUiState.Success -> {
                    AnalyticsDashboard(
                        viewModel = viewModel,
                        onNavigateToTagAnalytics = onNavigateToTagAnalytics,
                        onNavigateToActivityHeatmap = onNavigateToActivityHeatmap,
                        onExportReport = { showExportDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {}
            }
            
            // Show loading indicator when refreshing
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }
    }
    
    // Export Report Dialog
    if (showExportDialog) {
        ExportReportDialog(
            onDismissRequest = { showExportDialog = false },
            onConfirm = { format, startDate, endDate ->
                val repoFormat = when (format) {
                    UIExportFormat.PDF -> RepoExportFormat.PDF
                    UIExportFormat.CSV -> RepoExportFormat.CSV
                    UIExportFormat.EXCEL -> RepoExportFormat.CSV
                }
                viewModel.exportReport(repoFormat, startDate, endDate)
                showExportDialog = false
            },
            dateRange = when (selectedTimeRange) {
                TimeRange.WEEK -> LocalDate.now().minusDays(7)..LocalDate.now()
                TimeRange.MONTH -> LocalDate.now().minusDays(30)..LocalDate.now()
                TimeRange.QUARTER -> LocalDate.now().minusDays(90)..LocalDate.now()
                TimeRange.YEAR -> LocalDate.now().minusDays(365)..LocalDate.now()
                TimeRange.ALL_TIME -> LocalDate.MIN..LocalDate.now()
            }
        )
    }
}
