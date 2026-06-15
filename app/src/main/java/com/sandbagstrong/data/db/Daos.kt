package com.sandbagstrong.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun countSessions(): Int
}

@Dao
interface SetLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SetLogEntity>)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY completedAtMillis")
    suspend fun getForSession(sessionId: String): List<SetLogEntity>
}

@Dao
interface TargetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(target: ExerciseTargetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(targets: List<ExerciseTargetEntity>)

    @Update
    suspend fun update(target: ExerciseTargetEntity)

    @Query("SELECT * FROM exercise_targets WHERE workoutType = :type")
    suspend fun getTargetsForWorkout(type: String): List<ExerciseTargetEntity>

    @Query("SELECT * FROM exercise_targets WHERE id = :id")
    suspend fun getTarget(id: String): ExerciseTargetEntity?

    @Query("SELECT * FROM exercise_targets")
    fun getAllTargetsFlow(): Flow<List<ExerciseTargetEntity>>
}
