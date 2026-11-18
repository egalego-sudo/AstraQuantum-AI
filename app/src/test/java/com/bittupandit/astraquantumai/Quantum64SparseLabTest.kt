package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Quantum64SparseLabTest {
    @Test
    fun ghz64IsAnExactNormalizedTwoTermState() {
        val operations = AstraSparseQuantumSimulator.ghz64()
        val run = AstraSparseQuantumSimulator.run(64, operations)

        assertEquals(64, run.qubits)
        assertEquals(2, run.amplitudes.size)
        assertEquals(1.0, run.norm, 1e-12)
        assertTrue(AstraSparseQuantumSimulator.verifyGhz64(run))
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
            AstraSparseQuantumSimulator.basisLabel(0L, 64))
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111",
            AstraSparseQuantumSimulator.basisLabel(-1L, 64))
    }

    @Test
    fun bellPairHasExpectedProbabilitiesAndMeasurementCounts() {
        val run = AstraSparseQuantumSimulator.run(
            2,
            listOf(QuantumOp("H", 0), QuantumOp("CNOT", 1, 0))
        )

        assertEquals(2, run.amplitudes.size)
        assertEquals(0.5, run.amplitudes[0L]?.probability() ?: 0.0, 1e-12)
        assertEquals(0.5, run.amplitudes[3L]?.probability() ?: 0.0, 1e-12)
        assertEquals(1.0, run.norm, 1e-12)
        val counts = AstraSparseQuantumSimulator.sample(run, 500)
        assertEquals(500, counts.values.sum())
        assertTrue(counts.keys.all { it == "00" || it == "11" })
    }

    @Test
    fun rejectsInvalidQubitAndControlledGateArguments() {
        assertFalse(runCatching { AstraSparseQuantumSimulator.run(0, emptyList()) }.isSuccess)
        assertFalse(runCatching { AstraSparseQuantumSimulator.run(65, emptyList()) }.isSuccess)
        assertFalse(runCatching {
            AstraSparseQuantumSimulator.run(2, listOf(QuantumOp("CNOT", 1, 1)))
        }.isSuccess)
    }

    @Test
    fun stopsWhenSparseStateExceedsConfiguredTermCeiling() {
        val operations = (0 until 17).map { QuantumOp("H", it) }
        val result = runCatching { AstraSparseQuantumSimulator.run(17, operations) }
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Sparse simulation सीमा पार हुई") == true)
    }
}
