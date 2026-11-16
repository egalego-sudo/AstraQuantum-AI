package com.bittupandit.astraquantumai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale

@Composable
fun QuantumCircuitLab() {
    val white = Color(0xFF55D6FF)
    val muted = Color(0xFFA9B9D6)
    val panel = Color(0xFF1A2B4D)
    var qubits by remember { mutableIntStateOf(2) }
    var gate by remember { mutableStateOf("H") }
    var target by remember { mutableIntStateOf(0) }
    var control by remember { mutableIntStateOf(1) }
    var ops by remember { mutableStateOf(listOf<QuantumOp>()) }
    var result by remember { mutableStateOf<QuantumRun?>(null) }
    var message by remember { mutableStateOf("Circuit खाली है। H gate जोड़कर Run दबाएँ।") }
    var shots by remember { mutableIntStateOf(1000) }
    var sampleText by remember { mutableStateOf("") }
    var shorResult by remember { mutableStateOf<AstraQuantumSimulator.ShorDemoResult?>(null) }
    val gates = listOf("H", "X", "Y", "Z", "S", "T", "RX90", "RY90", "RZ90", "CNOT", "CZ")
    val isControlled = gate == "CNOT" || gate == "CZ"

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("EXACT STATE-VECTOR SIMULATOR", color = Color(0xFF6CF2D5), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Qubits: ${qubits}  •  ${1 shl qubits} complex amplitudes", color = white, fontWeight = FontWeight.Bold)
        Slider(value = qubits.toFloat(), onValueChange = { qubits = it.toInt().coerceIn(1, 10); ops = emptyList(); result = null; target = 0; control = if (qubits > 1) 1 else 0 }, valueRange = 1f..10f, steps = 8)

        Text("तैयार Quantum प्रयोग", color = white, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                qubits = 2
                target = 0
                control = 1
                gate = "H"
                ops = AstraQuantumSimulator.bellPhiPlus()
                val r = AstraQuantumSimulator.run(2, ops)
                result = r
                sampleText = ""
                message = if (AstraQuantumSimulator.verifyBellPhiPlus(r)) "Bell |Φ+⟩ परीक्षण PASS: |00⟩ और |11⟩ entangled state में हैं।" else "Bell state verification असफल।"
            }, modifier = Modifier.weight(1f)) { Text("Bell State") }
            Button(onClick = {
                qubits = 2
                target = 0
                control = 1
                gate = "H"
                ops = AstraQuantumSimulator.groverSearch11()
                val r = AstraQuantumSimulator.run(2, ops)
                result = r
                sampleText = ""
                message = if (AstraQuantumSimulator.verifyGroverSearch11(r)) "Grover परीक्षण PASS: 1 iteration में |11⟩ मिला।" else "Grover verification असफल।"
            }, modifier = Modifier.weight(1f)) { Text("Grover 2-Qubit") }
        }


        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                qubits = 3
                ops = listOf(QuantumOp("X", 0))
                gate = "H"
                target = 0
                control = 1
                result = AstraQuantumSimulator.qftBasisDemo()
                sampleText = ""
                message = "3-Qubit QFT सफल। Basis |001⟩ का Fourier spectrum नीचे है।"
            }, modifier = Modifier.weight(1f)) { Text("QFT Demo") }
            OutlinedButton(onClick = {
                qubits = 3
                ops = listOf(QuantumOp("X", 0))
                result = AstraQuantumSimulator.fourierTransform(AstraQuantumSimulator.qftBasisDemo(), inverse = true)
                sampleText = ""
                message = if (AstraQuantumSimulator.verifyQftRoundTrip()) "Inverse QFT PASS: मूल |001⟩ अवस्था वापस मिली।" else "Inverse QFT जाँच असफल।"
            }, modifier = Modifier.weight(1f)) { Text("Inverse QFT") }
        }


        Button(onClick = {
            runCatching { AstraQuantumSimulator.shorFactorDemo15() }
                .onSuccess { demo ->
                    shorResult = demo
                    qubits = 8
                    result = demo.run
                    ops = emptyList()
                    sampleText = ""
                    message = "Shor educational demo PASS: N=${demo.number} के factors ${demo.factors.first} और ${demo.factors.second}।"
                }
                .onFailure { message = "Shor demo error: ${it.message}"; shorResult = null }
        }, modifier = Modifier.fillMaxWidth()) { Text("Shor Algorithm · Factor 15") }
        shorResult?.let { demo ->
            Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("SHOR ORDER-FINDING RESULT", color = Color(0xFF6CF2D5), fontWeight = FontWeight.Bold)
                    Text("N = ${demo.number}  •  a = ${demo.base}  •  Period r = ${demo.period}", color = white)
                    Text("${demo.number} = ${demo.factors.first} × ${demo.factors.second}", color = white, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Inverse QFT phase peaks:", color = muted)
                    demo.phasePeaks.forEach { (phase, probability) ->
                        Text("k=$phase  •  P=${"%.2f".format(Locale.US, probability * 100)}%  •  k/16=$phase/16", color = white, fontSize = 11.sp)
                    }
                    Text("यह छोटा educational state-vector demo है: modular-function oracle को सीधे गणित से लागू किया गया है; reversible gate-by-gate Shor circuit और quantum hardware execution अभी शामिल नहीं हैं।", color = muted, fontSize = 11.sp)
                }
            }
        }

        Text("एक gate चुनें", color = muted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            gates.take(6).forEach { g -> FilterChip(selected = gate == g, onClick = { gate = g }, label = { Text(g) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            gates.drop(6).forEach { g -> FilterChip(selected = gate == g, onClick = { gate = g }, label = { Text(g) }) }
        }
        Text("Target Qubit: ${target}", color = white)
        Slider(value = target.toFloat(), onValueChange = { target = it.toInt().coerceIn(0, qubits - 1) }, valueRange = 0f..(qubits - 1).toFloat(), steps = (qubits - 2).coerceAtLeast(0))
        if (isControlled && qubits > 1) {
            Text("Control Qubit: ${control}", color = white)
            Slider(value = control.toFloat(), onValueChange = { control = it.toInt().coerceIn(0, qubits - 1) }, valueRange = 0f..(qubits - 1).toFloat(), steps = (qubits - 2).coerceAtLeast(0))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (isControlled && (qubits < 2 || control == target)) message = "CNOT/CZ के लिए अलग control और target चुनें।"
                else { ops = ops + QuantumOp(gate, target, if (isControlled) control else -1); message = "${gate}(Q${target}${if (isControlled) ", control Q${control}" else ""}) circuit में जुड़ा।"; result = null; sampleText = "" }
            }) { Text("＋ Gate जोड़ें") }
            OutlinedButton(onClick = { ops = emptyList(); result = null; sampleText = ""; message = "Circuit साफ़ कर दिया।" }) { Text("Reset", color = white) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("आपका Circuit (${ops.size} gates)", color = white, fontWeight = FontWeight.Bold)
                if (ops.isEmpty()) Text("|0…0⟩ प्रारंभिक अवस्था", color = muted)
                ops.forEachIndexed { i, op -> Text("${i + 1}. ${op.gate}  •  target Q${op.target}${if (op.control >= 0) "  •  control Q${op.control}" else ""}", color = muted) }
            }
        }
        Button(onClick = {
            runCatching { AstraQuantumSimulator.run(qubits, ops) }
                .onSuccess { result = it; message = "Simulation सफल। Norm = ${"%.10f".format(Locale.US, it.norm)}"; sampleText = "" }
                .onFailure { message = "Simulation error: ${it.message}" }
        }, modifier = Modifier.fillMaxWidth()) { Text("▶ Circuit Run करें") }
        Text(message, color = muted, fontSize = 12.sp)
        result?.let { run ->
            Text("STATE VECTOR · NON-ZERO AMPLITUDES", color = Color(0xFF6CF2D5), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            run.amplitudes.forEachIndexed { index, amp ->
                val p = amp.probability()
                if (p > 1e-9) {
                    val bits = index.toString(2).padStart(qubits, '0').reversed()
                    Text("|$bits⟩  ${"%.5f".format(Locale.US, p * 100)}%   α=(${"%.4f".format(Locale.US, amp.re)}, ${"%.4f".format(Locale.US, amp.im)}i)", color = white, fontSize = 11.sp)
                }
            }
            Text("Norm: ${"%.12f".format(Locale.US, run.norm)}  •  Entanglement संभव (CNOT/CZ)", color = muted, fontSize = 11.sp)
            Text("Measurement shots: ${shots}", color = white)
            Slider(value = shots.toFloat(), onValueChange = { shots = it.toInt().coerceIn(100, 10000) }, valueRange = 100f..10000f, steps = 98)
            OutlinedButton(onClick = {
                runCatching { AstraQuantumSimulator.sample(run, shots) }
                    .onSuccess { counts -> sampleText = counts.entries.joinToString("\n") { "| ${it.key}⟩ : ${it.value} (${"%.2f".format(Locale.US, it.value * 100.0 / shots)}%)" } }
                    .onFailure { sampleText = it.message ?: "Measurement failed" }
            }) { Text("Measurement चलाएँ", color = white) }
            if (sampleText.isNotBlank()) Text(sampleText, color = white, fontSize = 12.sp)
        }
        Text("सीमा और सटीकता: यह फोन पर चलने वाला classical pure-state simulator है, quantum hardware नहीं। अधिकतम 10 Qubits (1,024 complex amplitudes); noise, mixed states और वास्तविक decoherence अभी शामिल नहीं हैं।", color = muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
