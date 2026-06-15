package com.sandbagstrong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandbagstrong.data.db.MicroRoundSessionEntity
import com.sandbagstrong.data.db.WorkoutSessionEntity
import com.sandbagstrong.domain.CalendarMicroRoundSession
import com.sandbagstrong.domain.CalendarSandbagSession
import com.sandbagstrong.domain.FeelRating
import com.sandbagstrong.domain.IntentResult
import com.sandbagstrong.domain.KneeStatusAfter
import com.sandbagstrong.domain.KneeStatusBefore
import com.sandbagstrong.domain.MicroRoundProgram
import com.sandbagstrong.domain.MicroRoundStatus
import com.sandbagstrong.domain.PostSessionState
import com.sandbagstrong.domain.ResistanceLevel
import com.sandbagstrong.domain.TRAINING_DAY_SYMBOL_NONE
import com.sandbagstrong.domain.TrainingCalendar
import com.sandbagstrong.domain.TrainingCalendarMonth
import com.sandbagstrong.domain.TrainingDay
import com.sandbagstrong.ui.WorkoutViewModel
import com.sandbagstrong.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.historyFlow.collectAsState()
    val microRounds by viewModel.microRoundHistoryFlow.collectAsState()
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now(zoneId)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }

    val calendarWorkouts = sessions.map { it.toCalendarSession() }
    val calendarMicros = microRounds.map { it.toCalendarSession() }
    val calendarMonth = TrainingCalendar.buildMonth(
        month = displayedMonth,
        sandbagSessions = calendarWorkouts,
        microRoundSessions = calendarMicros,
        zoneId = zoneId
    )
    val selectedDay = selectedDate?.let { date ->
        if (YearMonth.from(date) == displayedMonth) {
            calendarMonth.days.find { it.date == date }
        } else {
            null
        }
    }
    val active7 = TrainingCalendar.countActiveDays(7, today, calendarWorkouts, calendarMicros, zoneId)
    val active30 = TrainingCalendar.countActiveDays(30, today, calendarWorkouts, calendarMicros, zoneId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "← BACK",
                color = AppColors.accent,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable { onBack() }
            )
            Text(
                text = "HISTORY",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                TrainingCalendarSection(
                    calendarMonth = calendarMonth,
                    selectedDate = selectedDate,
                    active7 = active7,
                    active30 = active30,
                    onPreviousMonth = {
                        displayedMonth = displayedMonth.minusMonths(1)
                        selectedDate = null
                    },
                    onNextMonth = {
                        displayedMonth = displayedMonth.plusMonths(1)
                        selectedDate = null
                    },
                    onSelectDay = { selectedDate = it }
                )
            }

            item {
                DayDetailsSection(
                    selectedDay = selectedDay,
                    selectedDate = selectedDate
                )
            }

            if (sessions.isEmpty() && microRounds.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No history yet.", color = AppColors.muted, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Complete your first workout or micro round to see it here.",
                            color = AppColors.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (microRounds.isNotEmpty()) {
                item {
                    SectionLabel("MICRO ROUNDS")
                }
                items(microRounds) { microRound ->
                    MicroRoundHistoryRow(microRound)
                }
            }

            if (sessions.isNotEmpty()) {
                item {
                    SectionLabel("WORKOUTS")
                }
                items(sessions) { session ->
                    WorkoutHistoryRow(session)
                }
            }
        }
    }
}

