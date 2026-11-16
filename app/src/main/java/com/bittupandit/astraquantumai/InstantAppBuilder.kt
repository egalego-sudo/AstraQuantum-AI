package com.bittupandit.astraquantumai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
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
    var websiteMode by remember { mutableStateOf(false) }
    var idea by remember { mutableStateOf("") }
    var html by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("ऐप या वेबसाइट चुनें और अपना आइडिया लिखें।") }
    var showPreview by remember { mutableStateOf(true) }
    var previewDevice by remember { mutableStateOf("मोबाइल") }
    var previewWebView by remember { mutableStateOf<WebView?>(null) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ASTRA INSTANT BUILDER", color = builderText, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text("YOUR IDEA → READY-TO-PREVIEW PROJECT", color = builderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (websiteMode) {
                OutlinedButton(onClick = { websiteMode = false; html = ""; status = "ऐप बनाने का आइडिया लिखें।" }) { Text("ऐप बनाएं") }
                Button(onClick = { websiteMode = true; html = ""; status = "वेबसाइट बनाने का आइडिया लिखें।" }, colors = ButtonDefaults.buttonColors(containerColor = builderAccent, contentColor = builderInk)) { Text("वेबसाइट बनाएं") }
            } else {
                Button(onClick = { websiteMode = false; html = ""; status = "ऐप बनाने का आइडिया लिखें।" }, colors = ButtonDefaults.buttonColors(containerColor = builderAccent, contentColor = builderInk)) { Text("ऐप बनाएं") }
                OutlinedButton(onClick = { websiteMode = true; html = ""; status = "वेबसाइट बनाने का आइडिया लिखें।" }) { Text("वेबसाइट बनाएं") }
            }
        }
        Text(
            if (websiteMode) "अपनी वेबसाइट का नाम, उद्देश्य और जरूरी पेज बताइए। ASTRA मोबाइल-फ्रेंडली वेबसाइट का डिजाइन और कोड बनाकर यहीं Preview दिखाएगा।"
            else "सिर्फ बताइए कि कैसी ऐप चाहिए। ASTRA उसका शुरुआती HTML/CSS/JavaScript प्रोटोटाइप बनाएगा और यहीं Preview दिखाएगा।",
            color = builderMuted, fontSize = 12.sp, lineHeight = 18.sp
        )
        OutlinedTextField(
            value = idea,
            onValueChange = { idea = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text(if (websiteMode) "किस तरह की वेबसाइट बनानी है?" else "आप कौन-सी ऐप बनाना चाहती हैं?") },
            placeholder = {
                Text(
                    if (websiteMode) "उदाहरण: मेरी बुटीक की वेबसाइट बनाओ—होम, हमारे बारे में, प्रोडक्ट गैलरी, संपर्क और WhatsApp बटन के साथ…"
                    else "उदाहरण: दुकानदार के लिए हिसाब-किताब ऐप, जिसमें ग्राहक, उधार और भुगतान का रिकॉर्ड हो…"
                )
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                idea = if (websiteMode) "एक आधुनिक बुटीक वेबसाइट बनाओ जिसमें होम पेज, हमारे बारे में, प्रोडक्ट गैलरी, कीमतें, संपर्क और WhatsApp संपर्क बटन हो।"
                else "एक सुंदर टू-डू ऐप बनाओ जिसमें काम जोड़ सकूँ, पूरा होने पर टिक कर सकूँ और काम हटाने का विकल्प हो।"
            }) { Text(if (websiteMode) "बुटीक वेबसाइट" else "To-do ऐप") }
            OutlinedButton(onClick = {
                idea = if (websiteMode) "एक प्रोफेशनल व्यक्तिगत पोर्टफोलियो वेबसाइट बनाओ जिसमें परिचय, कौशल, काम के नमूने और संपर्क सेक्शन हों।"
                else "एक छोटा खर्चा मैनेजर बनाओ जिसमें खर्च जोड़ना, कुल रकम और खर्चों की सूची दिखे।"
            }) { Text(if (websiteMode) "पोर्टफोलियो" else "खर्चा ऐप") }
        }
        Button(
            onClick = {
                busy = true
                html = ""
                status = if (websiteMode) "ASTRA आपकी वेबसाइट बना रहा है…" else "ASTRA आपका ऐप बना रहा है…"
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) { buildPrototype(backendUrl, idea, websiteMode) }
                        html = result
                        showPreview = true
                        status = if (websiteMode) "वेबसाइट तैयार है। नीचे Preview देखें।" else "ऐप प्रोटोटाइप तैयार है। नीचे Preview देखें।"
                    } catch (error: Exception) {
                        status = "बनाने में समस्या: " + (error.message ?: "कनेक्शन त्रुटि")
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
                Text(if (websiteMode) "  वेबसाइट बन रही है…" else "  ऐप तैयार हो रही है…")
            } else Text(if (websiteMode) "✦  वेबसाइट बनाइए" else "✦  ऐप बनाइए")
        }
        Text(status, color = builderMuted, fontSize = 12.sp)
        if (html.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPreview = true }, colors = ButtonDefaults.buttonColors(containerColor = builderCard)) { Text("▶ चलाकर देखें") }
                OutlinedButton(onClick = {
                    copyGeneratedCode(context, html, websiteMode)
                    status = if (websiteMode) "वेबसाइट का HTML कोड कॉपी हो गया।" else "ऐप का HTML कोड कॉपी हो गया।"
                }) { Text("कोड कॉपी करें") }
            }
            if (showPreview) {
                Text("PREVIEW DEVICE", color = builderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { previewDevice = "मोबाइल" }) { Text("मोबाइल") }
                    OutlinedButton(onClick = { previewDevice = "डेस्कटॉप" }) { Text("डेस्कटॉप") }
                    OutlinedButton(onClick = { previewWebView?.reload() }) { Text("↻ Refresh") }
                }
                Text(if (previewDevice == "मोबाइल") "फोन स्क्रीन Preview" else "बड़ी स्क्रीन Preview", color = builderMuted, fontSize = 11.sp)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = if (previewDevice == "मोबाइल") 390.dp else 900.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(if (previewDevice == "मोबाइल") 620.dp else 560.dp),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.loadsImagesAutomatically = true
                                    setBackgroundColor(android.graphics.Color.WHITE)
                                    tag = ""
                                    previewWebView = this
                                }
                            },
                            update = { webView ->
                                previewWebView = webView
                                if (webView.tag != html) {
                                    webView.tag = html
                                    webView.loadDataWithBaseURL("https://astra.local/", html, "text/html", "UTF-8", null)
                                }
                            }
                        )
                    }
                }
            }
            Text(
                if (websiteMode) "यह आपकी वेबसाइट का तैयार HTML/CSS/JavaScript प्रोजेक्ट है और Preview यहीं चलता है। इसे सार्वजनिक इंटरनेट पर लाइव करने के लिए अलग hosting/publish चरण जोड़ना होगा; अभी वेबसाइट अपने-आप ऑनलाइन प्रकाशित नहीं होती।"
                else "यह वेब-ऐप प्रोटोटाइप है। अभी इससे सीधे Android APK या Play Store पैकेज नहीं बनता; वह अलग निर्माण चरण होगा।",
                color = builderMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Creator by Bittu Pandit", color = builderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun buildPrototype(backendUrl: String, idea: String, websiteMode: Boolean): String {
    require(idea.isNotBlank()) { "पहले अपना आइडिया लिखें।" }
    val prompt = if (websiteMode) {
        """
You are ASTRA INSTANT WEBSITE BUILDER. Create a complete, polished, production-style responsive single-file WEBSITE from the user's brief.
Return ONLY raw HTML, no Markdown fences or explanations.
Requirements:
- This is a WEBSITE, not an app dashboard. Create a professional public-facing site with strong visual hierarchy and clear navigation.
- Include all CSS and JavaScript inline in one valid HTML document.
- Mobile-first responsive layout for Android phones and desktop.
- Build the pages/sections requested (as a single-page website with working section navigation if multiple pages are requested).
- Include meaningful website copy, header, hero section, calls to action, footer, and contact details only when provided.
- Add SEO title, meta description, viewport, semantic headings, accessible labels, and social sharing metadata.
- Make menus, galleries, FAQ, forms and other requested interactions genuinely work. Forms must clearly say they are demo-only unless a real backend exists.
- Use only inline SVG/CSS artwork; no external libraries, CDN, remote assets, API keys, payment claims, login claims, or network calls.
- Add a small footer exactly: Creator by Bittu Pandit
- Escape user text safely when rendering it; do not use eval.
- Do not invent phone numbers, addresses, testimonials, certifications, or business facts.
- Return compact but complete code.
Website brief:
""" + idea
    } else {
        """
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
""" + idea
    }
    val cleanBase = backendUrl.trim().trimEnd('/')
    require(cleanBase.startsWith("https://")) { "Settings में सही HTTPS Backend URL सेव करें।" }
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
            throw IllegalStateException(message.ifBlank { "Backend HTTP " + code })
        }
        val raw = JSONObject(body).optString("text").trim()
        val cleaned = raw.removePrefix("```html").removePrefix("```").removeSuffix("```").trim()
        if (!cleaned.lowercase().contains("<html") && !cleaned.lowercase().contains("<!doctype html")) {
            throw IllegalStateException("AI ने पूरा HTML प्रोजेक्ट नहीं लौटाया। फिर कोशिश करें।")
        }
        return cleaned
    } finally {
        connection.disconnect()
    }
}

private fun copyGeneratedCode(context: Context, value: String, websiteMode: Boolean) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(if (websiteMode) "ASTRA generated website" else "ASTRA generated app", value))
}
