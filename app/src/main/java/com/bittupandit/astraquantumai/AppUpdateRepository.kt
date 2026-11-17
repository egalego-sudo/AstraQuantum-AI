package com.bittupandit.astraquantumai

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject

internal object AppUpdateRepository {
    data class AvailableUpdate(
        val metadata: UpdateMetadataVerifier.VerifiedMetadata,
        val patchUrl: String,
        val apkUrl: String,
        val versionCode: Long,
        val versionName: String,
        val patchSizeBytes: Long,
        val apkSizeBytes: Long,
        val deltaCompatible: Boolean,
        val baseApkSha256: String?
    )

    data class PreparedUpdate(
        val apk: File,
        val isDelta: Boolean,
        val versionCode: Long
    )

    fun check(context: Context): AvailableUpdate {
        val release = getJson("https://api.github.com/repos/egalego-sudo/Quantum-AI/releases/latest")
        val assets = release.getJSONArray("assets")
        val urls = mutableMapOf<String, String>()
        val sizes = mutableMapOf<String, Long>()
        for (i in 0 until assets.length()) {
            val item = assets.getJSONObject(i)
            urls[item.getString("name")] = item.getString("browser_download_url")
            sizes[item.getString("name")] = item.optLong("size", 0L)
        }
        val metadataUrl = urls["UPDATE-METADATA.json"] ?: error("Latest release has no update metadata.")
        val signatureUrl = urls["UPDATE-METADATA.sig"] ?: error("Latest release has no metadata signature.")
        val metadataBytes = downloadBytes(metadataUrl, 256 * 1024)
        val signatureText = String(downloadBytes(signatureUrl, 16 * 1024), Charsets.UTF_8)
        val verified = UpdateMetadataVerifier.verify(context, metadataBytes, signatureText)
        val json = verified.json
        val apkUrl = json.getString("apkUrl")
        check(urls.values.any { it == apkUrl }) { "Signed APK URL does not match an asset in the latest release." }
        val installedHash = sha256(File(context.applicationInfo.sourceDir))
        val patches = json.optJSONArray("patches")
        var selectedPatchUrl = ""
        var selectedBaseHash: String? = null
        var selectedPatchSize = 0L
        if (patches != null) {
            for (i in 0 until patches.length()) {
                val item = patches.getJSONObject(i)
                val candidateUrl = item.getString("patchUrl")
                check(urls.values.any { it == candidateUrl }) { "Signed patch URL does not match an asset in the latest release." }
                if (installedHash.equals(item.getString("baseApkSha256"), true)) {
                    selectedPatchUrl = candidateUrl
                    selectedBaseHash = item.getString("baseApkSha256")
                    selectedPatchSize = item.optLong("patchSizeBytes", 0L)
                    break
                }
            }
        } else if (json.has("patchUrl") && json.has("baseApkSha256") &&
            installedHash.equals(json.getString("baseApkSha256"), true)) {
            selectedPatchUrl = json.getString("patchUrl")
            check(urls.values.any { it == selectedPatchUrl }) { "Signed legacy patch URL does not match a release asset." }
            selectedBaseHash = json.getString("baseApkSha256")
            selectedPatchSize = sizes["ASTRA-build150-to-current.patch"] ?: 0L
        }
        return AvailableUpdate(
            verified, selectedPatchUrl, apkUrl,
            json.getLong("targetVersionCode"), json.getString("targetVersionName"),
            selectedPatchSize, sizes["ASTRA-QUANTUM-AI.apk"] ?: 0L,
            selectedBaseHash != null, selectedBaseHash
        )
    }

    fun prepare(context: Context, update: AvailableUpdate): PreparedUpdate {
        val json = update.metadata.json
        val installedApk = File(context.applicationInfo.sourceDir)
        val installedHash = sha256(installedApk)
        val useDelta = update.deltaCompatible && update.baseApkSha256 != null &&
            installedHash.equals(update.baseApkSha256, true)
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
                update.baseApkSha256!!,
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
        connection.setRequestProperty("User-Agent", "QUANTUM-AI-Updater")
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
        connection.setRequestProperty("User-Agent", "QUANTUM-AI-Updater")
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
