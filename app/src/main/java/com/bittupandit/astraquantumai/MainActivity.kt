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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// ASTRA Premium Cosmic Glass palette
private val ink = Color(0xFF070B1B)       // deep-space navy
private val charcoal = Color(0xFF101A31)  // glass panel
private val graphite = Color(0xFF1A2B4D)  // raised panel
private val paper = Color(0xFF0B1226)     // app canvas
private val white = Color(0xFF55D6FF)    // electric cyan accent
private val muted = Color(0xFFA9B9D6)    // soft starlight
private val line = Color(0xFF263B63)     // subtle cosmic divider
private val violet = Color(0xFF9B8CFF)   // quantum violet
private val mint = Color(0xFF6CF2D5)     // secondary energy accent

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
                primary = white, onPrimary = ink, secondary = violet,
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
                    onPickFile = { fileLauncher.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }
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

private fun restoreChatHistory(raw: String?): List<ChatLine> {
    if (raw.isNullOrBlank()) return listOf(ChatLine("ASTRA", "नमस्ते! मैं ASTRA हूँ। अपना सवाल पूछिए—मैं Groq AI से उत्तर तैयार करूँगा।"))
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ChatLine(item.optString("from", "ASTRA"), item.optString("text"))
        }.filter { it.text.isNotBlank() }.ifEmpty {
            listOf(ChatLine("ASTRA", "नमस्ते! मैं ASTRA हूँ। अपना सवाल पूछिए।"))
        }
    }.getOrElse { listOf(ChatLine("ASTRA", "पुरानी चैट पढ़ी नहीं जा सकी। नई बातचीत शुरू करें।")) }
}

