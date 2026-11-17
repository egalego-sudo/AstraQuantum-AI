package com.bittupandit.astraquantumai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
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
fun InstantAppBuilderScreen(backendUrl: String, initialWebsiteMode: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var websiteMode by remember { mutableStateOf(initialWebsiteMode) }
    var idea by remember { mutableStateOf("") }
    var html by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("ऐप या वेबसाइट चुनें और अपना आइडिया लिखें।") }
    var showPreview by remember { mutableStateOf(true) }
    var previewDevice by remember { mutableStateOf("मोबाइल") }
    var previewWebView by remember { mutableStateOf<WebView?>(null) }
    var nativeAppName by remember { mutableStateOf("My Astra App") }
    var pendingNativeZip by remember { mutableStateOf<ByteArray?>(null) }
    val nativeZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        val bytes = pendingNativeZip
        if (uri != null && bytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("फाइल सेव नहीं हो सकी।") }
                .onSuccess { status = "Native Android source ZIP सेव हो गई। इसे Android Studio में खोलें।" }
                .onFailure { status = "ZIP सेव नहीं हुई: " + (it.message ?: "त्रुटि") }
        } else if (uri == null) status = "ZIP सेव करना रद्द किया गया।"
        pendingNativeZip = null
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ASTRA WEB APP BUILDER", color = builderText, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text("WEB PROTOTYPE → LIVE PREVIEW", color = builderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
            else "यह विकल्प केवल HTML/CSS/JavaScript वेब-ऐप प्रोटोटाइप बनाता है। यह Native Android प्रोजेक्ट या APK नहीं बनाता। APK निर्माण अलग सुविधा है और अभी उपलब्ध नहीं है।",
            color = builderMuted, fontSize = 12.sp, lineHeight = 18.sp
        )
        if (!websiteMode) {
            Card(colors = CardDefaults.cardColors(containerColor = builderCard), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NATIVE ANDROID PROJECT EXPORT", color = builderAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("यह Android Studio प्रोजेक्ट की शुरुआती Kotlin + Jetpack Compose फाइलें ZIP में बनाएगा। यह अभी APK compile नहीं करता।", color = builderMuted, fontSize = 11.sp, lineHeight = 16.sp)
                    OutlinedTextField(value = nativeAppName, onValueChange = { nativeAppName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("ऐप का नाम") })
                    OutlinedButton(onClick = {
                        busy = true
                        status = "Native Android प्रोजेक्ट ZIP तैयार हो रही है…"
                        scope.launch {
                            try {
                                pendingNativeZip = withContext(Dispatchers.IO) { createNativeAndroidProjectZip(nativeAppName) }
                                val fileName = nativeAppName.trim().replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifBlank { "AstraAndroidApp" } + "-Android-Project.zip"
                                nativeZipLauncher.launch(fileName)
                                status = "अब ZIP सेव करने की जगह चुनें।"
                            } catch (error: Exception) {
                                status = "Native प्रोजेक्ट नहीं बना: " + (error.message ?: "त्रुटि")
                            } finally { busy = false }
                        }
                    }, enabled = nativeAppName.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { Text("Native Android प्रोजेक्ट ZIP बनाएं") }
                }
            }
        }
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
                        status = if (websiteMode) "वेबसाइट तैयार है। नीचे Preview देखें।" else "वेब ऐप प्रोटोटाइप तैयार है। नीचे Preview देखें।"
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
            } else Text(if (websiteMode) "✦  वेबसाइट बनाइए" else "✦  वेब ऐप प्रोटोटाइप बनाइए")
        }
        Text(status, color = builderMuted, fontSize = 12.sp)
        if (html.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPreview = true }, colors = ButtonDefaults.buttonColors(containerColor = builderCard)) { Text("▶ चलाकर देखें") }
                OutlinedButton(onClick = {
                    copyGeneratedCode(context, html, websiteMode)
                    status = if (websiteMode) "वेबसाइट का HTML कोड कॉपी हो गया।" else "वेब ऐप प्रोटोटाइप का HTML कोड कॉपी हो गया।"
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
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val uri = request?.url ?: return true
                                            return !PreviewSecurityPolicy.allowsNavigation(uri.toString())
                                        }
                                    }
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.loadsImagesAutomatically = true
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    settings.allowFileAccessFromFileURLs = false
                                    settings.allowUniversalAccessFromFileURLs = false
                                    settings.javaScriptCanOpenWindowsAutomatically = false
                                    settings.setSupportMultipleWindows(false)
                                    settings.blockNetworkLoads = true
                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        settings.safeBrowsingEnabled = true
                                    }
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
                else "यह केवल वेब-ऐप प्रोटोटाइप है। इससे Android APK या Play Store पैकेज नहीं बनता। Native Android APK Generator अभी इस संस्करण में उपलब्ध नहीं है।",
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


private fun createNativeAndroidProjectZip(appNameInput: String): ByteArray {
    val appName = appNameInput.trim()
        .replace(Regex("[^\\p{L}\\p{N} _-]"), " ")
        .replace(Regex("\\s+"), " ")
        .take(40)
        .ifBlank { "My Astra App" }
    val slugRaw = appName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "my_astra_app" }
    val slug = if (slugRaw.firstOrNull()?.isLetter() == true) slugRaw else "app_$slugRaw"
    val packageName = "com.example." + slug.take(32)
    val packagePath = packageName.replace('.', '/')
    val files = linkedMapOf(
        "README.md" to "# $appName\n\nNative Android starter project generated by ASTRA. Open this folder in Android Studio and let Gradle sync.\n\nThis is source code, not a compiled APK. Build with Build > Build Bundle(s) / APK(s) > Build APK(s).\n\nCreator by Bittu Pandit\n",
        "settings.gradle.kts" to """pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "$appName"
include(":app")
""",
        "build.gradle.kts" to """plugins { id("com.android.application") version "8.7.3" apply false; id("org.jetbrains.kotlin.android") version "2.0.21" apply false; id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false }
""",
        "gradle.properties" to """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
""",
        "app/build.gradle.kts" to """plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "$packageName"
    compileSdk = 35
    defaultConfig { applicationId = "$packageName"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
}
""",
        "app/src/main/AndroidManifest.xml" to """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
<application android:label="$appName" android:theme="@android:style/Theme.Material.Light.NoActionBar">
<activity android:name=".MainActivity" android:exported="true">
<intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
</activity></application></manifest>
""",
        "app/src/main/java/$packagePath/MainActivity.kt" to """package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { StarterScreen() } }
    }
}

@Composable
private fun StarterScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$appName", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("आपका Native Android ऐप प्रोजेक्ट तैयार है।")
            Spacer(Modifier.height(24.dp))
            Text("Creator by Bittu Pandit", style = MaterialTheme.typography.labelSmall)
        }
    }
}
"""
    )
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        files.forEach { (path, content) ->
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
    return output.toByteArray()
}
