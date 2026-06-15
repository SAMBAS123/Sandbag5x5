package com.sandbagstrong

import com.sandbagstrong.domain.CalendarMicroRoundSession
import com.sandbagstrong.domain.CalendarSandbagSession
import com.sandbagstrong.domain.KneeStatusBefore
import com.sandbagstrong.domain.MicroRoundProgram
import com.sandbagstrong.domain.MicroRoundStatus
import com.sandbagstrong.domain.MicroRoundVariant
import com.sandbagstrong.domain.TRAINING_DAY_SYMBOL_BOTH
import com.sandbagstrong.domain.TRAINING_DAY_SYMBOL_MICRO
import com.sandbagstrong.domain.TRAINING_DAY_SYMBOL_NONE
import com.sandbagstrong.domain.TRAINING_DAY_SYMBOL_SANDBAG
import com.sandbagstrong.domain.TRAINING_DAY_SYMBOL_STOPPED
import com.sandbagstrong.domain.TrainingCalendar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingCalendarTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun sandbagOnlyDayIsMarkedActive() {
        val day = LocalDate.of(2026, 6, 10)
        val calendar = TrainingCalendar.buildMonth(
            YearMonth.of(2026, 6),
            sandbagSessions = listOf(sandbag(day)),
            microRoundSessions = emptyList(),
            zoneId = zone
        )

        val trainingDay = calendar.days.first { it.date == day }
        assertTrue(trainingDay.isActive)
        assertEquals(TRAINING_DAY_SYMBOL_SANDBAG, trainingDay.symbol)
        assertEquals("Sandbag", trainingDay.label)
    }

    @Test
    fun microRoundOnlyDayIsMarkedActive() {
        val day = LocalDate.of(2026, 6, 10)
        val calendar = TrainingCalendar.buildMonth(
            YearMonth.of(2026, 6),
            sandbagSessions = emptyList(),
            microRoundSessions = listOf(micro(day, MicroRoundStatus.COMPLETED)),
            zoneId = zone
        )

        val trainingDay = calendar.days.first { it.date == day }
        assertTrue(trainingDay.isActive)
        assertEquals(TRAINING_DAY_SYMBOL_MICRO, trainingDay.symbol)
        assertEquals("Micro Round", trainingDay.label)
    }

    @Test
    fun bothActivitiesOnSameDayAreRepresentedCorrectly() {
        val day = LocalDate.of(2026, 6, 10)
        val calendar = TrainingCalendar.buildMonth(
            YearMonth.of(2026, 6),
            sandbagSessions = listOf(sandbag(day)),
            microRoundSessions = listOf(micro(day, MicroRoundStatus.COMPLETED)),
            zoneId = zone
        )

        val trainingDay = calendar.days.first { it.date == day }
        assertTrue(trainingDay.isActive)
        assertTrue(trainingDay.hasSandbag)
        assertTrue(trainingDay.hasActiveMicroRound)
        assertEquals(TRAINING_DAY_SYMBOL_BOTH, trainingDay.symbol)
        assertEquals("Both", trainingDay.label)
    }

    @Test
    fun stoppedEarlyMicroRoundCountsAsActive() {
        val day = LocalDate.of(2026, 6, 10)
        val calendar = TrainingCalendar.buildMonth(
            YearMonth.of(2026, 6),
            sandbagSessions = emptyList(),
            microRoundSessions = listOf(micro(day, MicroRoundStatus.STOPPED_EARLY, duration = 45)),
            zoneId = zone
        )

        val trainingDay = calendar.days.first { it.date == day }
        assertTrue(trainingDay.isActive)
        assertTrue(trainingDay.hasStoppedEarlyMicroRound)
        assertEquals(TRAINING_DAY_SYMBOL_STOPPED, trainingDay.symbol)
        assertEquals("Stopped early", trainingDay.label)
    }

    @Test
    fun committedOnlyMicroRoundDoesNotCountAsActive() {
        val day = LocalDate.of(2026, 6, 10)
        val calendar = TrainingCalendar.buildMonth(
            YearMonth.of(2026, 6),
            sandbagSessions = emptyList(),
            microRoundSessions = listOf(micro(day, MicroRoundStatus.COMMITTED_ONLY, started = false)),
            zoneId = zone
        )

        val trainingDay = calendar.days.first { it.date == day }
        assertFalse(trainingDay.isActive)
        assertEquals(TRAINING_DAY_SYMBOL_NONE, trainingDay.symbol)
        assertEquals("No training", trainingDay.label)
        assertTrue(trainingDay.microRoundSessions.isEmpty())
    }

    @Test
    fun lastSevenDayCountIsCorrect() {
        val today = LocalDate.of(2026, 6, 15)
        val count = TrainingCalendar.countActiveDays(
            daysBack = 7,
            today = today,
            sandbagSessions = listOf(sandbag(today), sandbag(today.minusDays(8))),
            microRoundSessions = listOf(
                micro(today.minusDays(1), MicroRoundStatus.COMPLETED),
                micro(today.minusDays(6), MicroRoundStatus.STOPPED_EARLY),
                micro(today.minusDays(2), MicroRoundStatus.COMMITTED_ONLY, started = false)
            ),
            zoneId = zone
        )

        assertEquals(3, count)
    }

    @Test
    fun lastThirtyDayCountIsCorrect() {
        val today = LocalDate.of(2026, 6, 15)
        val count = TrainingCalendar.countActiveDays(
            daysBack = 30,
            today = today,
            sandbagSessions = listOf(sandbag(today), sandbag(today.minusDays(29)), sandbag(today.minusDays(30))),
            microRoundSessions = listOf(
                micro(today.minusDays(10), MicroRoundStatus.COMPLETED),
                micro(today.minusDays(20), MicroRoundStatus.STOPPED_EARLY),
                micro(today.minusDays(5), MicroRoundStatus.COMMITTED_ONLY, started = false)
            ),
            zoneId = zone
        )

        assertEquals(4, count)
    }

    @Test
    fun monthBoundariesAndLocalDatesAreHandledCorrectly() {
        val utcMillisThatIsMayLocal = LocalDateTime.of(2026, 6, 1, 2, 30)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val mayCalendar = TrainingCalendar.buildMonth(
            month = YearMonth.of(2026, 5),
            sandbagSessions = listOf(sandbag(utcMillisThatIsMayLocal)),
            microRoundSessions = emptyList(),
            zoneId = zone
        )
        val juneCalendar = TrainingCalendar.buildMonth(
            month = YearMonth.of(2026, 6),
            sandbagSessions = listOf(sandbag(utcMillisThatIsMayLocal)),
            microRoundSessions = emptyList(),
            zoneId = zone
        )

        assertTrue(mayCalendar.days.first { it.date == LocalDate.of(2026, 5, 31) }.isActive)
        assertFalse(juneCalendar.days.first { it.date == LocalDate.of(2026, 6, 1) }.isActive)
    }

    @Test
    fun existingMicroRoundFlowRulesRemainUnchanged() {
        assertEquals(MicroRoundVariant.STABLE, MicroRoundProgram.variantFor(KneeStatusBefore.STABLE))
        assertEquals(MicroRoundVariant.UNCERTAIN, MicroRoundProgram.variantFor(KneeStatusBefore.UNCERTAIN))
        assertFalse(MicroRoundProgram.canLaunchRound(KneeStatusBefore.PAINFUL))
    }

    private fun sandbag(day: LocalDate): CalendarSandbagSession =
        sandbag(day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli())

    private fun sandbag(dateMillis: Long): CalendarSandbagSession =
        CalendarSandbagSession(
            id = "sandbag-$dateMillis",
            dateMillis = dateMillis,
            workoutType = "A",
            feelRating = "SOLID",
            durationSeconds = 600
        )

    private fun micro(
        day: LocalDate,
        status: MicroRoundStatus,
        started: Boolean = true,
        duration: Int = 180
    ): CalendarMicroRoundSession {
        val committed = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val startedAt = if (started) committed + 1_000L else null
        val finished = if (started) committed + duration * 1_000L else null
        return CalendarMicroRoundSession(
            id = "micro-$committed-$status",
            committedAtMillis = committed,
            startedAtMillis = startedAt,
            finishedAtMillis = finished,
            completedDurationSeconds = duration,
            kneeStatusBefore = "STABLE",
            kneeStatusAfter = "FINE",
            resistanceLevel = "SOME",
            intentResult = "TRAINED_ASSIGNMENT",
            postSessionState = "CALMER",
            note = null,
            completionStatus = status.name
        )
    }
}
