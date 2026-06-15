package com.sandbagstrong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.sandbagstrong.domain.IntentResult
import com.sandbagstrong.domain.KneeStatusAfter
import com.sandbagstrong.domain.KneeStatusBefore
import com.sandbagstrong.domain.MicroRoundProgram
import com.sandbagstrong.domain.MicroRoundVariant
import com.sandbagstrong.domain.PostSessionState
import com.sandbagstrong.domain.ResistanceLevel
import com.sandbagstrong.ui.MicroRoundViewModel
import com.sandbagstrong.ui.components.BigButton
import com.sandbagstrong.ui.theme.AppColors
import kotlinx.coroutines.delay

private enum class MicroRoundPhase {
    PRE_ROUND,
    COMMITTED,
    RUNNING,
    DEBRIEF
}

@Composable
fun MicroRoundScreen(
    viewModel: MicroRoundViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    var phase by remember { mutableStateOf(MicroRoundPhase.PRE_ROUND) }
    var showCorner by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var stoppedEarly by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bg)
            .verticalScroll(rememberScrollState())
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
                text = "MICRO ROUND",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))

        when (phase) {
            MicroRoundPhase.PRE_ROUND -> PreRoundContent(
                selectedKnee = viewModel.selectedKnee,
                showCorner = showCorner,
                onSelectKnee = viewModel::selectKnee,
                onNeedCorner = { showCorner = true },
                onCommit = {
                    viewModel.commit()
                    phase = MicroRoundPhase.COMMITTED
                }
            )

            MicroRoundPhase.COMMITTED -> CommittedContent(
                selectedKnee = viewModel.selectedKnee,
                canStart = viewModel.activeSession != null && viewModel.selectedKnee?.let {
                    MicroRoundProgram.canLaunchRound(it)
                } == true,
                onStart = {
                    viewModel.startRound { didStart ->
                        if (didStart) {
                            elapsedSeconds = 0
                            stoppedEarly = false
                            phase = MicroRoundPhase.RUNNING
                        }
                    }
                }
            )

            MicroRoundPhase.RUNNING -> RunningRoundContent(
                variant = viewModel.activeSession?.variant?.let { MicroRoundVariant.valueOf(it) }
                    ?: MicroRoundVariant.STABLE,
                elapsedSeconds = elapsedSeconds,
                onTick = { elapsedSeconds += 1 },
                onStop = {
                    stoppedEarly = true
                    phase = MicroRoundPhase.DEBRIEF
                },
                onComplete = {
                    stoppedEarly = false
                    elapsedSeconds = MicroRoundProgram.PRESCRIBED_DURATION_SECONDS
                    phase = MicroRoundPhase.DEBRIEF
                }
            )

            MicroRoundPhase.DEBRIEF -> DebriefContent(
                completedDurationSeconds = elapsedSeconds.coerceIn(
                    0,
                    MicroRoundProgram.PRESCRIBED_DURATION_SECONDS
                ),
                stoppedEarly = stoppedEarly,
                onSave = { duration, resistance, kneeAfter, intent, stateAfter, note ->
                    viewModel.finishRound(
                        completedDurationSeconds = duration,
                        stoppedEarly = stoppedEarly,
                        resistanceLevel = resistance,
                        kneeStatusAfter = kneeAfter,
                        intentResult = intent,
                        postSessionState = stateAfter,
                        note = note,
                        onFinished = onFinished
                    )
                }
            )
        }
    }
}

@Composable
private fun PreRoundContent(
    selectedKnee: KneeStatusBefore?,
    showCorner: Boolean,
    onSelectKnee: (KneeStatusBefore) -> Unit,
    onNeedCorner: () -> Unit,
    onCommit: () -> Unit
) {
    Text(
        text = "How does the right knee feel right now?",
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(16.dp))
    OptionRow(
        options = KneeStatusBefore.entries,
        selected = selectedKnee,
        label = { it.display },
        onSelect = onSelectKnee
    )
    Spacer(Modifier.height(18.dp))
    OutlinedButton(
        onClick = onNeedCorner,
        enabled = selectedKnee != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("I NEED THE CORNER")
    }
    if (showCorner && selectedKnee != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = MicroRoundProgram.cornerText(selectedKnee),
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp
        )
    }
    if (selectedKnee == KneeStatusBefore.PAINFUL) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "The app will not use footwork to test joint pain. This is not an assessment or diagnosis of an LCL injury.",
            color = AppColors.muted,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
    }
    Spacer(Modifier.height(28.dp))
    BigButton(
        text = "COMMIT TO TRAIN",
        onClick = onCommit,
        enabled = selectedKnee != null
    )
}