private fun encodeChatHistory(chat: List<ChatLine>): String {
    val array = JSONArray()
    chat.takeLast(100).forEach { item ->
        array.put(JSONObject().put("from", item.from).put("text", item.text))
    }
    return array.toString()
}

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
    val offlineEngine = remember { OfflineAiEngine(context) }
    var page by remember { mutableStateOf("Home") }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1800); showSplash = false }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("GROQ AI · READY") }
    var backend by remember { mutableStateOf(prefs.getString("backend_url", "https://astraquantum-ai.onrender.com") ?: "https://astraquantum-ai.onrender.com") }
    var memoryInput by remember { mutableStateOf("") }
    var memories by remember { mutableStateOf(prefs.getString("memories", "") ?: "") }
    var projectInput by remember { mutableStateOf("") }
    var projects by remember { mutableStateOf(prefs.getString("projects", "") ?: "") }
    var chat by remember { mutableStateOf(restoreChatHistory(prefs.getString("chat_history", null))) }
    var qubitAngle by remember { mutableFloatStateOf(45f) }
    var qubitCount by remember { mutableIntStateOf(4) }
    var qubitResult by remember { mutableStateOf("Qubit तैयार है। Qubit संख्या और कोण चुनकर गणना करें।") }
    var fileAnalysis by remember { mutableStateOf("") }
    var fileBusy by remember { mutableStateOf(false) }
    var offlineProgress by remember { mutableIntStateOf(0) }
    var offlineStatus by remember { mutableStateOf("मॉडल अभी डाउनलोड नहीं हुआ है।") }
    var offlineBusy by remember { mutableStateOf(false) }
    var offlineTestPrompt by remember { mutableStateOf("नमस्ते ASTRA, अपना परिचय दीजिए।") }
    var offlineTestAnswer by remember { mutableStateOf("") }
    var researchResult by remember { mutableStateOf("") }
    var researchBusy by remember { mutableStateOf(false) }

    LaunchedEffect(speechText) { if (speechText.isNotBlank()) { prompt = speechText; page = "AI Chat" } }
    LaunchedEffect(chat) { prefs.edit().putString("chat_history", encodeChatHistory(chat)).apply() }
    LaunchedEffect(Unit) {
        offlineStatus = if (offlineEngine.isModelInstalled) "ऑफलाइन मॉडल फोन में उपलब्ध है।" else "पहली बार लगभग 328 MiB मॉडल डाउनलोड करना होगा।"
    }
    DisposableEffect(offlineEngine) {
        onDispose { offlineEngine.close() }
    }
    var healthBusy by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF02030D), Color(0xFF080D27), Color(0xFF170A32), Color(0xFF050B1B))))) {
        CosmicNebulaBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xC9101834), Color(0xB30A1025), Color.Transparent))).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(white), contentAlignment = Alignment.Center) {
                    Text("A", color = ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("ASTRA", color = white, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("QUANTUM AI PLUS", color = muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text("Creator by Bittu Pandit", color = mint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(color = graphite, shape = CircleShape) {
                    Text("● GROQ", color = white, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Home", "AI Chat", "Offline AI", "Research", "Memory", "Voice", "Files", "Quantum", "Projects", "Settings").forEach { item ->
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
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(white, violet, mint))), contentAlignment = Alignment.Center) {
                            Text("A", color = ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ASTRA", color = white, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Text("QUANTUM AI", color = Color(0xFFFFD86B), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        }
                        Surface(color = Color(0xAA152448), shape = CircleShape) {
                            Text("✦ AI", color = mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0x99101A3A)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().height(194.dp)) {
                            HomeCosmicArtwork(Modifier.fillMaxSize())
                            Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                                Text("WELCOME TO", color = mint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Text("YOUR UNIVERSE", color = white, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text("Explore beyond limits", color = Color(0xFFE1E7FF), fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xE5101A34)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⌕", color = white, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text("Ask ASTRA anything", color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = onListen) { Text("🎙", color = mint, fontSize = 19.sp) }
                            }
                            OutlinedTextField(
                                value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search or ask anything…", color = muted) },
                                shape = RoundedCornerShape(14.dp), minLines = 1, maxLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = white, unfocusedTextColor = white,
                                    focusedBorderColor = mint, unfocusedBorderColor = line,
                                    cursorColor = mint, focusedContainerColor = Color(0x66101834), unfocusedContainerColor = Color(0x66101834)
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(enabled = prompt.isNotBlank(), onClick = { page = "AI Chat" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176BFF), contentColor = Color.White), shape = RoundedCornerShape(13.dp)) {
                                Text("ASK ASTRA  →", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(17.dp))
                    Text("EXPLORE ASTRA", color = Color(0xFFFFD86B), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    val modules = listOf(
                        AstraModule("Smart Search", "Find anything", "⌕"),
                        AstraModule("AI Answers", "Ask AI anything", "✧"),
                        AstraModule("Deep Research", "Explore topics", "◉"),
                        AstraModule("Source Verification", "Check facts", "✓"),
                        AstraModule("Quantum Lab", "Quantum learning", "⚛"),
                        AstraModule("More Tools", "Your AI workspace", "✦")
                    )
                    val tileColors = listOf(Color(0xFF135DCE), Color(0xFF6336C8), Color(0xFF087E9A), Color(0xFF16845E), Color(0xFF6636A8), Color(0xFF9B4A20))
                    modules.chunked(2).forEachIndexed { rowIndex, pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEachIndexed { colIndex, module ->
                                val target = when (module.title) {
                                    "Smart Search", "Deep Research", "Source Verification" -> "Research"
                                    "AI Answers" -> "AI Chat"
                                    "Quantum Lab" -> "Quantum"
                                    else -> "Projects"
                                }
                                val tint = tileColors[rowIndex * 2 + colIndex]
                                Card(onClick = { page = target }, modifier = Modifier.weight(1f).height(104.dp), colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = .94f)), shape = RoundedCornerShape(19.dp)) {
                                    Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(module.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                            Spacer(Modifier.height(4.dp))
                                            Text(module.subtitle, color = Color.White.copy(alpha = .88f), fontSize = 9.sp)
                                        }
                                        Text(module.symbol, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                        if (rowIndex < 2) Spacer(Modifier.height(9.dp))
                    }
                    Spacer(Modifier.height(15.dp))
                    Text("Creator by Bittu Pandit", color = mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                }
            }
            "AI Chat" -> {
                Column(Modifier.weight(1f).fillMaxWidth().background(Color.Transparent).padding(14.dp)) {
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
                            Card(colors = CardDefaults.cardColors(containerColor = if (isAi) Color(0xA6202D50) else Color(0xA6123A52)), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
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
                    if (pickedUri != null && pickedMime.startsWith("image/")) {
                        Text("📷 फोटो तैयार: $pickedFile", color = mint, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onListen, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = white), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("🎙") }
                        OutlinedButton(onClick = onPickFile, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = mint), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("📷 फोटो") }
                        Button(
                            enabled = (prompt.isNotBlank() || (pickedUri != null && pickedMime.startsWith("image/"))) && !busy,
                            onClick = {
                                val imageUri = pickedUri?.takeIf { pickedMime in listOf("image/jpeg", "image/png", "image/webp") }
                                val question = prompt.trim().ifBlank { "इस फोटो में क्या दिखाई दे रहा है? विस्तार से हिंदी में विश्लेषण करें।" }
                                chat = chat + ChatLine("You", question + if (imageUri != null) "  [फोटो: $pickedFile]" else "")
                                prompt = ""
                                busy = true
                                status = if (imageUri != null) "VISION AI फोटो का विश्लेषण कर रहा है…" else "HYBRID ROUTER · ONLINE/OFFLINE"
                                activity.lifecycleScope.launch {
                                    val answer = withContext(Dispatchers.IO) {
                                        val onlineResult = if (imageUri != null) {
                                            runCatching {
                                                val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() } ?: error("फोटो पढ़ी नहीं जा सकी")
                                                require(bytes.size <= 7 * 1024 * 1024) { "फोटो 7 MB से छोटी रखें।" }
                                                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                                askBackend(backend, question, pickedMime, encoded)
                                            }.getOrElse { "फोटो विश्लेषण विफल: ${it.message}" }
                                        } else {
                                            askBackend(backend, question)
                                        }
                                        val remoteFailed = onlineResult.startsWith("ASTRA सर्वर से कनेक्शन नहीं हो पाया") ||
                                            onlineResult.startsWith("ASTRA से जुड़ने के लिए Backend HTTPS URL") ||
                                            onlineResult.startsWith("Groq सेवा में त्रुटि") ||
                                            onlineResult.startsWith("सुरक्षा के लिए backend URL")
                                        if (remoteFailed && imageUri == null && offlineEngine.isModelInstalled) {
                                            runCatching { "ON-DEVICE · QWEN3-0.6B\n" + offlineEngine.answer(question) }
                                                .getOrElse { "ऑनलाइन सेवा और स्थानीय मॉडल दोनों उपलब्ध नहीं हुए: ${it.message}" }
                                        } else if (remoteFailed && imageUri != null) {
                                            onlineResult + "\n\nOFFLINE VISION अभी ASTRA में उपलब्ध नहीं है।"
                                        } else if (imageUri != null) {
                                            "ONLINE · GROQ VISION\n" + onlineResult
                                        } else {
                                            "ONLINE · GROQ\n" + onlineResult
                                        }
                                    }
                                    chat = chat + ChatLine("ASTRA", answer)
                                    busy = false
                                    status = if (answer.startsWith("ON-DEVICE")) "OFFLINE AI · READY" else "HYBRID AI · READY"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) { Text(if (busy) "काम जारी…" else "भेजें ↑", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }
                }
            }
            "Research" -> SimplePage("DEEP RESEARCH", "एक ही विषय को अलग-अलग स्रोतों पर खोजें। दावों को कम-से-कम दो स्वतंत्र स्रोतों से मिलाएँ।") {
                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("किस विषय या दावे की जाँच करें?") })
                Text("खोज के स्रोत", color = mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Button(enabled = prompt.isNotBlank() && !researchBusy, onClick = {
                    researchBusy = true
                    researchResult = "Wikipedia और Crossref पर स्रोत खोजे जा रहे हैं…"
                    val query = prompt.trim()
                    activity.lifecycleScope.launch {
                        researchResult = withContext(Dispatchers.IO) { askResearch(backend, query) }
                        researchBusy = false
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = violet, contentColor = Color.White)) {
                    Text(if (researchBusy) "गहन शोध जारी…" else "ASTRA Deep Research + Source Check")
                }
                if (researchResult.isNotBlank()) {
                    SelectionContainer { Text(researchResult, color = white, lineHeight = 20.sp) }
                    TextButton(onClick = { copyText(researchResult) }) { Text("शोध रिपोर्ट कॉपी करें", color = mint) }
                }
                Button(enabled = prompt.isNotBlank(), onClick = {
                    val q = URLEncoder.encode(prompt.trim(), "UTF-8")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + q))) }
                        .onFailure { Toast.makeText(context, "ब्राउज़र नहीं खुल पाया", Toast.LENGTH_SHORT).show() }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("Google पर खोजें") }
                OutlinedButton(enabled = prompt.isNotBlank(), onClick = {
                    val q = URLEncoder.encode(prompt.trim(), "UTF-8")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bing.com/search?q=" + q))) }
                        .onFailure { Toast.makeText(context, "ब्राउज़र नहीं खुल पाया", Toast.LENGTH_SHORT).show() }
                }, modifier = Modifier.fillMaxWidth()) { Text("Bing पर खोजें", color = white) }
                OutlinedButton(enabled = prompt.isNotBlank(), onClick = {
                    val q = URLEncoder.encode(prompt.trim(), "UTF-8")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://scholar.google.com/scholar?q=" + q))) }
                        .onFailure { Toast.makeText(context, "ब्राउज़र नहीं खुल पाया", Toast.LENGTH_SHORT).show() }
                }, modifier = Modifier.fillMaxWidth()) { Text("Google Scholar · शोध पत्र", color = white) }
                OutlinedButton(enabled = prompt.isNotBlank(), onClick = {
                    val q = URLEncoder.encode("fact check " + prompt.trim(), "UTF-8")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + q))) }
                        .onFailure { Toast.makeText(context, "ब्राउज़र नहीं खुल पाया", Toast.LENGTH_SHORT).show() }
                }, modifier = Modifier.fillMaxWidth()) { Text("Fact Check · दावे की जाँच", color = white) }
                Text("ध्यान दें: ये खोज पेज खोलते हैं; ASTRA अभी स्रोतों की स्वतंत्र सत्यता अपने-आप प्रमाणित नहीं करता।", color = muted, fontSize = 11.sp)
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
            "Files" -> SimplePage("ASTRA FILE INTELLIGENCE", "PDF दस्तावेज़ का टेक्स्ट निकालकर AI से सारांश, मुख्य बिंदु और सवालों के जवाब पाएँ; JPG, PNG और WEBP फोटो का Vision विश्लेषण भी उपलब्ध है।") {
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
                                    askBackend(backend, if (pickedMime == "application/pdf") "इस PDF दस्तावेज़ का गहराई से हिंदी में विश्लेषण करें: पहले संक्षिप्त सारांश, फिर विषयवार मुख्य बिंदु, महत्वपूर्ण तिथियाँ/नाम/आँकड़े, दस्तावेज़ में दिए गए निष्कर्ष और अंत में संभावित अस्पष्टताएँ बताएँ। केवल दस्तावेज़ में मौजूद जानकारी को तथ्य कहें।" else "इस फ़ाइल का हिंदी में सारांश, मुख्य बिंदु और महत्वपूर्ण तथ्य बताइए।", pickedMime, encoded)
                                }.getOrElse { "फ़ाइल विश्लेषण विफल: ${it.message}" }
                            }
                            fileAnalysis = result
                            fileBusy = false
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text(if (fileBusy) "विश्लेषण…" else "Groq से विश्लेषण") }
                if (fileAnalysis.isNotBlank()) {
                    SelectionContainer { Text(fileAnalysis, color = white) }
                    TextButton(onClick = { copyText(fileAnalysis) }) { Text("कॉपी करें", color = white) }
                }
            }
            "Quantum" -> SimplePage("ASTRA QUANTUM LAB · 32-QUBIT HYBRID", "32-Qubit Hybrid Learning Engine: फोन पर क्लासिकल गणना, on-demand probability और measurement sampling। यह वास्तविक quantum hardware या हर प्रकार के entangled circuit का exact simulator नहीं है; अभी स्वतंत्र Qubits के product-state प्रयोग करता है।") {
                Text("Qubit की संख्या: ${qubitCount}", color = white, fontWeight = FontWeight.Bold)
                Slider(value = qubitCount.toFloat(), onValueChange = { qubitCount = it.toInt().coerceIn(1, 32) }, valueRange = 1f..32f, steps = 30, colors = SliderDefaults.colors(thumbColor = white, activeTrackColor = white))
                Text("Rotation angle: ${qubitAngle.toInt()}°", color = white, fontWeight = FontWeight.Bold)
                Slider(value = qubitAngle, onValueChange = { qubitAngle = it }, valueRange = 0f..360f, steps = 35, colors = SliderDefaults.colors(thumbColor = white, activeTrackColor = white))
                val basisStates = 1L shl qubitCount
                val fullVectorGiB = (16.0 * basisStates.toDouble()) / (1024.0 * 1024.0 * 1024.0)
                Card(colors = CardDefaults.cardColors(containerColor = graphite), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("कुल संभावित basis states", color = muted, fontSize = 11.sp)
                        Text("${String.format(Locale.US, "%,d", basisStates)} (2^${qubitCount})", color = white, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("हमारा product-state engine: on-demand गणना; पूरी state-vector RAM में नहीं रखता।", color = mint, fontSize = 11.sp)
                        Text("तुलना के लिए full complex state-vector: लगभग ${"%.2f".format(Locale.US, fullVectorGiB)} GiB RAM", color = muted, fontSize = 11.sp)
                    }
                }
                Button(onClick = {
                    val theta = Math.toRadians(qubitAngle.toDouble())
                    val p0 = kotlin.math.cos(theta / 2.0).let { it * it }
                    val p1 = 1.0 - p0
                    val allZero = Math.pow(p0, qubitCount.toDouble())
                    val allOne = Math.pow(p1, qubitCount.toDouble())
                    val expectedOnes = qubitCount * p1
                    val mostLikely = if (p0 >= p1) "0".repeat(qubitCount) else "1".repeat(qubitCount)
                    val mostLikelyProbability = maxOf(allZero, allOne)
                    qubitResult = "प्रति Qubit: P(0) = ${"%.4f".format(Locale.US, p0)} | P(1) = ${"%.4f".format(Locale.US, p1)}\n" +
                        "कुल basis states: ${String.format(Locale.US, "%,d", basisStates)}\n" +
                        "सभी शून्य की संभावना: ${"%.8f".format(Locale.US, allZero)}\n" +
                        "सभी एक की संभावना: ${"%.8f".format(Locale.US, allOne)}\n" +
                        "अपेक्षित 1 की संख्या: ${"%.3f".format(Locale.US, expectedOnes)} / ${qubitCount}\n" +
                        "सबसे संभावित छोर-अवस्था: |${mostLikely}⟩ = ${"%.8f".format(Locale.US, mostLikelyProbability)}\n" +
                        "मॉडल: स्वतंत्र Qubits का product-state; गणना O(n), n ≤ 32।"
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("32-Qubit probability निकालें") }
                OutlinedButton(onClick = {
                    val theta = Math.toRadians(qubitAngle.toDouble())
                    val p1 = kotlin.math.sin(theta / 2.0).let { it * it }
                    val shots = 1000
                    var totalOnes = 0
                    var allZeroShots = 0
                    var allOneShots = 0
                    repeat(shots) {
                        var ones = 0
                        repeat(qubitCount) { if (kotlin.random.Random.nextDouble() < p1) ones++ }
                        totalOnes += ones
                        if (ones == 0) allZeroShots++
                        if (ones == qubitCount) allOneShots++
                    }
                    qubitResult = "Measurement sampling · ${String.format(Locale.US, "%,d", shots)} shots\n" +
                        "औसत 1 प्रति shot: ${"%.3f".format(Locale.US, totalOnes.toDouble() / shots)} / ${qubitCount}\n" +
                        "सभी 0 मिले: ${allZeroShots} बार (${"%.2f".format(Locale.US, allZeroShots * 100.0 / shots)}%)\n" +
                        "सभी 1 मिले: ${allOneShots} बार (${"%.2f".format(Locale.US, allOneShots * 100.0 / shots)}%)\n" +
                        "यह classical random sampling है, वास्तविक quantum measurement नहीं।"
                }, modifier = Modifier.fillMaxWidth()) { Text("1,000 measurement shots चलाएँ", color = white) }
                SelectionContainer { Text(qubitResult, color = white, lineHeight = 21.sp) }
            }
            "Offline AI" -> SimplePage("OFFLINE AI CORE", "इंटरनेट बंद होने पर ASTRA इस स्थानीय मॉडल से जवाब दे सकेगा। पहली बार मॉडल डाउनलोड करने के लिए इंटरनेट और लगभग 328 MiB खाली जगह चाहिए।") {
                Text("स्थानीय मॉडल: ${OfflineAiEngine.MODEL_NAME}", color = mint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(if (offlineEngine.isModelInstalled) "स्थिति: मॉडल फोन में इंस्टॉल है" else "स्थिति: मॉडल डाउनलोड होना बाकी है", color = white, fontSize = 12.sp)
                if (offlineBusy && offlineProgress > 0) {
                    LinearProgressIndicator(progress = { offlineProgress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("डाउनलोड: $offlineProgress%", color = mint, fontSize = 12.sp)
                }
                Button(
                    enabled = !offlineBusy && !offlineEngine.isModelInstalled,
                    onClick = {
                        offlineBusy = true
                        offlineProgress = 0
                        offlineStatus = "Qwen मॉडल डाउनलोड हो रहा है…"
                        activity.lifecycleScope.launch {
                            try {
                                offlineEngine.downloadModel { offlineProgress = it }
                                offlineStatus = "मॉडल डाउनलोड और आकार-जाँच सफल। अब स्थानीय परीक्षण करें।"
                            } catch (error: Exception) {
                                offlineStatus = "डाउनलोड विफल: ${error.message ?: "नेटवर्क त्रुटि"}"
                            } finally {
                                offlineBusy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)
                ) { Text(if (offlineBusy) "डाउनलोड जारी…" else if (offlineEngine.isModelInstalled) "मॉडल डाउनलोड हो चुका है" else "328 MiB मॉडल डाउनलोड करें") }
                Text(offlineStatus, color = muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = offlineTestPrompt,
                    onValueChange = { offlineTestPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("स्थानीय AI का परीक्षण") },
                    minLines = 2
                )
                Button(
                    enabled = offlineEngine.isModelInstalled && !offlineBusy && offlineTestPrompt.isNotBlank(),
                    onClick = {
                        offlineBusy = true
                        offlineTestAnswer = ""
                        activity.lifecycleScope.launch {
                            try {
                                offlineStatus = offlineEngine.initialize()
                                offlineTestAnswer = offlineEngine.answer(offlineTestPrompt)
                            } catch (error: Exception) {
                                offlineTestAnswer = "स्थानीय मॉडल त्रुटि: ${error.message}"
                            } finally {
                                offlineBusy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = violet, contentColor = Color.White)
                ) { Text(if (offlineBusy) "स्थानीय AI चल रहा है…" else "ऑफलाइन AI टेस्ट चलाएँ") }
                if (offlineTestAnswer.isNotBlank()) {
                    SelectionContainer { Text(offlineTestAnswer, color = white, lineHeight = 21.sp) }
                }
                Text("मॉडल लाइसेंस: Apache 2.0 · Qwen3-0.6B. लाइव इंटरनेट खोज और वर्तमान समाचार ऑफलाइन उपलब्ध नहीं होंगे।", color = muted, fontSize = 11.sp)
                Text("Creator by Bittu Pandit", color = mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            "Projects" -> SimplePage("PROJECT WORKSPACE", "अपने प्रोजेक्ट के नोट्स सेव करें। ये इसी फोन में रहेंगे और ऐप दोबारा खोलने पर वापस मिलेंगे।") {
                OutlinedTextField(projectInput, { projectInput = it }, Modifier.fillMaxWidth(), label = { Text("नया प्रोजेक्ट या नोट लिखें") }, minLines = 3)
                Button(enabled = projectInput.isNotBlank(), onClick = { addProject(projectInput) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("प्रोजेक्ट / नोट सेव करें") }
                Text("सेव की गई सामग्री", color = mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                SelectionContainer { Text(projects.ifBlank { "अभी कोई प्रोजेक्ट नोट नहीं।" }, color = white, lineHeight = 21.sp) }
                Text("नोट फोन के निजी ऐप स्टोरेज में सेव हैं; ऐप डेटा हटाने या ऐप uninstall करने पर मिट सकते हैं।", color = muted, fontSize = 11.sp)
                TextButton(onClick = { projects = ""; prefs.edit().remove("projects").apply() }) { Text("सभी प्रोजेक्ट नोट मिटाएँ", color = muted) }
            }
            "Settings" -> SimplePage("CONNECTION SETTINGS", "Groq AI की API key ऐप में नहीं, Render के सुरक्षित Environment में रहती है।") {
                OutlinedTextField(backend, { backend = it }, Modifier.fillMaxWidth(), label = { Text("Backend HTTPS URL") })
                Button(onClick = {
                    backend = backend.trim().trimEnd('/')
                    prefs.edit().putString("backend_url", backend).apply()
                    status = "Backend URL सेव हुआ"
                    Toast.makeText(context, "Backend URL सेव हुआ", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = ink)) { Text("सेटिंग सेव करें") }
                OutlinedButton(enabled = !healthBusy, onClick = {
                    healthBusy = true
                    status = "सर्वर जाँचा जा रहा है…"
                    activity.lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO) { checkAstraBackend(backend) }
                        status = result
                        healthBusy = false
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (healthBusy) "जाँच जारी है…" else "AI सर्वर कनेक्शन जाँचें", color = white) }
                Text("API endpoint: /v1/generate", color = muted, fontSize = 12.sp)
                Text("स्थिति: $status", color = white, fontSize = 12.sp)
            }
        }
        if (page != "Home") {
            Text("Creator by Bittu Pandit", color = mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().background(Color(0xCC080F23)).padding(12.dp))
        }
        }
        if (showSplash) SplashScreen(Modifier.fillMaxSize())
    }
}

@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF02030D), Color(0xFF10103A), Color(0xFF050B1B)))), contentAlignment = Alignment.Center) {
        CosmicNebulaBackground(Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp).clip(RoundedCornerShape(34.dp)).background(Brush.linearGradient(listOf(white, violet, mint))), contentAlignment = Alignment.Center) {
                Text("A", color = ink, fontSize = 76.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(24.dp))
            Text("ASTRA", color = white, fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            Text("QUANTUM AI", color = muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 5.sp)
            Spacer(Modifier.height(22.dp))
            Text("Explore the unknown", color = mint, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(38.dp))
            CircularProgressIndicator(color = white, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(28.dp))
            Text("Creator by Bittu Pandit", color = mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CosmicNebulaBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val clouds = listOf(
            Triple(Offset(w * .18f, h * .25f), size.minDimension * .62f, Color(0xFF5526A8)),
            Triple(Offset(w * .86f, h * .42f), size.minDimension * .58f, Color(0xFF075D9B)),
            Triple(Offset(w * .48f, h * .82f), size.minDimension * .66f, Color(0xFF7A236E)),
            Triple(Offset(w * .06f, h * .88f), size.minDimension * .42f, Color(0xFF123F91))
        )
        clouds.forEach { (center, radius, tint) ->
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to tint.copy(alpha = .34f),
                    .34f to tint.copy(alpha = .17f),
                    .72f to tint.copy(alpha = .055f),
                    1.0f to Color.Transparent,
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        drawLine(Color(0xFF8A68D8).copy(alpha = .10f), Offset(w * .02f, h * .58f), Offset(w * .98f, h * .20f), strokeWidth = size.minDimension * .11f)
        val stars = listOf(
            .04f to .08f, .18f to .16f, .31f to .06f, .47f to .13f, .68f to .07f, .86f to .18f, .95f to .09f,
            .09f to .32f, .25f to .27f, .42f to .36f, .59f to .29f, .78f to .34f, .91f to .43f,
            .03f to .57f, .16f to .49f, .36f to .58f, .54f to .51f, .72f to .61f, .89f to .55f,
            .11f to .76f, .29f to .69f, .48f to .81f, .66f to .73f, .84f to .84f, .97f to .72f,
            .22f to .93f, .41f to .96f, .61f to .91f, .78f to .96f, .94f to .89f
        )
        stars.forEachIndexed { index, point ->
            val center = Offset(w * point.first, h * point.second)
            val radius = if (index % 5 == 0) 2.0.dp.toPx() else 1.05.dp.toPx()
            drawCircle(if (index % 4 == 0) Color(0xFF9DF7F0).copy(alpha = .95f) else Color.White.copy(alpha = .82f), radius, center)
            if (index % 5 == 0) drawCircle(Color(0xFF9DF7F0).copy(alpha = .22f), radius * 3.6f, center)
        }
        val planet = Offset(w * .84f, h * .28f)
        val pr = size.minDimension * .105f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFB9A5FF).copy(alpha = .78f), Color(0xFF5436A0).copy(alpha = .50f), Color.Transparent), planet, pr * 2.1f), pr * 2.1f, planet)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF6D58B8), Color(0xFF18234D), Color(0xFF070D20)), planet, pr), pr, planet)
        drawCircle(Color(0xFFB5A6FF).copy(alpha = .55f), pr * 1.12f, planet, style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun HomeCosmicArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val earth = Offset(w * .72f, h * .84f)
        val er = h * .48f
        drawCircle(Color(0xFF438BFF).copy(alpha = .24f), er * 1.55f, earth)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF73D9FF), Color(0xFF1762C8), Color(0xFF071A49)), earth, er), er, earth)
        drawArc(Color(0xFF54E7A0).copy(alpha = .95f), 195f, 115f, false, Offset(earth.x-er, earth.y-er), androidx.compose.ui.geometry.Size(er*2, er*2), style = Stroke(width = er*.20f))
        drawArc(Color(0xFFB8A2FF).copy(alpha = .75f), 205f, 125f, false, Offset(earth.x-er*1.15f, earth.y-er*1.10f), androidx.compose.ui.geometry.Size(er*2.3f, er*2.2f), style = Stroke(width = 2.dp.toPx()))
        for (i in 0..45) {
            val x = (i * 43f % w)
            val y = (i * 29f % h)
            drawCircle(if (i%5==0) Color(0xFFB6F7FF) else Color.White.copy(alpha=.7f), if (i%5==0) 1.8.dp.toPx() else 1.dp.toPx(), Offset(x,y))
        }
        drawCircle(Color(0xFFB49CFF).copy(alpha=.55f), h*.10f, Offset(w*.82f,h*.20f))
        drawCircle(Color(0xFFB49CFF).copy(alpha=.8f), h*.075f, Offset(w*.82f,h*.20f))
    }
}

