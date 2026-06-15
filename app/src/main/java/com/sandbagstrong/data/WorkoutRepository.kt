package com.sandbagstrong.data

import android.content.Context
import android.content.SharedPreferences
import com.sandbagstrong.data.db.AppDatabase
import com.sandbagstrong.data.db.ExerciseTargetEntity
import com.sandbagstrong.data.db.MicroRoundSessionEntity
import com.sandbagstrong.data.db.SetLogEntity
import com.sandbagstrong.data.db.WorkoutSessionEntity
import com.sandbagstrong.domain.Exercise
import com.sandbagstrong.domain.FeelRating
import com.sandbagstrong.domain.IntentResult
import com.sandbagstrong.domain.KneeStatusAfter
import com.sandbagstrong.domain.KneeStatusBefore
import com.sandbagstrong.domain.MicroRoundProgram
import com.sandbagstrong.domain.MicroRoundStatus
import com.sandbagstrong.domain.PostSessionState
import com.sandbagstrong.domain.Program
import com.sandbagstrong.domain.ResistanceLevel
import com.sandbagstrong.domain.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

data class CurrentTarget(
    val exercise: Exercise,
    val sets: Int,
    val repsPerSet: Int,
    val restSeconds: Int
)

class WorkoutRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sandbag_prefs", Context.MODE_PRIVATE)

    private val sessionDao = db.sessionDao()
    private val setLogDao = db.setLogDao()
    private val targetDao = db.targetDao()
    private val microRoundDao = db.microRoundDao()

    // ---- Next workout type (persist which one is scheduled) ----
    fun getNextWorkoutType(): WorkoutType {
        val raw = prefs.getString("next_workout", "A")
        return if (raw == "B") WorkoutType.B else WorkoutType.A
    }

    fun setNextWorkoutType(type: WorkoutType) {
        prefs.edit().putString("next_workout", type.name).apply()
    }

    fun flipNextWorkout() {
        val current = getNextWorkoutType()
        setNextWorkoutType(current.next())
    }

    // ---- Targets ----
    suspend fun getCurrentTargets(type: WorkoutType): List<CurrentTarget> {
        val defs = Program.exercisesFor(type)
        val entities = targetDao.getTargetsForWorkout(type.name)
        val entityMap = entities.associateBy { it.exerciseKey }

        return defs.map { def ->
            val e = entityMap[def.key]
            CurrentTarget(
                exercise = def,
                sets = e?.currentSets ?: def.sets,
                repsPerSet = e?.currentReps ?: 5,
                restSeconds = e?.currentRestSeconds ?: (Program.defaultRestSeconds[def.key] ?: 90)
            )
        }
    }

    // ---- Sessions & History ----
    fun getAllSessionsFlow(): Flow<List<WorkoutSessionEntity>> = sessionDao.getAllSessions()

    suspend fun saveCompletedWorkout(
        type: WorkoutType,
        feel: FeelRating,
        setCompletions: List<SetCompletion>, // from UI state
        durationSeconds: Int
    ) {
        val session = WorkoutSessionEntity(
            dateMillis = System.currentTimeMillis(),
            workoutType = type.name,
            feelRating = feel.name,
            durationSeconds = durationSeconds
        )
        sessionDao.insert(session)

        val logs = setCompletions.map { sc ->
            SetLogEntity(
                sessionId = session.id,
                exerciseKey = sc.exerciseKey,
                setNumber = sc.setNumber,
                repsCompleted = sc.repsCompleted,
                restTakenSeconds = sc.restTakenSeconds,
                completedAtMillis = sc.completedAtMillis
            )
        }
        if (logs.isNotEmpty()) {
            setLogDao.insertAll(logs)
        }

        // Apply progression
        applyProgression(type, feel)

        // Advance to next workout
        flipNextWorkout()
    }

    private suspend fun applyProgression(type: WorkoutType, feel: FeelRating) {
        val targets = targetDao.getTargetsForWorkout(type.name).toMutableList()
        if (targets.isEmpty()) return

        targets.forEachIndexed { index, t ->
            val updated = when (feel) {
                FeelRating.EASY -> {
                    // Add 1 rep per set (increase volume). Also shave 10s rest if reasonable.
                    val newReps = t.currentReps + 1
                    val newRest = (t.currentRestSeconds - 10).coerceAtLeast(30)
                    // For timed-ish (low sets or special keys) we may bump sets instead for carries
                    if (t.currentSets <= 2 && t.exerciseKey.contains("carry", true)) {
                        t.copy(currentSets = (t.currentSets + 1).coerceAtMost(6), currentRestSeconds = newRest)
                    } else {
                        t.copy(currentReps = newReps.coerceAtMost(20), currentRestSeconds = newRest)
                    }
                }
                FeelRating.SOLID, FeelRating.BRUTAL -> {
                    // Repeat same target. Optionally we could slowly reduce rest on brutal consistency but keep simple.
                    t
                }
                FeelRating.FAILED -> {
                    val newReps = (t.currentReps - 1).coerceAtLeast(1)
                    val newSets = (t.currentSets - 1).coerceAtLeast(1)
                    // Prefer reducing reps; only drop a set if very low volume
                    if (t.currentReps <= 2 && t.currentSets > 1) {
                        t.copy(currentSets = newSets)
                    } else {
                        t.copy(currentReps = newReps)
                    }
                }
            }
            targetDao.update(updated)
        }
    }

    // ---- Micro Round sessions ----
    fun getMicroRoundSessionsFlow(): Flow<List<MicroRoundSessionEntity>> =
        microRoundDao.getAllSessions()

    suspend fun commitMicroRound(
        kneeStatusBefore: KneeStatusBefore,
        nowMillis: Long = System.currentTimeMillis()
    ): MicroRoundSessionEntity {
        val session = MicroRoundSessionEntity(
            committedAtMillis = nowMillis,
            prescribedDurationSeconds = MicroRoundProgram.PRESCRIBED_DURATION_SECONDS,
            variant = MicroRoundProgram.variantFor(kneeStatusBefore)?.name,
            kneeStatusBefore = kneeStatusBefore.name,
            completionStatus = MicroRoundStatus.COMMITTED_ONLY.name
        )
        microRoundDao.insert(session)
        return session
    }

    suspend fun startMicroRound(
        sessionId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): MicroRoundSessionEntity? {
        val session = microRoundDao.getById(sessionId) ?: return null
        val kneeStatus = KneeStatusBefore.valueOf(session.kneeStatusBefore)
        if (!MicroRoundProgram.canLaunchRound(kneeStatus) || session.variant == null) {
            return session
        }
        val updated = session.copy(
            startedAtMillis = nowMillis,
            completionStatus = MicroRoundStatus.STARTED.name
        )
        microRoundDao.update(updated)
        return updated
    }

    suspend fun finishMicroRound(
        sessionId: String,
        nowMillis: Long = System.currentTimeMillis(),
        completedDurationSeconds: Int,
        stoppedEarly: Boolean,
        resistanceLevel: ResistanceLevel,
        kneeStatusAfter: KneeStatusAfter,
        intentResult: IntentResult,
        postSessionState: PostSessionState,
        note: String?
    ): MicroRoundSessionEntity? {
        val session = microRoundDao.getById(sessionId) ?: return null
        val status = if (stoppedEarly) {
            MicroRoundStatus.STOPPED_EARLY
        } else {
            MicroRoundStatus.COMPLETED
        }
        val updated = session.copy(
            finishedAtMillis = nowMillis,
            completedDurationSeconds = completedDurationSeconds.coerceIn(
                0,
                session.prescribedDurationSeconds
            ),
            resistanceLevel = resistanceLevel.name,
            kneeStatusAfter = kneeStatusAfter.name,
            intentResult = intentResult.name,
            postSessionState = postSessionState.name,
            note = MicroRoundProgram.cleanNote(note.orEmpty()),
            completionStatus = status.name
        )
        microRoundDao.update(updated)
        return updated
    }

    // For history detail if wanted
    suspend fun getSetLogsForSession(sessionId: String): List<SetLogEntity> =
        setLogDao.getForSession(sessionId)

    // Helper to reset targets to initial (for testing / future settings)
    suspend fun resetAllTargetsToSeed() {
        // Re-insert seeds (REPLACE)
        val targets = mutableListOf<ExerciseTargetEntity>()
        Program.workoutA.forEach { ex ->
            val rest = Program.defaultRestSeconds[ex.key] ?: 90
            targets += ExerciseTargetEntity(
                id = "A_${ex.key}",
                workoutType = "A",
                exerciseKey = ex.key,
                currentSets = ex.sets,
                currentReps = extractBaseReps(ex.reps),
                currentRestSeconds = rest
            )
        }
        Program.workoutB.forEach { ex ->
            val rest = Program.defaultRestSeconds[ex.key] ?: 60
            targets += ExerciseTargetEntity(
                id = "B_${ex.key}",
                workoutType = "B",
                exerciseKey = ex.key,
                currentSets = ex.sets,
                currentReps = extractBaseReps(ex.reps),
                currentRestSeconds = rest
            )
        }
        targetDao.insertAll(targets)
    }

    private fun extractBaseReps(reps: String): Int {
        if (reps.contains("minute", ignoreCase = true)) return 1
        if (reps.contains("each", ignoreCase = true)) {
            return reps.filter { it.isDigit() }.toIntOrNull() ?: 3
        }
        val digits = reps.filter { it.isDigit() }
        return if (digits.isNotEmpty()) digits.toIntOrNull() ?: 5 else 3
    }
}

/** In-memory representation of one completed set during a live workout. */
data class SetCompletion(
    val exerciseKey: String,
    val setNumber: Int,
    val repsCompleted: Int,
    val restTakenSeconds: Int,
    val completedAtMillis: Long
)
