package com.sandbagstrong.domain

/**
 * The fixed initial program. Weight is always 70 lb sandbag.
 * These are the seed definitions. Actual live targets come from ExerciseTarget in DB.
 */
object Program {
    val workoutA: List<Exercise> = listOf(
        Exercise(
            key = "a_clean",
            name = "Sandbag clean to shoulder",
            sets = 5,
            reps = "3 each side",
            notes = "Alternate sides each rep or as comfortable"
        ),
        Exercise(
            key = "a_squat",
            name = "Bear hug squat",
            sets = 5,
            reps = "5"
        ),
        Exercise(
            key = "a_press",
            name = "Sandbag floor press / push-up",
            sets = 5,
            reps = "5-10",
            notes = "Use floor press or push-ups. Pick one."
        ),
        Exercise(
            key = "a_row",
            name = "Bent-over sandbag row",
            sets = 5,
            reps = "8"
        ),
        Exercise(
            key = "a_carry",
            name = "Bear hug carry",
            sets = 3,
            reps = "rounds",
            isTimed = false,
            notes = "Heavy, controlled. Walk as far as form allows each round."
        )
    )

    val workoutB: List<Exercise> = listOf(
        Exercise(
            key = "b_shoulder",
            name = "Sandbag shouldering",
            sets = 1,
            reps = "10 minutes",
            isTimed = true,
            notes = "Alternate sides continuously. Mark complete when time is up."
        ),
        Exercise(
            key = "b_lunge",
            name = "Front-loaded reverse lunge",
            sets = 3,
            reps = "5 each leg",
            notes = "Sandbag in front rack or bear hug"
        ),
        Exercise(
            key = "b_g2os",
            name = "Ground-to-over-shoulder",
            sets = 5,
            reps = "5",
            notes = "Explosive. One side or alternate as needed."
        ),
        Exercise(
            key = "b_drag",
            name = "Sandbag pull-through / plank drag",
            sets = 3,
            reps = "10",
            notes = "Pick pull-throughs or plank drags"
        ),
        Exercise(
            key = "b_carry",
            name = "Carry finisher",
            sets = 1,
            reps = "5 minutes",
            isTimed = true,
            notes = "Pick your carry variation. Go the distance."
        )
    )

    fun exercisesFor(type: WorkoutType): List<Exercise> =
        if (type == WorkoutType.A) workoutA else workoutB

    // Default rest seconds per exercise key (used for initial seed)
    val defaultRestSeconds: Map<String, Int> = mapOf(
        "a_clean" to 120,
        "a_squat" to 120,
        "a_press" to 90,
        "a_row" to 90,
        "a_carry" to 90,
        "b_shoulder" to 0,   // continuous
        "b_lunge" to 90,
        "b_g2os" to 120,
        "b_drag" to 60,
        "b_carry" to 0
    )
}