@Composable
private fun CommittedContent(
    selectedKnee: KneeStatusBefore?,
    canStart: Boolean,
    onStart: () -> Unit
) {
    Text(
        text = "COMMIT TO TRAIN",
        color = AppColors.accent,
        fontSize = 13.sp,
        letterSpacing = 2.sp
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Committed.",
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.Black
    )
    Spacer(Modifier.height(16.dp))
    if (selectedKnee == KneeStatusBefore.PAINFUL) {
        Text(
            text = "The round cannot begin while the knee is painful. The app will not use footwork to test joint pain.",
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 25.sp
        )
    } else {
        Text(
            text = "Start latency begins at commit and ends when START ROUND is tapped.",
            color = AppColors.muted,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(28.dp))
        BigButton(
            text = "START ROUND",
            onClick = onStart,
            enabled = canStart
        )
    }
}

@Composable
private fun RunningRoundContent(
    variant: MicroRoundVariant,
    elapsedSeconds: Int,
    onTick: () -> Unit,
    onStop: () -> Unit,
    onComplete: () -> Unit
) {
    LaunchedEffect(elapsedSeconds) {
        if (elapsedSeconds >= MicroRoundProgram.PRESCRIBED_DURATION_SECONDS) {
            onComplete()
        } else {
            delay(1000)
            onTick()
        }
    }

    val remaining = (MicroRoundProgram.PRESCRIBED_DURATION_SECONDS - elapsedSeconds)
        .coerceAtLeast(0)
    val assignment = MicroRoundProgram.assignmentFor(variant, elapsedSeconds)

    Text(
        text = formatDuration(remaining),
        color = Color.White,
        fontSize = 72.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(18.dp))
    Text(
        text = assignment.title,
        color = AppColors.accent,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    assignment.bullets.forEach { bullet ->
        Text(
            text = "• $bullet",
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 26.sp
        )
    }
    Spacer(Modifier.height(20.dp))
    Text(
        text = MicroRoundProgram.KNEE_STOP_INSTRUCTION,
        color = AppColors.muted,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
    Spacer(Modifier.height(32.dp))
    BigButton(
        text = "STOP",
        onClick = onStop
    )
}

@Composable
private fun DebriefContent(
    completedDurationSeconds: Int,
    stoppedEarly: Boolean,
    onSave: (
        Int,
        ResistanceLevel,
        KneeStatusAfter,
        IntentResult,
        PostSessionState,
        String
    ) -> Unit
) {
    var resistance by remember { mutableStateOf<ResistanceLevel?>(null) }
    var kneeAfter by remember { mutableStateOf<KneeStatusAfter?>(null) }
    var intent by remember { mutableStateOf<IntentResult?>(null) }
    var stateAfter by remember { mutableStateOf<PostSessionState?>(null) }
    var note by remember { mutableStateOf("") }

    Text(
        text = if (stoppedEarly) "Stopped early" else "Round complete",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Black
    )
    Text(
        text = "Completed ${formatDuration(completedDurationSeconds)}",
        color = AppColors.muted,
        fontSize = 15.sp
    )
    Spacer(Modifier.height(22.dp))

    DebriefQuestion("Resistance before starting", ResistanceLevel.entries, resistance, { it.display }) {
        resistance = it
    }
    DebriefQuestion("Right knee afterward", KneeStatusAfter.entries, kneeAfter, { it.display }) {
        kneeAfter = it
    }
    DebriefQuestion("Intent", IntentResult.entries, intent, { it.display }) {
        intent = it
    }
    DebriefQuestion("State afterward", PostSessionState.entries, stateAfter, { it.display }) {
        stateAfter = it
    }

    OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(MicroRoundProgram.MAX_NOTE_LENGTH) },
        label = { Text("What held up or broke down?") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
    )
    Spacer(Modifier.height(24.dp))
    BigButton(
        text = "SAVE DEBRIEF",
        enabled = resistance != null && kneeAfter != null && intent != null && stateAfter != null,
        onClick = {
            onSave(
                completedDurationSeconds,
                resistance!!,
                kneeAfter!!,
                intent!!,
                stateAfter!!,
                note
            )
        }
    )
}

@Composable
private fun <T> DebriefQuestion(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    OptionRow(options, selected, label, onSelect)
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun <T> OptionRow(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Button(
                onClick = { onSelect(option) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label(option).uppercase(),
                    color = if (isSelected) AppColors.accent else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "%d:%02d".format(mins, secs)
}
