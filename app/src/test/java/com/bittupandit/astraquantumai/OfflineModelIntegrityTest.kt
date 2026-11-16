package com.bittupandit.astraquantumai

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineModelIntegrityTest {
    @Test
    fun acceptsFileWithExpectedSha256() {
        val file = File.createTempFile("astra-model", ".bin")
        try {
            file.writeBytes("trusted-model".toByteArray())
            assertTrue(
                OfflineModelIntegrity.matches(
                    file,
                    "a838b2faead8e6ae8f27cca6629fb1c660bfdedb6763a5c4e5f268f80e557496"
                )
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsFileWhoseContentsWereChanged() {
        val file = File.createTempFile("astra-model", ".bin")
        try {
            file.writeBytes("tampered-model".toByteArray())
            assertFalse(
                OfflineModelIntegrity.matches(
                    file,
                    "a838b2faead8e6ae8f27cca6629fb1c660bfdedb6763a5c4e5f268f80e557496"
                )
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsMissingFilesAndMalformedHashes() {
        val file = File.createTempFile("astra-model", ".bin")
        try {
            file.writeBytes("trusted-model".toByteArray())
            assertFalse(OfflineModelIntegrity.matches(file, "not-a-sha256"))
        } finally {
            file.delete()
        }
        assertFalse(
            OfflineModelIntegrity.matches(
                File(file.parentFile, "astra-model-that-does-not-exist.bin"),
                "a838b2faead8e6ae8f27cca6629fb1c660bfdedb6763a5c4e5f268f80e557496"
            )
        )
    }
}
