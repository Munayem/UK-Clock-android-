package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun verify_london_timezone_identifier() {
        assertEquals("Europe/London", ClockTimeHelper.LONDON_ZONE_ID.id)
    }

    @Test
    fun verify_clock_hand_mathematics() {
        // At 10:25:
        val hour = 10
        val minute = 25
        val expectedHourAngle = (hour % 12 + minute / 60.0f) * 30.0f
        val expectedMinuteAngle = minute * 6.0f

        // 10 + 25/60 = 10.416667 -> * 30 = 312.5 degrees
        assertEquals(312.5f, expectedHourAngle, 0.01f)
        // 25 * 6 = 150.0 degrees
        assertEquals(150.0f, expectedMinuteAngle, 0.01f)

        // At 12:00 (hour hand points straight up at 0 degrees):
        val hour12Angle = (12 % 12 + 0 / 60.0f) * 30.0f
        assertEquals(0.0f, hour12Angle, 0.01f)

        // At 6:30 (halfway between 6 and 7):
        val halfAngle = (6 % 12 + 30 / 60.0f) * 30.0f
        assertEquals(195.0f, halfAngle, 0.01f)
    }

    @Test
    fun verify_ampm_and_12hour_conversion() {
        val zone = ZoneId.of("Europe/London")

        // 21:30 should be 9:30 PM
        val time2130 = ZonedDateTime.of(2026, 9, 12, 21, 30, 0, 0, zone)
        val isAm2130 = time2130.get(ChronoField.AMPM_OF_DAY) == 0
        val hour12_2130 = when (val h = time2130.hour % 12) {
            0 -> 12
            else -> h
        }
        assertEquals(false, isAm2130)
        assertEquals(9, hour12_2130)

        // 09:30 should be 9:30 AM
        val time0930 = ZonedDateTime.of(2026, 9, 12, 9, 30, 0, 0, zone)
        val isAm0930 = time0930.get(ChronoField.AMPM_OF_DAY) == 0
        val hour12_0930 = when (val h = time0930.hour % 12) {
            0 -> 12
            else -> h
        }
        assertEquals(true, isAm0930)
        assertEquals(9, hour12_0930)

        // 00:00 (Midnight) should be 12:00 AM
        val midnight = ZonedDateTime.of(2026, 9, 12, 0, 0, 0, 0, zone)
        val isAmMidnight = midnight.get(ChronoField.AMPM_OF_DAY) == 0
        val hour12_midnight = when (val h = midnight.hour % 12) {
            0 -> 12
            else -> h
        }
        assertEquals(true, isAmMidnight)
        assertEquals(12, hour12_midnight)

        // 12:00 (Noon) should be 12:00 PM
        val noon = ZonedDateTime.of(2026, 9, 12, 12, 0, 0, 0, zone)
        val isAmNoon = noon.get(ChronoField.AMPM_OF_DAY) == 0
        val hour12_noon = when (val h = noon.hour % 12) {
            0 -> 12
            else -> h
        }
        assertEquals(false, isAmNoon)
        assertEquals(12, hour12_noon)
    }

    @Test
    fun verify_dst_detection() {
        val zone = ZoneId.of("Europe/London")

        // Summer time: July 1st (BST is UTC+1)
        val summerDate = ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, zone)
        val isSummerDst = zone.rules.isDaylightSavings(summerDate.toInstant())
        assertTrue("July should be British Summer Time (BST)", isSummerDst)
        assertEquals(3600, summerDate.offset.totalSeconds) // +01:00

        // Winter time: January 1st (GMT is UTC+0)
        val winterDate = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, zone)
        val isWinterDst = zone.rules.isDaylightSavings(winterDate.toInstant())
        assertTrue("January should be Greenwich Mean Time (GMT)", !isWinterDst)
        assertEquals(0, winterDate.offset.totalSeconds) // +00:00
    }

    @Test
    fun verify_day_night_sun_moon_cycles() {
        val zone = ZoneId.of("Europe/London")

        // 06:00 morning -> Day (Sun)
        val morning6am = ZonedDateTime.of(2026, 9, 12, 6, 0, 0, 0, zone)
        assertTrue("06:00 is Day (Sun)", morning6am.hour in 6..17)

        // 12:00 noon -> Day (Sun)
        val noon = ZonedDateTime.of(2026, 9, 12, 12, 0, 0, 0, zone)
        assertTrue("12:00 noon is Day (Sun)", noon.hour in 6..17)

        // 17:59 afternoon -> Day (Sun)
        val afternoon559pm = ZonedDateTime.of(2026, 9, 12, 17, 59, 59, 0, zone)
        assertTrue("17:59 is Day (Sun)", afternoon559pm.hour in 6..17)

        // 18:00 evening -> Night (Moon)
        val evening6pm = ZonedDateTime.of(2026, 9, 12, 18, 0, 0, 0, zone)
        assertTrue("18:00 is Night (Moon)", evening6pm.hour !in 6..17)

        // 23:30 late night -> Night (Moon)
        val lateNight = ZonedDateTime.of(2026, 9, 12, 23, 30, 0, 0, zone)
        assertTrue("23:30 is Night (Moon)", lateNight.hour !in 6..17)

        // 00:00 midnight -> Night (Moon)
        val midnight = ZonedDateTime.of(2026, 9, 12, 0, 0, 0, 0, zone)
        assertTrue("00:00 midnight is Night (Moon)", midnight.hour !in 6..17)

        // 05:59 dawn before 6am -> Night (Moon)
        val dawn559am = ZonedDateTime.of(2026, 9, 12, 5, 59, 59, 0, zone)
        assertTrue("05:59 is Night (Moon)", dawn559am.hour !in 6..17)
    }

    @Test
    fun verify_next_minute_calculation() {
        val nextMillis = ClockTimeHelper.getNextMinuteMillis()
        val now = System.currentTimeMillis()

        assertTrue(nextMillis > now)
        assertEquals(0L, nextMillis % 60000L)
    }
}
