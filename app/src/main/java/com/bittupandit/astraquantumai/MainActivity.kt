package com.bittupandit.astraquantumai

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private val ink = Color(0xFF090909)
private val charcoal = Color(0xFF171717)
private val graphite = Color(0xFF252525)
private val paper = Color(0xFFF4F4F1)
private val white = Color(0xFFFFFFFF)
private val muted = Color(0xFFB8B8B3)
private val line = Color(0xFF3A3A38)

class MainActivity : ComponentActivity() {
    private var speechText by mutableStateOf("")
    private var pickedFile by mutableStateOf("")
    private var pickedMime by mutableStateOf("")
    private var pickedUri by mutableStateOf<Uri?>(null)
    private var speaking: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaking = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) speaking?.language = Locale("hi", "IN")
        }
        val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                speechText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            }
        }
        val fileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pickedUri = it
                pickedFile = it.lastPathSegment ?: it.toString()
                pickedMime = contentResolver.getType(it) ?: "application/octet-stream"
            }
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = white, onPrimary = ink, secondary = muted,
                background = ink, surface = charcoal, onSurface = white
            )) {
                AstraApp(
                    activity = this,
                    speechText = speechText,
                    pickedFile = pickedFile,
                    pickedMime = pickedMime,
                    pickedUri = pickedUri,
                    onSpeak = { text -> speaking?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "astra_reply") },
                    onListen = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "ASTRA से बोलकर पूछें")
                        }
                        runCatching { speechLauncher.launch(intent) }
                    },
                    onPickFile = { fileLauncher.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")) }
                )
            }
        }
    }

    override fun onDestroy() {
        speaking?.stop()
        speaking?.shutdown()
        super.onDestroy()
    }
}

private data class ChatLine(val from: String, val text: String)
private data class AstraModule(val title: String, val subtitle: String, val symbol: String)

