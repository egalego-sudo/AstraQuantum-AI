package com.bittupandit.astraquantumai

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest

@Composable
internal fun AppBuildInfoScreen() {
    val context = LocalContext.current
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionCode = if (Build.VERSION.SDK_INT >= 28) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    var apkSha256 by remember { mutableStateOf("जाँच जारी है…") }
    var hashError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val apkPath = context.applicationInfo.sourceDir
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(apkPath).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }
        apkSha256 = result.getOrNull() ?: "हैश नहीं पढ़ा जा सका"
        hashError = result.isFailure
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("INSTALLED APP IDENTITY", color = Color(0xFFECECEC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("यह जानकारी आपके फोन में इंस्टॉल APK से सीधे पढ़ी जाती है। डेल्टा अपडेट केवल उसी पुराने APK पर लागू होगा जिसका SHA-256 बिल्कुल मेल खाए।", color = Color(0xFFB4B4B4), fontSize = 12.sp)
        InfoCard("APP VERSION", packageInfo.versionName ?: "अज्ञात")
        InfoCard("BUILD NUMBER", versionCode.toString())
        InfoCard("PACKAGE ID", context.packageName)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF303030))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("INSTALLED APK · SHA-256", color = Color(0xFFD6D6D6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (apkSha256 == "जाँच जारी है…") CircularProgressIndicator(color = Color(0xFFECECEC))
                Text(apkSha256, color = if (hashError) Color(0xFFFFB4AB) else Color(0xFFECECEC), fontSize = 10.sp)
            }
        }
        Text("अपडेटर का अगला चरण: इस पहचान को सर्वर पर उपलब्ध पैच के base APK से मिलाना, फिर ही छोटा पैच डाउनलोड करना। अभी इस स्क्रीन से कोई डाउनलोड या इंस्टॉल नहीं होता।", color = Color(0xFFB4B4B4), fontSize = 11.sp)
        Text("Creator by Bittu Pandit", color = Color(0xFFD6D6D6), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF303030))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = Color(0xFFD6D6D6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color(0xFFECECEC), fontSize = 13.sp)
        }
    }
}
