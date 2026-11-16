package com.bittupandit.astraquantumai

/** Versioned subset of exact/defined and measured SI constants with provenance. */
data class ScientificConstant(
    val id: String,
    val name: String,
    val symbol: String,
    val value: Double,
    val unit: String,
    val uncertainty: String,
    val source: String,
    val sourceUrl: String
)

object ScientificConstantsCatalog {
    const val VERSION = "0.1.0"
    val constants = listOf(
        ScientificConstant("si.c", "Speed of light in vacuum", "c", 299792458.0, "m/s", "exact",
            "BIPM SI Brochure", "https://www.bipm.org/en/publications/si-brochure"),
        ScientificConstant("si.h", "Planck constant", "h", 6.62607015e-34, "J·s", "exact",
            "BIPM SI Brochure", "https://www.bipm.org/en/publications/si-brochure"),
        ScientificConstant("si.e", "Elementary charge", "e", 1.602176634e-19, "C", "exact",
            "BIPM SI Brochure", "https://www.bipm.org/en/publications/si-brochure"),
        ScientificConstant("si.kb", "Boltzmann constant", "kB", 1.380649e-23, "J/K", "exact",
            "BIPM SI Brochure", "https://www.bipm.org/en/publications/si-brochure"),
        ScientificConstant("si.na", "Avogadro constant", "NA", 6.02214076e23, "mol⁻¹", "exact",
            "BIPM SI Brochure", "https://www.bipm.org/en/publications/si-brochure"),
        ScientificConstant("si.g0", "Standard acceleration of gravity", "g₀", 9.80665, "m/s²", "conventional exact value",
            "NIST Guide to SI", "https://www.nist.gov/pml/special-publication-811"),
        ScientificConstant("codata.g", "Newtonian constant of gravitation", "G", 6.67430e-11, "m³·kg⁻¹·s⁻²", "measured; consult current CODATA adjustment",
            "NIST CODATA", "https://physics.nist.gov/cuu/Constants/"),
        ScientificConstant("codata.r", "Molar gas constant", "R", 8.31446261815324, "J·mol⁻¹·K⁻¹", "derived from exact kB and NA",
            "NIST CODATA", "https://physics.nist.gov/cuu/Constants/")
    )

    fun search(query: String): List<ScientificConstant> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return constants
        return constants.filter {
            listOf(it.id, it.name, it.symbol, it.unit).any { field -> field.lowercase().contains(q) }
        }
    }
}
