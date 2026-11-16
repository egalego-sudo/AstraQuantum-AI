package com.bittupandit.astraquantumai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Exact sparse state-vector simulator for up to 64 qubits.
 * It is exact while the number of non-zero basis amplitudes stays within MAX_TERMS.
 * Qubit 0 is the least-significant bit.
 */
data class SparseQuantumRun(
    val qubits: Int,
    val amplitudes: Map<Long, ComplexAmp>,
    val norm: Double,
    val operations: List<QuantumOp>
)

object AstraSparseQuantumSimulator {
    const val MAX_QUBITS = 64
    const val MAX_TERMS = 100_000
    private const val EPS = 1e-14
    private val invSqrt2 = 1.0 / kotlin.math.sqrt(2.0)

    fun run(qubits: Int, operations: List<QuantumOp>): SparseQuantumRun {
        require(qubits in 1..MAX_QUBITS) { "कृपया 1 से 64 Qubits चुनें।" }
        var state: Map<Long, ComplexAmp> = mapOf(0L to ComplexAmp(1.0, 0.0))
        operations.forEachIndexed { index, op ->
            require(op.target in 0 until qubits) { "Gate ${index + 1}: Target Qubit सीमा से बाहर है।" }
            state = when (op.gate) {
                "CNOT", "CZ" -> {
                    require(op.control in 0 until qubits && op.control != op.target) {
                        "Gate ${index + 1}: Control और target अलग Qubits होने चाहिए।"
                    }
                    if (op.gate == "CNOT") applyCnot(state, op.control, op.target)
                    else applyCz(state, op.control, op.target)
                }
                else -> applySingle(state, op.target, op.gate)
            }
            require(state.size <= MAX_TERMS) {
                "Sparse simulation सीमा पार हुई: non-zero amplitudes ${state.size} से अधिक। इस circuit के लिए अधिक memory वाला backend चाहिए।"
            }
        }
        val norm = state.values.sumOf { it.probability() }
        return SparseQuantumRun(qubits, state, norm, operations.toList())
    }

