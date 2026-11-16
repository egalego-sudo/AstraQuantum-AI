package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScienceCalculatorTest {
    @Test fun calculatesNewtonForce() {
        assertEquals(20.0, ScienceCalculator.netForce(5.0, 4.0).value, 1e-12)
    }
    @Test fun calculatesKineticEnergy() {
        assertEquals(100.0, ScienceCalculator.kineticEnergy(2.0, 10.0).value, 1e-12)
    }
    @Test fun calculatesOhmsLawCurrent() {
        assertEquals(2.0, ScienceCalculator.ohmsLawCurrent(12.0, 6.0).value, 1e-12)
    }
    @Test fun calculatesIdealGasPressure() {
        val result = ScienceCalculator.idealGasPressure(1.0, 273.15, 0.022414)
        assertEquals(101325.0, result.value, 180.0)
        assertEquals("Pa", result.unit)
    }
    @Test fun calculatesMolarConcentrationInSiUnits() {
        assertEquals(1000.0, ScienceCalculator.molarConcentration(1.0, 0.001).value, 1e-12)
        assertEquals("mol/m³", ScienceCalculator.molarConcentration(1.0, 0.001).unit)
    }
    @Test fun calculatesSensibleHeat() {
        assertEquals(4184.0, ScienceCalculator.sensibleHeat(1.0, 4184.0, 1.0).value, 1e-9)
    }
    @Test fun calculatesWaveSpeed() {
        assertEquals(340.0, ScienceCalculator.waveSpeed(170.0, 2.0).value, 1e-12)
    }
    @Test fun calculatesNewtonianGravity() {
        assertEquals(6.67430e-11, ScienceCalculator.gravitationalForce(1.0, 1.0, 1.0).value, 1e-20)
    }
    @Test fun calculatesWorkAtRightAngleAsZero() {
        assertEquals(0.0, ScienceCalculator.workByConstantForce(10.0, 5.0, 90.0).value, 1e-12)
    }
    @Test fun calculatesDensityAndPressure() {
        assertEquals(1000.0, ScienceCalculator.density(1.0, 0.001).value, 1e-9)
        assertEquals(100.0, ScienceCalculator.pressure(200.0, 2.0).value, 1e-12)
    }
    @Test fun calculatesElectricalPowerAndPhotonEnergy() {
        assertEquals(60.0, ScienceCalculator.electricPower(12.0, 5.0).value, 1e-12)
        assertEquals(6.62607015e-34, ScienceCalculator.photonEnergy(1.0).value, 1e-45)
    }
    @Test fun rejectsInvalidPhysicalInputs() {
        try { ScienceCalculator.idealGasPressure(1.0, 0.0, 1.0); fail("Expected validation error") }
        catch (_: IllegalArgumentException) { }
        try { ScienceCalculator.ohmsLawCurrent(12.0, 0.0); fail("Expected validation error") }
        catch (_: IllegalArgumentException) { }
        try { ScienceCalculator.kineticEnergy(1.0, -2.0); fail("Expected validation error") }
        catch (_: IllegalArgumentException) { }
    }
}
