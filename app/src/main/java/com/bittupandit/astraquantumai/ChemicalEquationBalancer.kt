package com.bittupandit.astraquantumai

/**
 * Bounded stoichiometric balancer for ordinary neutral molecular formulae.
 * Supports element symbols, integer subscripts and nested parentheses.
 * Hydrates, charges, isotopes and redox half-reaction notation are not supported.
 */
data class BalancedChemicalEquation(
    val reactants: List<Int>,
    val products: List<Int>,
    val equation: String
)

object ChemicalEquationBalancer {
    fun balance(input: String, maxCoefficient: Int = 12): BalancedChemicalEquation {
        require(maxCoefficient in 2..20) { "Coefficient limit must be between 2 and 20." }
        val arrowParts = input.replace("→", "->").split("->")
        require(arrowParts.size == 2) { "Use exactly one -> reaction arrow." }
        val left = arrowParts[0].split("+").map { it.trim() }
        val right = arrowParts[1].split("+").map { it.trim() }
        require(left.all { it.isNotBlank() } && right.all { it.isNotBlank() }) { "Every reaction term needs a formula." }
        val terms = left + right
        require(terms.size in 2..6) { "This version supports 2 to 6 reaction terms." }
        val formulae = terms.map(::parseFormula)
        val elements = formulae.flatMap { it.keys }.distinct().sorted()
        val coefficients = IntArray(terms.size)
        var best: List<Int>? = null
        var bestSum = Int.MAX_VALUE

        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        fun primitive(): Boolean = coefficients.reduce { a, b -> gcd(a, b) } == 1
        fun balanced(): Boolean = elements.all { e ->
            val lhs = left.indices.sumOf { formulae[it].getOrDefault(e, 0) * coefficients[it] }
            val rhs = right.indices.sumOf { formulae[left.size + it].getOrDefault(e, 0) * coefficients[left.size + it] }
            lhs == rhs
        }
        fun search(pos: Int, sum: Int) {
            if (sum >= bestSum) return
            if (pos == terms.lastIndex) {
                for (c in 1..maxCoefficient) {
                    if (sum + c >= bestSum) break
                    coefficients[pos] = c
                    if (primitive() && balanced()) {
                        best = coefficients.toList()
                        bestSum = sum + c
                    }
                }
                return
            }
            for (c in 1..maxCoefficient) {
                if (sum + c >= bestSum) break
                coefficients[pos] = c
                search(pos + 1, sum + c)
            }
        }
        search(0, 0)
        val result = best ?: throw IllegalArgumentException(
            "No balance found within coefficient limit " + maxCoefficient + ". Check the formula or use a supported reaction."
        )
        fun render(formula: String, coefficient: Int) = (if (coefficient == 1) "" else coefficient.toString()) + formula
        val rendered = left.indices.joinToString(" + ") { render(left[it], result[it]) } +
            " → " + right.indices.joinToString(" + ") { render(right[it], result[left.size + it]) }
        return BalancedChemicalEquation(result.take(left.size), result.drop(left.size), rendered)
    }

    private fun parseFormula(raw: String): Map<String, Int> {
        val formula = raw.trim().replace(Regex("^\\d+"), "")
        require(formula.isNotEmpty() && formula.all { it.isLetterOrDigit() || it == '(' || it == ')' }) {
            "Unsupported formula: " + raw
        }
        var index = 0
        fun readNumber(): Int {
            val start = index
            while (index < formula.length && formula[index].isDigit()) index++
            if (start == index) return 1
            val value = formula.substring(start, index).toInt()
            require(value > 0) { "Formula subscripts must be positive." }
            return value
        }
        fun parseGroup(): MutableMap<String, Int> {
            val counts = mutableMapOf<String, Int>()
            while (index < formula.length && formula[index] != ')') {
                if (formula[index] == '(') {
                    index++
                    val nested = parseGroup()
                    require(index < formula.length && formula[index] == ')') { "Unclosed parenthesis in " + raw }
                    index++
                    val multiplier = readNumber()
                    nested.forEach { (element, count) ->
                        counts[element] = counts.getOrDefault(element, 0) + count * multiplier
                    }
                } else {
                    require(formula[index].isUpperCase()) { "Invalid element symbol in " + raw }
                    val start = index++
                    if (index < formula.length && formula[index].isLowerCase()) index++
                    val symbol = formula.substring(start, index)
                    val count = readNumber()
                    counts[symbol] = counts.getOrDefault(symbol, 0) + count
                }
            }
            return counts
        }
        val parsed = parseGroup()
        require(index == formula.length && parsed.isNotEmpty()) { "Unexpected closing parenthesis in " + raw }
        return parsed
    }
}
