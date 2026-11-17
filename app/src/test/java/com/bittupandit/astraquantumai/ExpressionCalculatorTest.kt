package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionCalculatorTest {
    @Test fun respectsOperatorPrecedenceAndParentheses() {
        assertEquals("14", ExpressionCalculator.evaluate("2 + 3 * 4").displayValue)
        assertEquals("20", ExpressionCalculator.evaluate("(2 + 3) * 4").displayValue)
    }

    @Test fun supportsUnarySignsAndRightAssociativePowers() {
        assertEquals("-4", ExpressionCalculator.evaluate("-2^2").displayValue)
        assertEquals("512", ExpressionCalculator.evaluate("2^3^2").displayValue)
    }

    @Test fun supportsDivisionRemainderAndScientificNotation() {
        assertEquals("2.5", ExpressionCalculator.evaluate("5 / 2").displayValue)
        assertEquals("1", ExpressionCalculator.evaluate("10 % 3").displayValue)
        assertEquals("0.000001", ExpressionCalculator.evaluate("1e-6").displayValue)
    }

    @Test fun roundsFloatingPointNoiseForDisplay() {
        assertEquals("0.3", ExpressionCalculator.evaluate("0.1 + 0.2").displayValue)
    }

    @Test fun rejectsDivisionByZero() {
        val error = runCatching { ExpressionCalculator.evaluate("10 / (4 - 4)") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test fun rejectsCodeAndUnbalancedParentheses() {
        assertTrue(runCatching { ExpressionCalculator.evaluate("System.exit(0)") }.isFailure)
        assertTrue(runCatching { ExpressionCalculator.evaluate("(2 + 3") }.isFailure)
    }

    @Test fun rejectsOversizedAndNonFiniteExpressions() {
        assertTrue(runCatching { ExpressionCalculator.evaluate("1".repeat(257)) }.isFailure)
        assertTrue(runCatching { ExpressionCalculator.evaluate("10^1000") }.isFailure)
    }
}
