package com.sandbagstrong.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandbagstrong.data.SetCompletion
import com.sandbagstrong.data.WorkoutRepository
import com.sandbagstrong.data.CurrentTarget
import com.sandbagstrong.domain.FeelRating
import com.sandbagstrong.domain.WorkoutType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Very simple state holder for the active workout session.
 */
class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    // Current scheduled workout
    var currentWorkoutType by mutableStateOf(repository.getNextWorkoutType())
        private set

    // Loaded targets for current workout (merged definition + persisted numbers)
    var targets by mutableStateOf<List<CurrentTarget>>(emptyList())
        private set

    // Live completed count per exercise key during this session
    var completedCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    // Set logs recorded in this active session (for saving at end)
    private val setCompletions = mutableListOf<SetCompletion>()

    // Timing
    private var workoutStartTime = 0L

    // For rest timer
    var showRestTimer by mutableStateOf(false)
    var currentRestSeconds by mutableStateOf(90)

    // After workout
    var showFeelRating by mutableStateOf(false)

    val historyFlow: StateFlow<List<com.sandbagstrong.data.db.WorkoutSessionEntity>> =
        repository.getAllSessionsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshWorkout()
    }

    fun refreshWorkout() {
        currentWorkoutType = repository.getNextWorkoutType()
        viewModelScope.launch {
            targets = repository.getCurrentTargets(currentWorkoutType)
            completedCounts = targets.associate { it.exercise.key to 0 }
            setCompletions.clear()
            workoutStartTime = System.currentTimeMillis()
            showFeelRating = false
        }
    }

    fun completeSet(exerciseKey: String) {
        val target = targets.find { it.exercise.key == exerciseKey } ?: return
        val current = completedCounts[exerciseKey] ?: 0
        if (current >= target.sets) return

        val newCount = current + 1
        completedCounts = completedCounts + (exerciseKey to newCount)

        // Record the completion
        val now = System.currentTimeMillis()
        setCompletions += SetCompletion(
            exerciseKey = exerciseKey,
            setNumber = newCount,
            repsCompleted = target.repsPerSet,
            restTakenSeconds = 0, // will be updated when rest finishes if wanted, simplified here
            completedAtMillis = now
        )

        // Trigger rest timer (skip for timed exercises with 0 rest)
        val rest = target.restSeconds
        if (rest > 0) {
            currentRestSeconds = rest
            showRestTimer = true
        } else if (isWorkoutComplete()) {
            showFeelRating = true
        }
    }

    fun dismissRestTimer() {
        showRestTimer = false
        if (isWorkoutComplete()) {
            showFeelRating = true
        }
    }

    fun isWorkoutComplete(): Boolean {
        if (targets.isEmpty()) return false
        return targets.all { t ->
            (completedCounts[t.exercise.key] ?: 0) >= t.sets
        }
    }

    fun finishWorkout(feel: FeelRating) {
        viewModelScope.launch {
            val durationSec = ((System.currentTimeMillis() - workoutStartTime) / 1000).toInt()
            repository.saveCompletedWorkout(
                type = currentWorkoutType,
                feel = feel,
                setCompletions = setCompletions.toList(),
                durationSeconds = durationSec
            )
            // Reset for next
            refreshWorkout()
        }
    }

    fun getCompletedCount(key: String): Int = completedCounts[key] ?: 0

    fun resetToSeedTargets() {
        viewModelScope.launch {
            repository.resetAllTargetsToSeed()
            refreshWorkout()
        }
    }
}
