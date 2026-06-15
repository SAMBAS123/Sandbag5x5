package com.sandbagstrong

import com.sandbagstrong.domain.KneeStatusBefore
import com.sandbagstrong.domain.MicroRoundProgram
import com.sandbagstrong.domain.MicroRoundVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroRoundProgramTest {
    @Test
    fun painfulKneeCannotLaunchRound() {
        assertFalse(MicroRoundProgram.canLaunchRound(KneeStatusBefore.PAINFUL))
        assertNull(MicroRoundProgram.variantFor(KneeStatusBefore.PAINFUL))
    }

    @Test
    fun uncertainKneeSelectsConservativeSequence() {
        assertEquals(MicroRoundVariant.UNCERTAIN, MicroRoundProgram.variantFor(KneeStatusBefore.UNCERTAIN))

        val first = MicroRoundProgram.assignmentFor(MicroRoundVariant.UNCERTAIN, 0)
        val second = MicroRoundProgram.assignmentFor(MicroRoundVariant.UNCERTAIN, 60)
        val third = MicroRoundProgram.assignmentFor(MicroRoundVariant.UNCERTAIN, 120)

        assertTrue(first.bullets.contains("no bouncing"))
        assertTrue(second.bullets.contains("no lateral cutting"))
        assertTrue(second.bullets.contains("no pivoting"))
        assertTrue(third.bullets.contains("no explosive direction changes"))
    }

    @Test
    fun stableKneeSelectsStandardSequence() {
        assertEquals(MicroRoundVariant.STABLE, MicroRoundProgram.variantFor(KneeStatusBefore.STABLE))

        val first = MicroRoundProgram.assignmentFor(MicroRoundVariant.STABLE, 0)
        val second = MicroRoundProgram.assignmentFor(MicroRoundVariant.STABLE, 60)
        val third = MicroRoundProgram.assignmentFor(MicroRoundVariant.STABLE, 120)

        assertTrue(first.bullets.contains("short forward/backward step-drag"))
        assertTrue(second.bullets.contains("controlled slip-step"))
        assertTrue(third.bullets.contains("diagonal entry"))
        assertTrue(third.bullets.contains("controlled angle change"))
    }

    @Test
    fun startLatencyUsesCommittedAndStartedTimestamps() {
        assertEquals(2500L, MicroRoundProgram.startLatencyMillis(1_000L, 3_500L))
        assertEquals(0L, MicroRoundProgram.startLatencyMillis(3_500L, 1_000L))
        assertNull(MicroRoundProgram.startLatencyMillis(1_000L, null))
    }
}
