package com.bittupandit.astraquantumai

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.cundong.utils.PatchUtils
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Reconstructs an APK only when the installed base APK matches the patch metadata exactly.
 * Call from a worker thread. This class never installs or silently replaces the app.
 */
internal object ApkDeltaEngine {

    data class VerifiedApk(
        val file: File,
        val versionCode: Long,
        val sha256: String,
        val signerSha256: String
    )

    fun reconstructAndVerify(
        context: Context,
        patchFile: File,
        expectedBaseSha256: String,
        expectedTargetSha256: String,
        expectedPackageName: String,
        expectedVersionCode: Long,
        expectedSignerSha256: String
    ): VerifiedApk {
        require(patchFile.isFile && patchFile.length() > 0L) { "Update patch is missing or empty." }
        require(expectedBaseSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid base APK SHA-256." }
        require(expectedTargetSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid target APK SHA-256." }
        require(expectedSignerSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid signer SHA-256." }
        require(expectedPackageName == context.packageName) { "Update package ID does not match this app." }

        val packageManager = context.packageManager
        val installedInfo = packageManager.getPackageInfoCompat(context.packageName)
        val oldApk = File(context.applicationInfo.sourceDir)
        val baseHash = sha256(oldApk)
        check(baseHash.equals(expectedBaseSha256, ignoreCase = true)) {
            "This patch is not for the APK installed on this phone."
        }

        val installedSigner = signerSha256(installedInfo)
        check(installedSigner.equals(expectedSignerSha256, ignoreCase = true)) {
            "Installed app signing certificate does not match trusted update metadata."
        }

        val outputDirectory = File(context.filesDir, "verified-updates")
        check(outputDirectory.exists() || outputDirectory.mkdirs()) { "Cannot create private update folder." }
        val output = File(outputDirectory, "astra-update-candidate.apk")
        if (output.exists()) check(output.delete()) { "Cannot clear previous update candidate." }

        val patchCode = PatchUtils.patch(oldApk.absolutePath, output.absolutePath, patchFile.absolutePath)
        check(patchCode == PatchUtils.SUCCESS && output.isFile) {
            "APK reconstruction failed (code $patchCode)."
        }

        val targetHash = sha256(output)
        check(targetHash.equals(expectedTargetSha256, ignoreCase = true)) {
            "Reconstructed APK integrity check failed."
        }

        val archive = packageManager.getPackageArchiveInfoCompat(output.absolutePath)
            ?: error("Reconstructed file is not a valid Android APK.")
        val archiveVersion = archive.versionCodeCompat()
        check(archive.packageName == expectedPackageName) { "Reconstructed APK package ID mismatch." }
        check(archiveVersion == expectedVersionCode) { "Reconstructed APK version mismatch." }
        check(archiveVersion > installedInfo.versionCodeCompat()) { "Target APK is not newer than the installed app." }

        val targetSigner = signerSha256(archive)
        check(targetSigner.equals(expectedSignerSha256, ignoreCase = true)) {
            "Reconstructed APK signer does not match trusted update metadata."
        }
        check(targetSigner.equals(installedSigner, ignoreCase = true)) {
            "APK signing certificate differs from the installed app."
        }

        return VerifiedApk(output, archiveVersion, targetHash, targetSigner)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo =
        getPackageInfo(packageName, if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)

    @Suppress("DEPRECATION")
    private fun PackageManager.getPackageArchiveInfoCompat(path: String): PackageInfo? =
        getPackageArchiveInfo(path, if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)

    private fun PackageInfo.versionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= 28) longVersionCode else @Suppress("DEPRECATION") versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signerSha256(info: PackageInfo): String {
        val certificates = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            info.signatures?.map { it.toByteArray() }.orEmpty()
        }
        check(certificates.size == 1) { "Only single-signer APKs are supported by this updater." }
        return MessageDigest.getInstance("SHA-256").digest(certificates.single())
            .joinToString("") { "%02x".format(it) }
    }
}
