package com.bittupandit.astraquantumai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val builderInk = Color(0xFF212121)
private val builderCard = Color(0xFF303030)
private val builderText = Color(0xFFECECEC)
private val builderMuted = Color(0xFFB4B4B4)
private val builderAccent = Color(0xFFD6D6D6)

@Composable
fun InstantAppBuilderScreen(backendUrl: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var idea by remember { mutableStateOf("") }
    var html by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("अपना ऐप आइडिया लिखें। ASTRA पहले एक चलने योग्य वेब-ऐप प्रोटोटाइप बनाएगा।") }
    var showPreview by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ASTRA INSTANT APP BUILDER", color = builderText, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text("IDEA → DESIGN → WORKING WEB APP", color = builderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("सिर्फ बताइए कि कैसी ऐप चाहिए। ASTRA उसका शुरुआती HTML/CSS/JavaScript प्रोटोटाइप बनाएगा, जिसे यहीं Preview कर सकेंगी।", color = builderMuted, fontSize = 12.sp, lineHeight = 18.sp)
        OutlinedTextField(
            value = idea,
            onValueChange = { idea = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("आप कौन-सी ऐप बनाना चाहती हैं?") },
            placeholder = { Text("उदाहरण: दुकानदार के लिए हिसाब-किताब ऐप, जिसमें ग्राहक, उधार और भुगतान का रिकॉर्ड हो…") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { idea = "एक सुंदर टू-डू ऐप बनाओ जिसमें काम जोड़ सकूँ, पूरा होने पर टिक कर सकूँ और काम हटाने का विकल्प हो।" }) { Text("To-do ऐप") }
            OutlinedButton(onClick = { idea = "एक छोटा खर्चा मैनेजर बनाओ जिसमें खर्च जोड़ना, कुल रकम और खर्चों की सूची दिखे।" }) { Text("खर्चा ऐप") }
        }
        Button(
            onClick = {
                busy = true
                html = ""
                status = "ASTRA आपका ऐप बना रहा है…"
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) { buildPrototype(backendUrl, idea) }
                        html = result
                        showPreview = true
                        status = "प्रोटोटाइप तैयार है। नीचे Preview देखें।"
                    } catch (error: Exception) {
                        status = "बनाने में समस्या: ${error.message ?: "कनेक्शन त्रुटि"}"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = idea.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = builderAccent, contentColor = builderInk),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(color = builderInk, modifier = Modifier.height(18.dp))
                Text("  ऐप तैयार हो रही है…")
            } else Text("✦  ऐप बनाइए")
        }
        Text(status, color = builderMuted, fontSize = 12.sp)
        if (html.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPreview = true }, colors = ButtonDefaults.buttonColors(containerColor = builderCard)) { Text("Preview") }
                OutlinedButton(onClick = { copyGeneratedCode(context, html); status = "पूरा HTML कोड कॉपी हो गया।" }) { Text("कोड कॉपी करें") }
            }
            if (showPreview) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(520.dp),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                setBackgroundColor(android.graphics.Color.WHITE)
                                tag = ""
                            }
                        },
                        update = { webView ->
                            if (webView.tag != html) {
                                webView.tag = html
                                webView.loadDataWithBaseURL("https://astra.local/", html, "text/html", "UTF-8", null)
                            }
                        }
                    )
                }
            }
            Text("यह पहला वेब-ऐप प्रोटोटाइप है। अभी इससे सीधे Android APK या Play Store पैकेज नहीं बनता; वह अगला निर्माण चरण होगा।", color = builderMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Creator by Bittu Pandit", color = builderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun buildPrototype(backendUrl: String, idea: String): String {
    require(idea.isNotBlank()) { "पहले ऐप का आइडिया लिखें।" }
    val prompt = """
You are ASTRA INSTANT APP BUILDER. Create a complete, polished, responsive single-file web app prototype from the user's idea.
Return ONLY raw HTML, no Markdown fences or explanations.
Requirements:
- Include all CSS and JavaScript inline in this one HTML document.
- Mobile-first layout that works on a 360px Android screen.
- Make the main user-requested features genuinely interactive, not decorative.
- Use accessible labels, clear Hindi UI by default unless the user asks otherwise.
- Use localStorage for simple demo data persistence when appropriate.
- No external libraries, CDN, remote assets, API keys, payments, login claims, or network calls.
- Add a small footer exactly: Creator by Bittu Pandit
- Escape user text safely when rendering it; do not use eval.
- Keep code compact enough for one response.
User's app idea:
$idea
""".trimIndent()
    val cleanBase = backendUrl.trim().trimEnd('/')
    val connection = (URL(cleanBase + "/v1/generate").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 90000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }
    try {
        connection.outputStream.use { it.write(JSONObject().put("prompt", prompt).toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching { JSONObject(body).optString("error") }.getOrDefault("")
            throw IllegalStateException(message.ifBlank { "Backend HTTP $code" })
        }
        val raw = JSONObject(body).optString("text").trim()
        val cleaned = raw.removePrefix("```html").removePrefix("```").removeSuffix("```").trim()
        if (!cleaned.lowercase().contains("<html") && !cleaned.lowercase().contains("<!doctype html")) {
            throw IllegalStateException("AI ने पूरा HTML प्रोटोटाइप नहीं लौटाया। फिर कोशिश करें।")
        }
        return cleaned
    } finally {
        connection.disconnect()
    }
}

private fun copyGeneratedCode(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ASTRA generated app", value))
}
