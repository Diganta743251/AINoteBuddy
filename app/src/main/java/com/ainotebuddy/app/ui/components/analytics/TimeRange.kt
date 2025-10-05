package com.ainotebuddy.app.ui.components.analytics

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Represents the time range for filtering analytics data.
 */
enum class TimeRange(val days: Int, val displayName: String) {
    WEEK(7, "Last 7 days"),
    MONTH(30, "Last 30 days"),
    QUARTER(90, "Last 90 days"),
    YEAR(365, "Last year"),
    ALL_TIME(Int.MAX_VALUE, "All time");
    
    /**
     * Gets the start date for this time range.
     */
    fun startDate(): LocalDate {
        return if (this == ALL_TIME) {
            LocalDate.MIN
        } else {
            LocalDate.now().minusDays(days.toLong() - 1)
        }
    }
    
    /**
     * Gets the end date for this time range (inclusive).
     */
    fun endDate(): LocalDate {
        return LocalDate.now()
    }
    
    /**
     * Gets a list of dates in this range, useful for heatmap generation.
     */
    fun dateRange(): List<LocalDate> {
        val start = startDate()
        val end = endDate()
        val numOfDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
        return List(numOfDays) { start.plusDays(it.toLong()) }
    }
    
    companion object {
        /**
         * Gets the default time range.
         */
        fun default(): TimeRange = MONTH
    }
}
