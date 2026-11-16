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