@Composable
private fun TrainingCalendarSection(
    calendarMonth: TrainingCalendarMonth,
    selectedDate: LocalDate?,
    active7: Int,
    active30: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(14.dp)
    ) {
        Text(
            text = "Training Calendar",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))
        Text("Active on $active7 of the last 7 days", color = AppColors.muted, fontSize = 14.sp)
        Text("Active on $active30 of the last 30 days", color = AppColors.muted, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onPreviousMonth, modifier = Modifier.weight(1f)) {
                Text("PREV")
            }
            Text(
                text = calendarMonth.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(2f)
            )
            Button(onClick = onNextMonth, modifier = Modifier.weight(1f)) {
                Text("NEXT")
            }
        }

        Spacer(Modifier.height(12.dp))
        CalendarLegend()
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth()) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { label ->
                Text(
                    text = label,
                    color = AppColors.muted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val cells = List(calendarMonth.leadingBlankDays) { null } + calendarMonth.days.map { it }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        selected = day?.date == selectedDate,
                        onSelectDay = onSelectDay,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(7 - week.size) {
                    CalendarDayCell(
                        day = null,
                        selected = false,
                        onSelectDay = onSelectDay,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("S Sandbag", color = AppColors.muted, fontSize = 12.sp)
        Text("M Micro Round", color = AppColors.muted, fontSize = 12.sp)
        Text("S+M Both", color = AppColors.muted, fontSize = 12.sp)
        Text("M! Stopped early", color = AppColors.muted, fontSize = 12.sp)
        Text("- No training", color = AppColors.muted, fontSize = 12.sp)
    }
}

@Composable
private fun CalendarDayCell(
    day: TrainingDay?,
    selected: Boolean,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        selected -> AppColors.accent
        day?.isActive == true -> Color.White
        else -> AppColors.muted.copy(alpha = 0.35f)
    }
    val symbol = day?.symbol ?: ""
    val label = day?.label ?: ""

    Box(
        modifier = modifier
            .height(70.dp)
            .padding(3.dp)
            .border(1.dp, borderColor)
            .background(if (day?.isActive == true) Color(0xFF2A2A2A) else AppColors.bg)
            .clickable(enabled = day != null) { day?.let { onSelectDay(it.date) } }
            .padding(4.dp)
    ) {
        if (day != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = symbol,
                    color = if (symbol == TRAINING_DAY_SYMBOL_NONE) AppColors.muted else AppColors.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = label,
                    color = AppColors.muted,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun DayDetailsSection(
    selectedDay: TrainingDay?,
    selectedDate: LocalDate?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(16.dp)
    ) {
        Text(
            text = selectedDate?.let { "Day Details ${it.format(DateTimeFormatter.ofPattern("MMM d"))}" }
                ?: "Day Details",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        if (selectedDay == null || !selectedDay.isActive) {
            Text("No training recorded for this day.", color = AppColors.muted, fontSize = 14.sp)
            return@Column
        }
        selectedDay.sandbagSessions.forEach { session ->
            CalendarSandbagDetail(session)
            Spacer(Modifier.height(8.dp))
        }
        selectedDay.microRoundSessions.forEach { session ->
            CalendarMicroRoundDetail(session)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CalendarSandbagDetail(session: CalendarSandbagSession) {
    val feel = FeelRating.fromString(session.feelRating)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.muted.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Text("Sandbag", color = AppColors.accent, fontWeight = FontWeight.Bold)
        Text("Workout ${session.workoutType}", color = Color.White)
        Text("Feel ${feel.display}", color = AppColors.muted, fontSize = 13.sp)
        if (session.durationSeconds > 0) {
            Text("Duration ${formatHistoryDuration(session.durationSeconds)}", color = AppColors.muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CalendarMicroRoundDetail(session: CalendarMicroRoundSession) {
    val status = safeDisplay(session.completionStatus) { MicroRoundStatus.valueOf(it).display }
    val kneeBefore = safeDisplay(session.kneeStatusBefore) { KneeStatusBefore.valueOf(it).display }
    val kneeAfter = session.kneeStatusAfter?.let { safeDisplay(it) { raw -> KneeStatusAfter.valueOf(raw).display } } ?: "-"
    val resistance = session.resistanceLevel?.let { safeDisplay(it) { raw -> ResistanceLevel.valueOf(raw).display } } ?: "-"
    val intent = session.intentResult?.let { safeDisplay(it) { raw -> IntentResult.valueOf(raw).display } } ?: "-"
    val state = session.postSessionState?.let { safeDisplay(it) { raw -> PostSessionState.valueOf(raw).display } } ?: "-"
    val latency = MicroRoundProgram.startLatencyMillis(session.committedAtMillis, session.startedAtMillis)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.muted.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Text("Micro Round", color = AppColors.accent, fontWeight = FontWeight.Bold)
        Text("Status $status", color = Color.White)
        Text("Duration ${formatHistoryDuration(session.completedDurationSeconds)}", color = AppColors.muted, fontSize = 13.sp)
        Text("Start latency ${formatLatency(latency)}", color = AppColors.muted, fontSize = 13.sp)
        Text("Knee $kneeBefore to $kneeAfter", color = AppColors.muted, fontSize = 13.sp)
        Text("Resistance $resistance", color = AppColors.muted, fontSize = 13.sp)
        Text("Intent $intent", color = AppColors.muted, fontSize = 13.sp)
        Text("State $state", color = AppColors.muted, fontSize = 13.sp)
        session.note?.let { note ->
            Text("Note $note", color = AppColors.muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AppColors.muted,
        fontSize = 13.sp,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun WorkoutHistoryRow(session: WorkoutSessionEntity) {
    val date = SimpleDateFormat("EEE, MMM d  •  HH:mm", Locale.getDefault())
        .format(Date(session.dateMillis))
    val feel = FeelRating.fromString(session.feelRating)
    val feelColor = when (feel) {
        FeelRating.EASY -> AppColors.completeGreen
        FeelRating.SOLID -> AppColors.accent
        FeelRating.BRUTAL -> Color(0xFFFF6D00)
        FeelRating.FAILED -> Color(0xFFE53935)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(16.dp)
    ) {
        Row {
            Text(
                text = "WORKOUT ${session.workoutType}",
                color = AppColors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = feel.display.uppercase(),
                color = feelColor,
                fontWeight = FontWeight.Bold
            )
        }
        Text(date, color = AppColors.muted, fontSize = 13.sp)
        if (session.durationSeconds > 0) {
            Text(formatHistoryDuration(session.durationSeconds), color = AppColors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MicroRoundHistoryRow(session: MicroRoundSessionEntity) {
    val date = SimpleDateFormat("EEE, MMM d  •  HH:mm", Locale.getDefault())
        .format(Date(session.committedAtMillis))
    val kneeBefore = KneeStatusBefore.valueOf(session.kneeStatusBefore).display
    val kneeAfter = session.kneeStatusAfter?.let { KneeStatusAfter.valueOf(it).display } ?: "-"
    val resistance = session.resistanceLevel?.let { ResistanceLevel.valueOf(it).display } ?: "-"
    val stateAfter = session.postSessionState?.let { PostSessionState.valueOf(it).display } ?: "-"
    val status = MicroRoundStatus.valueOf(session.completionStatus).display
    val latency = MicroRoundProgram.startLatencyMillis(
        session.committedAtMillis,
        session.startedAtMillis
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(16.dp)
    ) {
        Row {
            Text(
                text = "MICRO ROUND",
                color = AppColors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = status.uppercase(),
                color = if (session.completionStatus == MicroRoundStatus.STOPPED_EARLY.name) {
                    Color(0xFFFF6D00)
                } else {
                    AppColors.completeGreen
                },
                fontWeight = FontWeight.Bold
            )
        }
        Text(date, color = AppColors.muted, fontSize = 13.sp)
        Text(
            text = "Duration ${formatHistoryDuration(session.completedDurationSeconds)}  •  latency ${formatLatency(latency)}",
            color = AppColors.muted,
            fontSize = 12.sp
        )
        Text(
            text = "Knee $kneeBefore to $kneeAfter  •  $resistance  •  $stateAfter",
            color = AppColors.muted,
            fontSize = 12.sp
        )
    }
}

private fun WorkoutSessionEntity.toCalendarSession(): CalendarSandbagSession =
    CalendarSandbagSession(
        id = id,
        dateMillis = dateMillis,
        workoutType = workoutType,
        feelRating = feelRating,
        durationSeconds = durationSeconds
    )

private fun MicroRoundSessionEntity.toCalendarSession(): CalendarMicroRoundSession =
    CalendarMicroRoundSession(
        id = id,
        committedAtMillis = committedAtMillis,
        startedAtMillis = startedAtMillis,
        finishedAtMillis = finishedAtMillis,
        completedDurationSeconds = completedDurationSeconds,
        kneeStatusBefore = kneeStatusBefore,
        kneeStatusAfter = kneeStatusAfter,
        resistanceLevel = resistanceLevel,
        intentResult = intentResult,
        postSessionState = postSessionState,
        note = note,
        completionStatus = completionStatus
    )

private fun formatHistoryDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "${mins}m ${secs}s"
}

private fun formatLatency(latencyMillis: Long?): String =
    latencyMillis?.let { "${it / 1000}s" } ?: "-"

private fun safeDisplay(value: String, block: (String) -> String): String =
    runCatching { block(value) }.getOrElse { value.lowercase().replace('_', ' ') }
