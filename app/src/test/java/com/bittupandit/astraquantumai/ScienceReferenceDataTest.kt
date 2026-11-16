package com.bittupandit.astraquantumai

import org.junit.Assert.*
import org.junit.Test

class ScienceReferenceDataTest {
    @Test fun constantsHaveUnitsAndProvenance() {
        assertTrue(ScientificConstantsCatalog.constants.size >= 8)
        ScientificConstantsCatalog.constants.forEach {
            assertTrue(it.unit.isNotBlank())
            assertTrue(it.source.isNotBlank())
            assertTrue(it.sourceUrl.startsWith("https://"))
        }
        assertEquals(299792458.0, ScientificConstantsCatalog.search("speed of light").single().value, 0.0)
    }

    @Test fun periodicTableLookupUsesAtomicNumberAndSymbol() {
        assertEquals("Carbon", PeriodicTableCatalog.byAtomicNumber(6)?.name)
        assertEquals(79, PeriodicTableCatalog.search("Au").single().atomicNumber)
        assertEquals("Hydrogen", PeriodicTableCatalog.search("hydrogen").single().name)
    }

    @Test fun missingElementReturnsNull() {
        assertNull(PeriodicTableCatalog.byAtomicNumber(200))
    }
}
