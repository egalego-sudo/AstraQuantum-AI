package com.bittupandit.astraquantumai

import java.io.File
import java.security.MessageDigest

/** Verifies downloaded update artifacts before any native parser or installer sees them. */
internal object UpdateArtifactIntegrity {
    fun matchesSha256(file: File, expectedSha256: String): Boolean {
        if (!expectedSha256.matches(Regex("[0-9a-fA-F]{64}")) || !file.isFile) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedSha256, ignoreCase = true)
    }
}
