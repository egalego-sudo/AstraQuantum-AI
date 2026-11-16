package com.bittupandit.astraquantumai

/** Dimension-safe unit conversion. Affine offsets are used for temperature scales. */
data class ScienceUnit(
    val symbol: String,
    val dimension: String,
    val factorToBase: Double,
    val offsetToBase: Double = 0.0
)

object ScienceUnitConverter {
    private val units = listOf(
        ScienceUnit("m", "length", 1.0), ScienceUnit("km", "length", 1000.0),
        ScienceUnit("cm", "length", 0.01), ScienceUnit("mm", "length", 0.001),
        ScienceUnit("mi", "length", 1609.344), ScienceUnit("ft", "length", 0.3048),
        ScienceUnit("kg", "mass", 1.0), ScienceUnit("g", "mass", 0.001),
        ScienceUnit("mg", "mass", 1e-6), ScienceUnit("lb", "mass", 0.45359237),
        ScienceUnit("s", "time", 1.0), ScienceUnit("min", "time", 60.0),
        ScienceUnit("h", "time", 3600.0), ScienceUnit("day", "time", 86400.0),
        ScienceUnit("K", "temperature", 1.0), ScienceUnit("°C", "temperature", 1.0, 273.15),
        ScienceUnit("°F", "temperature", 5.0 / 9.0, 255.3722222222222),
        ScienceUnit("Pa", "pressure", 1.0), ScienceUnit("kPa", "pressure", 1000.0),
        ScienceUnit("bar", "pressure", 100000.0), ScienceUnit("atm", "pressure", 101325.0),
        ScienceUnit("J", "energy", 1.0), ScienceUnit("kJ", "energy", 1000.0),
        ScienceUnit("cal", "energy", 4.184), ScienceUnit("kcal", "energy", 4184.0),
        ScienceUnit("Wh", "energy", 3600.0), ScienceUnit("kWh", "energy", 3600000.0),
        ScienceUnit("L", "volume", 0.001), ScienceUnit("mL", "volume", 1e-6),
        ScienceUnit("m³", "volume", 1.0), ScienceUnit("m/s", "speed", 1.0),
        ScienceUnit("km/h", "speed", 1.0 / 3.6), ScienceUnit("mph", "speed", 0.44704)
    )
    fun supportedUnits(): List<ScienceUnit> = units
    fun convert(value: Double, from: String, to: String): Double {
        require(value.isFinite()) { "Value must be finite." }
        val source = units.firstOrNull { it.symbol == from } ?: throw IllegalArgumentException("Unknown source unit: $from")
        val target = units.firstOrNull { it.symbol == to } ?: throw IllegalArgumentException("Unknown target unit: $to")
        require(source.dimension == target.dimension) { "Incompatible dimensions: ${source.dimension} and ${target.dimension}." }
        val baseValue = value * source.factorToBase + source.offsetToBase
        val result = (baseValue - target.offsetToBase) / target.factorToBase
        require(result.isFinite()) { "Converted value is outside the supported numeric range." }
        return result
    }
}
