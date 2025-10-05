package com.ainotebuddy.app.data.repository

import com.ainotebuddy.app.data.local.analytics.AnalyticsDatabase
import com.ainotebuddy.app.data.local.analytics.toEntity
import com.ainotebuddy.app.data.model.analytics.ActivityHeatmap
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.NoteActivity
import com.ainotebuddy.app.data.model.analytics.ProductivityReport
import com.ainotebuddy.app.data.model.analytics.TagUsage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsRepository {
    // Track note activities
    suspend fun trackNoteActivity(activity: NoteActivity)
    
    // Get activities within a date range
    fun getActivitiesInRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<NoteActivity>>
    
    // Get most used tags
    fun getMostUsedTags(limit: Int = 10): Flow<List<TagUsage>>
    
    // Get activity heatmap data
    fun getActivityHeatmap(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<ActivityHeatmap>>
    
    // Generate productivity report
    suspend fun generateProductivityReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): ProductivityReport
    
    // Export report to file
    suspend fun exportReport(
        report: ProductivityReport,
        format: ExportFormat
    ): Result<String> // Returns file path
    
    // Get note statistics
    suspend fun getNoteStatistics(noteId: Long): Map<String, Any>
}

enum class ExportFormat {
    PDF,
    CSV,
    HTML
}

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val database: AnalyticsDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AnalyticsRepository {

    override suspend fun trackNoteActivity(activity: NoteActivity) {
        withContext(ioDispatcher) {
            database.noteActivityDao().insert(activity.toEntity())
            // Update tag usage if tags are involved
            if (activity.activityType == ActivityType.CREATED || activity.activityType == ActivityType.UPDATED) {
                // Extract tags from note content if available
                val tags = extractTagsFromContent(activity)
                tags.forEach { tag ->
                    database.tagUsageDao().incrementTagUsage(tag, activity.timestamp)
                }
            }
        }
    }

    override fun getActivitiesInRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<NoteActivity>> {
        return database.noteActivityDao()
            .getActivitiesInRange(startDate, endDate)
            .map { entities -> entities.map { it.toModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getMostUsedTags(limit: Int): Flow<List<TagUsage>> {
        return database.tagUsageDao()
            .getMostUsedTags(limit)
            .map { entities -> entities.map { it.toModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getActivityHeatmap(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<ActivityHeatmap>> {
        // Bridge DAO signature (String dates) with LocalDate inputs
        val start = startDate.toString()
        val end = endDate.toString()
        return kotlinx.coroutines.flow.flow {
            val raw = database.noteActivityDao().getActivityHeatmap(start, end)
            val mapped = raw
                .groupBy { it.date to it.hour }
                .map { (key, activities) ->
                    ActivityHeatmap(
                        date = key.first,
                        hour = key.second,
                        activityCount = activities.sumOf { it.count }
                    )
                }
                .sortedWith(compareBy<ActivityHeatmap> { it.date }.thenBy { it.hour })
            emit(mapped)
        }.flowOn(ioDispatcher)
    }

    override suspend fun generateProductivityReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): ProductivityReport {
        return withContext(ioDispatcher) {
            val activities = database.noteActivityDao()
                .getActivitiesInRange(
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay()
                )
                .first()
                .map { it.toModel() }

            val mostActiveDay = activities
                .groupBy { it.timestamp.toLocalDate() }
                .maxByOrNull { it.value.size }?.key?.toString() ?: "N/A"

            val mostUsedTags = database.tagUsageDao()
                .getMostUsedTags(5)
                .first()
                .map { it.toModel() }

            val wordCount = activities.sumOf { it.wordCount }
            val notesCreated = activities.count { it.activityType == ActivityType.CREATED }

            ProductivityReport(
                period = "${startDate} to $endDate",
                notesCreated = notesCreated,
                wordsWritten = wordCount,
                mostActiveDay = mostActiveDay,
                mostUsedTags = mostUsedTags,
                activityHeatmap = getActivityHeatmap(startDate, endDate).first()
            )
        }
    }

    override suspend fun exportReport(
        report: ProductivityReport,
        format: ExportFormat
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val dir = java.io.File(System.getProperty("java.io.tmpdir"))
            val fileName = "AINoteBuddy_Report_${System.currentTimeMillis()}.${format.name.lowercase()}"
            val file = File(dir, fileName)
            
            when (format) {
                ExportFormat.PDF -> exportToPdf(report, file)
                ExportFormat.CSV -> exportToCsv(report, file)
                ExportFormat.HTML -> exportToHtml(report, file)
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNoteStatistics(noteId: Long): Map<String, Any> {
        return withContext(ioDispatcher) {
            val activities = database.noteActivityDao()
                .getActivitiesForNote(noteId)
                .map { it.toModel() }

            val totalEdits = activities.count { it.activityType == ActivityType.EDITED }
            val totalViews = activities.count { it.activityType == ActivityType.VIEWED }
            val creationDate = activities.firstOrNull { it.activityType == ActivityType.CREATED }?.timestamp
            val lastUpdated = activities.maxOfOrNull { it.timestamp }
            val averageEditDuration = activities
                .filter { it.duration > 0 }
                .map { it.duration }
                .average()
                .toLong()

            mapOf(
                "totalEdits" to totalEdits,
                "totalViews" to totalViews,
                "creationDate" to creationDate.toString(),
                "lastUpdated" to lastUpdated.toString(),
                "averageEditDurationMs" to averageEditDuration
            )
        }
    }

    private fun extractTagsFromContent(activity: NoteActivity): List<String> {
        // Implement tag extraction logic from note content
        // This is a placeholder - implement based on your actual tag format
        return emptyList()
    }
    
    private suspend fun exportToPdf(report: ProductivityReport, file: File) {
        // Implement PDF export using a library like iText or Android's PdfDocument
        // This is a placeholder implementation
        file.writeText("PDF Export: ${report.period}\n")
        file.appendText("Notes Created: ${report.notesCreated}\n")
        file.appendText("Words Written: ${report.wordsWritten}\n")
        file.appendText("Most Active Day: ${report.mostActiveDay}\n")
        file.appendText("\nMost Used Tags:\n")
        report.mostUsedTags.forEach { tag ->
            file.appendText("- ${tag.tag}: ${tag.count} uses\n")
        }
    }
    
    private suspend fun exportToCsv(report: ProductivityReport, file: File) {
        // Implement CSV export
        file.writeText("Period,Notes Created,Words Written,Most Active Day\n")
        file.appendText("${report.period},${report.notesCreated},${report.wordsWritten},${report.mostActiveDay}\n\n")
        file.appendText("Tag,Count,Last Used\n")
        report.mostUsedTags.forEach { tag ->
            file.appendText("${tag.tag},${tag.count},${tag.lastUsed}\n")
        }
    }
    
    private suspend fun exportToHtml(report: ProductivityReport, file: File) {
        // Implement HTML export
        file.writeText("""
            <!DOCTYPE html>
            <html>
            <head><title>AINoteBuddy Report - ${report.period}</title></head>
            <body>
                <h1>Productivity Report: ${report.period}</h1>
                <div>Notes Created: ${report.notesCreated}</div>
                <div>Words Written: ${report.wordsWritten}</div>
                <div>Most Active Day: ${report.mostActiveDay}</div>
                <h2>Most Used Tags</h2>
                <ul>
                    ${report.mostUsedTags.joinToString("\n") { "<li>${it.tag}: ${it.count} uses (last: ${it.lastUsed})</li>" }}
                </ul>
            </body>
            </html>
        """.trimIndent())
    }
}
