package com.ainotebuddy.app.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Advanced analytics data processor for comprehensive insights
 */

@Singleton
class AnalyticsDataProcessor @Inject constructor(
    private val context: Context
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    suspend fun processUserEngagementData(
        sessions: List<UserSession>,
        timeRange: TimeRange
    ): EngagementAnalysis {
        return withContext(Dispatchers.IO) {
            val filteredSessions = filterSessionsByTimeRange(sessions, timeRange)
            
            val totalSessions = filteredSessions.size
            val totalDuration = filteredSessions.sumOf { it.durationMs }
            val averageDuration = if (totalSessions > 0) totalDuration / totalSessions else 0L
            
            val engagementScore = calculateEngagementScore(filteredSessions)
            val peakHours = findPeakUsageHours(filteredSessions)
            val sessionDistribution = analyzeSessionDistribution(filteredSessions)
            val trends = calculateEngagementTrends(filteredSessions, timeRange)
            
            EngagementAnalysis(
                timeRange = timeRange,
                totalSessions = totalSessions,
                totalDurationMs = totalDuration,
                averageDurationMs = averageDuration,
                engagementScore = engagementScore,
                peakUsageHours = peakHours,
                sessionDistribution = sessionDistribution,
                trends = trends
            )
        }
    }
    
    suspend fun processContentAnalytics(
        notes: List<NoteAnalyticsData>,
        timeRange: TimeRange
    ): ContentAnalytics {
        return withContext(Dispatchers.IO) {
            val filteredNotes = filterNotesByTimeRange(notes, timeRange)
            
            val totalNotes = filteredNotes.size
            val totalWords = filteredNotes.sumOf { it.wordCount }
            val averageWordsPerNote = if (totalNotes > 0) totalWords / totalNotes else 0
            
            val categoryDistribution = analyzeCategoryDistribution(filteredNotes)
            val lengthDistribution = analyzeLengthDistribution(filteredNotes)
            val creationPatterns = analyzeCreationPatterns(filteredNotes)
            val popularTags = findMostUsedTags(filteredNotes)
            val contentTypes = analyzeContentTypes(filteredNotes)
            
            ContentAnalytics(
                timeRange = timeRange,
                totalNotes = totalNotes,
                totalWords = totalWords,
                averageWordsPerNote = averageWordsPerNote,
                categoryDistribution = categoryDistribution,
                lengthDistribution = lengthDistribution,
                creationPatterns = creationPatterns,
                popularTags = popularTags,
                contentTypes = contentTypes
            )
        }
    }
    
    suspend fun processPerformanceMetrics(
        performanceData: List<PerformanceDataPoint>,
        timeRange: TimeRange
    ): PerformanceAnalytics {
        return withContext(Dispatchers.IO) {
            val filteredData = filterPerformanceByTimeRange(performanceData, timeRange)
            
            val averageStartupTime = filteredData
                .filter { it.metric == "app_startup" }
                .map { it.value }
                .average()
            
            val averageLoadTime = filteredData
                .filter { it.metric == "note_load" }
                .map { it.value }
                .average()
            
            val memoryUsage = filteredData
                .filter { it.metric == "memory_usage" }
                .map { it.value }
                .average()
            
            val crashRate = calculateCrashRate(filteredData)
            val performanceScore = calculatePerformanceScore(filteredData)
            val trends = analyzePerformanceTrends(filteredData)
            
            PerformanceAnalytics(
                timeRange = timeRange,
                averageStartupTime = averageStartupTime,
                averageLoadTime = averageLoadTime,
                memoryUsage = memoryUsage,
                crashRate = crashRate,
                performanceScore = performanceScore,
                trends = trends
            )
        }
    }
    
    suspend fun generateProductivityInsights(
        sessions: List<UserSession>,
        notes: List<NoteAnalyticsData>,
        timeRange: TimeRange
    ): ProductivityInsights {
        return withContext(Dispatchers.IO) {
            val dailyProductivity = calculateDailyProductivity(sessions, notes)
            val weeklyPatterns = analyzeWeeklyPatterns(sessions)
            val peakProductivityHours = findPeakProductivityHours(sessions, notes)
            val productivityScore = calculateProductivityScore(sessions, notes)
            val suggestions = generateProductivitySuggestions(sessions, notes)
            
            ProductivityInsights(
                timeRange = timeRange,
                dailyProductivity = dailyProductivity,
                weeklyPatterns = weeklyPatterns,
                peakProductivityHours = peakProductivityHours,
                productivityScore = productivityScore,
                suggestions = suggestions
            )
        }
    }
    
    suspend fun createDataVisualization(
        data: List<DataPoint>,
        visualizationType: VisualizationType
    ): VisualizationData {
        return withContext(Dispatchers.IO) {
            when (visualizationType) {
                VisualizationType.LINE_CHART -> createLineChartData(data)
                VisualizationType.BAR_CHART -> createBarChartData(data)
                VisualizationType.PIE_CHART -> createPieChartData(data)
                VisualizationType.HEATMAP -> createHeatmapData(data)
                VisualizationType.SCATTER_PLOT -> createScatterPlotData(data)
            }
        }
    }
    
    // Helper functions
    private fun filterSessionsByTimeRange(
        sessions: List<UserSession>,
        timeRange: TimeRange
    ): List<UserSession> {
        val now = System.currentTimeMillis()
        val cutoffTime = when (timeRange) {
            TimeRange.DAY -> now - (24 * 60 * 60 * 1000L)
            TimeRange.WEEK -> now - (7 * 24 * 60 * 60 * 1000L)
            TimeRange.MONTH -> now - (30 * 24 * 60 * 60 * 1000L)
            TimeRange.YEAR -> now - (365 * 24 * 60 * 60 * 1000L)
            TimeRange.ALL_TIME -> 0L
        }
        
        return sessions.filter { it.startTime >= cutoffTime }
    }
    
    private fun filterNotesByTimeRange(
        notes: List<NoteAnalyticsData>,
        timeRange: TimeRange
    ): List<NoteAnalyticsData> {
        val now = System.currentTimeMillis()
        val cutoffTime = when (timeRange) {
            TimeRange.DAY -> now - (24 * 60 * 60 * 1000L)
            TimeRange.WEEK -> now - (7 * 24 * 60 * 60 * 1000L)
            TimeRange.MONTH -> now - (30 * 24 * 60 * 60 * 1000L)
            TimeRange.YEAR -> now - (365 * 24 * 60 * 60 * 1000L)
            TimeRange.ALL_TIME -> 0L
        }
        
        return notes.filter { it.createdAt >= cutoffTime }
    }
    
    private fun filterPerformanceByTimeRange(
        data: List<PerformanceDataPoint>,
        timeRange: TimeRange
    ): List<PerformanceDataPoint> {
        val now = System.currentTimeMillis()
        val cutoffTime = when (timeRange) {
            TimeRange.DAY -> now - (24 * 60 * 60 * 1000L)
            TimeRange.WEEK -> now - (7 * 24 * 60 * 60 * 1000L)
            TimeRange.MONTH -> now - (30 * 24 * 60 * 60 * 1000L)
            TimeRange.YEAR -> now - (365 * 24 * 60 * 60 * 1000L)
            TimeRange.ALL_TIME -> 0L
        }
        
        return data.filter { it.timestamp >= cutoffTime }
    }
    
    private fun calculateEngagementScore(sessions: List<UserSession>): Float {
        if (sessions.isEmpty()) return 0f
        
        val averageDuration = sessions.map { it.durationMs }.average()
        val sessionFrequency = sessions.size.toFloat()
        val actionsPerSession = sessions.map { it.actionsCount }.average()
        
        // Normalize scores (0-1 range)
        val durationScore = kotlin.math.min(averageDuration / (30 * 60 * 1000.0), 1.0)
        val frequencyScore = kotlin.math.min(sessionFrequency / 10.0, 1.0)
        val actionScore = kotlin.math.min(actionsPerSession / 20.0, 1.0)
        
        return ((durationScore + frequencyScore + actionScore) / 3).toFloat()
    }
    
    private fun findPeakUsageHours(sessions: List<UserSession>): List<Int> {
        val hourlyUsage = mutableMapOf<Int, Int>()
        
        sessions.forEach { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyUsage[hour] = (hourlyUsage[hour] ?: 0) + 1
        }
        
        return hourlyUsage.toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }
    
    private fun analyzeSessionDistribution(sessions: List<UserSession>): SessionDistribution {
        val shortSessions = sessions.count { it.durationMs < 5 * 60 * 1000 } // < 5 minutes
        val mediumSessions = sessions.count { 
            it.durationMs >= 5 * 60 * 1000 && it.durationMs < 30 * 60 * 1000 
        } // 5-30 minutes
        val longSessions = sessions.count { it.durationMs >= 30 * 60 * 1000 } // >= 30 minutes
        
        return SessionDistribution(
            shortSessions = shortSessions,
            mediumSessions = mediumSessions,
            longSessions = longSessions
        )
    }
    
    private fun calculateEngagementTrends(
        sessions: List<UserSession>,
        timeRange: TimeRange
    ): List<TrendPoint> {
        val groupedSessions = when (timeRange) {
            TimeRange.DAY -> groupSessionsByHour(sessions)
            TimeRange.WEEK -> groupSessionsByDay(sessions)
            TimeRange.MONTH -> groupSessionsByWeek(sessions)
            TimeRange.YEAR -> groupSessionsByMonth(sessions)
            TimeRange.ALL_TIME -> groupSessionsByMonth(sessions)
        }
        
        return groupedSessions.map { (period, sessionList) ->
            TrendPoint(
                period = period,
                value = sessionList.size.toFloat(),
                timestamp = sessionList.minOfOrNull { it.startTime } ?: 0L
            )
        }
    }
    
    private fun groupSessionsByHour(sessions: List<UserSession>): Map<String, List<UserSession>> {
        return sessions.groupBy { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            "${calendar.get(Calendar.HOUR_OF_DAY)}:00"
        }
    }
    
    private fun groupSessionsByDay(sessions: List<UserSession>): Map<String, List<UserSession>> {
        return sessions.groupBy { session ->
            dateFormatter.format(Date(session.startTime))
        }
    }
    
    private fun groupSessionsByWeek(sessions: List<UserSession>): Map<String, List<UserSession>> {
        return sessions.groupBy { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            "Week ${calendar.get(Calendar.WEEK_OF_YEAR)}"
        }
    }
    
    private fun groupSessionsByMonth(sessions: List<UserSession>): Map<String, List<UserSession>> {
        return sessions.groupBy { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}"
        }
    }
    
    private fun analyzeCategoryDistribution(notes: List<NoteAnalyticsData>): Map<String, Int> {
        return notes
            .groupingBy { it.category ?: "Uncategorized" }
            .eachCount()
    }
    
    private fun analyzeLengthDistribution(notes: List<NoteAnalyticsData>): LengthDistribution {
        val short = notes.count { it.wordCount <= 50 }
        val medium = notes.count { it.wordCount > 50 && it.wordCount <= 200 }
        val long = notes.count { it.wordCount > 200 && it.wordCount <= 500 }
        val veryLong = notes.count { it.wordCount > 500 }
        
        return LengthDistribution(
            shortNotes = short,
            mediumNotes = medium,
            longNotes = long,
            veryLongNotes = veryLong
        )
    }
    
    private fun analyzeCreationPatterns(notes: List<NoteAnalyticsData>): CreationPatterns {
        val hourlyCreation = mutableMapOf<Int, Int>()
        val dailyCreation = mutableMapOf<Int, Int>() // Day of week
        
        notes.forEach { note ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = note.createdAt
            
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            hourlyCreation[hour] = (hourlyCreation[hour] ?: 0) + 1
            dailyCreation[dayOfWeek] = (dailyCreation[dayOfWeek] ?: 0) + 1
        }
        
        return CreationPatterns(
            hourlyDistribution = hourlyCreation,
            dailyDistribution = dailyCreation
        )
    }
    
    private fun findMostUsedTags(notes: List<NoteAnalyticsData>): List<TagUsage> {
        val tagCounts = mutableMapOf<String, Int>()
        
        notes.forEach { note ->
            note.tags.forEach { tag ->
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }
        
        return tagCounts.toList()
            .sortedByDescending { it.second }
            .take(10)
            .map { (tag, count) -> TagUsage(tag, count) }
    }
    
    private fun analyzeContentTypes(notes: List<NoteAnalyticsData>): Map<String, Int> {
        return notes
            .groupingBy { determineContentType(it) }
            .eachCount()
    }
    
    private fun determineContentType(note: NoteAnalyticsData): String {
        return when {
            note.hasCheckboxes -> "Checklist"
            note.hasImages -> "Visual Note"
            note.hasVoiceRecording -> "Voice Note"
            note.wordCount > 500 -> "Article"
            note.wordCount < 50 -> "Quick Note"
            else -> "Regular Note"
        }
    }
    
    private fun calculateCrashRate(data: List<PerformanceDataPoint>): Float {
        val crashEvents = data.count { it.metric == "crash" }
        val totalSessions = data.count { it.metric == "session_start" }
        
        return if (totalSessions > 0) {
            (crashEvents.toFloat() / totalSessions) * 100
        } else 0f
    }
    
    private fun calculatePerformanceScore(data: List<PerformanceDataPoint>): Float {
        if (data.isEmpty()) return 0f
        
        val startupTimes = data.filter { it.metric == "app_startup" }.map { it.value }
        val loadTimes = data.filter { it.metric == "note_load" }.map { it.value }
        val memoryUsage = data.filter { it.metric == "memory_usage" }.map { it.value }
        
        val startupScore = if (startupTimes.isNotEmpty()) {
            val avgStartup = startupTimes.average()
            when {
                avgStartup < 1000 -> 1.0f
                avgStartup < 2000 -> 0.8f
                avgStartup < 3000 -> 0.6f
                else -> 0.4f
            }
        } else 1.0f
        
        val loadScore = if (loadTimes.isNotEmpty()) {
            val avgLoad = loadTimes.average()
            when {
                avgLoad < 500 -> 1.0f
                avgLoad < 1000 -> 0.8f
                avgLoad < 2000 -> 0.6f
                else -> 0.4f
            }
        } else 1.0f
        
        val memoryScore = if (memoryUsage.isNotEmpty()) {
            val avgMemory = memoryUsage.average()
            when {
                avgMemory < 50 -> 1.0f // < 50MB
                avgMemory < 100 -> 0.8f // < 100MB
                avgMemory < 200 -> 0.6f // < 200MB
                else -> 0.4f
            }
        } else 1.0f
        
        return (startupScore + loadScore + memoryScore) / 3
    }
    
    private fun analyzePerformanceTrends(data: List<PerformanceDataPoint>): List<TrendPoint> {
        return data
            .filter { it.metric == "app_startup" }
            .groupBy { 
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = it.timestamp
                dateFormatter.format(calendar.time)
            }
            .map { (date, points) ->
                TrendPoint(
                    period = date,
                    value = points.map { it.value }.average().toFloat(),
                    timestamp = points.first().timestamp
                )
            }
            .sortedBy { it.timestamp }
    }
    
    private fun calculateDailyProductivity(
        sessions: List<UserSession>,
        notes: List<NoteAnalyticsData>
    ): List<DailyProductivity> {
        val dailyData = mutableMapOf<String, MutableList<UserSession>>()
        val dailyNotes = mutableMapOf<String, MutableList<NoteAnalyticsData>>()
        
        sessions.forEach { session ->
            val date = dateFormatter.format(Date(session.startTime))
            dailyData.getOrPut(date) { mutableListOf() }.add(session)
        }
        
        notes.forEach { note ->
            val date = dateFormatter.format(Date(note.createdAt))
            dailyNotes.getOrPut(date) { mutableListOf() }.add(note)
        }
        
        return dailyData.map { (date, sessionList) ->
            val noteList = dailyNotes[date] ?: emptyList()
            val activityCount = sessionList.sumOf { it.actionsCount }
            
            DailyProductivity(
                date = date,
                sessionCount = sessionList.size,
                noteCount = noteList.size,
                activityCount = activityCount,
                productivityScore = calculateProductivityScore(sessionList, noteList)
            )
        }.sortedBy { it.date }
    }
    
    private fun analyzeWeeklyPatterns(sessions: List<UserSession>): Map<Int, Float> {
        val weeklyData = mutableMapOf<Int, MutableList<UserSession>>()
        
        sessions.forEach { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            weeklyData.getOrPut(dayOfWeek) { mutableListOf() }.add(session)
        }
        
        return weeklyData.mapValues { (_, sessionList) ->
            sessionList.map { it.durationMs }.average().toFloat()
        }
    }
    
    private fun findPeakProductivityHours(
        sessions: List<UserSession>,
        notes: List<NoteAnalyticsData>
    ): List<Int> {
        val hourlyProductivity = mutableMapOf<Int, Int>()
        
        sessions.forEach { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyProductivity[hour] = (hourlyProductivity[hour] ?: 0) + session.actionsCount
        }
        
        notes.forEach { note ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = note.createdAt
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyProductivity[hour] = (hourlyProductivity[hour] ?: 0) + 1
        }
        
        return hourlyProductivity.toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }
    
    private fun calculateProductivityScore(
        sessions: List<UserSession>,
        notes: List<NoteAnalyticsData>
    ): Float {
        if (sessions.isEmpty() && notes.isEmpty()) return 0f
        
        val averageSessionDuration = if (sessions.isNotEmpty()) {
            sessions.map { it.durationMs }.average()
        } else 0.0
        
        val notesPerSession = if (sessions.isNotEmpty()) {
            notes.size.toDouble() / sessions.size
        } else 0.0
        
        val averageNoteLength = if (notes.isNotEmpty()) {
            notes.map { it.wordCount }.average()
        } else 0.0
        
        // Normalize scores
        val durationScore = minOf(averageSessionDuration / (20 * 60 * 1000), 1.0) // Max 20 minutes
        val notesScore = minOf(notesPerSession / 2, 1.0) // Max 2 notes per session
        val lengthScore = minOf(averageNoteLength / 200, 1.0) // Max 200 words
        
        return ((durationScore + notesScore + lengthScore) / 3).toFloat()
    }
    
    private fun generateProductivitySuggestions(
        sessions: List<UserSession>,
        notes: List<NoteAnalyticsData>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        val averageSessionDuration = if (sessions.isNotEmpty()) {
            sessions.map { it.durationMs }.average()
        } else 0.0
        
        if (averageSessionDuration < 5 * 60 * 1000) { // Less than 5 minutes
            suggestions.add("Try to spend more time in focused sessions to increase productivity")
        }
        
        val shortNotes = notes.count { it.wordCount < 50 }
        if (shortNotes > notes.size * 0.7) {
            suggestions.add("Consider expanding your notes with more details for better reference")
        }
        
        val peakHours = findPeakUsageHours(sessions)
        if (peakHours.isNotEmpty()) {
            suggestions.add("Your peak productivity hours are around ${peakHours.first()}:00. Plan important tasks during this time.")
        }
        
        return suggestions
    }
    
    // Visualization creation functions
    private fun createLineChartData(data: List<DataPoint>): VisualizationData {
        val sortedData = data.sortedBy { it.timestamp }
        
        return VisualizationData(
            type = VisualizationType.LINE_CHART,
            points = sortedData.map { 
                ChartPoint(it.x, it.y, it.label, it.timestamp) 
            },
            xAxisLabel = "Time",
            yAxisLabel = "Value",
            colorsArgb = listOf(Color.Blue.toArgb())
        )
    }
    
    private fun createBarChartData(data: List<DataPoint>): VisualizationData {
        return VisualizationData(
            type = VisualizationType.BAR_CHART,
            points = data.map { 
                ChartPoint(it.x, it.y, it.label, it.timestamp) 
            },
            xAxisLabel = "Category",
            yAxisLabel = "Count",
            colorsArgb = generateColorPaletteArgb(data.size)
        )
    }
    
    private fun createPieChartData(data: List<DataPoint>): VisualizationData {
        val total = data.sumOf { it.y.toDouble() }.toFloat()
        
        return VisualizationData(
            type = VisualizationType.PIE_CHART,
            points = data.map { point ->
                ChartPoint(
                    x = point.x,
                    y = (point.y / total * 100).toFloat(),
                    label = "${point.label}: ${(point.y / total * 100).toInt()}%",
                    timestamp = point.timestamp
                )
            },
            colorsArgb = generateColorPaletteArgb(data.size)
        )
    }
    
    private fun createHeatmapData(data: List<DataPoint>): VisualizationData {
        return VisualizationData(
            type = VisualizationType.HEATMAP,
            points = data.map { 
                ChartPoint(it.x, it.y, it.label, it.timestamp) 
            },
            colorsArgb = listOf(Color.Blue.toArgb(), Color.Red.toArgb())
        )
    }
    
    private fun createScatterPlotData(data: List<DataPoint>): VisualizationData {
        return VisualizationData(
            type = VisualizationType.SCATTER_PLOT,
            points = data.map { 
                ChartPoint(it.x, it.y, it.label, it.timestamp) 
            },
            xAxisLabel = "X Value",
            yAxisLabel = "Y Value",
            colorsArgb = listOf(Color.Green.toArgb())
        )
    }
    
    private fun generateColorPaletteArgb(size: Int): List<Int> {
        val baseColors = listOf(
            Color(0xFF2196F3), // Blue
            Color(0xFF4CAF50), // Green
            Color(0xFFFF9800), // Orange
            Color(0xFF9C27B0), // Purple
            Color(0xFFF44336), // Red
            Color(0xFF00BCD4), // Cyan
            Color(0xFFFFEB3B), // Yellow
            Color(0xFF795548)  // Brown
        )
        
        return if (size <= baseColors.size) {
            baseColors.take(size).map { it.toArgb() }
        } else {
            val colors = mutableListOf<Int>()
            repeat(size) { index ->
                val baseColor = baseColors[index % baseColors.size]
                val variation = (index / baseColors.size) * 0.03f // small variation per cycle
                val varied = Color(
                    red = (baseColor.red + variation).coerceIn(0f, 1f),
                    green = (baseColor.green + variation).coerceIn(0f, 1f),
                    blue = (baseColor.blue + variation).coerceIn(0f, 1f)
                )
                colors.add(varied.toArgb())
            }
            colors
        }
    }
}

