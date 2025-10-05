package com.ainotebuddy.app.ai

import java.time.*
import java.util.Locale

/**
 * Minimal, lightweight NLU for reminder extraction.
 * - Regex for absolute/relative times
 * - java.time for parsing
 * - Confidence scoring: explicit > relative > vague
 */
object ReminderNlu {
    data class ReminderCandidate(
        val timeMillis: Long,
        val title: String,
        val confidence: Float
    )

    data class Config(
        val defaultTime: LocalTime = LocalTime.of(9, 0) // used for weekday-only, tomorrow, next week
    )

    // Common patterns
    private val time12 = Regex("\\b(1[0-2]|0?[1-9])(:[0-5][0-9])?\\s?(am|pm)\\b", RegexOption.IGNORE_CASE)
    private val time24 = Regex("\\b([01]?[0-9]|2[0-3]):[0-5][0-9]\\b")
    private val tomorrow = Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE)
    private val today = Regex("\\btoday\\b", RegexOption.IGNORE_CASE)
    private val tonight = Regex("\\btonight\\b", RegexOption.IGNORE_CASE)
    private val nextWeek = Regex("\\bnext\\s+week\\b", RegexOption.IGNORE_CASE)
    private val weekdays = listOf(
        DayOfWeek.MONDAY to Regex("\\bmonday\\b", RegexOption.IGNORE_CASE),
        DayOfWeek.TUESDAY to Regex("\\btuesday\\b", RegexOption.IGNORE_CASE),
        DayOfWeek.WEDNESDAY to Regex("\\bwednesday\\b", RegexOption.IGNORE_CASE),
        DayOfWeek.THURSDAY to Regex("\\bthursday\\b", RegexOption.IGNORE_CASE),
        DayOfWeek.FRIDAY to Regex("\\bfriday\\b", RegexOption.IGNORE_CASE),
        DayOfWeek.SATURDAY to Regex("\\bsaturday\\b", RegexOption.IGNORE_CASE),
        DayOfWeek.SUNDAY to Regex("\\bsunday\\b", RegexOption.IGNORE_CASE),
    )

    private val relativeInHours = Regex("\\bin\\s+(\\d{1,2})\\s+hours?\\b", RegexOption.IGNORE_CASE)
    private val relativeInMinutes = Regex("\\bin\\s+(\\d{1,3})\\s+minutes?\\b", RegexOption.IGNORE_CASE)
    private val relativeInDays = Regex("\\bin\\s+(\\d{1,2})\\s+days?\\b", RegexOption.IGNORE_CASE)
    private val relativeInWeeks = Regex("\\bin\\s+(\\d{1,2})\\s+weeks?\\b", RegexOption.IGNORE_CASE)

    private val midnight = Regex("\\bmidnight\\b", RegexOption.IGNORE_CASE)
    private val noon = Regex("\\bnoon\\b", RegexOption.IGNORE_CASE)
    private val inHalfHour = Regex("\\bin\\s+half\\s+an\\s+hour\\b", RegexOption.IGNORE_CASE)
    private val halfAnHour = Regex("\\bhalf\\s+an\\s+hour\\b", RegexOption.IGNORE_CASE)
    private val quarterPast = Regex("\\bquarter\\s+past\\s+(\\d{1,2})(?:\\s?(am|pm))?\\b", RegexOption.IGNORE_CASE)

    // Date formats e.g. "Aug 21", "Aug 21 6pm"
    private val monthDay = Regex("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+([0-2]?[0-9]|3[01])\\b", RegexOption.IGNORE_CASE)

    // Keywords to boost intent
    private val keywords = listOf("remind", "reminder", "meet", "deadline", "call", "appointment", "schedule")

    fun parse(
        raw: String,
        nowClock: Clock = Clock.systemDefaultZone(),
        confidenceThreshold: Float = 0.7f,
        config: Config = Config()
    ): ReminderCandidate? {
        val text = raw.trim()
        if (text.isBlank()) return null

        val now = ZonedDateTime.now(nowClock)
        var confidence = 0.0f

        // Title extraction heuristic: sentence before/around time keywords
        val title = extractTitle(text)

        // Absolute time parsing
        val abs = parseAbsolute(text, now, config)
        if (abs != null) {
            confidence = 0.9f
            val boosted = boostByKeywords(confidence, text)
            return if (boosted >= confidenceThreshold) ReminderCandidate(abs.toInstant().toEpochMilli(), title, boosted) else null
        }

        // Relative time parsing
        val rel = parseRelative(text, now, config)
        if (rel != null) {
            confidence = 0.8f
            val boosted = boostByKeywords(confidence, text)
            return if (boosted >= confidenceThreshold) ReminderCandidate(rel.toInstant().toEpochMilli(), title, boosted) else null
        }

        // Weekday-only (e.g., "Friday at 3")
        val wd = parseWeekday(text, now, config)
        if (wd != null) {
            confidence = 0.8f
            val boosted = boostByKeywords(confidence, text)
            return if (boosted >= confidenceThreshold) ReminderCandidate(wd.toInstant().toEpochMilli(), title, boosted) else null
        }

        // Month Day (e.g., "Aug 21", optional time)
        val md = parseMonthDay(text, now, config)
        if (md != null) {
            confidence = 0.85f
            val boosted = boostByKeywords(confidence, text)
            return if (boosted >= confidenceThreshold) ReminderCandidate(md.toInstant().toEpochMilli(), title, boosted) else null
        }

        return null
    }

    private fun boostByKeywords(base: Float, text: String): Float {
        val hits = keywords.count { text.contains(it, ignoreCase = true) }
        return (base + hits * 0.03f).coerceAtMost(0.98f)
    }

    private fun extractTitle(text: String): String {
        // Simple heuristic: take up to 80 chars around the first keyword/time, fallback to first sentence.
        val idx = keywords.mapNotNull { k -> text.indexOf(k, ignoreCase = true).takeIf { it >= 0 } }.minOrNull()
        val timeIdx = listOf(
            time12.find(text)?.range?.first,
            time24.find(text)?.range?.first,
            monthDay.find(text)?.range?.first
        ).filterNotNull().minOrNull()
        val cut = listOfNotNull(idx, timeIdx).minOrNull() ?: 0
        val start = (cut - 40).coerceAtLeast(0)
        val end = (cut + 40).coerceAtMost(text.length)
        val snippet = text.substring(start, end).trim()
        return snippet.lines().firstOrNull()?.take(80)?.ifBlank { text.take(80) } ?: text.take(80)
    }

    private fun parseAbsolute(text: String, now: ZonedDateTime, config: Config): ZonedDateTime? {
        val t12 = time12.find(text)?.value
        val t24 = time24.find(text)?.value
        val qp = quarterPast.find(text)
        val baseDate = when {
            tomorrow.containsMatchIn(text) -> now.plusDays(1).toLocalDate()
            today.containsMatchIn(text) -> now.toLocalDate()
            tonight.containsMatchIn(text) -> now.toLocalDate()
            else -> now.toLocalDate()
        }
        val time: LocalTime? = when {
            t12 != null -> parseTime12(t12)
            t24 != null -> parseTime24(t24)
            qp != null -> parseQuarterPast(qp)
            tonight.containsMatchIn(text) -> LocalTime.of(21, 0)
            noon.containsMatchIn(text) -> LocalTime.NOON
            midnight.containsMatchIn(text) -> LocalTime.MIDNIGHT
            else -> null
        }
        // Midnight should move to next day if from same day context
        return time?.let {
            var zdt = ZonedDateTime.of(baseDate, it, now.zone)
            if (it == LocalTime.MIDNIGHT && !tomorrow.containsMatchIn(text)) {
                zdt = zdt.plusDays(1)
            }
            zdt.adjustIfPast(now)
        }
    }

    private fun parseRelative(text: String, now: ZonedDateTime, config: Config): ZonedDateTime? {
        inHalfHour.find(text)?.let { return now.plusMinutes(30) }
        // If phrase is present without leading "in", still treat as 30 minutes from now
        if (halfAnHour.containsMatchIn(text)) return now.plusMinutes(30)

        relativeInMinutes.find(text)?.let {
            val minutes = it.groupValues[1].toIntOrNull() ?: return null
            return now.plusMinutes(minutes.toLong())
        }
        relativeInHours.find(text)?.let {
            val hours = it.groupValues[1].toIntOrNull() ?: return null
            return now.plusHours(hours.toLong())
        }
        relativeInDays.find(text)?.let {
            val days = it.groupValues[1].toIntOrNull() ?: return null
            return now.plusDays(days.toLong())
        }
        relativeInWeeks.find(text)?.let {
            val weeks = it.groupValues[1].toIntOrNull() ?: return null
            return now.plusWeeks(weeks.toLong())
        }
        if (nextWeek.containsMatchIn(text)) return now.plusWeeks(1).withHour(config.defaultTime.hour).withMinute(config.defaultTime.minute)
        if (tomorrow.containsMatchIn(text)) return now.plusDays(1).withHour(config.defaultTime.hour).withMinute(config.defaultTime.minute)
        return null
    }

    private fun parseWeekday(text: String, now: ZonedDateTime, config: Config): ZonedDateTime? {
        val match = weekdays.firstOrNull { it.second.containsMatchIn(text) } ?: return null
        val time = time12.find(text)?.value?.let { parseTime12(it) }
            ?: time24.find(text)?.value?.let { parseTime24(it) }
            ?: config.defaultTime
        val targetDow = match.first
        var date = now.toLocalDate()
        while (date.dayOfWeek != targetDow) date = date.plusDays(1)
        return ZonedDateTime.of(date, time, now.zone).adjustIfPast(now)
    }

    private fun parseMonthDay(text: String, now: ZonedDateTime, config: Config): ZonedDateTime? {
        val m = monthDay.find(text) ?: return null
        val monthStr = m.groupValues[1]
        val dayStr = m.groupValues[2]
        val month = Month.of(parseMonth(monthStr))
        val day = dayStr.toIntOrNull() ?: return null
        val year = now.year
        // If date already passed this year, roll to next
        val tentative = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return null
        val time = time12.find(text)?.value?.let { parseTime12(it) }
            ?: time24.find(text)?.value?.let { parseTime24(it) }
            ?: config.defaultTime
        var zdt = ZonedDateTime.of(LocalDate.of(year, month, day), time, now.zone)
        if (zdt.isBefore(now)) zdt = zdt.plusYears(1)
        return zdt
    }

    private fun parseMonth(token: String): Int {
        return when (token.lowercase(Locale.getDefault()).take(3)) {
            "jan" -> 1
            "feb" -> 2
            "mar" -> 3
            "apr" -> 4
            "may" -> 5
            "jun" -> 6
            "jul" -> 7
            "aug" -> 8
            "sep" -> 9
            "oct" -> 10
            "nov" -> 11
            "dec" -> 12
            else -> 1
        }
    }

    private fun parseTime12(value: String): LocalTime? = try {
        val normalized = value.lowercase(Locale.getDefault()).replace(" ", "")
        val ampm = if (normalized.endsWith("am") || normalized.endsWith("pm")) normalized.takeLast(2) else "am"
        val hmp = normalized.removeSuffix("am").removeSuffix("pm").split(":")
        var hour = hmp[0].toInt()
        val minute = if (hmp.size > 1) hmp[1].toInt() else 0
        if (ampm == "pm" && hour != 12) hour += 12
        if (ampm == "am" && hour == 12) hour = 0
        LocalTime.of(hour, minute)
    } catch (_: Exception) { null }

    private fun parseTime24(value: String): LocalTime? = try {
        val parts = value.split(":")
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) { null }

    private fun parseQuarterPast(match: MatchResult): LocalTime? = try {
        val hour12 = match.groupValues[1].toInt()
        val ampm = match.groupValues.getOrNull(2)?.lowercase(Locale.getDefault())
        var hour = hour12
        if (ampm == "pm" && hour != 12) hour += 12
        if (ampm == "am" && hour == 12) hour = 0
        LocalTime.of(hour, 15)
    } catch (_: Exception) { null }

    private fun ZonedDateTime.adjustIfPast(now: ZonedDateTime): ZonedDateTime =
        if (this.isBefore(now)) this.plusDays(1) else this

    // Debug helper
    fun parseDebug(text: String, nowClock: Clock = Clock.systemDefaultZone()): ReminderCandidate? = parse(text, nowClock, 0.0f)
}