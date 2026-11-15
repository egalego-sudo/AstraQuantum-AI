package com.bittupandit.astraquantumai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF8B9CFF),
                secondary = Color(0xFFB58CFF),
                background = Color(0xFF070B1B),
                surface = Color(0xFF111A35)
            )) {
                AstraHome()
            }
        }
    }
}

private data class AstraModule(val title: String, val subtitle: String, val symbol: String)

@Composable
private fun AstraHome() {
    val modules = listOf(
        AstraModule("AI Chat", "बातचीत और सवाल-जवाब", "✦"),
        AstraModule("Deep Research", "जानकारी और स्रोत", "⌕"),
        AstraModule("Memory", "आपकी चुनी हुई यादें", "◈"),
        AstraModule("Voice", "आवाज़ से संवाद", "♫"),
        AstraModule("Image & PDF", "फाइल और चित्र सहायता", "▧"),
        AstraModule("Quantum Lab", "शैक्षिक क्वांटम सिम्युलेटर", "⚛"),
        AstraModule("Workspace", "आपके प्रोजेक्ट", "▦"),
        AstraModule("Settings", "ऐप की सेटिंग", "⚙")
    )
    var prompt by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("AI Chat") }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070B1B)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF101B43), Color(0xFF070B1B))))
                .padding(20.dp)
        ) {
            Text("ASTRA", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("QUANTUM AI PLUS", color = Color(0xFFB8C5FF), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your personal intelligence space", color = Color(0xFF9BA9D6), fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x332D4D9A)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("नमस्ते! मैं ASTRA हूँ", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("आज आप क्या जानना या बनाना चाहते हैं?", color = Color(0xFFB8C5E8), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("अपना सवाल लिखें…", color = Color(0xFF8F9ABD)) },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = false
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { selected = "AI Chat" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF596FE8))
                    ) { Text("Start Chat  →") }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("YOUR INTELLIGENCE TOOLS", color = Color(0xFF9BA9D6), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(modules) { module ->
                    Card(
                        onClick = { selected = module.title },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected == module.title) Color(0xFF26366D) else Color(0xFF121B36)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(module.symbol, color = Color(0xFFB6C3FF), fontSize = 22.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(module.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(module.subtitle, color = Color(0xFF9BA9C8), fontSize = 11.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Creator by Bittu Pandit", color = Color(0xFF8795BC), fontSize = 11.sp)
            }
        }
    }
}