// Data classes for analytics
@Serializable
data class UserSession(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val actionsCount: Int,
    val screenViews: List<String>
)

@Serializable
data class NoteAnalyticsData(
    val id: Long,
    val wordCount: Int,
    val characterCount: Int,
    val createdAt: Long,
    val lastModified: Long,
    val category: String?,
    val tags: List<String>,
    val hasImages: Boolean,
    val hasVoiceRecording: Boolean,
    val hasCheckboxes: Boolean
)

@Serializable
data class PerformanceDataPoint(
    val timestamp: Long,
    val metric: String,
    val value: Double,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class EngagementAnalysis(
    val timeRange: TimeRange,
    val totalSessions: Int,
    val totalDurationMs: Long,
    val averageDurationMs: Long,
    val engagementScore: Float,
    val peakUsageHours: List<Int>,
    val sessionDistribution: SessionDistribution,
    val trends: List<TrendPoint>
)

@Serializable
data class ContentAnalytics(
    val timeRange: TimeRange,
    val totalNotes: Int,
    val totalWords: Int,
    val averageWordsPerNote: Int,
    val categoryDistribution: Map<String, Int>,
    val lengthDistribution: LengthDistribution,
    val creationPatterns: CreationPatterns,
    val popularTags: List<TagUsage>,
    val contentTypes: Map<String, Int>
)

@Serializable
data class PerformanceAnalytics(
    val timeRange: TimeRange,
    val averageStartupTime: Double,
    val averageLoadTime: Double,
    val memoryUsage: Double,
    val crashRate: Float,
    val performanceScore: Float,
    val trends: List<TrendPoint>
)

@Serializable
data class ProductivityInsights(
    val timeRange: TimeRange,
    val dailyProductivity: List<DailyProductivity>,
    val weeklyPatterns: Map<Int, Float>,
    val peakProductivityHours: List<Int>,
    val productivityScore: Float,
    val suggestions: List<String>
)

@Serializable
data class SessionDistribution(
    val shortSessions: Int,    // < 5 minutes
    val mediumSessions: Int,   // 5-30 minutes
    val longSessions: Int      // >= 30 minutes
)

@Serializable
data class LengthDistribution(
    val shortNotes: Int,      // <= 50 words
    val mediumNotes: Int,     // 51-200 words
    val longNotes: Int,       // 201-500 words
    val veryLongNotes: Int    // > 500 words
)

@Serializable
data class CreationPatterns(
    val hourlyDistribution: Map<Int, Int>,
    val dailyDistribution: Map<Int, Int>
)

@Serializable
data class TagUsage(
    val tag: String,
    val count: Int
)

@Serializable
data class TrendPoint(
    val period: String,
    val value: Float,
    val timestamp: Long
)

@Serializable
data class DailyProductivity(
    val date: String,
    val sessionCount: Int,
    val noteCount: Int,
    val activityCount: Int,
    val productivityScore: Float
)

@Serializable
data class DataPoint(
    val x: Float,
    val y: Float,
    val label: String,
    val timestamp: Long
)

@Serializable
data class ChartPoint(
    val x: Float,
    val y: Float,
    val label: String,
    val timestamp: Long
)

@Serializable
// Note: Compose Color is not serializable; store ARGB ints for persistence/serialization
// and convert to Color when needed in UI layer
 data class VisualizationData(
    val type: VisualizationType,
    val points: List<ChartPoint>,
    val xAxisLabel: String = "",
    val yAxisLabel: String = "",
    val colorsArgb: List<Int>
)

enum class VisualizationType {
    LINE_CHART,
    BAR_CHART,
    PIE_CHART,
    HEATMAP,
    SCATTER_PLOT
}