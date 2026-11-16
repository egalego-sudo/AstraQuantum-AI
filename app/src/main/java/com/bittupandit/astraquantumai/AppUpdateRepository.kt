package com.bittupandit.astraquantumai

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal object AppUpdateRepository {
    data class AvailableUpdate(
        val metadata: UpdateMetadataVerifier.VerifiedMetadata,
        val patchUrl: String,
        val apkUrl: String,
        val versionCode: Long,
        val versionName: String
    )

    data class PreparedUpdate(
        val apk: File,
        val isDelta: Boolean,
        val versionCode: Long
    )

    fun check(context: Context): AvailableUpdate {
        val release = getJson("https://api.github.com/repos/egalego-sudo/AstraQuantum-AI/releases/latest")
        val assets = release.getJSONArray("assets")
        val urls = mutableMapOf<String, String>()
        for (i in 0 until assets.length()) {
            val item = assets.getJSONObject(i)
            urls[item.getString("name")] = item.getString("browser_download_url")
        }
        val metadataUrl = urls["UPDATE-METADATA.json"] ?: error("Latest release has no update metadata.")
        val signatureUrl = urls["UPDATE-METADATA.sig"] ?: error("Latest release has no metadata signature.")
        val metadataBytes = downloadBytes(metadataUrl, 256 * 1024)
        val signatureText = String(downloadBytes(signatureUrl, 16 * 1024), Charsets.UTF_8)
        val verified = UpdateMetadataVerifier.verify(context, metadataBytes, signatureText)
        val json = verified.json
        val patchUrl = json.getString("patchUrl")
        val apkUrl = json.getString("apkUrl")
        check(urls.values.any { it == patchUrl } && urls.values.any { it == apkUrl }) {
            "Signed update URLs do not match assets in the latest release."
        }
        return AvailableUpdate(
            verified, patchUrl, apkUrl,
            json.getLong("targetVersionCode"),
            json.getString("targetVersionName")
        )
    }

    fun prepare(context: Context, update: AvailableUpdate): PreparedUpdate {
        val json = update.metadata.json
        val installedApk = File(context.applicationInfo.sourceDir)
        val installedHash = sha256(installedApk)
        val useDelta = installedHash.equals(json.getString("baseApkSha256"), true)
        val directory = File(context.cacheDir, "updates")
        check(directory.exists() || directory.mkdirs()) { "Cannot create update download folder." }
        val downloaded = File(directory, if (useDelta) "astra-update.patch" else "astra-update.apk")
        if (downloaded.exists()) check(downloaded.delete()) { "Cannot replace previous update download." }

        downloadToFile(if (useDelta) update.patchUrl else update.apkUrl, downloaded,
            if (useDelta) 80L * 1024 * 1024 else 150L * 1024 * 1024)

        val expectedSigner = json.getString("signerCertificateSha256")
        val expectedPackage = json.getString("packageName")
        val expectedVersion = update.versionCode
        val verifiedApk = if (useDelta) {
            ApkDeltaEngine.reconstructAndVerify(
                context, downloaded,
                json.getString("baseApkSha256"),
                json.getString("targetApkSha256"),
                expectedPackage,
                expectedVersion,
                expectedSigner
            )
        } else {
            ApkDeltaEngine.verifyFullApk(
                context, downloaded,
                json.getString("targetApkSha256"),
                expectedPackage,
                expectedVersion,
                expectedSigner
            )
        }
        return PreparedUpdate(verifiedApk.file, useDelta, verifiedApk.versionCode)
    }

    private fun getJson(url: String): JSONObject =
        JSONObject(String(downloadBytes(url, 2 * 1024 * 1024), Charsets.UTF_8))

    private fun downloadBytes(url: String, maxBytes: Int): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "ASTRA-QUANTUM-AI-PLUS-Updater")
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Update server returned HTTP ${connection.responseCode}."
            }
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= maxBytes) { "Update metadata is unexpectedly large." }
                    output.write(buffer, 0, count)
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadToFile(url: String, target: File, maxBytes: Long) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 60000
        connection.setRequestProperty("User-Agent", "ASTRA-QUANTUM-AI-PLUS-Updater")
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Update download failed with HTTP ${connection.responseCode}."
            }
            val declared = connection.contentLengthLong
            check(declared <= 0L || declared <= maxBytes) { "Update file exceeds the safety size limit." }
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= maxBytes) { "Update file exceeds the safety size limit." }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            check(target.length() > 0L) { "Downloaded update file is empty." }
        } catch (error: Exception) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
