package com.sandbagstrong.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long,
    val workoutType: String, // "A" or "B"
    val feelRating: String,  // EASY / SOLID / BRUTAL / FAILED
    val durationSeconds: Int = 0
)

@Entity(tableName = "set_logs")
data class SetLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val exerciseKey: String,
    val setNumber: Int,       // 1-based
    val repsCompleted: Int,   // for timed exercises this can be 1 or duration marker
    val restTakenSeconds: Int = 0,
    val completedAtMillis: Long
)

@Entity(tableName = "exercise_targets")
data class ExerciseTargetEntity(
    @PrimaryKey val id: String, // e.g. "A_a_clean" or composite but use string key
    val workoutType: String,
    val exerciseKey: String,
    val currentSets: Int,
    val currentReps: Int,     // numeric target per set. For display we still use definition for text like "each side"
    val currentRestSeconds: Int
)

@Entity(tableName = "micro_round_sessions")
data class MicroRoundSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val committedAtMillis: Long,
    val startedAtMillis: Long? = null,
    val finishedAtMillis: Long? = null,
    val prescribedDurationSeconds: Int,
    val completedDurationSeconds: Int = 0,
    val variant: String? = null,
    val kneeStatusBefore: String,
    val resistanceLevel: String? = null,
    val kneeStatusAfter: String? = null,
    val intentResult: String? = null,
    val postSessionState: String? = null,
    val note: String? = null,
    val completionStatus: String
)
