package com.bittupandit.astraquantumai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpressionCalculatorScreen() {
    var expression by remember { mutableStateOf("((12 + 8) * 3) / 5") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val white = Color(0xFFECECEC)
    val muted = Color(0xFFB4B4B4)
    val mint = Color(0xFFD6D6D6)
    val panel = Color(0xFF303030)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DETERMINISTIC CALCULATOR", color = white, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(
            "गणना फोन पर एक सीमित arithmetic parser से होती है—AI से नहीं। Supported: +, −, ×, ÷, remainder %, power ^, decimals और parentheses.",
            color = muted, fontSize = 12.sp
        )
        Card(colors = CardDefaults.cardColors(containerColor = panel)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it; error = ""; result = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("गणितीय expression") },
                    minLines = 2
                )
                Button(onClick = {
                    val evaluated = runCatching { ExpressionCalculator.evaluate(expression) }
                    evaluated.onSuccess {
                        result = it.displayValue
                        error = ""
                    }.onFailure {
                        result = ""
                        error = it.message ?: "गणना नहीं हो सकी।"
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("सटीक गणना करें") }
                if (result.isNotBlank()) {
                    Text("परिणाम", color = mint, fontWeight = FontWeight.Bold)
                    Text(result, color = white, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Expression: ${expression.trim()}", color = muted, fontSize = 11.sp)
                }
                if (error.isNotBlank()) Text(error, color = Color(0xFFFFB4AB), fontSize = 12.sp)
            }
        }
        Text("उदाहरण", color = mint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        listOf("2 + 3 * 4", "(2 + 3) * 4", "0.1 + 0.2", "2^10", "5 / 2", "10 % 3").forEach { sample ->
            OutlinedButton(onClick = { expression = sample; result = ""; error = "" }, modifier = Modifier.fillMaxWidth()) {
                Text(sample, color = white)
            }
        }
        Text(
            "सुरक्षा सीमा: यह केवल गणितीय expression पढ़ता है; कोई Kotlin, JavaScript, shell command या arbitrary code execute नहीं करता। Division by zero, invalid syntax, overflow और बहुत बड़े inputs अस्वीकार किए जाते हैं.",
            color = muted, fontSize = 10.sp
        )
        Text("Creator by Bittu Pandit", color = mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