@Composable
private fun ColumnScope.SimplePage(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().background(Color(0x78101834)).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(title, color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(description, color = muted, fontSize = 13.sp)
        content()
        Spacer(Modifier.weight(1f))
        Text("Creator by Bittu Pandit", color = mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuantumArtwork(modifier: Modifier = Modifier, variant: Int = 0) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * .31f
        val thin = 1.15.dp.toPx()
        when (variant) {
            1 -> {
                drawCircle(violet, radius = radius * 1.35f, center = center, style = Stroke(thin))
                drawCircle(mint, radius = radius * .9f, center = center, style = Stroke(thin))
                drawArc(white, -25f, 235f, false, Offset(center.x - radius * 1.4f, center.y - radius * .7f), androidx.compose.ui.geometry.Size(radius * 2.8f, radius * 1.4f), style = Stroke(thin * 2))
                drawCircle(white, radius = radius * .13f, center = Offset(center.x + radius * .8f, center.y - radius * .7f))
                drawCircle(violet, radius = radius * .09f, center = Offset(center.x - radius * .9f, center.y + radius * .4f))
            }
            2 -> {
                drawCircle(violet, radius = radius * 1.2f, center = center, style = Stroke(thin))
                for (i in 0..5) {
                    val x = center.x + (i - 2.5f) * radius * .32f
                    val y = center.y + if (i % 2 == 0) -radius * .48f else radius * .48f
                    drawLine(mint, center, Offset(x, y), thin, cap = StrokeCap.Round)
                    drawCircle(white, radius = radius * .13f, center = Offset(x, y))
                }
                drawOval(white, Offset(center.x - radius * .7f, center.y - radius * .9f), androidx.compose.ui.geometry.Size(radius * 1.4f, radius * 1.8f), style = Stroke(thin))
            }
            3 -> {
                val nodes = listOf(
                    Offset(center.x - radius, center.y - radius * .7f),
                    Offset(center.x + radius, center.y - radius * .7f),
                    Offset(center.x - radius, center.y + radius * .7f),
                    Offset(center.x + radius, center.y + radius * .7f),
                    Offset(center.x, center.y)
                )
                listOf(0 to 4, 1 to 4, 2 to 4, 3 to 4, 0 to 2, 1 to 3).forEach { (a, b) ->
                    drawLine(violet, nodes[a], nodes[b], thin, cap = StrokeCap.Round)
                }
                nodes.forEachIndexed { index, point ->
                    drawCircle(if (index == 4) white else mint, radius = if (index == 4) radius * .22f else radius * .15f, center = point)
                }
            }
            else -> {
                drawCircle(white, radius = radius * .42f, center = center)
                drawCircle(mint, radius = radius * .72f, center = center, style = Stroke(width = thin))
                drawOval(Color(0xFFE8E8E8), topLeft = Offset(center.x - radius * 1.4f, center.y - radius * .55f), size = androidx.compose.ui.geometry.Size(radius * 2.8f, radius * 1.1f), style = Stroke(width = thin))
                drawOval(violet, topLeft = Offset(center.x - radius * 1.4f, center.y - radius * .55f), size = androidx.compose.ui.geometry.Size(radius * 2.8f, radius * 1.1f), style = Stroke(width = thin), alpha = .8f)
                drawCircle(white, radius = radius * .12f, center = Offset(center.x + radius * 1.15f, center.y - radius * .35f))
                drawCircle(mint, radius = radius * .09f, center = Offset(center.x - radius * .95f, center.y + radius * .7f))
                drawCircle(violet, radius = radius * 1.45f, center = center, style = Stroke(width = .7.dp.toPx()), alpha = .8f)
            }
        }
    }
}

private fun checkAstraBackend(baseUrl: String): String {
    if (baseUrl.isBlank()) return "त्रुटि: Backend URL खाली है।"
    return try {
        val clean = baseUrl.trim().trimEnd('/')
        if (!URLUtil.isHttpsUrl(clean)) return "त्रुटि: केवल HTTPS backend स्वीकार है।"
        val connection = (URL("$clean/health").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) "सर्वर उपलब्ध नहीं · HTTP $code"
        else {
            val json = JSONObject(body)
            if (json.optBoolean("ok")) "कनेक्शन सफल · " + json.optString("provider", "AI") + " · " + json.optString("model", "मॉडल सक्रिय")
            else "सर्वर मिला, पर health check सफल नहीं हुआ।"
        }
    } catch (e: Exception) {
        "कनेक्शन विफल: " + (e.message ?: "नेटवर्क त्रुटि")
    }
}

private fun askResearch(baseUrl: String, query: String): String {
    if (baseUrl.isBlank()) return "ASTRA backend URL सेव करें।"
    return try {
        val clean = baseUrl.trim().trimEnd('/')
        if (!URLUtil.isHttpsUrl(clean)) return "सुरक्षा के लिए backend URL HTTPS होना चाहिए।"
        val connection = (URL("$"+"clean/v1/research").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val payload = JSONObject().put("query", query)
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) {
            val message = runCatching { JSONObject(body).optString("error") }.getOrDefault("")
            "Deep Research त्रुटि (HTTP $code): $"+"{message.ifBlank { body.take(180) }}"
        } else {
            val json = JSONObject(body)
            val report = json.optString("text")
            val sources = json.optJSONArray("sources") ?: JSONArray()
            val sourceText = buildString {
                for (index in 0 until sources.length()) {
                    val item = sources.optJSONObject(index) ?: continue
                    append("\n[").append(item.optInt("id")).append("] ")
                    append(item.optString("title")).append(" (").append(item.optString("year")).append(")")
                    append("\n").append(item.optString("url")).append("\n")
                }
            }
            report + if (sourceText.isNotBlank()) "\n\nस्रोत सूची\n" + sourceText else ""
        }
    } catch (error: Exception) {
        "Deep Research कनेक्शन विफल: $"+"{error.message ?: "नेटवर्क त्रुटि"}"
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
            "Groq सेवा में त्रुटि (HTTP $code): ${message.ifBlank { body.take(180) }}"
        } else {
            val json = JSONObject(body)
            json.optString("text").ifBlank { json.optString("response").ifBlank { "सर्वर से उत्तर मिला, लेकिन टेक्स्ट नहीं मिला।" } }
        }
    } catch (e: Exception) {
        "ASTRA सर्वर से कनेक्शन नहीं हो पाया: ${e.message ?: "नेटवर्क त्रुटि"}"
    }
}
