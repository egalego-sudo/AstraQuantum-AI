package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementaryMathSolverTest {
    @Test fun solvesLinearEquation() {
        assertEquals(2.0, ElementaryMathSolver.solveLinear(3.0, -6.0).roots.single(), 1e-10)
    }
    @Test fun solvesTwoRealQuadraticRoots() {
        val result = ElementaryMathSolver.solveQuadratic(1.0, -3.0, 2.0)
        assertEquals("TWO_DISTINCT_REAL_ROOTS", result.classification)
        assertEquals(listOf(1.0, 2.0), result.roots)
    }
    @Test fun solvesRepeatedRoot() {
        val result = ElementaryMathSolver.solveQuadratic(1.0, 2.0, 1.0)
        assertEquals("ONE_REPEATED_REAL_ROOT", result.classification)
        assertEquals(-1.0, result.roots.single(), 1e-10)
    }
    @Test fun identifiesComplexRootsWithoutInventingRealValues() {
        val result = ElementaryMathSolver.solveQuadratic(1.0, 0.0, 1.0)
        assertEquals("NO_REAL_ROOTS", result.classification)
        assertTrue(result.roots.isEmpty())
    }
    @Test fun evaluatesPolynomialByHornerRule() {
        // 2x^2 + 3x + 1 at x=2 is 15.
        assertEquals(15.0, ElementaryMathSolver.evaluatePolynomial(listOf(2.0, 3.0, 1.0), 2.0), 1e-10)
    }
}
