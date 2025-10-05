package com.ainotebuddy.app.ai

import org.junit.Test
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ReminderNluTest {

    private fun fixedClock(epochMillis: Long): Clock = Clock.fixed(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())

    @Test
    fun parses_simple_time_today() {
        // Fixed time: 2025-08-20 12:00 local
        val base = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2025)
            set(java.util.Calendar.MONTH, java.util.Calendar.AUGUST)
            set(java.util.Calendar.DAY_OF_MONTH, 20)
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val clock = fixedClock(base)
        val candidate = ReminderNlu.parse("remind me at 5pm", nowClock = clock)
        assertThat(candidate).isNotNull()

        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = base
            set(java.util.Calendar.HOUR_OF_DAY, 17)
            set(java.util.Calendar.MINUTE, 0)
        }
        val expected = cal.timeInMillis
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun parses_tomorrow_morning() {
        val base = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.AUGUST, 20, 14, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val clock = fixedClock(base)
        val candidate = ReminderNlu.parse("set a reminder for tomorrow at 9am", nowClock = clock)
        assertThat(candidate).isNotNull()

        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = base
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
        }
        val expected = cal.timeInMillis
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun parses_relative_minutes() {
        val base = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.AUGUST, 20, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val clock = fixedClock(base)
        val candidate = ReminderNlu.parse("remind me in 30 minutes", nowClock = clock)
        assertThat(candidate).isNotNull()

        val expected = base + 30 * 60_000
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun parses_relative_hours() {
        val base = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.AUGUST, 20, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val clock = fixedClock(base)
        val candidate = ReminderNlu.parse("in 2 hours call mom", nowClock = clock)
        assertThat(candidate).isNotNull()

        val expected = base + 2 * 60 * 60_000
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun parses_weekday_time() {
        val base = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.AUGUST, 20, 12, 0, 0) // 2025-08-20 is Wednesday
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val clock = fixedClock(base)
        val candidate = ReminderNlu.parse("meeting on Friday at 3", nowClock = clock)
        assertThat(candidate).isNotNull()

        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = base
            while (get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.FRIDAY) add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 15)
            set(java.util.Calendar.MINUTE, 0)
        }
        val expected = cal.timeInMillis
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun parses_full_date_time() {
        val base = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.JULY, 15, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val clock = fixedClock(base)
        val candidate = ReminderNlu.parse("remind me August 21 6:30pm", nowClock = clock)
        assertThat(candidate).isNotNull()

        val cal = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.AUGUST, 21, 18, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val expected = cal.timeInMillis
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun returns_null_for_nonsense() {
        val candidate = ReminderNlu.parse("this is not a reminder")
        assertThat(candidate).isNull()
    }

    @Test
    fun returns_null_for_vague() {
        val candidate = ReminderNlu.parse("remind me later")
        assertThat(candidate).isNull()
    }

    // ---- Edge cases ----
    private fun fixedUTCClock(iso: String): Clock = Clock.fixed(Instant.parse(iso), ZoneId.of("UTC"))

    @Test
    fun noon_resolves_to_12_00() {
        val clock = fixedUTCClock("2025-08-20T08:00:00Z")
        val candidate = ReminderNlu.parse("remind me at noon", clock)
        assertThat(candidate).isNotNull()
        val expected = Instant.parse("2025-08-20T12:00:00Z").toEpochMilli()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun half_an_hour_resolves_to_30_min() {
        val clock = fixedUTCClock("2025-08-20T10:00:00Z")
        val candidate = ReminderNlu.parse("half an hour", clock)
        assertThat(candidate).isNotNull()
        val expected = Instant.parse("2025-08-20T10:30:00Z").toEpochMilli()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun in_half_an_hour_resolves_to_30_min() {
        val clock = fixedUTCClock("2025-08-20T10:00:00Z")
        val candidate = ReminderNlu.parse("in half an hour", clock)
        assertThat(candidate).isNotNull()
        val expected = Instant.parse("2025-08-20T10:30:00Z").toEpochMilli()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun quarter_past_3_resolves_to_15_15() {
        val clock = fixedUTCClock("2025-08-20T10:00:00Z")
        val candidate = ReminderNlu.parse("quarter past 3", clock)
        assertThat(candidate).isNotNull()
        val expected = Instant.parse("2025-08-20T15:15:00Z").toEpochMilli()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun in_3_days_and_in_2_weeks() {
        val clock = fixedUTCClock("2025-08-20T10:00:00Z")
        val c1 = ReminderNlu.parse("in 3 days", clock)
        val c2 = ReminderNlu.parse("in 2 weeks", clock)
        assertThat(c1).isNotNull()
        assertThat(c2).isNotNull()
        val expected1 = Instant.parse("2025-08-23T10:00:00Z").toEpochMilli()
        val expected2 = Instant.parse("2025-09-03T10:00:00Z").toEpochMilli()
        assertThat(c1!!.timeMillis).isIn((expected1 - 60_000)..(expected1 + 60_000))
        assertThat(c2!!.timeMillis).isIn((expected2 - 60_000)..(expected2 + 60_000))
    }

    @Test
    fun tonight_resolves_to_21_00_same_day() {
        val clock = fixedUTCClock("2025-08-20T10:00:00Z") // Wed 10:00 UTC
        val candidate = ReminderNlu.parse("remind me tonight", clock)
        assertThat(candidate).isNotNull()
        val expected = Instant.parse("2025-08-20T21:00:00Z").toEpochMilli()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun next_week_monday_resolves_to_09_00() {
        val clock = fixedUTCClock("2025-08-20T10:00:00Z") // Wed
        val candidate = ReminderNlu.parse("next week Monday", clock)
        assertThat(candidate).isNotNull()
        // Compute Monday next week from base date
        val base = Instant.parse("2025-08-20T10:00:00Z").atZone(ZoneId.of("UTC"))
        var date = base.toLocalDate()
        while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) date = date.plusDays(1)
        val expectedReal = date.plusWeeks(1).atTime(9, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expectedReal - 60_000)..(expectedReal + 60_000))
    }

    @Test
    fun midnight_rolls_to_next_day_00_00() {
        val clock = fixedUTCClock("2025-08-20T22:00:00Z")
        val candidate = ReminderNlu.parse("wake me up at midnight", clock)
        assertThat(candidate).isNotNull()
        val expected = Instant.parse("2025-08-21T00:00:00Z").toEpochMilli()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((expected - 60_000)..(expected + 60_000))
    }

    @Test
    fun zero_minutes_means_now() {
        val clock = fixedUTCClock("2025-08-20T10:00:00Z")
        val base = Instant.parse("2025-08-20T10:00:00Z").toEpochMilli()
        val candidate = ReminderNlu.parse("remind me in 0 minutes", clock)
        assertThat(candidate).isNotNull()
        val actual = candidate!!.timeMillis
        assertThat(actual).isIn((base - 60_000)..(base + 60_000))
    }
}