@Composable
private fun AstraApp(
    activity: MainActivity,
    speechText: String,
    pickedFile: String,
    pickedMime: String,
    pickedUri: Uri?,
    onSpeak: (String) -> Unit,
    onListen: () -> Unit,
    onPickFile: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("astra_private", 0) }
    var page by remember { mutableStateOf("Home") }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("OPENAI ENGINE · READY") }
    var backend by remember { mutableStateOf(prefs.getString("backend_url", "https://astraquantum-ai.onrender.com") ?: "https://astraquantum-ai.onrender.com") }
    var memoryInput by remember { mutableStateOf("") }
    var memories by remember { mutableStateOf(prefs.getString("memories", "") ?: "") }
    var projectInput by remember { mutableStateOf("") }
    var projects by remember { mutableStateOf(prefs.getString("projects", "") ?: "") }
    var chat by remember { mutableStateOf(listOf(ChatLine("ASTRA", "नमस्ते! मैं ASTRA हूँ। अपना सवाल पूछिए—मैं OpenAI से उत्तर तैयार करूँगा।"))) }
    var qubitAngle by remember { mutableFloatStateOf(45f) }
    var qubitResult by remember { mutableStateOf("Qubit तैयार है। कोण बदलकर मापन करें।") }
    var fileAnalysis by remember { mutableStateOf("") }
    var fileBusy by remember { mutableStateOf(false) }

    LaunchedEffect(speechText) { if (speechText.isNotBlank()) { prompt = speechText; page = "AI Chat" } }

    fun copyText(value: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("ASTRA", value))
        Toast.makeText(context, "कॉपी हो गया", Toast.LENGTH_SHORT).show()
    }
    fun addMemory(value: String) {
        memories = (memories + if (memories.isBlank()) "" else "\n" + "• " + value).trim()
        prefs.edit().putString("memories", memories).apply()
        memoryInput = ""
    }
    fun addProject(value: String) {
        projects = (projects + if (projects.isBlank()) "" else "\n" + "• " + value).trim()
        prefs.edit().putString("projects", projects).apply()
        projectInput = ""
    }

    Column(Modifier.fillMaxSize().background(ink)) {
        Column(Modifier.fillMaxWidth().background(ink).padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(white), contentAlignment = Alignment.Center) {
                    Text("A", color = ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("ASTRA", color = white, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("QUANTUM AI PLUS", color = muted, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                Surface(color = graphite, shape = CircleShape) {
                    Text("● OPENAI", color = white, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Home", "AI Chat", "Research", "Memory", "Voice", "Files", "Quantum", "Projects", "Settings").forEach { item ->
                    val selected = page == item
                    Surface(
                        onClick = { page = item },
                        color = if (selected) white else charcoal,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(item, color = if (selected) ink else muted, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = line)
        when (page) {
            "Home" -> {
                Column(Modifier.weight(1f).fillMaxWidth().background(paper).padding(18.dp)) {
                    Text("YOUR INTELLIGENCE SPACE", color = Color(0xFF666660), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = ink), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Think beyond.", color = white, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("Explore the unknown.", color = muted, fontSize = 15.sp)
                                    Spacer(Modifier.height(13.dp))
                                    Text("AI CHAT  ·  RESEARCH  ·  QUANTUM LAB", color = white, fontSize = 9.sp, letterSpacing = 1.sp)
                                }
                                QuantumArtwork(Modifier.size(124.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { page = "AI Chat" }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink), shape = RoundedCornerShape(12.dp)) {
                                Text("शुरू करें  →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("EXPLORE MODULES", color = Color(0xFF555550), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    val modules = listOf(
                        AstraModule("AI Chat", "OpenAI के साथ बातचीत", "✳"),
                        AstraModule("Research", "वेब पर जानकारी खोजें", "⌕"),
                        AstraModule("Memory", "अपने निजी नोट्स सँभालें", "◈"),
                        AstraModule("Voice", "बोलें और उत्तर सुनें", "◖"),
                        AstraModule("Files", "PDF और चित्र का विश्लेषण", "▧"),
                        AstraModule("Quantum", "Qubit probability lab", "◎"),
                        AstraModule("Projects", "अपने काम व्यवस्थित करें", "▤"),
                        AstraModule("Settings", "Backend और ऐप सेटिंग", "⚙")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modules.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { module ->
                                    Card(onClick = { page = module.title }, modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = white), shape = RoundedCornerShape(16.dp)) {
                                        Column(Modifier.padding(13.dp)) {
                                            Text(module.symbol, color = ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(8.dp))
                                            Text(module.title, color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(module.subtitle, color = Color(0xFF666660), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Creator by Bittu Pandit", color = Color(0xFF666660), fontSize = 10.sp)
                }
            }
            "AI Chat" -> {
                Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("AI CONVERSATION", color = white, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(status, color = muted, fontSize = 9.sp)
                        }
                        TextButton(onClick = { chat = listOf(ChatLine("ASTRA", "नई बातचीत शुरू हुई। अपना सवाल पूछिए।")) }) { Text("नई चैट", color = muted) }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                        items(chat) { item ->
                            val isAi = item.from == "ASTRA"
                            Card(colors = CardDefaults.cardColors(containerColor = if (isAi) charcoal else Color(0xFF303030)), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(if (isAi) "✳  ASTRA" else "YOU", color = if (isAi) white else muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(Modifier.height(7.dp))
                                    SelectionContainer { Text(item.text, color = white, fontSize = 14.sp, lineHeight = 21.sp) }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { copyText(item.text) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("▢ कॉपी करें", color = white, fontSize = 11.sp) }
                                        if (isAi) TextButton(onClick = { onSpeak(item.text) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("▶ सुनें", color = muted, fontSize = 11.sp) }
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ASTRA से कुछ भी पूछें…", color = muted) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = white, unfocusedTextColor = white,
                            focusedBorderColor = white, unfocusedBorderColor = line,
                            cursorColor = white, focusedContainerColor = charcoal, unfocusedContainerColor = charcoal
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onListen, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = white)) { Text("🎙") }
                        Button(
                            enabled = prompt.isNotBlank() && !busy,
                            onClick = {
                                val question = prompt.trim()
                                chat = chat + ChatLine("You", question)
                                prompt = ""
                                busy = true
                                status = "OPENAI से उत्तर आ रहा है…"
                                activity.lifecycleScope.launch {
                                    val answer = withContext(Dispatchers.IO) { askBackend(backend, question) }
                                    chat = chat + ChatLine("ASTRA", answer)
                                    busy = false
                                    status = "OPENAI ENGINE · READY"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(if (busy) "उत्तर तैयार हो रहा है…" else "भेजें  ↑", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            "Research" -> SimplePage("DEEP RESEARCH", "अपने विषय को खोजें। खोज परिणाम ब्राउज़र में खुलेंगे; स्रोतों को अलग-अलग जाँचें।") {
                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("किस विषय पर शोध करें?") })
                Button(enabled = prompt.isNotBlank(), onClick = {
                    val q = URLEncoder.encode(prompt, "UTF-8")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
                }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("वेब पर खोजें") }
            }
            "Memory" -> SimplePage("PRIVATE MEMORY", "यहाँ सेव किए गए नोट्स इसी फोन में रखे जाते हैं।") {
                OutlinedTextField(memoryInput, { memoryInput = it }, Modifier.fillMaxWidth(), label = { Text("याद रखने के लिए नोट") })
                Button(enabled = memoryInput.isNotBlank(), onClick = { addMemory(memoryInput) }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("नोट सेव करें") }
                SelectionContainer { Text(memories.ifBlank { "अभी कोई नोट नहीं।" }, color = white) }
                TextButton(onClick = { memories = ""; prefs.edit().remove("memories").apply() }) { Text("सभी नोट मिटाएँ", color = muted) }
            }
            "Voice" -> SimplePage("VOICE ASSISTANT", "हिंदी में बोलें। पहचाना गया टेक्स्ट AI Chat में भेज सकते हैं।") {
                Button(onClick = onListen, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("🎙 बोलकर पूछें") }
                SelectionContainer { Text("पहचाना गया: $speechText", color = white) }
                Button(enabled = speechText.isNotBlank(), onClick = { onSpeak(speechText) }, colors = ButtonDefaults.buttonColors(containerColor = graphite, contentColor = white)) { Text("आवाज़ में सुनें") }
            }
            "Files" -> SimplePage("IMAGE & PDF ANALYSIS", "PDF, JPG, PNG या WEBP चुनें और OpenAI से उसका विश्लेषण कराएँ।") {
                Button(onClick = onPickFile, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("फोन से फ़ाइल चुनें") }
                Text("चुनी गई फ़ाइल: $pickedFile", color = white)
                Text("प्रकार: $pickedMime", color = muted, fontSize = 12.sp)
                Button(enabled = pickedUri != null && !fileBusy, onClick = {
                    val uri = pickedUri
                    if (uri != null) {
                        fileBusy = true
                        fileAnalysis = "फ़ाइल का विश्लेषण हो रहा है…"
                        activity.lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("फ़ाइल पढ़ी नहीं जा सकी")
                                    require(bytes.size <= 7 * 1024 * 1024) { "फ़ाइल 7 MB से छोटी रखें।" }
                                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    askBackend(backend, "इस फ़ाइल का हिंदी में सारांश, मुख्य बिंदु और महत्वपूर्ण तथ्य बताइए।", pickedMime, encoded)
                                }.getOrElse { "फ़ाइल विश्लेषण विफल: ${it.message}" }
                            }
                            fileAnalysis = result
                            fileBusy = false
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text(if (fileBusy) "विश्लेषण…" else "OpenAI से विश्लेषण") }
                if (fileAnalysis.isNotBlank()) {
                    SelectionContainer { Text(fileAnalysis, color = white) }
                    TextButton(onClick = { copyText(fileAnalysis) }) { Text("कॉपी करें", color = white) }
                }
            }
            "Quantum" -> SimplePage("QUANTUM LAB · QUBIT", "यह एक शैक्षिक single-qubit probability मॉडल है, असली quantum hardware नहीं।") {
                Text("State angle: ${qubitAngle.toInt()}°", color = white)
                Slider(value = qubitAngle, onValueChange = { qubitAngle = it }, valueRange = 0f..180f, colors = SliderDefaults.colors(thumbColor = white, activeTrackColor = white))
                Button(onClick = {
                    val p0 = kotlin.math.cos(Math.toRadians((qubitAngle / 2.0).toDouble())).let { it * it }
                    val p1 = 1.0 - p0
                    qubitResult = "P(0) = ${"%.3f".format(p0)}   |   P(1) = ${"%.3f".format(p1)}"
                }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("मापन संभावना निकालें") }
                SelectionContainer { Text(qubitResult, color = white) }
            }
            "Projects" -> SimplePage("PROJECT WORKSPACE", "अपने प्रोजेक्ट और काम के नोट्स यहाँ व्यवस्थित करें।") {
                OutlinedTextField(projectInput, { projectInput = it }, Modifier.fillMaxWidth(), label = { Text("प्रोजेक्ट / कार्य नोट") })
                Button(enabled = projectInput.isNotBlank(), onClick = { addProject(projectInput) }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("सेव करें") }
                SelectionContainer { Text(projects.ifBlank { "अभी कोई प्रोजेक्ट नोट नहीं।" }, color = white) }
            }
            "Settings" -> SimplePage("CONNECTION SETTINGS", "केवल OpenAI इंजन। API key Android ऐप में नहीं, Render के सुरक्षित Environment में रहती है।") {
                OutlinedTextField(backend, { backend = it }, Modifier.fillMaxWidth(), label = { Text("Backend HTTPS URL") })
                Button(onClick = {
                    backend = backend.trim().trimEnd('/')
                    prefs.edit().putString("backend_url", backend).apply()
                    status = "Backend URL सेव हुआ"
                    Toast.makeText(context, "Backend URL सेव हुआ", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("सेटिंग सेव करें") }
                Text("API endpoint: /v1/generate", color = muted, fontSize = 12.sp)
                Text("स्थिति: $status", color = white, fontSize = 12.sp)
            }
        }
        if (page != "Home") {
            Text("Creator by Bittu Pandit", color = muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(ink).padding(12.dp))
        }
    }
}

@Composable
private fun SimplePage(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().background(ink).padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(title, color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(description, color = muted, fontSize = 13.sp)
        content()
        Spacer(Modifier.weight(1f))
        Text("Creator by Bittu Pandit", color = muted, fontSize = 10.sp)
    }
}

@Composable
private fun QuantumArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * .31f
        drawCircle(Color.White, radius = radius * .42f, center = center)
        drawCircle(Color(0xFFBDBDBD), radius = radius * .72f, center = center, style = Stroke(width = 1.3.dp.toPx()))
        drawOval(Color(0xFFE8E8E8), topLeft = Offset(center.x - radius * 1.4f, center.y - radius * .55f), size = androidx.compose.ui.geometry.Size(radius * 2.8f, radius * 1.1f), style = Stroke(width = 1.2.dp.toPx()))
        drawOval(Color(0xFF9B9B9B), topLeft = Offset(center.x - radius * 1.4f, center.y - radius * .55f), size = androidx.compose.ui.geometry.Size(radius * 2.8f, radius * 1.1f), style = Stroke(width = 1.2.dp.toPx()), alpha = .8f)
        drawCircle(Color.White, radius = radius * .12f, center = Offset(center.x + radius * 1.15f, center.y - radius * .35f))
        drawCircle(Color(0xFFAAAAAA), radius = radius * .09f, center = Offset(center.x - radius * .95f, center.y + radius * .7f))
        drawCircle(Color(0xFF777777), radius = radius * 1.45f, center = center, style = Stroke(width = .7.dp.toPx()), alpha = .8f)
    }
}

private fun askBackend(baseUrl: String, prompt: String, mimeType: String? = null, base64: String? = null): String {
    if (baseUrl.isBlank()) return "ASTRA से जुड़ने के लिए Backend HTTPS URL सेव करें।"
    return try {
        val clean = baseUrl.trim().trimEnd('/')
        if (!URLUtil.isHttpsUrl(clean)) return "सुरक्षा के लिए backend URL HTTPS होना चाहिए।"
        val connection = (URL("$clean/v1/generate").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 65000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val payload = JSONObject().put("prompt", prompt)
        if (!base64.isNullOrBlank() && !mimeType.isNullOrBlank()) {
            payload.put("mimeType", mimeType).put("base64", base64)
        }
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) {
            val message = runCatching { JSONObject(body).optString("error") }.getOrDefault("")
            "OpenAI सेवा में त्रुटि (HTTP $code): ${message.ifBlank { body.take(180) }}"
        } else {
            val json = JSONObject(body)
            json.optString("text").ifBlank { json.optString("response").ifBlank { "सर्वर से उत्तर मिला, लेकिन टेक्स्ट नहीं मिला।" } }
        }
    } catch (e: Exception) {
        "OpenAI सर्वर से कनेक्शन नहीं हो पाया: ${e.message ?: "नेटवर्क त्रुटि"}"
    }
}
