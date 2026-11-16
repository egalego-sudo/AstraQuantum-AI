package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScienceUnitConverterTest {
    @Test fun convertsMetricLength() { assertEquals(1000.0, ScienceUnitConverter.convert(1.0, "km", "m"), 1e-10) }
    @Test fun convertsTemperatureWithOffset() { assertEquals(32.0, ScienceUnitConverter.convert(0.0, "°C", "°F"), 1e-9) }
    @Test fun rejectsIncompatibleDimensions() { assertThrows(IllegalArgumentException::class.java) { ScienceUnitConverter.convert(1.0, "kg", "m") } }
    @Test fun convertsEnergy() { assertEquals(3600000.0, ScienceUnitConverter.convert(1.0, "kWh", "J"), 1e-6) }
}
