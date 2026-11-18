package com.bittupandit.astraquantumai

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateArtifactIntegrityTest {
    @Test
    fun acceptsOnlyExactSha256ForDownloadedPatch() {
        val file = File.createTempFile("quantum-update", ".patch")
        try {
            file.writeBytes("trusted patch bytes".toByteArray())
            val expected = MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes()).joinToString("") { "%02x".format(it) }

            assertTrue(UpdateArtifactIntegrity.matchesSha256(file, expected))
            assertFalse(UpdateArtifactIntegrity.matchesSha256(file, "0".repeat(64)))
            assertFalse(UpdateArtifactIntegrity.matchesSha256(file, "not-a-hash"))
        } finally {
            file.delete()
        }
    }
}
