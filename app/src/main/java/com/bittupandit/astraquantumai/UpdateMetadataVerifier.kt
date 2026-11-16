package com.bittupandit.astraquantumai

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

internal object UpdateMetadataVerifier {
    data class VerifiedMetadata(
        val json: JSONObject,
        val rawBytes: ByteArray,
        val signerCertificateSha256: String
    )

    fun verify(context: Context, metadataBytes: ByteArray, signatureText: String): VerifiedMetadata {
        require(metadataBytes.isNotEmpty() && metadataBytes.size <= 256 * 1024) {
            "Update metadata size is invalid."
        }
        val certificate = context.assets.open("update_signer.pem").use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
        val algorithm = when (certificate.publicKey.algorithm.uppercase()) {
            "RSA" -> "SHA256withRSA"
            "EC", "ECDSA" -> "SHA256withECDSA"
            else -> error("Unsupported update-signing key algorithm.")
        }
        val verifier = Signature.getInstance(algorithm)
        verifier.initVerify(certificate.publicKey)
        verifier.update(metadataBytes)
        val signatureBytes = Base64.decode(signatureText.trim(), Base64.DEFAULT)
        check(signatureBytes.isNotEmpty() && verifier.verify(signatureBytes)) {
            "Update metadata signature is invalid. Update blocked."
        }

        val certificateHash = MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString("") { "%02x".format(it) }

        val json = JSONObject(String(metadataBytes, StandardCharsets.UTF_8))
        require(json.getString("packageName") == context.packageName) {
            "Update belongs to a different application."
        }
        require(json.getLong("targetVersionCode") > installedVersionCode(context)) {
            "No newer update is available."
        }
        require(json.getString("targetApkSha256").matches(Regex("[0-9a-fA-F]{64}"))) {
            "Invalid target APK hash."
        }
        require(json.getString("signerCertificateSha256").equals(certificateHash, true)) {
            "Update signing certificate does not match the embedded trusted certificate."
        }
        require(json.getString("patchUrl").startsWith("https://github.com/egalego-sudo/AstraQuantum-AI/releases/download/") &&
            json.getString("apkUrl").startsWith("https://github.com/egalego-sudo/AstraQuantum-AI/releases/download/")) {
            "Update download URL is not an approved ASTRA release URL."
        }
        return VerifiedMetadata(json, metadataBytes, certificateHash)
    }

    private fun installedVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }
}
