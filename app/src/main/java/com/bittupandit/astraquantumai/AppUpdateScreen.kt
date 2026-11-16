package com.bittupandit.astraquantumai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AppUpdateScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<AppUpdateRepository.AvailableUpdate?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("अपडेट जाँचने के लिए बटन दबाएँ।") }
    var readyFile by remember { mutableStateOf<java.io.File?>(null) }
    var deltaUsed by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SECURE APP UPDATER", color = Color(0xFFECECEC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "अपडेट जाँचने के बाद ऐप आपके मौजूदा APK का SHA-256 मिलाएगा। मेल होने पर केवल बदलाव वाला पैच डाउनलोड होगा। मेल न होने पर ही पूरा APK चुना जाएगा।",
            color = Color(0xFFB4B4B4), fontSize = 12.sp
        )
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF303030))) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(status, color = Color(0xFFECECEC), fontSize = 12.sp)
                if (busy) CircularProgressIndicator(color = Color(0xFFD6D6D6))
                Button(
                    onClick = {
                        busy = true
                        readyFile = null
                        status = "GitHub से उपलब्ध अपडेट और डिजिटल हस्ताक्षर जाँचे जा रहे हैं…"
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { AppUpdateRepository.check(context) }
                            }
                            busy = false
                            result.onSuccess {
                                update = it
                                status = buildString {
                                    append("अपडेट उपलब्ध: ${it.versionName} (Build ${it.versionCode})\n")
                                    if (it.deltaCompatible) {
                                        append("✓ आपका इंस्टॉल किया APK पैच के आधार से मेल खाता है।\n")
                                        append("डाउनलोड होगा: छोटा पैच (${formatBytes(it.patchSizeBytes)})")
                                    } else {
                                        append("आपका इंस्टॉल किया APK इस पैच के आधार से मेल नहीं खाता।\n")
                                        append("डाउनलोड होगा: पूरा APK (${formatBytes(it.apkSizeBytes)})")
                                    }
                                }
                            }.onFailure {
                                update = null
                                status = "अपडेट उपलब्ध नहीं या जाँच विफल: ${it.message ?: "अज्ञात त्रुटि"}"
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("CHECK FOR UPDATE") }

                update?.let { available ->
                    Text(
                        if (available.deltaCompatible) "चुना गया अपडेट: छोटा पैच • ${formatBytes(available.patchSizeBytes)}"
                        else "चुना गया अपडेट: पूरा APK • ${formatBytes(available.apkSizeBytes)}",
                        color = Color(0xFFECECEC), fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            busy = true
                            status = "सही पैच/APK डाउनलोड और सत्यापित किया जा रहा है…"
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { AppUpdateRepository.prepare(context, available) }
                                }
                                busy = false
                                result.onSuccess {
                                    readyFile = it.apk
                                    deltaUsed = it.isDelta
                                    status = if (it.isDelta) {
                                        "छोटा डेल्टा पैच सफलतापूर्वक जाँचा गया। Android इंस्टॉलर खोलने के लिए नीचे दबाएँ।"
                                    } else {
                                        "पूरा APK सफलतापूर्वक जाँचा गया। Android इंस्टॉलर खोलने के लिए नीचे दबाएँ।"
                                    }
                                }.onFailure {
                                    readyFile = null
                                    status = "अपडेट रोक दिया गया: ${it.message ?: "सुरक्षा जाँच विफल"}"
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("DOWNLOAD & VERIFY UPDATE") }
                }

                readyFile?.let { apk ->
                    Text(
                        if (deltaUsed) "डाउनलोड: डेल्टा पैच" else "डाउनलोड: पूरा APK",
                        color = Color(0xFFD6D6D6), fontSize = 11.sp
                    )
                    Button(
                        onClick = { openAndroidInstaller(context, apk) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("OPEN ANDROID INSTALLER") }
                    Text(
                        "इंस्टॉल Android की सिस्टम स्क्रीन पर आपकी अनुमति से ही होगा। ASTRA अपने-आप ऐप को बदल नहीं सकता।",
                        color = Color(0xFFB4B4B4), fontSize = 11.sp
                    )
                }
            }
        }
        AppBuildInfoScreen()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "आकार उपलब्ध नहीं"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(java.util.Locale.US, "%.2f MB", mb)
}

private fun openAndroidInstaller(context: Context, apk: java.io.File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        val settings = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(settings)
        return
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apk
    )
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(installIntent)
}
