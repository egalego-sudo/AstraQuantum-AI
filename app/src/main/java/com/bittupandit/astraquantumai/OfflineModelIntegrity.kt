package com.bittupandit.astraquantumai

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** Small, streaming SHA-256 verifier for downloaded on-device model files. */
internal object OfflineModelIntegrity {
    fun matches(file: File, expectedSha256: String): Boolean {
        if (!file.isFile || !expectedSha256.matches(Regex("[0-9a-fA-F]{64}"))) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(256 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expectedSha256, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
