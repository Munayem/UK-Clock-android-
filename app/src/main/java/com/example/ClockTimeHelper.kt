package com.example

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Encapsulates all Europe/London timezone logic, mathematics, and formatting.
 * Guaranteed to operate strictly on the Europe/London timezone regardless of device locale.
 */
object ClockTimeHelper {

    val LONDON_ZONE_ID: ZoneId = ZoneId.of("Europe/London")

    private val TIME_FORMAT_12 = DateTimeFormatter.ofPattern("h:mm", Locale.UK)
    private val TIME_FORMAT_12_SEC = DateTimeFormatter.ofPattern("h:mm:ss", Locale.UK)
    private val DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.UK)
    private val DATE_FORMAT_LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK)

    data class LondonTimeInfo(
        val zonedDateTime: ZonedDateTime,
        val hour12: Int,
        val minute: Int,
        val second: Int,
        val millisecond: Int,
        val isAm: Boolean,
        val amPmString: String,
        val isDayTime: Boolean,
        val dayNightSymbol: String,
        val dayNightLabel: String,
        val hourAngle: Float,
        val minuteAngle: Float,
        val secondAngle: Float,
        val isDst: Boolean,
        val tzCode: String,
        val tzFullName: String,
        val offsetString: String,
        val digitalTime12: String,
        val digitalTime12WithSec: String,
        val dateShort: String,
        val dateLong: String,
        val accessibilityDescription: String
    )

    fun getCurrentLondonTime(): LondonTimeInfo {
        val now = ZonedDateTime.now(LONDON_ZONE_ID)
        val instant = now.toInstant()

        val hour24 = now.hour
        val minute = now.minute
        val second = now.second
        val nano = now.nano
        val milli = nano / 1_000_000

        // 12-hour AM/PM calculation
        val isAm = now.get(ChronoField.AMPM_OF_DAY) == 0
        val amPmString = if (isAm) "AM" else "PM"
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }

        // Day vs Night (Day: 06:00 to 17:59 -> Sun; Night: 18:00 to 05:59 -> Moon)
        val isDayTime = hour24 in 6..17
        val dayNightSymbol = if (isDayTime) "☀️" else "🌙"
        val dayNightLabel = if (isDayTime) "Day" else "Night"

        // Exact mathematical angles in degrees (0 degrees = 12 o'clock / top)
        // Hour hand: (hour % 12 + minute / 60.0) * 30.0
        val hourAngle = (hour12 % 12 + (minute / 60.0f) + (second / 3600.0f)) * 30.0f
        // Minute hand: (minute + second / 60.0) * 6.0
        val minuteAngle = (minute + (second / 60.0f)) * 6.0f
        // Second hand: (second + milli / 1000.0) * 6.0
        val secondAngle = (second + (milli / 1000.0f)) * 6.0f

        // Daylight Saving Time determination dynamically from zone rules
        val isDst = LONDON_ZONE_ID.rules.isDaylightSavings(instant)
        val tzCode = if (isDst) "BST" else "GMT"
        val tzFullName = if (isDst) "British Summer Time" else "Greenwich Mean Time"

        val offsetTotalSeconds = now.offset.totalSeconds
        val offsetHours = offsetTotalSeconds / 3600
        val offsetMinutes = Math.abs((offsetTotalSeconds % 3600) / 60)
        val offsetString = if (offsetHours >= 0) {
            String.format(Locale.UK, "UTC+%d", offsetHours)
        } else {
            String.format(Locale.UK, "UTC%d", offsetHours)
        }

        val digitalTime12 = now.format(TIME_FORMAT_12)
        val digitalTime12WithSec = now.format(TIME_FORMAT_12_SEC)
        val dateShort = now.format(DATE_FORMAT_SHORT)
        val dateLong = now.format(DATE_FORMAT_LONG)

        val accessibilityDesc = "UK London time, $digitalTime12 $amPmString ($dayNightLabel), $tzFullName"

        return LondonTimeInfo(
            zonedDateTime = now,
            hour12 = hour12,
            minute = minute,
            second = second,
            millisecond = milli,
            isAm = isAm,
            amPmString = amPmString,
            isDayTime = isDayTime,
            dayNightSymbol = dayNightSymbol,
            dayNightLabel = dayNightLabel,
            hourAngle = hourAngle,
            minuteAngle = minuteAngle,
            secondAngle = secondAngle,
            isDst = isDst,
            tzCode = tzCode,
            tzFullName = tzFullName,
            offsetString = offsetString,
            digitalTime12 = digitalTime12,
            digitalTime12WithSec = digitalTime12WithSec,
            dateShort = dateShort,
            dateLong = dateLong,
            accessibilityDescription = accessibilityDesc
        )
    }

    /**
     * Calculates the exact epoch millisecond for the upcoming minute boundary (00 seconds).
     */
    fun getNextMinuteMillis(): Long {
        val now = System.currentTimeMillis()
        return ((now / 60000L) + 1L) * 60000L
    }
}
