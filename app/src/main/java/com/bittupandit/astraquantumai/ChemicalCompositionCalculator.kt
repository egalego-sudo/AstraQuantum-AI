package com.bittupandit.astraquantumai

data class ChemicalComposition(
    val formula: String,
    val atomCounts: Map<String, Int>,
    val molarMassGPerMol: Double,
    val massPercentByElement: Map<String, Double>
)

/** Formula parser and molar-mass calculator for neutral formulae using the local periodic table. */
object ChemicalCompositionCalculator {
    fun analyze(rawFormula: String): ChemicalComposition {
        val formula = rawFormula.trim()
        require(formula.isNotEmpty() && formula.length <= 120) { "Enter a formula up to 120 characters." }
        require(formula.all { it.isLetterOrDigit() || it == '(' || it == ')' }) { "Only element symbols, integer subscripts and parentheses are supported." }
        var index = 0
        fun number(): Int {
            val start = index
            while (index < formula.length && formula[index].isDigit()) index++
            if (start == index) return 1
            val value = formula.substring(start, index).toInt()
            require(value in 1..10000) { "Subscript must be between 1 and 10000." }
            return value
        }
        fun group(nested: Boolean = false): MutableMap<String, Int> {
            val result = linkedMapOf<String, Int>()
            while (index < formula.length && formula[index] != ')') {
                if (formula[index] == '(') {
                    index++
                    val inside = group(true)
                    require(index < formula.length && formula[index] == ')') { "Unclosed parenthesis." }
                    index++
                    val multiplier = number()
                    inside.forEach { (symbol, count) ->
                        result[symbol] = Math.addExact(result.getOrDefault(symbol, 0), Math.multiplyExact(count, multiplier))
                    }
                } else {
                    require(formula[index].isUpperCase()) { "Invalid element symbol near position ${index + 1}." }
                    val start = index++
                    if (index < formula.length && formula[index].isLowerCase()) index++
                    val symbol = formula.substring(start, index)
                    require(PeriodicTableCatalog.elements.any { it.symbol == symbol }) { "Unknown element symbol: $symbol" }
                    val count = number()
                    result[symbol] = Math.addExact(result.getOrDefault(symbol, 0), count)
                }
                require(result.values.all { it <= 100000 }) { "Atom count is too large." }
            }
            if (nested) require(index < formula.length && formula[index] == ')') { "Unclosed parenthesis." }
            return result
        }
        val counts = group()
        require(index == formula.length && counts.isNotEmpty()) { "Unexpected closing parenthesis." }
        val masses = counts.mapValues { (symbol, count) ->
            val element = PeriodicTableCatalog.elements.first { it.symbol == symbol }
            val atomicMass = element.abridgedAtomicMass
                ?: throw IllegalArgumentException("No standard atomic weight is listed for $symbol; use an isotope-specific mass.")
            atomicMass * count
        }
        val total = masses.values.sum()
        require(total.isFinite() && total > 0.0) { "Molar mass is outside the supported range." }
        val percentages = masses.mapValues { it.value / total * 100.0 }
        return ChemicalComposition(formula, counts.toSortedMap(), total, percentages.toSortedMap())
    }
}
