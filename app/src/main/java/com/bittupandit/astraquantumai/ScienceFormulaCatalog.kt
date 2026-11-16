package com.bittupandit.astraquantumai

/**
 * Curated, source-attributed starter catalogue for ASTRA's Science Core.
 * This is deliberately a versioned seed set, not a claim to contain every scientific formula.
 */
data class ScienceVariable(val symbol: String, val meaning: String, val siUnit: String)

data class ScienceFormula(
    val id: String,
    val discipline: String,
    val topic: String,
    val name: String,
    val equation: String,
    val variables: List<ScienceVariable>,
    val assumptions: String,
    val validity: String,
    val sourceName: String,
    val sourceUrl: String
)

object ScienceFormulaCatalog {
    const val VERSION = "0.1.0"

    private fun v(symbol: String, meaning: String, unit: String) =
        ScienceVariable(symbol, meaning, unit)

    val formulas: List<ScienceFormula> = listOf(
        ScienceFormula("physics.newton2", "Physics", "Mechanics", "Newton's second law",
            "ΣF = m a", listOf(v("ΣF", "net force", "N"), v("m", "mass", "kg"), v("a", "acceleration", "m/s²")),
            "Classical inertial frame; mass treated as constant.", "Speeds much smaller than light speed; classical mechanics.",
            "OpenStax University Physics, Vol. 1", "https://openstax.org/details/books/university-physics-volume-1"),
        ScienceFormula("physics.kinetic_energy", "Physics", "Mechanics", "Translational kinetic energy",
            "K = ½ m v²", listOf(v("K", "kinetic energy", "J"), v("m", "mass", "kg"), v("v", "speed", "m/s")),
            "Non-relativistic point particle or rigid-body translational motion.", "v much smaller than c.",
            "OpenStax University Physics, Vol. 1", "https://openstax.org/details/books/university-physics-volume-1"),
        ScienceFormula("physics.gravitational_force", "Physics", "Gravitation", "Newton's universal gravitation",
            "F = G m₁ m₂ / r²", listOf(v("F", "gravitational force magnitude", "N"), v("G", "gravitational constant", "m³ kg⁻¹ s⁻²"), v("m₁,m₂", "masses", "kg"), v("r", "centre-to-centre distance", "m")),
            "Spherically symmetric bodies or point masses; weak-field Newtonian limit.", "Weak gravitational fields and non-relativistic motion.",
            "NIST CODATA Fundamental Constants", "https://www.nist.gov/pml/fundamental-physical-constants"),
        ScienceFormula("physics.work", "Physics", "Energy", "Work by a constant force",
            "W = F d cos(θ)", listOf(v("W", "work", "J"), v("F", "force magnitude", "N"), v("d", "displacement", "m"), v("θ", "angle between force and displacement", "rad")),
            "Force is constant over the displacement.", "For variable force use the line integral of force.",
            "OpenStax University Physics, Vol. 1", "https://openstax.org/details/books/university-physics-volume-1"),
        ScienceFormula("physics.ohm", "Physics", "Electricity", "Ohm's law",
            "V = I R", listOf(v("V", "potential difference", "V"), v("I", "current", "A"), v("R", "resistance", "Ω")),
            "Ohmic element under fixed physical conditions.", "Not universal for nonlinear devices or changing temperature.",
            "OpenStax University Physics, Vol. 2", "https://openstax.org/details/books/university-physics-volume-2"),
        ScienceFormula("physics.electric_power", "Physics", "Electricity", "Electrical power",
            "P = V I = I²R = V²/R", listOf(v("P", "power", "W"), v("V", "voltage", "V"), v("I", "current", "A"), v("R", "resistance", "Ω")),
            "Lumped circuit element; resistive forms require Ohm's law.", "For AC use appropriate RMS/phase or complex-power treatment.",
            "OpenStax University Physics, Vol. 2", "https://openstax.org/details/books/university-physics-volume-2"),
        ScienceFormula("physics.coulomb", "Physics", "Electricity", "Coulomb's law",
            "F = kₑ |q₁ q₂| / r²", listOf(v("F", "electrostatic force magnitude", "N"), v("kₑ", "Coulomb constant", "N m²/C²"), v("q₁,q₂", "charges", "C"), v("r", "separation", "m")),
            "Point charges in vacuum; magnitude form shown.", "Electrostatic limit; use fields/material permittivity for other media.",
            "OpenStax University Physics, Vol. 2", "https://openstax.org/details/books/university-physics-volume-2"),
        ScienceFormula("physics.heat", "Physics", "Thermodynamics", "Sensible heat",
            "Q = m cₚ ΔT", listOf(v("Q", "heat transferred", "J"), v("m", "mass", "kg"), v("cₚ", "specific heat capacity", "J kg⁻¹ K⁻¹"), v("ΔT", "temperature change", "K")),
            "Specific heat approximately constant over the temperature interval; no phase change.", "Use temperature-dependent heat capacity or latent heat when required.",
            "OpenStax University Physics, Vol. 2", "https://openstax.org/details/books/university-physics-volume-2"),
        ScienceFormula("physics.ideal_gas", "Physics", "Thermodynamics", "Ideal gas equation",
            "P V = n R T", listOf(v("P", "absolute pressure", "Pa"), v("V", "volume", "m³"), v("n", "amount of substance", "mol"), v("R", "molar gas constant", "J mol⁻¹ K⁻¹"), v("T", "absolute temperature", "K")),
            "Gas treated as ideal; equilibrium state.", "Low density/high temperature away from condensation and strong intermolecular effects.",
            "OpenStax Chemistry 2e", "https://openstax.org/details/books/chemistry-2e"),
        ScienceFormula("physics.wave", "Physics", "Waves", "Wave speed",
            "v = f λ", listOf(v("v", "wave speed", "m/s"), v("f", "frequency", "Hz"), v("λ", "wavelength", "m")),
            "Single-frequency sinusoidal component in a medium.", "For dispersive media phase and group velocities differ.",
            "OpenStax University Physics, Vol. 1", "https://openstax.org/details/books/university-physics-volume-1"),
        ScienceFormula("chem.molarity", "Chemistry", "Solutions", "Molar concentration",
            "c = n / V", listOf(v("c", "amount concentration", "mol/L"), v("n", "amount of solute", "mol"), v("V", "solution volume", "L")),
            "Volume refers to final solution volume.", "Temperature can affect solution volume; use consistent units.",
            "OpenStax Chemistry 2e", "https://openstax.org/details/books/chemistry-2e"),
        ScienceFormula("chem.ph", "Chemistry", "Acids and bases", "pH definition",
            "pH = −log₁₀(aH⁺)", listOf(v("pH", "negative logarithm of hydrogen-ion activity", "dimensionless"), v("aH⁺", "hydrogen-ion activity", "dimensionless")),
            "Activity is dimensionless relative to the standard state.", "For dilute solutions activity may be approximated by concentration with a stated convention.",
            "IUPAC Gold Book", "https://goldbook.iupac.org/terms/view/P04524"),
        ScienceFormula("chem.gibbs", "Chemistry", "Thermodynamics", "Gibbs energy change",
            "ΔG = ΔH − T ΔS", listOf(v("ΔG", "Gibbs energy change", "J/mol"), v("ΔH", "enthalpy change", "J/mol"), v("T", "absolute temperature", "K"), v("ΔS", "entropy change", "J mol⁻¹ K⁻¹")),
            "Quantities refer to the same reaction basis and temperature; equation shown for the corresponding state change.", "For non-standard conditions include reaction quotient: ΔG = ΔG° + RT ln Q.",
            "OpenStax Chemistry 2e", "https://openstax.org/details/books/chemistry-2e"),
        ScienceFormula("math.quadratic", "Mathematics", "Algebra", "Quadratic formula",
            "x = (−b ± √(b² − 4ac)) / (2a)", listOf(v("a,b,c", "coefficients in ax²+bx+c=0", "dimension-dependent"), v("x", "root", "same dimension as x")),
            "a ≠ 0; coefficients belong to a field where the required square root is defined.", "Real roots when discriminant is non-negative; complex roots otherwise.",
            "OpenStax College Algebra", "https://openstax.org/details/books/college-algebra"),
        ScienceFormula("math.derivative_power", "Mathematics", "Calculus", "Power rule",
            "d(xⁿ)/dx = n xⁿ⁻¹", listOf(v("x", "independent variable", "domain-dependent"), v("n", "constant exponent", "dimensionless")),
            "Differentiability on the domain; real-variable form for standard real n where defined.", "Check domain at singular points and endpoints.",
            "OpenStax Calculus, Vol. 1", "https://openstax.org/details/books/calculus-volume-1")
    )

    fun search(query: String): List<ScienceFormula> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return formulas
        return formulas.filter { formula ->
            val haystack = listOf(formula.id, formula.discipline, formula.topic, formula.name, formula.equation,
                formula.assumptions, formula.validity).joinToString(" ").lowercase()
            terms.all(haystack::contains)
        }
    }

    fun byId(id: String): ScienceFormula? = formulas.firstOrNull { it.id == id }
}
