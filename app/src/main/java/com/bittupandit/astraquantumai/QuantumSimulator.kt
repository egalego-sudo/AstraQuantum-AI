package com.bittupandit.astraquantumai

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Exact pure-state state-vector simulator. Qubit 0 is the least-significant bit. */
data class QuantumOp(val gate: String, val target: Int, val control: Int = -1)

data class ComplexAmp(val re: Double, val im: Double) {
    operator fun plus(o: ComplexAmp) = ComplexAmp(re + o.re, im + o.im)
    operator fun times(o: ComplexAmp) = ComplexAmp(re * o.re - im * o.im, re * o.im + im * o.re)
    fun probability() = re * re + im * im
}

data class QuantumRun(val amplitudes: List<ComplexAmp>, val norm: Double, val operations: List<QuantumOp>)

object AstraQuantumSimulator {
    const val MAX_QUBITS = 10
    private val invSqrt2 = 1.0 / kotlin.math.sqrt(2.0)

    fun run(qubits: Int, operations: List<QuantumOp>): QuantumRun {
        require(qubits in 1..MAX_QUBITS) { "कृपया 1 से 10 Qubits चुनें।" }
        val size = 1 shl qubits
        var state = Array(size) { ComplexAmp(0.0, 0.0) }
        state[0] = ComplexAmp(1.0, 0.0)
        operations.forEach { op ->
            require(op.target in 0 until qubits) { "Target Qubit सीमा से बाहर है।" }
            if (op.gate == "CNOT" || op.gate == "CZ") {
                require(op.control in 0 until qubits && op.control != op.target) { "Control और target अलग Qubits होने चाहिए।" }
                state = when (op.gate) {
                    "CNOT" -> applyCnot(state, op.control, op.target)
                    else -> applyCz(state, op.control, op.target)
                }
            } else state = applySingle(state, op.target, op.gate)
        }
        val norm = state.sumOf { it.probability() }
        return QuantumRun(state.toList(), norm, operations.toList())
    }

