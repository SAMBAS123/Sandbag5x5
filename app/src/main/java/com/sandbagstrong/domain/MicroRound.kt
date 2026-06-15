package com.sandbagstrong.domain

enum class KneeStatusBefore(val display: String) {
    STABLE("Stable"),
    UNCERTAIN("Uncertain"),
    PAINFUL("Painful")
}

enum class MicroRoundVariant(val display: String) {
    STABLE("Stable"),
    UNCERTAIN("Uncertain")
}

enum class ResistanceLevel(val display: String) {
    NONE("None"),
    SOME("Some"),
    STRONG("Strong"),
    NEARLY_SKIPPED("Nearly skipped")
}

enum class KneeStatusAfter(val display: String) {
    FINE("Fine"),
    UNCERTAIN("Uncertain"),
    PAINFUL("Painful")
}

enum class IntentResult(val display: String) {
    TRAINED_ASSIGNMENT("Trained the assignment"),
    CHASED_PUNISHMENT("Chased punishment")
}

enum class PostSessionState(val display: String) {
    CALMER("Calmer"),
    UNCHANGED("Unchanged"),
    WORSE("Worse")
}

enum class MicroRoundStatus(val display: String) {
    COMMITTED_ONLY("committed only"),
    STARTED("started"),
    COMPLETED("completed"),
    STOPPED_EARLY("stopped early")
}

data class MicroRoundAssignment(
    val title: String,
    val bullets: List<String>
)

object MicroRoundProgram {
    const val PRESCRIBED_DURATION_SECONDS = 180
    const val MAX_NOTE_LENGTH = 240
    const val KNEE_STOP_INSTRUCTION =
        "Stop if the knee becomes painful, gives way, or feels less stable."

    fun cornerText(kneeStatus: KneeStatusBefore): String =
        when (kneeStatus) {
            KneeStatusBefore.STABLE ->
                "Give me one clean three-minute round. Decide about more after it ends."
            KneeStatusBefore.UNCERTAIN ->
                "Controlled feet only. The goal is clean movement, not proof."
            KneeStatusBefore.PAINFUL ->
                "Do not use toughness to test joint pain. End the round before it begins."
        }

    fun variantFor(kneeStatus: KneeStatusBefore): MicroRoundVariant? =
        when (kneeStatus) {
            KneeStatusBefore.STABLE -> MicroRoundVariant.STABLE
            KneeStatusBefore.UNCERTAIN -> MicroRoundVariant.UNCERTAIN
            KneeStatusBefore.PAINFUL -> null
        }

    fun canLaunchRound(kneeStatus: KneeStatusBefore): Boolean =
        variantFor(kneeStatus) != null

    fun assignmentFor(
        variant: MicroRoundVariant,
        elapsedSeconds: Int
    ): MicroRoundAssignment {
        val minute = (elapsedSeconds.coerceIn(0, PRESCRIBED_DURATION_SECONDS - 1)) / 60
        return when (variant) {
            MicroRoundVariant.STABLE -> stableAssignments[minute]
            MicroRoundVariant.UNCERTAIN -> uncertainAssignments[minute]
        }
    }

    fun startLatencyMillis(committedAtMillis: Long, startedAtMillis: Long?): Long? =
        startedAtMillis?.let { (it - committedAtMillis).coerceAtLeast(0L) }

    fun cleanNote(note: String): String? =
        note.trim().take(MAX_NOTE_LENGTH).ifBlank { null }

    private val stableAssignments = listOf(
        MicroRoundAssignment(
            title = "Find the base",
            bullets = listOf(
                "relaxed stance",
                "short forward/backward step-drag",
                "return to balance after every movement"
            )
        ),
        MicroRoundAssignment(
            title = "Connect defense to feet",
            bullets = listOf(
                "controlled slip-step",
                "reset to stance",
                "no deep or violent head movement"
            )
        ),
        MicroRoundAssignment(
            title = "Enter and leave cleanly",
            bullets = listOf(
                "diagonal entry",
                "controlled angle change",
                "balanced exit",
                "work at approximately 60-70%, not maximum speed"
            )
        )
    )

    private val uncertainAssignments = listOf(
        MicroRoundAssignment(
            title = "Find the base",
            bullets = listOf(
                "relaxed stance",
                "small weight shifts",
                "no bouncing"
            )
        ),
        MicroRoundAssignment(
            title = "Straight-line movement",
            bullets = listOf(
                "short forward/backward steps",
                "no lateral cutting",
                "no pivoting"
            )
        ),
        MicroRoundAssignment(
            title = "Upper-body movement with stable feet",
            bullets = listOf(
                "controlled slips",
                "shallow level-change feints only if comfortable",
                "no explosive direction changes"
            )
        )
    )
}
