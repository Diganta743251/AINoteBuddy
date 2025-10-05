package com.ainotebuddy.app.data

import com.ainotebuddy.app.data.model.analytics.ActivityHeatmapData
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.NoteActivity
import com.ainotebuddy.app.data.model.analytics.ProductivityReport
import com.ainotebuddy.app.data.model.analytics.TagUsage
import com.ainotebuddy.app.data.repository.AnalyticsRepository
import com.ainotebuddy.app.data.repository.ExportFormat
import com.ainotebuddy.app.data.repository.Result
import com.ainotebuddy.app.util.getDatesInRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

/**
 * Fake implementation of [AnalyticsRepository] for testing.
 */
class FakeAnalyticsRepository : AnalyticsRepository {

    // Test data
    private val _activities = mutableListOf<NoteActivity>()
    private val _tagUsage = mutableListOf<TagUsage>()
    private val _activityHeatmap = mutableListOf<ActivityHeatmapData>()
    
    // Test control flags
    var shouldThrowError = false
    var errorMessage = "Test error"
    
    // State flows for testing
    private val _activitiesFlow = MutableStateFlow(_activities.toList())
    private val _tagUsageFlow = MutableStateFlow(_tagUsage.toList())
    private val _activityHeatmapFlow = MutableStateFlow(_activityHeatmap.toList())
    
    // Test data generation
    init {
        // Generate some test data
        val today = LocalDate.now()
        val startDate = today.minusMonths(1)
        
        // Generate random activities
        repeat(50) {
            val date = startDate.plusDays(Random.nextLong(30))
            _activities.add(
                NoteActivity(
                    noteId = Random.nextLong(10),
                    activityType = ActivityType.values().random(),
                    timestamp = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
                    durationMs = Random.nextLong(1000, 60000)
                )
            )
        }
        
        // Generate some tag usage data
        _tagUsage.addAll(
            listOf(
                TagUsage("work", 25),
                TagUsage("personal", 15),
                TagUsage("important", 10),
                TagUsage("ideas", 8),
                TagUsage("meeting", 5)
            )
        )
        
        // Generate activity heatmap data
        getDatesInRange(startDate, today).forEach { date ->
            _activityHeatmap.add(
                ActivityHeatmapData(
                    date = date,
                    activityCount = Random.nextInt(0, 10)
                )
            )
        }
    }
    
    // Test control methods
    fun setTestActivities(activities: List<NoteActivity>) {
        _activities.clear()
        _activities.addAll(activities)
        _activitiesFlow.value = _activities.toList()
    }
    
    fun setTestTagUsage(tagUsage: List<TagUsage>) {
        _tagUsage.clear()
        _tagUsage.addAll(tagUsage)
        _tagUsageFlow.value = _tagUsage.toList()
    }
    
    fun addTagUsage(tagUsage: TagUsage) {
        _tagUsage.add(tagUsage)
        _tagUsageFlow.value = _tagUsage.toList()
    }
    
    // Repository implementation
    override suspend fun trackNoteActivity(activity: NoteActivity) {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        _activities.add(activity)
        _activitiesFlow.value = _activities.toList()
    }

    override fun getNoteActivities(
        noteId: Long?,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<NoteActivity>> {
        return if (shouldThrowError) {
            flow { throw RuntimeException(errorMessage) }
        } else {
            _activitiesFlow
        }
    }

    override suspend fun getMostUsedTags(limit: Int): List<TagUsage> {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        return _tagUsage.sortedByDescending { it.count }.take(limit)
    }

    override suspend fun getActivityHeatmap(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<ActivityHeatmapData>> {
        return if (shouldThrowError) {
            flow { throw RuntimeException(errorMessage) }
        } else {
            _activityHeatmapFlow
        }
    }

    override suspend fun getNotesCreatedCount(startDate: LocalDate, endDate: LocalDate): Int {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        return _activities
            .filter { it.activityType == ActivityType.CREATED }
            .filter { activity ->
                val activityDate = LocalDate.ofEpochDay(activity.timestamp / (1000 * 60 * 60 * 24))
                !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
            }
            .size
    }

    override suspend fun getNotesUpdatedCount(startDate: LocalDate, endDate: LocalDate): Int {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        return _activities
            .filter { it.activityType == ActivityType.UPDATED }
            .filter { activity ->
                val activityDate = LocalDate.ofEpochDay(activity.timestamp / (1000 * 60 * 60 * 24))
                !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
            }
            .size
    }

    override suspend fun getNotesViewedCount(startDate: LocalDate, endDate: LocalDate): Int {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        return _activities
            .filter { it.activityType == ActivityType.VIEWED }
            .filter { activity ->
                val activityDate = LocalDate.ofEpochDay(activity.timestamp / (1000 * 60 * 60 * 24))
                !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
            }
            .size
    }

    override suspend fun getNotesDeletedCount(startDate: LocalDate, endDate: LocalDate): Int {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        return _activities
            .filter { it.activityType == ActivityType.DELETED }
            .filter { activity ->
                val activityDate = LocalDate.ofEpochDay(activity.timestamp / (1000 * 60 * 60 * 24))
                !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
            }
            .size
    }

    override suspend fun getTotalTimeSpentMinutes(startDate: LocalDate, endDate: LocalDate): Int {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        return _activities
            .filter { activity ->
                val activityDate = LocalDate.ofEpochDay(activity.timestamp / (1000 * 60 * 60 * 24))
                !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
            }
            .sumOf { it.durationMs.toInt() } / (1000 * 60) // Convert ms to minutes
    }

    override suspend fun generateProductivityReport(
        startDate: LocalDate,
        endDate: LocalDate,
        tagUsage: List<TagUsage>,
        activityStats: Map<ActivityType, Int>,
        activityHeatmap: Map<LocalDate, Int>
    ): ProductivityReport {
        if (shouldThrowError) throw RuntimeException(errorMessage)
        
        return ProductivityReport(
            id = UUID.randomUUID().toString(),
            startDate = startDate,
            endDate = endDate,
            notesCreated = activityStats[ActivityType.CREATED] ?: 0,
            notesUpdated = activityStats[ActivityType.UPDATED] ?: 0,
            notesViewed = activityStats[ActivityType.VIEWED] ?: 0,
            notesDeleted = activityStats[ActivityType.DELETED] ?: 0,
            totalTimeSpentMinutes = activityStats[ActivityType.TIME_SPENT] ?: 0,
            mostActiveDay = activityHeatmap.maxByOrNull { it.value }?.key ?: LocalDate.now(),
            mostUsedTags = tagUsage.take(5),
            activityByDate = activityHeatmap.mapKeys { it.key.toString() }
        )
    }

    override suspend fun exportReport(
        report: ProductivityReport,
        format: ExportFormat
    ): Result<String> {
        return if (shouldThrowError) {
            Result.failure(RuntimeException(errorMessage))
        } else {
            Result.success("/path/to/exported/report.${format.fileExtension}")
        }
    }
}
