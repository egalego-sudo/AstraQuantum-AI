package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedScienceFormulaCatalogTest {
    @Test fun catalogHasBroadDisciplinesAndSources() {
        assertTrue(AdvancedScienceFormulaCatalog.formulas.size >= 25)
        assertTrue(AdvancedScienceFormulaCatalog.formulas.any { it.discipline == "Biology" })
        assertTrue(AdvancedScienceFormulaCatalog.formulas.any { it.discipline == "Engineering" })
        assertTrue(AdvancedScienceFormulaCatalog.formulas.all { it.sourceName.isNotBlank() && it.sourceUrl.startsWith("https://") })
    }
    @Test fun formulaIdsAreUnique() {
        val ids = AdvancedScienceFormulaCatalog.formulas.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }
}
