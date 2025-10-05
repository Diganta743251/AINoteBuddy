package com.ainotebuddy.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.analytics.ActivityHeatmap
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.NoteActivity
import com.ainotebuddy.app.data.model.analytics.ProductivityReport
import com.ainotebuddy.app.data.model.analytics.TagUsage
import com.ainotebuddy.app.data.repository.AnalyticsRepository
import com.ainotebuddy.app.data.repository.ExportFormat

import com.ainotebuddy.app.ui.components.analytics.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    // State flows for analytics data
    private val _tagUsage = MutableStateFlow<List<TagUsage>>(emptyList())
    val tagUsage: StateFlow<List<TagUsage>> = _tagUsage.asStateFlow()

    private val _activityHeatmap = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val activityHeatmap: StateFlow<Map<LocalDate, Int>> = _activityHeatmap.asStateFlow()

    private val _noteStats = MutableStateFlow<Map<ActivityType, Int>>(emptyMap())
    val noteStats: StateFlow<Map<ActivityType, Int>> = _noteStats.asStateFlow()

    // Per-note statistics when inspecting a single note
    private val _noteStatistics = MutableStateFlow<Map<ActivityType, Int>>(emptyMap())
    val noteStatistics: StateFlow<Map<ActivityType, Int>> = _noteStatistics.asStateFlow()

    // ExportStatus removed; use UI state for export phases

    private val _selectedTimeRange = MutableStateFlow(TimeRange.default())
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadAnalyticsData()
    }

    /**
     * Loads analytics data for the specified time range
     * @param timeRange The time range to load data for
     */
    fun loadAnalyticsData(timeRange: TimeRange = _selectedTimeRange.value) {
        _selectedTimeRange.value = timeRange
        _isRefreshing.value = true
        
        viewModelScope.launch {
            try {
                _uiState.value = AnalyticsUiState.Loading
                
                // Calculate date range based on the selected time range
                val (startDate, endDate) = when (timeRange) {
                    TimeRange.WEEK -> LocalDate.now().minusDays(7) to LocalDate.now()
                    TimeRange.MONTH -> LocalDate.now().minusDays(30) to LocalDate.now()
                    TimeRange.QUARTER -> LocalDate.now().minusDays(90) to LocalDate.now()
                    TimeRange.YEAR -> LocalDate.now().minusDays(365) to LocalDate.now()
                    TimeRange.ALL_TIME -> LocalDate.MIN to LocalDate.now()
                }

                // Load data in parallel
                launch { loadTagUsage() }
                launch { loadActivityHeatmap(startDate, endDate) }
                launch { loadNoteStats(startDate, endDate) }
                
                _uiState.value = AnalyticsUiState.Success
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(
                    message = e.message ?: "Failed to load analytics data"
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    private suspend fun loadTagUsage() {
        analyticsRepository.getMostUsedTags(10)
            .catch { e ->
                _uiState.value = AnalyticsUiState.Error("Failed to load tag usage: ${e.message}")
            }
            .collect { tags ->
                _tagUsage.value = tags
            }
    }
    
    /**
     * Loads activity heatmap data for the specified date range
     */
    private suspend fun loadActivityHeatmap(startDate: LocalDate, endDate: LocalDate) {
        analyticsRepository.getActivityHeatmap(startDate, endDate)
            .catch { e ->
                _uiState.value = AnalyticsUiState.Error("Failed to load activity heatmap: ${e.message}")
            }
            .collect { heatmapData ->
                // Convert to map of date to activity count
                val activityByDate: Map<LocalDate, Int> = heatmapData.associate { LocalDate.parse(it.date) to it.activityCount }
                _activityHeatmap.value = activityByDate
            }
    }
    
    /**
     * Loads note statistics for the specified date range
     */
    private suspend fun loadNoteStats(startDate: LocalDate, endDate: LocalDate) {
        try {
            val stats = mutableMapOf<ActivityType, Int>()
            
            // Build stats from activities within range (repository provides flow)
            val activities = analyticsRepository.getActivitiesInRange(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
            ).first()

            stats[ActivityType.CREATED] = activities.count { it.activityType == ActivityType.CREATED }
            stats[ActivityType.UPDATED] = activities.count { it.activityType == ActivityType.UPDATED }
            stats[ActivityType.VIEWED] = activities.count { it.activityType == ActivityType.VIEWED }
            stats[ActivityType.DELETED] = activities.count { it.activityType == ActivityType.DELETED }
            stats[ActivityType.EDITED] = activities.count { it.activityType == ActivityType.EDITED }
            
            _noteStats.value = stats
        } catch (e: Exception) {
            _uiState.value = AnalyticsUiState.Error("Failed to load note statistics: ${e.message}")
        }
    }

    /**
     * Exports a report with the specified parameters
     */
    fun exportReport(
        format: ExportFormat = ExportFormat.PDF,
        startDate: LocalDate = LocalDate.now().minusDays(30),
        endDate: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Exporting
            
            try {
                // Generate the productivity report
                val report = analyticsRepository.generateProductivityReport(
                    startDate = startDate,
                    endDate = endDate
                )
                
                // Export the report
                val result = analyticsRepository.exportReport(report, format)
                
                if (result.isSuccess) {
                    _uiState.value = AnalyticsUiState.ExportSuccess(
                        filePath = result.getOrNull() ?: ""
                    )
                } else {
                    _uiState.value = AnalyticsUiState.ExportError(
                        message = result.exceptionOrNull()?.message ?: "Export failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.ExportError(
                    message = e.message ?: "Export failed"
                )
            }
        }
    }

/**
     * Tracks a note view event
     */
    fun trackNoteView(noteId: Long? = null) {
        viewModelScope.launch {
            try {
                val id = noteId ?: return@launch
                val activity = NoteActivity(
                    noteId = id,
                    timestamp = LocalDateTime.now(),
                    activityType = ActivityType.VIEWED,
                    duration = 0
                )
                analyticsRepository.trackNoteActivity(activity)
                
                // Refresh relevant data after tracking
                loadTagUsage()
                loadActivityHeatmap(
                    LocalDate.now().minusDays(30),
                    LocalDate.now()
                )
            } catch (e: Exception) {
                // Silently fail for analytics events
            }
        }
    }
    
    fun loadNoteStatistics(noteId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AnalyticsUiState.Loading
                val statsMap = analyticsRepository.getNoteStatistics(noteId)
                // Map the repository's String->Any stats to ActivityType->Int when applicable
                val mapped: Map<ActivityType, Int> = buildMap {
                    ActivityType.values().forEach { type ->
                        val key = type.name.lowercase()
                        val value = statsMap[key]
                        val intValue = when (value) {
                            is Int -> value
                            is Number -> value.toInt()
                            else -> null
                        }
                        if (intValue != null) put(type, intValue)
                    }
                }
                _noteStatistics.value = mapped
                _uiState.value = AnalyticsUiState.Success
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(
                    message = "Failed to load note statistics: ${e.message}"
                )
            }
        }
    }
    
    fun refreshData() {
        loadAnalyticsData()
    }
    
    fun clearExportStatus() {
        if (_uiState.value is AnalyticsUiState.ExportError || _uiState.value is AnalyticsUiState.ExportSuccess) {
            _uiState.value = AnalyticsUiState.Success
        }
    }
    
    fun clearError() {
        if (_uiState.value is AnalyticsUiState.Error) {
            _uiState.value = AnalyticsUiState.Success
        }
    }
}

/**
 * UI state for the analytics screen
 */
sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    object Success : AnalyticsUiState()
    object Exporting : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
    data class ExportSuccess(val filePath: String) : AnalyticsUiState()
    data class ExportError(val message: String) : AnalyticsUiState()
    
    val isLoading: Boolean get() = this is Loading
    val isError: Boolean get() = this is Error
    val isSuccess: Boolean get() = this is Success
    val isExporting: Boolean get() = this is Exporting
    val isExportSuccess: Boolean get() = this is ExportSuccess
    val isExportError: Boolean get() = this is ExportError
    
    val error: String? get() = (this as? Error)?.message
    val exportError: String? get() = (this as? ExportError)?.message
}

/**
 * State holder for the analytics screen
 */
// Legacy AnalyticsState kept for reference; not used in current UI
// If needed later, update types to match current models and remove ExportStatus dependency.
data class AnalyticsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tagUsage: List<TagUsage> = emptyList(),
    val activityHeatmap: Map<LocalDate, Int> = emptyMap(),
    val noteStatistics: Map<ActivityType, Int>? = null,
    val selectedTimeRange: TimeRange = TimeRange.default()
) {
    companion object {
        val Initial = AnalyticsState()
    }
}
