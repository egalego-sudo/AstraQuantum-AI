package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChemicalCompositionCalculatorTest {
    @Test fun calculatesWaterMolarMassAndComposition() {
        val result = ChemicalCompositionCalculator.analyze("H2O")
        assertEquals(18.015, result.molarMassGPerMol, 0.002)
        assertEquals(2, result.atomCounts["H"])
        assertEquals(1, result.atomCounts["O"])
        assertEquals(100.0, result.massPercentByElement.values.sum(), 1e-9)
    }
    @Test fun supportsParenthesizedFormula() {
        val result = ChemicalCompositionCalculator.analyze("Ca(OH)2")
        assertEquals(2, result.atomCounts["O"])
        assertEquals(2, result.atomCounts["H"])
        assertEquals(1, result.atomCounts["Ca"])
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownElement() { ChemicalCompositionCalculator.analyze("Xx2") }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsElementsWithoutStandardAtomicWeight() { ChemicalCompositionCalculator.analyze("Tc") }
    @Test fun formulaHasPositiveMass() { assertTrue(ChemicalCompositionCalculator.analyze("CO2").molarMassGPerMol > 40.0) }
}
