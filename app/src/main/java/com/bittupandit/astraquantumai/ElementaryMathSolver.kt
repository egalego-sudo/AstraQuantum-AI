package com.bittupandit.astraquantumai

/**
 * Deterministic solver for real-coefficient linear and quadratic equations.
 * Equation is represented as a*x^2 + b*x + c = 0.
 */
data class PolynomialSolution(
    val degree: Int,
    val roots: List<Double>,
    val discriminant: Double?,
    val classification: String,
    val working: String
)

object ElementaryMathSolver {
    fun solveLinear(a: Double, b: Double): PolynomialSolution {
        require(a.isFinite() && b.isFinite()) { "Coefficients must be finite." }
        require(a != 0.0) { "For a = 0 this is not a unique linear equation." }
        val root = -b / a
        require(root.isFinite()) { "The solution is outside the supported numeric range." }
        return PolynomialSolution(1, listOf(root), null, "ONE_REAL_ROOT",
            "ax + b = 0; x = -b/a = " + root)
    }

    fun solveQuadratic(a: Double, b: Double, c: Double): PolynomialSolution {
        require(a.isFinite() && b.isFinite() && c.isFinite()) { "Coefficients must be finite." }
        require(a != 0.0) { "Quadratic coefficient a must be non-zero." }
        val d = b * b - 4.0 * a * c
        require(d.isFinite()) { "Discriminant overflow; rescale the coefficients." }
        val scale = maxOf(1.0, kotlin.math.abs(b * b), kotlin.math.abs(4.0 * a * c))
        val tolerance = 1e-12 * scale
        return when {
            d > tolerance -> {
                val sqrtD = kotlin.math.sqrt(d)
                // Stable form avoids cancellation for one of the roots.
                val q = -0.5 * (b + kotlin.math.copySign(sqrtD, b))
                val r1 = q / a
                val r2 = c / q
                PolynomialSolution(2, listOf(r1, r2).sorted(), d, "TWO_DISTINCT_REAL_ROOTS",
                    "D = b² - 4ac = " + d + "; x = (-b ± √D)/(2a)")
            }
            d < -tolerance -> PolynomialSolution(2, emptyList(), d, "NO_REAL_ROOTS",
                "D = b² - 4ac = " + d + "; roots are a complex-conjugate pair.")
            else -> {
                val root = -b / (2.0 * a)
                PolynomialSolution(2, listOf(root), 0.0, "ONE_REPEATED_REAL_ROOT",
                    "D ≈ 0; x = -b/(2a) = " + root)
            }
        }
    }

    fun evaluatePolynomial(coefficientsDescending: List<Double>, x: Double): Double {
        require(coefficientsDescending.isNotEmpty() && coefficientsDescending.size <= 21) {
            "Provide 1 to 21 polynomial coefficients."
        }
        require(x.isFinite() && coefficientsDescending.all { it.isFinite() }) { "Inputs must be finite." }
        return coefficientsDescending.fold(0.0) { acc, coefficient -> acc * x + coefficient }
    }

    fun derivativePowerRule(coefficient: Double, exponent: Int): Pair<Double, Int> {
        require(coefficient.isFinite()) { "Coefficient must be finite." }
        require(exponent in -1000..1000) { "Exponent must be between -1000 and 1000." }
        return Pair(coefficient * exponent, exponent - 1)
    }
}
