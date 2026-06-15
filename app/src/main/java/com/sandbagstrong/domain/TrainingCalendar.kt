package com.sandbagstrong.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

const val TRAINING_DAY_SYMBOL_NONE = "-"
const val TRAINING_DAY_SYMBOL_SANDBAG = "S"
const val TRAINING_DAY_SYMBOL_MICRO = "M"
const val TRAINING_DAY_SYMBOL_BOTH = "S+M"
const val TRAINING_DAY_SYMBOL_STOPPED = "M!"

data class CalendarSandbagSession(
    val id: String,
    val dateMillis: Long,
    val workoutType: String,
    val feelRating: String,
    val durationSeconds: Int
)

data class CalendarMicroRoundSession(
    val id: String,
    val committedAtMillis: Long,
    val startedAtMillis: Long?,
    val finishedAtMillis: Long?,
    val completedDurationSeconds: Int,
    val kneeStatusBefore: String,
    val kneeStatusAfter: String?,
    val resistanceLevel: String?,
    val intentResult: String?,
    val postSessionState: String?,
    val note: String?,
    val completionStatus: String
)

data class TrainingDay(
    val date: LocalDate,
    val sandbagSessions: List<CalendarSandbagSession> = emptyList(),
    val microRoundSessions: List<CalendarMicroRoundSession> = emptyList()
) {
    val completedMicroRounds: List<CalendarMicroRoundSession> =
        microRoundSessions.filter { it.completionStatus == MicroRoundStatus.COMPLETED.name }

    val stoppedEarlyMicroRounds: List<CalendarMicroRoundSession> =
        microRoundSessions.filter { it.completionStatus == MicroRoundStatus.STOPPED_EARLY.name }

    val hasSandbag: Boolean = sandbagSessions.isNotEmpty()
    val hasCompletedMicroRound: Boolean = completedMicroRounds.isNotEmpty()
    val hasStoppedEarlyMicroRound: Boolean = stoppedEarlyMicroRounds.isNotEmpty()
    val hasActiveMicroRound: Boolean = microRoundSessions.any { it.countsAsActive() }
    val isActive: Boolean = hasSandbag || hasActiveMicroRound

    val symbol: String = when {
        hasSandbag && hasActiveMicroRound -> TRAINING_DAY_SYMBOL_BOTH
        hasSandbag -> TRAINING_DAY_SYMBOL_SANDBAG
        hasStoppedEarlyMicroRound -> TRAINING_DAY_SYMBOL_STOPPED
        hasCompletedMicroRound -> TRAINING_DAY_SYMBOL_MICRO
        else -> TRAINING_DAY_SYMBOL_NONE
    }

    val label: String = when {
        hasSandbag && hasActiveMicroRound -> "Both"
        hasSandbag -> "Sandbag"
        hasStoppedEarlyMicroRound -> "Stopped early"
        hasCompletedMicroRound -> "Micro Round"
        else -> "No training"
    }
}

data class TrainingCalendarMonth(
    val month: YearMonth,
    val leadingBlankDays: Int,
    val days: List<TrainingDay>
)

object TrainingCalendar {
    fun buildMonth(
        month: YearMonth,
        sandbagSessions: List<CalendarSandbagSession>,
        microRoundSessions: List<CalendarMicroRoundSession>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TrainingCalendarMonth {
        val workoutsByDate = sandbagSessions.groupBy { it.dateMillis.toLocalDate(zoneId) }
        val microsByDate = microRoundSessions
            .filter { it.countsAsTrainingDayDetail() }
            .groupBy { it.activityMillis().toLocalDate(zoneId) }
        val days = (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            TrainingDay(
                date = date,
                sandbagSessions = workoutsByDate[date].orEmpty(),
                microRoundSessions = microsByDate[date].orEmpty()
            )
        }
        return TrainingCalendarMonth(
            month = month,
            leadingBlankDays = month.atDay(1).dayOfWeek.value % 7,
            days = days
        )
    }

    fun countActiveDays(
        daysBack: Int,
        today: LocalDate,
        sandbagSessions: List<CalendarSandbagSession>,
        microRoundSessions: List<CalendarMicroRoundSession>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val firstDay = today.minusDays((daysBack - 1).toLong())
        val activeDates = mutableSetOf<LocalDate>()
        sandbagSessions.forEach { session ->
            val date = session.dateMillis.toLocalDate(zoneId)
            if (!date.isBefore(firstDay) && !date.isAfter(today)) {
                activeDates += date
            }
        }
        microRoundSessions.filter { it.countsAsActive() }.forEach { session ->
            val date = session.activityMillis().toLocalDate(zoneId)
            if (!date.isBefore(firstDay) && !date.isAfter(today)) {
                activeDates += date
            }
        }
        return activeDates.size
    }
}

fun CalendarMicroRoundSession.countsAsActive(): Boolean =
    startedAtMillis != null && (
        completionStatus == MicroRoundStatus.COMPLETED.name ||
            completionStatus == MicroRoundStatus.STOPPED_EARLY.name
        )

fun CalendarMicroRoundSession.countsAsTrainingDayDetail(): Boolean =
    countsAsActive()

fun CalendarMicroRoundSession.activityMillis(): Long =
    finishedAtMillis ?: startedAtMillis ?: committedAtMillis

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