    private fun applySingle(input: Map<Long, ComplexAmp>, q: Int, gate: String): Map<Long, ComplexAmp> {
        val mask = 1L shl q
        val (a, b, c, d) = when (gate) {
            "H" -> listOf(ComplexAmp(invSqrt2, 0.0), ComplexAmp(invSqrt2, 0.0), ComplexAmp(invSqrt2, 0.0), ComplexAmp(-invSqrt2, 0.0))
            "X" -> listOf(ComplexAmp(0.0, 0.0), ComplexAmp(1.0, 0.0), ComplexAmp(1.0, 0.0), ComplexAmp(0.0, 0.0))
            "Y" -> listOf(ComplexAmp(0.0, 0.0), ComplexAmp(0.0, -1.0), ComplexAmp(0.0, 1.0), ComplexAmp(0.0, 0.0))
            "Z" -> listOf(ComplexAmp(1.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(-1.0, 0.0))
            "S" -> listOf(ComplexAmp(1.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(0.0, 1.0))
            "SDG" -> listOf(ComplexAmp(1.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(0.0, -1.0))
            "T" -> listOf(ComplexAmp(1.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(invSqrt2, invSqrt2))
            "RX90" -> listOf(ComplexAmp(invSqrt2, 0.0), ComplexAmp(0.0, -invSqrt2), ComplexAmp(0.0, -invSqrt2), ComplexAmp(invSqrt2, 0.0))
            "RY90" -> listOf(ComplexAmp(invSqrt2, 0.0), ComplexAmp(-invSqrt2, 0.0), ComplexAmp(invSqrt2, 0.0), ComplexAmp(invSqrt2, 0.0))
            "RZ90" -> listOf(ComplexAmp(cos(PI / 4), -sin(PI / 4)), ComplexAmp(0.0, 0.0), ComplexAmp(0.0, 0.0), ComplexAmp(cos(PI / 4), sin(PI / 4)))
            else -> error("अज्ञात gate: $gate")
        }
        val bases = input.keys.map { it and mask.inv() }.toSet()
        val output = HashMap<Long, ComplexAmp>(minOf(MAX_TERMS, bases.size * 2))
        for (base in bases) {
            val v0 = input[base] ?: ComplexAmp(0.0, 0.0)
            val upper = base or mask
            val v1 = input[upper] ?: ComplexAmp(0.0, 0.0)
            val out0 = a * v0 + b * v1
            val out1 = c * v0 + d * v1
            if (out0.probability() > EPS * EPS) output[base] = out0
            if (out1.probability() > EPS * EPS) output[upper] = out1
            if (output.size > MAX_TERMS) {
                error("इस gate के बाद non-zero amplitudes $MAX_TERMS से अधिक हो जाएँगे। Sparse simulator ने सुरक्षित रूप से रोक दिया।")
            }
        }
        return output
    }

    private fun applyCnot(input: Map<Long, ComplexAmp>, control: Int, target: Int): Map<Long, ComplexAmp> {
        val controlMask = 1L shl control
        val targetMask = 1L shl target
        val output = HashMap<Long, ComplexAmp>(input.size)
        input.forEach { (basis, amp) ->
            val next = if ((basis and controlMask) != 0L) basis xor targetMask else basis
            output[next] = amp
        }
        return output
    }

    private fun applyCz(input: Map<Long, ComplexAmp>, control: Int, target: Int): Map<Long, ComplexAmp> {
        val controlMask = 1L shl control
        val targetMask = 1L shl target
        val output = HashMap<Long, ComplexAmp>(input.size)
        input.forEach { (basis, amp) ->
            output[basis] = if ((basis and controlMask) != 0L && (basis and targetMask) != 0L)
                ComplexAmp(-amp.re, -amp.im) else amp
        }
        return output
    }

    fun ghz64(): List<QuantumOp> = buildList {
        add(QuantumOp("H", target = 0))
        for (target in 1 until 64) add(QuantumOp("CNOT", target = target, control = 0))
    }

    fun basisLabel(index: Long, qubits: Int): String =
        java.lang.Long.toBinaryString(index).padStart(qubits, '0').takeLast(qubits).reversed()

    fun sample(run: SparseQuantumRun, shots: Int): Map<String, Int> {
        require(shots in 1..10_000) { "Shots 1 से 10,000 के बीच होने चाहिए।" }
        val entries = run.amplitudes.entries.toList()
        val total = entries.sumOf { it.value.probability() }
        require(kotlin.math.abs(total - 1.0) < 1e-8) { "Normalization जाँच विफल: $total" }
        val cumulative = DoubleArray(entries.size)
        var running = 0.0
        entries.forEachIndexed { i, entry ->
            running += entry.value.probability()
            cumulative[i] = running
        }
        val counts = linkedMapOf<String, Int>()
        repeat(shots) {
            val r = Random.nextDouble() * total
            val index = cumulative.indexOfFirst { it >= r }.let { if (it < 0) cumulative.lastIndex else it }
            val label = basisLabel(entries[index].key, run.qubits)
            counts[label] = (counts[label] ?: 0) + 1
        }
        return counts.toList().sortedByDescending { it.second }.toMap()
    }

    fun verifyGhz64(run: SparseQuantumRun, tolerance: Double = 1e-9): Boolean {
        if (run.qubits != 64 || kotlin.math.abs(run.norm - 1.0) > tolerance || run.amplitudes.size != 2) return false
        val zero = run.amplitudes[0L] ?: return false
        val allOnes = run.amplitudes[-1L] ?: return false
        return kotlin.math.abs(zero.re - invSqrt2) < tolerance &&
            kotlin.math.abs(zero.im) < tolerance &&
            kotlin.math.abs(allOnes.re - invSqrt2) < tolerance &&
            kotlin.math.abs(allOnes.im) < tolerance
    }
}

@Composable
fun Quantum64SparseLab() {
    val white = Color(0xFFECECEC)
    val muted = Color(0xFFB4B4B4)
    val panel = Color(0xFF2F2F2F)
    var qubits by remember { mutableIntStateOf(64) }
    var gate by remember { mutableStateOf("H") }
    var target by remember { mutableIntStateOf(0) }
    var control by remember { mutableIntStateOf(1) }
    var operations by remember { mutableStateOf(emptyList<QuantumOp>()) }
    var result by remember { mutableStateOf<SparseQuantumRun?>(null) }
    var shots by remember { mutableIntStateOf(1000) }
    var sampleText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("64-Qubit Sparse Mode तैयार है। GHZ-64 परीक्षण चलाएँ।") }
    val gates = listOf("H", "X", "Y", "Z", "S", "SDG", "T", "RX90", "RY90", "RZ90", "CNOT", "CZ")
    val controlled = gate == "CNOT" || gate == "CZ"

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ASTRA QUANTUM ENGINE · SPARSE MODE", color = Color(0xFF8E8EA0), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("64-Qubit Sparse Simulator", color = white, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Qubits: $qubits  •  संभावित basis states: 2^$qubits  •  वर्तमान circuit gates: ${operations.size}", color = muted, fontSize = 12.sp)
        Slider(value = qubits.toFloat(), onValueChange = {
            qubits = it.toInt().coerceIn(1, 64)
            target = target.coerceIn(0, qubits - 1)
            control = control.coerceIn(0, qubits - 1)
            operations = emptyList(); result = null; sampleText = ""
        }, valueRange = 1f..64f, steps = 62)
        Text("तैयार 64-Qubit प्रयोग", color = white, fontWeight = FontWeight.Bold)
        Button(onClick = {
            qubits = 64
            operations = AstraSparseQuantumSimulator.ghz64()
            val run = AstraSparseQuantumSimulator.run(64, operations)
            result = run
            sampleText = ""
            status = if (AstraSparseQuantumSimulator.verifyGhz64(run))
                "PASS: 64-Qubit GHZ entanglement तैयार। केवल 2 non-zero amplitudes, norm = ${"%.12f".format(run.norm)}"
            else "GHZ-64 verification असफल।"
        }, modifier = Modifier.fillMaxWidth()) { Text("▶ 64-Qubit GHZ Test") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                qubits = 2; target = 0; control = 1
                operations = listOf(QuantumOp("H", 0), QuantumOp("CNOT", 1, 0))
                result = AstraSparseQuantumSimulator.run(qubits, operations)
                sampleText = ""; status = "Bell pair test पूरा।"
            }, modifier = Modifier.weight(1f)) { Text("Bell Pair") }
            OutlinedButton(onClick = {
                operations = emptyList(); result = null; sampleText = ""; status = "Circuit reset।"
            }, modifier = Modifier.weight(1f)) { Text("Reset") }
        }
        Text("Gate चुनें", color = muted)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            gates.take(6).forEach { g -> FilterChip(selected = gate == g, onClick = { gate = g }, label = { Text(g) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            gates.drop(6).forEach { g -> FilterChip(selected = gate == g, onClick = { gate = g }, label = { Text(g) }) }
        }
        Text("Target Qubit: Q$target", color = white)
        Slider(value = target.toFloat(), onValueChange = { target = it.toInt().coerceIn(0, qubits - 1) }, valueRange = 0f..(qubits - 1).toFloat(), steps = (qubits - 2).coerceAtLeast(0))
        if (controlled && qubits > 1) {
            Text("Control Qubit: Q$control", color = white)
            Slider(value = control.toFloat(), onValueChange = { control = it.toInt().coerceIn(0, qubits - 1) }, valueRange = 0f..(qubits - 1).toFloat(), steps = (qubits - 2).coerceAtLeast(0))
        }
        Button(onClick = {
            if (controlled && (qubits < 2 || control == target)) status = "CNOT/CZ के लिए अलग control और target चुनें।"
            else {
                operations = operations + QuantumOp(gate, target, if (controlled) control else -1)
                result = null; sampleText = ""; status = "$gate gate circuit में जोड़ा गया।"
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("＋ Gate जोड़ें") }
        Button(onClick = {
            runCatching { AstraSparseQuantumSimulator.run(qubits, operations) }
                .onSuccess { result = it; status = "Simulation PASS · norm = ${"%.12f".format(it.norm)} · non-zero terms = ${it.amplitudes.size}"; sampleText = "" }
                .onFailure { status = "Simulation रुकी: ${it.message}"; result = null }
        }, modifier = Modifier.fillMaxWidth()) { Text("▶ Circuit Run करें") }
        Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(status, color = white, fontWeight = FontWeight.SemiBold)
                Text("Creator by Bittu Pandit", color = muted, fontSize = 10.sp)
            }
        }
        result?.let { run ->
            Text("Non-zero amplitudes: ${run.amplitudes.size} / 2^${run.qubits}", color = Color(0xFF6CF2D5), fontWeight = FontWeight.Bold)
            run.amplitudes.entries.sortedBy { it.key }.take(40).forEach { (basis, amp) ->
                Text("| ${AstraSparseQuantumSimulator.basisLabel(basis, run.qubits)} ⟩  P=${"%.5f".format(amp.probability() * 100)}%", color = white, fontSize = 11.sp)
            }
            if (run.amplitudes.size > 40) Text("पहली 40 अवस्थाएँ दिखाई गई हैं।", color = muted, fontSize = 10.sp)
            Text("Measurement shots: $shots", color = white)
            Slider(value = shots.toFloat(), onValueChange = { shots = it.toInt().coerceIn(100, 10000) }, valueRange = 100f..10000f, steps = 98)
            OutlinedButton(onClick = {
                runCatching { AstraSparseQuantumSimulator.sample(run, shots) }
                    .onSuccess { counts -> sampleText = counts.entries.joinToString("\n") { "| ${it.key}⟩ : ${it.value} (${"%.2f".format(it.value * 100.0 / shots)}%)" } }
                    .onFailure { sampleText = it.message ?: "Measurement failed" }
            }) { Text("Measurement चलाएँ", color = white) }
            if (sampleText.isNotBlank()) SelectionContainer { Text(sampleText, color = white, fontSize = 11.sp) }
        }
        Text("सीमा: यह classical sparse state-vector simulation है, वास्तविक quantum hardware नहीं। यह 64 qubits तक उन circuits को ठीक-ठीक simulate करता है जिनमें non-zero amplitudes 100,000 से अधिक न हों। सामान्य arbitrary 64-qubit circuits इस सीमा से बाहर हो सकते हैं।", color = muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
