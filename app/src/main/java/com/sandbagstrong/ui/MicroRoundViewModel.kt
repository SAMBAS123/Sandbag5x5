package com.sandbagstrong.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandbagstrong.data.WorkoutRepository
import com.sandbagstrong.data.db.MicroRoundSessionEntity
import com.sandbagstrong.domain.IntentResult
import com.sandbagstrong.domain.KneeStatusAfter
import com.sandbagstrong.domain.KneeStatusBefore
import com.sandbagstrong.domain.PostSessionState
import com.sandbagstrong.domain.ResistanceLevel
import kotlinx.coroutines.launch

class MicroRoundViewModel(
    private val repository: WorkoutRepository,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    var selectedKnee by mutableStateOf<KneeStatusBefore?>(null)
        private set

    var activeSession by mutableStateOf<MicroRoundSessionEntity?>(null)
        private set

    var committed by mutableStateOf(false)
        private set

    var started by mutableStateOf(false)
        private set

    fun selectKnee(kneeStatus: KneeStatusBefore) {
        if (committed) return
        selectedKnee = kneeStatus
    }

    fun commit() {
        val knee = selectedKnee ?: return
        viewModelScope.launch {
            activeSession = repository.commitMicroRound(knee, nowMillis())
            committed = true
        }
    }

    fun startRound(onStarted: (Boolean) -> Unit) {
        val session = activeSession ?: return
        viewModelScope.launch {
            val updated = repository.startMicroRound(session.id, nowMillis())
            activeSession = updated
            started = updated?.startedAtMillis != null
            onStarted(started)
        }
    }

    fun finishRound(
        completedDurationSeconds: Int,
        stoppedEarly: Boolean,
        resistanceLevel: ResistanceLevel,
        kneeStatusAfter: KneeStatusAfter,
        intentResult: IntentResult,
        postSessionState: PostSessionState,
        note: String,
        onFinished: () -> Unit
    ) {
        val session = activeSession ?: return
        viewModelScope.launch {
            activeSession = repository.finishMicroRound(
                sessionId = session.id,
                nowMillis = nowMillis(),
                completedDurationSeconds = completedDurationSeconds,
                stoppedEarly = stoppedEarly,
                resistanceLevel = resistanceLevel,
                kneeStatusAfter = kneeStatusAfter,
                intentResult = intentResult,
                postSessionState = postSessionState,
                note = note
            )
            resetLocalState()
            onFinished()
        }
    }

    fun resetLocalState() {
        selectedKnee = null
        activeSession = null
        committed = false
        started = false
    }
}
