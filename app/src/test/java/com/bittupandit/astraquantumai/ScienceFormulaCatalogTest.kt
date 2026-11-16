package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScienceFormulaCatalogTest {
    @Test fun catalogueHasVersionedSourceAttributedEntries() {
        assertTrue(ScienceFormulaCatalog.VERSION.isNotBlank())
        assertTrue(ScienceFormulaCatalog.formulas.size >= 15)
        ScienceFormulaCatalog.formulas.forEach {
            assertTrue(it.id.isNotBlank())
            assertTrue(it.equation.isNotBlank())
            assertTrue(it.variables.isNotEmpty())
            assertTrue(it.assumptions.isNotBlank())
            assertTrue(it.validity.isNotBlank())
            assertTrue(it.sourceName.isNotBlank())
            assertTrue(it.sourceUrl.startsWith("https://"))
        }
    }

    @Test fun searchesByDisciplineAndTopic() {
        assertTrue(ScienceFormulaCatalog.search("Physics Electricity").any { it.id == "physics.ohm" })
        assertTrue(ScienceFormulaCatalog.search("Chemistry Solutions").any { it.id == "chem.molarity" })
        assertTrue(ScienceFormulaCatalog.search("not-a-real-topic").isEmpty())
    }

    @Test fun findsFormulaByStableId() {
        assertNotNull(ScienceFormulaCatalog.byId("physics.ideal_gas"))
        assertEquals(null, ScienceFormulaCatalog.byId("missing"))
    }
}
