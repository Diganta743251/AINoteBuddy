package com.ainotebuddy.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.analytics.ActivityHeatmap
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.NoteActivity
import com.ainotebuddy.app.data.model.analytics.ProductivityReport
import com.ainotebuddy.app.data.model.analytics.TagUsage
// import com.ainotebuddy.app.data.model.analytics.TimeRange
import com.ainotebuddy.app.ui.components.analytics.TimeRange
import com.ainotebuddy.app.data.repository.AnalyticsRepository
import com.ainotebuddy.app.data.repository.ExportFormat
// import com.ainotebuddy.app.data.repository.Result
// Simplify: remove optional caches/throttling/optimizations
// import com.ainotebuddy.app.util.AnalyticsCache
// import com.ainotebuddy.app.util.Debouncer
// import com.ainotebuddy.app.util.Throttler
// import com.ainotebuddy.app.util.getDateRangeForTimeRange
// import com.ainotebuddy.app.util.optimizeActivityStats
// import com.ainotebuddy.app.util.optimizeTagUsage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Optimized version of AnalyticsViewModel with performance improvements
 */
@HiltViewModel
class OptimizedAnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    // Time range state
    private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    // Analytics data
    private val _tagUsage = MutableStateFlow<List<TagUsage>>(emptyList())
    val tagUsage: StateFlow<List<TagUsage>> = _tagUsage.asStateFlow()

    private val _activityHeatmap = MutableStateFlow<List<ActivityHeatmap>>(emptyList())
    val activityHeatmap: StateFlow<List<ActivityHeatmap>> = _activityHeatmap.asStateFlow()

    private val _noteStats = MutableStateFlow<Map<ActivityType, Int>>(emptyMap())
    val noteStats: StateFlow<Map<ActivityType, Int>> = _noteStats.asStateFlow()

    // Optimizations (disabled for simplicity)

    init {
        // Initial load
        loadAnalyticsData()
        
        // Set up automatic refresh when time range changes
        setupTimeRangeObserver()
    }

    /**
     * Load analytics data for the current time range
     */
    fun loadAnalyticsData(timeRange: TimeRange = _selectedTimeRange.value) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = AnalyticsUiState.Loading
                _selectedTimeRange.value = timeRange

                // Derive a simple date window from util TimeRange
                val now = LocalDate.now()
                val startDate = when (timeRange) {
                    TimeRange.WEEK -> now.minusDays(7)
                    TimeRange.MONTH -> now.minusDays(30)
                    TimeRange.QUARTER -> now.minusDays(90)
                    TimeRange.YEAR -> now.minusDays(365)
                    TimeRange.ALL_TIME -> LocalDate.MIN
                }
                val endDate = now

                // Load basic datasets from repository
                val tags = repository.getMostUsedTags(10).first()
                _tagUsage.value = tags

                val heatmap = repository.getActivityHeatmap(startDate, endDate).first()
                _activityHeatmap.value = heatmap

                // If per-activity stats API isn’t available, compute simple counts from heatmap
                val stats = mapOf(
                    ActivityType.VIEWED to heatmap.sumOf { it.activityCount }
                )
                _noteStats.value = stats

                _uiState.value = AnalyticsUiState.Success(
                    tagUsage = tags,
                    activityHeatmap = heatmap,
                    noteStats = stats
                )
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(
                    message = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }

    /**
     * Track a note view event with debouncing
     */
    fun trackNoteView(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val activity = NoteActivity(
                noteId = noteId,
                activityType = ActivityType.VIEWED,
                timestamp = java.time.LocalDateTime.now(),
                duration = 0
            )
            repository.trackNoteActivity(activity)
            _noteStats.update { current ->
                current + (ActivityType.VIEWED to (current[ActivityType.VIEWED] ?: 0) + 1)
            }
        }
    }

    /**
     * Export analytics report with throttling
     */
    fun exportReport(
        format: ExportFormat = ExportFormat.PDF,
        startDate: LocalDate = LocalDate.now().minusDays(30),
        endDate: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = AnalyticsUiState.Exporting

                // Generate report using repository API
                val report = repository.generateProductivityReport(
                    startDate = startDate,
                    endDate = endDate
                )

                // Export report
                val result = repository.exportReport(report, format)
                result.onSuccess { filePath ->
                    _uiState.value = AnalyticsUiState.ExportSuccess(
                        filePath = filePath,
                        format = format
                    )
                }.onFailure { e ->
                    _uiState.value = AnalyticsUiState.ExportError(
                        message = e.message ?: "Failed to export report"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.ExportError(
                    message = e.message ?: "An error occurred during export"
                )
            }
        }
    }

    /**
     * Refresh analytics data
     */
    fun refreshData() {
        // No cache layer; just reload
        loadAnalyticsData()
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        if (_uiState.value is AnalyticsUiState.Error) {
            _uiState.value = AnalyticsUiState.Success(
                tagUsage = _tagUsage.value,
                activityHeatmap = _activityHeatmap.value,
                noteStats = _noteStats.value
            )
        }
    }

    /**
     * Set up observer for time range changes
     */
    private fun setupTimeRangeObserver() {
        selectedTimeRange
            .onEach { loadAnalyticsData(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Load note statistics with optimized queries
     */
    // Removed loadNoteStats: repository does not provide per-metric counters in this baseline

    /**
     * UI state for the analytics screen
     */
    sealed class AnalyticsUiState {
        object Loading : AnalyticsUiState()
        data class Error(val message: String) : AnalyticsUiState()
        data class Success(
            val tagUsage: List<TagUsage>,
            val activityHeatmap: List<ActivityHeatmap>,
            val noteStats: Map<ActivityType, Int>
        ) : AnalyticsUiState()
        
        object Exporting : AnalyticsUiState()
        data class ExportSuccess(val filePath: String, val format: ExportFormat) : AnalyticsUiState()
        data class ExportError(val message: String) : AnalyticsUiState()
    }
}
