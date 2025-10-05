package com.ainotebuddy.app.data.model.organization

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

/**
 * Defines a recurrence pattern for recurring notes
 */
@Entity(tableName = "recurrence_patterns")
data class RecurrencePattern(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val repeatType: RepeatType,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
    val monthDay: Int? = null,
    val timeOfDay: LocalTime = LocalTime.NOON,
    val timeZone: TimeZone = TimeZone.getDefault(),
    val occurrences: Int? = null,
    val lastRun: LocalDate? = null,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    enum class RepeatType {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
        CUSTOM
    }
    
    /**
     * Calculates the next occurrence date based on the pattern
     */
    fun nextOccurrence(after: LocalDate = LocalDate.now()): LocalDate? {
        if (!isActive) return null
        if (endDate != null && endDate.isBefore(after)) return null
        
        return when (repeatType) {
            RepeatType.DAILY -> calculateDailyNext(after)
            RepeatType.WEEKLY -> calculateWeeklyNext(after)
            RepeatType.MONTHLY -> calculateMonthlyNext(after)
            RepeatType.YEARLY -> calculateYearlyNext(after)
            RepeatType.CUSTOM -> calculateCustomNext(after)
        }?.takeIf { endDate == null || !it.isAfter(endDate) }
    }
    
    private fun calculateDailyNext(after: LocalDate): LocalDate {
        var next = if (lastRun != null) lastRun.plusDays(interval.toLong())
                  else startDate
        
        while (next.isBefore(after) || next.isEqual(after)) {
            next = next.plusDays(interval.toLong())
        }
        
        return next
    }
    
    private fun calculateWeeklyNext(after: LocalDate): LocalDate? {
        if (daysOfWeek.isEmpty()) return null
        
        var current = if (lastRun != null && lastRun.isAfter(startDate)) lastRun.plusDays(1)
                     else startDate
        
        // If we're before the start date, start from start date
        if (current.isBefore(startDate)) {
            current = startDate
        }
        
        // Check each day from current date
        while (current.isBefore(after) || current.isEqual(after)) {
            current = current.plusDays(1)
        }
        
        // Find next matching day of week
        while (current.dayOfWeek !in daysOfWeek) {
            current = current.plusDays(1)
            
            // If we've gone through a full week without finding a match, return null
            if (current.dayOfWeek == startDate.dayOfWeek) {
                return null
            }
        }
        
        return current
    }
    
    private fun calculateMonthlyNext(after: LocalDate): LocalDate? {
        var next = if (lastRun != null) {
            lastRun.plusMonths(interval.toLong())
        } else {
            startDate
        }
        
        // If day of month is specified (e.g., 15th of each month)
        if (dayOfMonth != null) {
            next = next.withDayOfMonth(minOf(dayOfMonth, next.month.maxLength()))
            
            while (next.isBefore(after) || next.isEqual(after)) {
                next = next.plusMonths(interval.toLong())
                next = next.withDayOfMonth(minOf(dayOfMonth, next.month.maxLength()))
            }
            
            return next
        }
        
        // If specific day of week (e.g., second Tuesday)
        if (daysOfWeek.isNotEmpty() && monthDay != null) {
            // Reset to start of month and find the nth occurrence
            next = next.withDayOfMonth(1)
            var count = 0
            
            while (true) {
                if (next.dayOfWeek in daysOfWeek) {
                    count++
                    if (count == monthDay) break
                }
                
                next = next.plusDays(1)
                
                // Prevent infinite loop
                if (next.dayOfMonth == 1) {
                    next = next.plusMonths(interval.toLong() - 1)
                    break
                }
            }
            
            while (next.isBefore(after) || next.isEqual(after)) {
                next = next.plusMonths(interval.toLong())
                
                val monthStart = next.withDayOfMonth(1)
                var current = monthStart
                var found = false
                
                for (day in 1..monthStart.month.maxLength()) {
                    if (current.dayOfWeek in daysOfWeek) {
                        count++
                        if (count == monthDay) {
                            next = current
                            found = true
                            break
                        }
                    }
                    current = current.plusDays(1)
                }
                
                if (!found) {
                    next = next.plusMonths(1).withDayOfMonth(1)
                    count = 0
                }
            }
            
            return next
        }
        
        // Default to same day of month as start date
        val dayOfMonth = minOf(startDate.dayOfMonth, next.month.maxLength())
        next = next.withDayOfMonth(dayOfMonth)
        
        while (next.isBefore(after) || next.isEqual(after)) {
            next = next.plusMonths(interval.toLong())
            val day = minOf(startDate.dayOfMonth, next.month.maxLength())
            next = next.withDayOfMonth(day)
        }
        
        return next
    }
    
    private fun calculateYearlyNext(after: LocalDate): LocalDate? {
        var next = if (lastRun != null) {
            lastRun.plusYears(interval.toLong())
        } else {
            startDate
        }
        
        // If specific month and day (e.g., January 1st)
        if (next.isBefore(after) || next.isEqual(after)) {
            next = next.plusYears(interval.toLong())
        }
        
        return next
    }
    
    private fun calculateCustomNext(after: LocalDate): LocalDate? {
        // For custom patterns, we'd need to implement specific logic
        // This is a placeholder that could be expanded based on requirements
        return null
    }
    
    /**
     * Checks if the pattern should run on the given date
     */
    fun shouldRunOn(date: LocalDate): Boolean {
        if (!isActive) return false
        if (date.isBefore(startDate)) return false
        if (endDate != null && date.isAfter(endDate)) return false
        if (lastRun != null && !lastRun.isBefore(date)) return false
        
        return when (repeatType) {
            RepeatType.DAILY -> checkDaily(date)
            RepeatType.WEEKLY -> checkWeekly(date)
            RepeatType.MONTHLY -> checkMonthly(date)
            RepeatType.YEARLY -> checkYearly(date)
            RepeatType.CUSTOM -> checkCustom(date)
        }
    }
    
    private fun checkDaily(date: LocalDate): Boolean {
        val daysBetween = startDate.datesUntil(date).count()
        return daysBetween % interval == 0L
    }
    
    private fun checkWeekly(date: LocalDate): Boolean {
        if (date.dayOfWeek !in daysOfWeek) return false
        
        val weeksBetween = (startDate.datesUntil(date).count() / 7).toInt()
        return weeksBetween % interval == 0
    }
    
    private fun checkMonthly(date: LocalDate): Boolean {
        if (dayOfMonth != null) {
            // Specific day of month (e.g., 15th)
            return date.dayOfMonth == dayOfMonth && 
                   (date.dayOfYear - startDate.dayOfYear) % (interval * 30) == 0
        }
        
        if (daysOfWeek.isNotEmpty() && monthDay != null) {
            // Specific day of week in month (e.g., 2nd Tuesday)
            if (date.dayOfWeek !in daysOfWeek) return false
            
            val monthStart = date.withDayOfMonth(1)
            val dayOfWeekCount = monthStart.datesUntil(date.plusDays(1))
                .filter { it.dayOfWeek == date.dayOfWeek }
                .count()
                
            return dayOfWeekCount.toInt() == monthDay
        }
        
        // Default to same day of month as start date
        return date.dayOfMonth == startDate.dayOfMonth && 
               (date.monthValue - startDate.monthValue) % interval == 0
    }
    
    private fun checkYearly(date: LocalDate): Boolean {
        return date.month == startDate.month && 
               date.dayOfMonth == startDate.dayOfMonth &&
               (date.year - startDate.year) % interval == 0
    }
    
    private fun checkCustom(date: LocalDate): Boolean {
        // Custom logic would go here
        return false
    }
    
    /**
     * Creates a copy of this pattern with updated last run time
     */
    fun withLastRun(date: LocalDate): RecurrencePattern {
        return copy(
            lastRun = date,
            updatedAt = Date()
        )
    }
    
    /**
     * Creates a copy of this pattern with updated active status
     */
    fun withActiveStatus(active: Boolean): RecurrencePattern {
        return copy(
            isActive = active,
            updatedAt = Date()
        )
    }
}

private fun java.time.Month.maxLength(): Int {
    return when (this) {
        java.time.Month.FEBRUARY -> 28 // Simplified, doesn't account for leap years
        java.time.Month.APRIL, java.time.Month.JUNE, 
        java.time.Month.SEPTEMBER, java.time.Month.NOVEMBER -> 30
        else -> 31
    }
}

/**
 * Data class for creating a new recurrence pattern
 */
data class RecurrencePatternCreate(
    val noteId: Long,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val repeatType: RecurrencePattern.RepeatType,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
    val monthDay: Int? = null,
    val timeOfDay: LocalTime = LocalTime.NOON,
    val timeZone: TimeZone = TimeZone.getDefault(),
    val occurrences: Int? = null
)

/**
 * Data class for updating an existing recurrence pattern
 */
data class RecurrencePatternUpdate(
    val id: Long,
    val endDate: LocalDate? = null,
    val interval: Int? = null,
    val daysOfWeek: Set<DayOfWeek>? = null,
    val dayOfMonth: Int? = null,
    val monthDay: Int? = null,
    val timeOfDay: LocalTime? = null,
    val timeZone: TimeZone? = null,
    val isActive: Boolean? = null
)
