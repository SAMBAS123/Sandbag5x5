package com.sandbagstrong.domain

/**
 * Static definition of an exercise in the program.
 * Targets (sets, reps, rest) are loaded from DB and can progress.
 */
data class Exercise(
    val key: String,           // stable id e.g. "a_clean", "b_lunge"
    val name: String,
    val sets: Int,
    val reps: String,          // "3 each side", "5", "5-10", "1 min" etc. For display only
    val isTimed: Boolean = false, // for carry / shouldering style
    val notes: String = ""
) {
    val displayTarget: String
        get() = if (isTimed) reps else "$sets × $reps"
}