    private fun applySingle(input: Array<ComplexAmp>, q: Int, gate: String): Array<ComplexAmp> {
        val out = input.copyOf()
        val mask = 1 shl q
        val (a, b, c, d) = when (gate) {
            "H" -> listOf(ComplexAmp(invSqrt2,0.0), ComplexAmp(invSqrt2,0.0), ComplexAmp(invSqrt2,0.0), ComplexAmp(-invSqrt2,0.0))
            "X" -> listOf(ComplexAmp(0.0,0.0), ComplexAmp(1.0,0.0), ComplexAmp(1.0,0.0), ComplexAmp(0.0,0.0))
            "Y" -> listOf(ComplexAmp(0.0,0.0), ComplexAmp(0.0,-1.0), ComplexAmp(0.0,1.0), ComplexAmp(0.0,0.0))
            "Z" -> listOf(ComplexAmp(1.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(-1.0,0.0))
            "S" -> listOf(ComplexAmp(1.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(0.0,1.0))
            "SDG" -> listOf(ComplexAmp(1.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(0.0,-1.0))
            "T" -> listOf(ComplexAmp(1.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(invSqrt2,invSqrt2))
            "RX90" -> listOf(ComplexAmp(invSqrt2,0.0), ComplexAmp(0.0,-invSqrt2), ComplexAmp(0.0,-invSqrt2), ComplexAmp(invSqrt2,0.0))
            "RY90" -> listOf(ComplexAmp(invSqrt2,0.0), ComplexAmp(-invSqrt2,0.0), ComplexAmp(invSqrt2,0.0), ComplexAmp(invSqrt2,0.0))
            "RZ90" -> listOf(ComplexAmp(cos(PI/4),-sin(PI/4)), ComplexAmp(0.0,0.0), ComplexAmp(0.0,0.0), ComplexAmp(cos(PI/4),sin(PI/4)))
            else -> error("अज्ञात gate: $gate")
        }
        for (base in input.indices) if ((base and mask) == 0) {
            val i0 = base
            val i1 = base or mask
            val v0 = input[i0]
            val v1 = input[i1]
            out[i0] = a * v0 + b * v1
            out[i1] = c * v0 + d * v1
        }
        return out
    }

    private fun applyCnot(input: Array<ComplexAmp>, control: Int, target: Int): Array<ComplexAmp> {
        val out = input.copyOf()
        val cm = 1 shl control
        val tm = 1 shl target
        for (i in input.indices) if ((i and cm) != 0 && (i and tm) == 0) {
            val j = i or tm
            out[i] = input[j]
            out[j] = input[i]
        }
        return out
    }

    private fun applyCz(input: Array<ComplexAmp>, control: Int, target: Int): Array<ComplexAmp> {
        val out = input.copyOf()
        val cm = 1 shl control
        val tm = 1 shl target
        for (i in input.indices) if ((i and cm) != 0 && (i and tm) != 0) {
            val v = input[i]
            out[i] = ComplexAmp(-v.re, -v.im)
        }
        return out
    }


    /** Bell |Phi+> = (|00> + |11>)/sqrt(2), with Q0 as control. */
    fun bellPhiPlus(): List<QuantumOp> = listOf(
        QuantumOp("H", target = 0),
        QuantumOp("CNOT", target = 1, control = 0)
    )

    /** Two-qubit Grover search for marked basis state |11>, one optimal iteration. */
    fun groverSearch11(): List<QuantumOp> = buildList {
        add(QuantumOp("H", 0)); add(QuantumOp("H", 1))
        add(QuantumOp("CZ", 1, 0)) // oracle: phase flip only on |11>
        add(QuantumOp("H", 0)); add(QuantumOp("H", 1))
        add(QuantumOp("X", 0)); add(QuantumOp("X", 1))
        add(QuantumOp("CZ", 1, 0)) // diffuser reflection about |++>
        add(QuantumOp("X", 0)); add(QuantumOp("X", 1))
        add(QuantumOp("H", 0)); add(QuantumOp("H", 1))
    }

    fun verifyBellPhiPlus(run: QuantumRun, tolerance: Double = 1e-9): Boolean {
        if (run.amplitudes.size != 4 || kotlin.math.abs(run.norm - 1.0) > tolerance) return false
        val a = run.amplitudes
        return kotlin.math.abs(a[0].re - invSqrt2) < tolerance &&
            kotlin.math.abs(a[0].im) < tolerance &&
            kotlin.math.abs(a[3].re - invSqrt2) < tolerance &&
            kotlin.math.abs(a[3].im) < tolerance &&
            a[1].probability() < tolerance && a[2].probability() < tolerance
    }

    fun verifyGroverSearch11(run: QuantumRun, tolerance: Double = 1e-9): Boolean {
        if (run.amplitudes.size != 4 || kotlin.math.abs(run.norm - 1.0) > tolerance) return false
        return run.amplitudes[3].probability() > 1.0 - tolerance &&
            run.amplitudes[0].probability() < tolerance &&
            run.amplitudes[1].probability() < tolerance &&
            run.amplitudes[2].probability() < tolerance
    }


    /** Discrete Quantum Fourier Transform over the full 2^n-dimensional state vector. */
    fun fourierTransform(run: QuantumRun, inverse: Boolean = false): QuantumRun {
        val n = run.amplitudes.size
        require(n in 2..(1 shl MAX_QUBITS) && (n and (n - 1)) == 0) { "QFT के लिए 1 से 10 Qubits चाहिए।" }
        val sign = if (inverse) -1.0 else 1.0
        val scale = 1.0 / kotlin.math.sqrt(n.toDouble())
        val output = MutableList(n) { ComplexAmp(0.0, 0.0) }
        for (y in 0 until n) {
            var sum = ComplexAmp(0.0, 0.0)
            for (x in 0 until n) {
                val angle = sign * 2.0 * PI * x.toDouble() * y.toDouble() / n.toDouble()
                val phase = ComplexAmp(cos(angle), sin(angle))
                sum += run.amplitudes[x] * phase
            }
            output[y] = ComplexAmp(sum.re * scale, sum.im * scale)
        }
        val norm = output.sumOf { it.probability() }
        return QuantumRun(output, norm, run.operations + QuantumOp(if (inverse) "IQFT" else "QFT", -1))
    }

    fun qftBasisDemo(): QuantumRun {
        val basisOne = run(3, listOf(QuantumOp("X", 0)))
        return fourierTransform(basisOne)
    }

    fun verifyQftRoundTrip(tolerance: Double = 1e-9): Boolean {
        val original = run(3, listOf(QuantumOp("X", 0)))
        val restored = fourierTransform(fourierTransform(original), inverse = true)
        return restored.amplitudes.indices.all { i ->
            kotlin.math.abs(restored.amplitudes[i].re - original.amplitudes[i].re) < tolerance &&
                kotlin.math.abs(restored.amplitudes[i].im - original.amplitudes[i].im) < tolerance
        }
    }


    data class ShorDemoResult(
        val run: QuantumRun,
        val number: Int,
        val base: Int,
        val period: Int,
        val factors: Pair<Int, Int>,
        val phasePeaks: List<Pair<Int, Double>>,
        val exponentQubits: Int
    )

    /**
     * Educational 2-register order-finding simulation for small N.
     * The modular-function oracle is evaluated directly in the simulator;
     * it is not a compiled gate-by-gate reversible oracle or hardware run.
     */
    fun shorFactorDemo15(): ShorDemoResult {
        val number = 15
        val base = 2
        val exponentQubits = 4
        val workQubits = 4
        val m = 1 shl exponentQubits
        val workSize = 1 shl workQubits
        val total = m * workSize
        val amplitudes = MutableList(total) { ComplexAmp(0.0, 0.0) }
        val initialAmp = 1.0 / kotlin.math.sqrt(m.toDouble())
        var value = 1
        for (x in 0 until m) {
            amplitudes[x or (value shl exponentQubits)] = ComplexAmp(initialAmp, 0.0)
            value = (value * base) % number
        }

        // Apply inverse QFT only to the exponent register, independently in each work-register slice.
        val transformed = MutableList(total) { ComplexAmp(0.0, 0.0) }
        for (work in 0 until workSize) {
            for (outX in 0 until m) {
                var sum = ComplexAmp(0.0, 0.0)
                for (inX in 0 until m) {
                    val input = amplitudes[inX or (work shl exponentQubits)]
                    val angle = -2.0 * PI * inX.toDouble() * outX.toDouble() / m.toDouble()
                    sum += input * ComplexAmp(cos(angle), sin(angle))
                }
                transformed[outX or (work shl exponentQubits)] =
                    ComplexAmp(sum.re / kotlin.math.sqrt(m.toDouble()), sum.im / kotlin.math.sqrt(m.toDouble()))
            }
        }
        val norm = transformed.sumOf { it.probability() }
        val phaseProbabilities = DoubleArray(m)
        transformed.forEachIndexed { index, amp -> phaseProbabilities[index and (m - 1)] += amp.probability() }
        val peaks = phaseProbabilities.indices
            .filter { phaseProbabilities[it] > 1e-8 }
            .sortedByDescending { phaseProbabilities[it] }
            .take(8)
            .map { it to phaseProbabilities[it] }

        val bestPhase = peaks.firstOrNull { it.first != 0 }?.first ?: 0
        val candidateOrder = if (bestPhase == 0) 0 else {
            (1..number).minByOrNull { q ->
                kotlin.math.abs(bestPhase.toDouble() / m.toDouble() - (bestPhase.toDouble() / m.toDouble() * q).let { kotlin.math.round(it) / q })
            } ?: 0
        }
        val period = if (candidateOrder > 0 && modularPower(base, candidateOrder, number) == 1) {
            candidateOrder
        } else {
            (1..number).firstOrNull { modularPower(base, it, number) == 1 } ?: 0
        }
        require(period > 0 && period % 2 == 0) { "इस demo से उपयोगी even period नहीं मिला।" }
        val halfPower = modularPower(base, period / 2, number)
        require(halfPower != 1) { "चुना हुआ period trivial है; दूसरा run चाहिए।" }
        val factorA = gcd(halfPower - 1, number)
        val factorB = gcd(halfPower + 1, number)
        require(factorA > 1 && factorB > 1 && factorA * factorB == number) { "Factor extraction सफल नहीं हुआ।" }
        val run = QuantumRun(transformed, norm, listOf(QuantumOp("MOD_EXP", -1), QuantumOp("IQFT", -1)))
        return ShorDemoResult(run, number, base, period, factorA to factorB, peaks, exponentQubits)
    }

    private fun modularPower(base: Int, exponent: Int, modulus: Int): Int {
        var result = 1
        repeat(exponent) { result = (result * base) % modulus }
        return result
    }

    private fun gcd(aValue: Int, bValue: Int): Int {
        var a = kotlin.math.abs(aValue)
        var b = kotlin.math.abs(bValue)
        while (b != 0) { val t = a % b; a = b; b = t }
        return a
    }

    fun sample(run: QuantumRun, shots: Int): Map<String, Int> {
        require(shots in 1..10000)
        val cumulative = DoubleArray(run.amplitudes.size)
        var total = 0.0
        run.amplitudes.forEachIndexed { i, amp -> total += amp.probability(); cumulative[i] = total }
        require(total > 0.999999 && total < 1.000001) { "State normalization जाँच विफल: $total" }
        val counts = linkedMapOf<String, Int>()
        repeat(shots) {
            val r = Random.nextDouble()
            val idx = cumulative.indexOfFirst { it >= r }.let { if (it < 0) cumulative.lastIndex else it }
            val bits = idx.toString(2).padStart(Integer.numberOfTrailingZeros(run.amplitudes.size), '0').reversed()
            counts[bits] = (counts[bits] ?: 0) + 1
        }
        return counts.toList().sortedByDescending { it.second }.toMap()
    }
}
