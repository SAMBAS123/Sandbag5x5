package com.sandbagstrong

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.sandbagstrong.data.SetCompletion
import com.sandbagstrong.data.WorkoutRepository
import com.sandbagstrong.data.db.AppDatabase
import com.sandbagstrong.domain.FeelRating
import com.sandbagstrong.domain.IntentResult
import com.sandbagstrong.domain.KneeStatusAfter
import com.sandbagstrong.domain.KneeStatusBefore
import com.sandbagstrong.domain.MicroRoundStatus
import com.sandbagstrong.domain.MicroRoundVariant
import com.sandbagstrong.domain.PostSessionState
import com.sandbagstrong.domain.ResistanceLevel
import com.sandbagstrong.domain.WorkoutType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryMicroRoundTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("sandbag_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun existingWorkoutSelectionAndProgressionRemainUnchanged() = runTest {
        repository.resetAllTargetsToSeed()
        repository.setNextWorkoutType(WorkoutType.A)

        repository.saveCompletedWorkout(
            type = WorkoutType.A,
            feel = FeelRating.EASY,
            setCompletions = emptyList(),
            durationSeconds = 600
        )

        assertEquals(WorkoutType.B, repository.getNextWorkoutType())
        val squatTarget = db.targetDao().getTarget("A_a_squat")
        assertEquals(6, squatTarget?.currentReps)
        assertEquals(110, squatTarget?.currentRestSeconds)
    }

    @Test
    fun microRoundCommitIsPersistedBeforeRoundStarts() = runTest {
        val committed = repository.commitMicroRound(KneeStatusBefore.STABLE, nowMillis = 1_000L)
        val stored = db.microRoundDao().getById(committed.id)

        assertNotNull(stored)
        assertEquals(1_000L, stored?.committedAtMillis)
        assertNull(stored?.startedAtMillis)
        assertEquals(MicroRoundStatus.COMMITTED_ONLY.name, stored?.completionStatus)
    }

    @Test
    fun painfulKneeStatusCannotLaunchRound() = runTest {
        val committed = repository.commitMicroRound(KneeStatusBefore.PAINFUL, nowMillis = 1_000L)
        val afterStartAttempt = repository.startMicroRound(committed.id, nowMillis = 2_000L)

        assertNull(afterStartAttempt?.startedAtMillis)
        assertEquals(MicroRoundStatus.COMMITTED_ONLY.name, afterStartAttempt?.completionStatus)
    }

    @Test
    fun completedDebriefDataPersistsCorrectly() = runTest {
        val committed = repository.commitMicroRound(KneeStatusBefore.STABLE, nowMillis = 1_000L)
        repository.startMicroRound(committed.id, nowMillis = 4_000L)

        repository.finishMicroRound(
            sessionId = committed.id,
            nowMillis = 184_000L,
            completedDurationSeconds = 180,
            stoppedEarly = false,
            resistanceLevel = ResistanceLevel.SOME,
            kneeStatusAfter = KneeStatusAfter.FINE,
            intentResult = IntentResult.TRAINED_ASSIGNMENT,
            postSessionState = PostSessionState.CALMER,
            note = "stance held up"
        )

        val stored = db.microRoundDao().getById(committed.id)
        assertEquals(4_000L, stored?.startedAtMillis)
        assertEquals(180, stored?.completedDurationSeconds)
        assertEquals(ResistanceLevel.SOME.name, stored?.resistanceLevel)
        assertEquals(KneeStatusAfter.FINE.name, stored?.kneeStatusAfter)
        assertEquals(IntentResult.TRAINED_ASSIGNMENT.name, stored?.intentResult)
        assertEquals(PostSessionState.CALMER.name, stored?.postSessionState)
        assertEquals("stance held up", stored?.note)
        assertEquals(MicroRoundStatus.COMPLETED.name, stored?.completionStatus)
    }

    @Test
    fun stoppedEarlyDurationAndStatusPersistCorrectly() = runTest {
        val committed = repository.commitMicroRound(KneeStatusBefore.UNCERTAIN, nowMillis = 1_000L)
        repository.startMicroRound(committed.id, nowMillis = 2_000L)

        repository.finishMicroRound(
            sessionId = committed.id,
            nowMillis = 47_000L,
            completedDurationSeconds = 45,
            stoppedEarly = true,
            resistanceLevel = ResistanceLevel.STRONG,
            kneeStatusAfter = KneeStatusAfter.UNCERTAIN,
            intentResult = IntentResult.TRAINED_ASSIGNMENT,
            postSessionState = PostSessionState.UNCHANGED,
            note = ""
        )

        val stored = db.microRoundDao().getById(committed.id)
        assertEquals(45, stored?.completedDurationSeconds)
        assertEquals(MicroRoundStatus.STOPPED_EARLY.name, stored?.completionStatus)
        assertNull(stored?.note)
    }

    @Test
    fun microRoundsNeverFlipNextWorkout() = runTest {
        repository.setNextWorkoutType(WorkoutType.A)
        val committed = repository.commitMicroRound(KneeStatusBefore.STABLE, nowMillis = 1_000L)
        repository.startMicroRound(committed.id, nowMillis = 2_000L)
        repository.finishMicroRound(
            sessionId = committed.id,
            nowMillis = 182_000L,
            completedDurationSeconds = 180,
            stoppedEarly = false,
            resistanceLevel = ResistanceLevel.NONE,
            kneeStatusAfter = KneeStatusAfter.FINE,
            intentResult = IntentResult.TRAINED_ASSIGNMENT,
            postSessionState = PostSessionState.CALMER,
            note = null
        )

        assertEquals(WorkoutType.A, repository.getNextWorkoutType())
    }

    @Test
    fun committedVariantsMatchKneeSelection() = runTest {
        val stable = repository.commitMicroRound(KneeStatusBefore.STABLE, nowMillis = 1_000L)
        val uncertain = repository.commitMicroRound(KneeStatusBefore.UNCERTAIN, nowMillis = 2_000L)

        assertEquals(MicroRoundVariant.STABLE.name, db.microRoundDao().getById(stable.id)?.variant)
        assertEquals(MicroRoundVariant.UNCERTAIN.name, db.microRoundDao().getById(uncertain.id)?.variant)
    }

    @Test
    fun roomMigrationPreservesExistingWorkoutData() = runTest {
        db.close()
        val dbName = "migration-test.db"
        context.deleteDatabase(dbName)
        val sqlite = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        sqlite.execSQL("PRAGMA user_version = 1")
        sqlite.execSQL(
            """
            CREATE TABLE workout_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                dateMillis INTEGER NOT NULL,
                workoutType TEXT NOT NULL,
                feelRating TEXT NOT NULL,
                durationSeconds INTEGER NOT NULL
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            CREATE TABLE set_logs (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                exerciseKey TEXT NOT NULL,
                setNumber INTEGER NOT NULL,
                repsCompleted INTEGER NOT NULL,
                restTakenSeconds INTEGER NOT NULL,
                completedAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            CREATE TABLE exercise_targets (
                id TEXT NOT NULL PRIMARY KEY,
                workoutType TEXT NOT NULL,
                exerciseKey TEXT NOT NULL,
                currentSets INTEGER NOT NULL,
                currentReps INTEGER NOT NULL,
                currentRestSeconds INTEGER NOT NULL
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            "INSERT INTO workout_sessions (id, dateMillis, workoutType, feelRating, durationSeconds) VALUES ('s1', 1000, 'A', 'SOLID', 600)"
        )
        sqlite.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        assertEquals(1, migrated.sessionDao().countSessions())
        val cursor = migrated.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM micro_round_sessions"))
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
        migrated.close()
        context.deleteDatabase(dbName)
    }
}
