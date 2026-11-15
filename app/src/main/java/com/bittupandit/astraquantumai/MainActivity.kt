package com.bittupandit.astraquantumai

import android.app.Activity
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.net.Uri
import android.util.Base64
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val night = Color(0xFF070B1B)
private val panel = Color(0xFF121B36)
private val accent = Color(0xFF7187FF)
private val pale = Color(0xFFB8C5FF)

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
                pickedMime = contentResolver.getType(it) ?: "Unknown file"
            }
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = accent, secondary = Color(0xFFB58CFF),
                background = night, surface = panel
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
                    onPickFile = { fileLauncher.launch(arrayOf("*/*")) }
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
    var status by remember { mutableStateOf("तैयार") }
    var backend by remember { mutableStateOf(prefs.getString("backend_url", "https://astraquantum-ai.onrender.com") ?: "https://astraquantum-ai.onrender.com") }
    var memoryInput by remember { mutableStateOf("") }
    var memories by remember { mutableStateOf(prefs.getString("memories", "") ?: "") }
    var projectInput by remember { mutableStateOf("") }
    var projects by remember { mutableStateOf(prefs.getString("projects", "") ?: "") }
    var chat by remember { mutableStateOf(listOf(ChatLine("ASTRA", "नमस्ते! अपना सवाल लिखें। ऑनलाइन AI के लिए Settings में सुरक्षित backend URL जोड़ें।"))) }
    var qubitAngle by remember { mutableFloatStateOf(45f) }
    var qubitResult by remember { mutableStateOf("Qubit तैयार है। कोण बदलकर मापन करें।") }
    var fileAnalysis by remember { mutableStateOf("") }
    var fileBusy by remember { mutableStateOf(false) }

    LaunchedEffect(speechText) { if (speechText.isNotBlank()) prompt = speechText }

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

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101B43), night))).padding(16.dp)
    ) {
        Text("ASTRA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("QUANTUM AI PLUS", color = pale, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("Creator by Bittu Pandit", color = Color(0xFF8795BC), fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Home", "AI Chat", "Research", "Memory").forEach { item ->
                TextButton(onClick = { page = item }) { Text(item, fontSize = 11.sp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Voice", "Files", "Quantum", "Projects", "Settings").forEach { item ->
                TextButton(onClick = { page = item }) { Text(item, fontSize = 10.sp) }
            }
        }
        HorizontalDivider(color = Color(0x334B5D9B))
        Spacer(Modifier.height(12.dp))
        when (page) {
            "Home" -> {
                Text("आपका व्यक्तिगत AI कार्यक्षेत्र", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("एक जगह बातचीत, खोज, यादें और प्रयोग।", color = pale)
                Spacer(Modifier.height(14.dp))
                listOf("AI Chat" to "AI से बातचीत", "Research" to "वेब पर स्रोत खोजें", "Memory" to "स्थानीय निजी नोट्स", "Voice" to "बोलकर पूछें और सुनें", "Files" to "PDF और चित्र चुनें", "Quantum" to "Qubit सिम्युलेटर", "Projects" to "प्रोजेक्ट नोटबुक", "Settings" to "AI सर्वर कनेक्शन").forEach { (name, desc) ->
                    Card(
                        onClick = { page = name },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = panel),
                        shape = RoundedCornerShape(14.dp)
                    ) { Column(Modifier.padding(13.dp)) {
                        Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(desc, color = pale, fontSize = 12.sp)
                    } }
                }
            }
            "AI Chat" -> {
                Text("AI CHAT", color = pale, fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chat) { line ->
                        Card(colors = CardDefaults.cardColors(containerColor = if (line.from == "You") Color(0xFF26366D) else panel)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(line.from, color = pale, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(line.text, color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("ASTRA संदेश", line.text))
                                        android.widget.Toast.makeText(context, "मैसेज कॉपी हो गया", android.widget.Toast.LENGTH_SHORT).show()
                                    }) { Text("कॉपी करें") }
                                    if (line.from == "ASTRA") TextButton(onClick = { onSpeak(line.text) }) { Text("उत्तर सुनें") }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("अपना सवाल लिखें") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onListen) { Text("बोलें") }
                    Button(enabled = prompt.isNotBlank() && !busy, onClick = {
                        val question = prompt.trim()
                        chat = chat + ChatLine("You", question)
                        prompt = ""
                        busy = true
                        status = "उत्तर तैयार हो रहा है…"
                        activity.lifecycleScope.launch {
                            val answer = withContext(Dispatchers.IO) { askBackend(backend, question) }
                            chat = chat + ChatLine("ASTRA", answer)
                            busy = false
                            status = if (backend.isBlank()) "Offline मोड" else "उत्तर प्राप्त"
                        }
                    }) { Text(if (busy) "..." else "भेजें") }
                }
                Text(status, color = pale, fontSize = 11.sp)
            }
            "Research" -> {
                Text("DEEP RESEARCH", color = pale, fontWeight = FontWeight.Bold)
                Text("यह मॉड्यूल आपके प्रश्न के लिए वेब खोज खोलेगा। परिणामों और स्रोतों की जाँच ब्राउज़र में करें।", color = Color.White)
                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("क्या शोध करना है?") })
                Button(enabled = prompt.isNotBlank(), onClick = {
                    val q = URLEncoder.encode(prompt, "UTF-8")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
                }) { Text("वेब पर खोजें") }
                Text("ध्यान दें: यह अभी स्वतः स्रोत सत्यापन या रिपोर्ट संकलन नहीं करता।", color = pale, fontSize = 12.sp)
            }
            "Memory" -> {
                Text("PRIVATE MEMORY", color = pale, fontWeight = FontWeight.Bold)
                OutlinedTextField(memoryInput, { memoryInput = it }, Modifier.fillMaxWidth(), label = { Text("याद रखने के लिए नोट") })
                Button(enabled = memoryInput.isNotBlank(), onClick = { addMemory(memoryInput) }) { Text("नोट सेव करें") }
                Text("आपके फोन में सेव नोट्स:", color = Color.White)
                Text(memories.ifBlank { "अभी कोई नोट नहीं।" }, color = pale)
                TextButton(onClick = { memories = ""; prefs.edit().remove("memories").apply() }) { Text("सभी स्थानीय नोट मिटाएँ") }
            }
            "Voice" -> {
                Text("VOICE ASSISTANT", color = pale, fontWeight = FontWeight.Bold)
                Text("हिंदी में बोलें। पहचाना गया टेक्स्ट AI Chat में आ जाएगा।", color = Color.White)
                Button(onClick = onListen) { Text("🎙 बोलकर पूछें") }
                Text("पहचाना गया: $speechText", color = pale)
                Button(enabled = speechText.isNotBlank(), onClick = { onSpeak(speechText) }) { Text("आवाज़ में सुनें") }
            }
            "Files" -> {
                Text("IMAGE & PDF", color = pale, fontWeight = FontWeight.Bold)
                Button(onClick = onPickFile) { Text("फोन से फ़ाइल चुनें") }
                Text("चुनी गई फ़ाइल: $pickedFile", color = Color.White)
                Text("प्रकार: $pickedMime", color = pale)
                Button(enabled = pickedUri != null && !fileBusy && backend.isNotBlank(), onClick = {
                    val uri = pickedUri
                    if (uri != null) {
                        fileBusy = true
                        fileAnalysis = "फ़ाइल का विश्लेषण हो रहा है…"
                        activity.lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                        ?: error("फ़ाइल पढ़ी नहीं जा सकी")
                                    require(bytes.size <= 7 * 1024 * 1024) { "फ़ाइल 7 MB से छोटी रखें।" }
                                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    askBackend(backend, "इस फ़ाइल का हिंदी में सारांश, मुख्य बिंदु और महत्वपूर्ण तथ्य बताइए।", pickedMime, encoded)
                                }.getOrElse { "फ़ाइल विश्लेषण विफल: ${it.message}" }
                            }
                            fileAnalysis = result
                            fileBusy = false
                        }
                    }
                }) { Text(if (fileBusy) "विश्लेषण…" else "AI से फ़ाइल का विश्लेषण") }
                if (backend.isBlank()) Text("पहले Settings में HTTPS backend URL सेव करें।", color = pale)
                Text(fileAnalysis, color = Color.White)
                if (fileAnalysis.isNotBlank()) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("ASTRA फ़ाइल त्रुटि", fileAnalysis))
                        android.widget.Toast.makeText(context, "मैसेज कॉपी हो गया", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text("कॉपी करें") }
                }
            }
            "Quantum" -> {
                Text("QUANTUM LAB · QUBIT SIMULATOR", color = pale, fontWeight = FontWeight.Bold)
                Text("एक शैक्षिक single-qubit probability मॉडल। यह असली quantum hardware नहीं है।", color = Color.White)
                Spacer(Modifier.height(18.dp))
                Text("State angle: ${qubitAngle.toInt()}°", color = Color.White)
                Slider(value = qubitAngle, onValueChange = { qubitAngle = it }, valueRange = 0f..180f)
                Button(onClick = {
                    val p0 = kotlin.math.cos(Math.toRadians((qubitAngle / 2.0).toDouble())).let { it * it }
                    val p1 = 1.0 - p0
                    qubitResult = "P(0) = ${"%.3f".format(p0)}   |   P(1) = ${"%.3f".format(p1)}"
                }) { Text("मापन संभावना निकालें") }
                Text(qubitResult, color = pale)
            }
            "Projects" -> {
                Text("WORKSPACE", color = pale, fontWeight = FontWeight.Bold)
                OutlinedTextField(projectInput, { projectInput = it }, Modifier.fillMaxWidth(), label = { Text("प्रोजेक्ट / कार्य नोट") })
                Button(enabled = projectInput.isNotBlank(), onClick = { addProject(projectInput) }) { Text("सेव करें") }
                Text(projects.ifBlank { "अभी कोई प्रोजेक्ट नोट नहीं।" }, color = Color.White)
            }
            "Settings" -> {
                Text("CONNECTION SETTINGS", color = pale, fontWeight = FontWeight.Bold)
                Text("अपने सुरक्षित backend का HTTPS URL यहाँ दें। Gemini API key ऐप में न डालें; उसे backend secret में ही रखें।", color = Color.White)
                OutlinedTextField(backend, { backend = it }, Modifier.fillMaxWidth(), label = { Text("Backend URL (उदा. https://your-server)") })
                Button(onClick = {
                    backend = backend.trim().trimEnd('/')
                    prefs.edit().putString("backend_url", backend).apply()
                    status = "Backend URL सेव हुआ"
                }) { Text("सेटिंग सेव करें") }
                Text("Backend API contract: POST /v1/generate  { prompt: string } → { text: string }", color = pale, fontSize = 11.sp)
                Text("कोई URL न होने पर चैट केवल offline सूचना देगी।", color = pale, fontSize = 11.sp)
            }
            else -> Text("मॉड्यूल उपलब्ध नहीं", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text("Creator by Bittu Pandit", color = Color(0xFF8795BC), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
    }
}

private fun askBackend(baseUrl: String, prompt: String, mimeType: String? = null, base64: String? = null): String {
    if (baseUrl.isBlank()) return "अभी ASTRA Offline मोड में है। ऑनलाइन AI उत्तर के लिए Settings में अपना HTTPS backend URL जोड़ना होगा। Gemini API key को Android ऐप में कभी न रखें।"
    return try {
        val clean = baseUrl.trim().trimEnd('/')
        if (!URLUtil.isHttpsUrl(clean)) return "सुरक्षा के लिए backend URL HTTPS होना चाहिए।"
        val connection = (URL("$clean/v1/generate").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        connection.outputStream.use { it.write(JSONObject().put("prompt", prompt).toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) "AI सेवा ने HTTP $code लौटाया: ${body.take(240)}"
        else {
            val json = JSONObject(body)
            json.optString("text").ifBlank { json.optString("response").ifBlank { "सर्वर से उत्तर मिला, लेकिन 'text' फ़ील्ड नहीं मिली।" } }
        }
    } catch (e: Exception) {
        "AI सर्वर से कनेक्शन नहीं हो पाया: ${e.message ?: "नेटवर्क त्रुटि"}"
    }
}
