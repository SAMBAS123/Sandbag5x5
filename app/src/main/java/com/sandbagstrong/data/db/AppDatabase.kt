package com.sandbagstrong.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sandbagstrong.domain.Program
import com.sandbagstrong.domain.WorkoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        ExerciseTargetEntity::class,
        MicroRoundSessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun targetDao(): TargetDao
    abstract fun microRoundDao(): MicroRoundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS micro_round_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        committedAtMillis INTEGER NOT NULL,
                        startedAtMillis INTEGER,
                        finishedAtMillis INTEGER,
                        prescribedDurationSeconds INTEGER NOT NULL,
                        completedDurationSeconds INTEGER NOT NULL,
                        variant TEXT,
                        kneeStatusBefore TEXT NOT NULL,
                        resistanceLevel TEXT,
                        kneeStatusAfter TEXT,
                        intentResult TEXT,
                        postSessionState TEXT,
                        note TEXT,
                        completionStatus TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sandbag5x5.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(SeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed initial targets on first DB creation
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    val targets = mutableListOf<ExerciseTargetEntity>()

                    // Workout A seeds
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
                    // Workout B seeds
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
                    database.targetDao().insertAll(targets)
                }
            }
        }

        /**
         * Extract a numeric base rep count from strings like "3 each side", "5", "5-10", "8".
         * For timed entries we seed 1 (completion marker) or the minutes value.
         */
        private fun extractBaseReps(reps: String): Int {
            if (reps.contains("minute", ignoreCase = true)) {
                // 10 minutes -> treat volume as 10 "units" or just 1 for completion. Use 1.
                return 1
            }
            if (reps.contains("each", ignoreCase = true)) {
                // "3 each side" -> 3
                return reps.filter { it.isDigit() }.toIntOrNull() ?: 3
            }
            val digits = reps.filter { it.isDigit() }
            if (digits.isNotEmpty()) {
                val num = digits.toIntOrNull() ?: 5
                // For "5-10" take the lower
                return num
            }
            return if (reps.contains("round", true)) 3 else 5
        }
    }
}
