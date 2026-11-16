package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantumSimulatorTest {
    @Test
    fun bellPairHasExpectedEntangledState() {
        val run = AstraQuantumSimulator.run(2, AstraQuantumSimulator.bellPhiPlus())
        assertTrue(AstraQuantumSimulator.verifyBellPhiPlus(run))
        assertEquals(1.0, run.norm, 1e-9)
    }

    @Test
    fun groverFindsMarkedBasisState() {
        val run = AstraQuantumSimulator.run(2, AstraQuantumSimulator.groverSearch11())
        assertTrue(AstraQuantumSimulator.verifyGroverSearch11(run))
        assertTrue(run.amplitudes[3].probability() > 0.999999)
    }

    @Test
    fun qftAndInverseQftRoundTrip() {
        assertTrue(AstraQuantumSimulator.verifyQftRoundTrip())
    }

    @Test
    fun shorEducationalDemoFindsFactorsOfFifteen() {
        val result = AstraQuantumSimulator.shorFactorDemo15()
        assertEquals(15, result.number)
        assertEquals(setOf(3, 5), setOf(result.factors.first, result.factors.second))
        assertEquals(1.0, result.run.norm, 1e-9)
    }

    @Test
    fun sparseSimulatorBuildsExactGhz64State() {
        val run = AstraSparseQuantumSimulator.run(64, AstraSparseQuantumSimulator.ghz64())
        assertTrue(AstraSparseQuantumSimulator.verifyGhz64(run))
        assertEquals(2, run.amplitudes.size)
        assertEquals(1.0, run.norm, 1e-9)
    }

    @Test
    fun sparseSimulatorRejectsMoreThanSixtyFourQubits() {
        try {
            AstraSparseQuantumSimulator.run(65, emptyList())
            throw AssertionError("Expected invalid qubit count to be rejected")
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun denseSimulatorRejectsMoreThanTenQubits() {
        try {
            AstraQuantumSimulator.run(11, emptyList())
            throw AssertionError("Expected invalid qubit count to be rejected")
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }
}
