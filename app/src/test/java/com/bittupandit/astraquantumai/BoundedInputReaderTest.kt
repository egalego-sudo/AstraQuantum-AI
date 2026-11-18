package com.bittupandit.astraquantumai

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedInputReaderTest {
    @Test
    fun readsAnInputThatFitsExactlyWithinTheLimit() {
        val expected = ByteArray(32) { it.toByte() }
        assertArrayEquals(expected, BoundedInputReader.read(ByteArrayInputStream(expected), 32))
    }

    @Test
    fun rejectsAnInputOneByteLargerThanTheLimit() {
        val input = ByteArrayInputStream(ByteArray(33))
        val error = assertThrows(IOException::class.java) {
            BoundedInputReader.read(input, 32, "too large")
        }
        assertEquals("too large", error.message)
    }

    @Test
    fun readsEmptyInput() {
        assertArrayEquals(ByteArray(0), BoundedInputReader.read(ByteArrayInputStream(ByteArray(0)), 1))
    }

    @Test
    fun rejectsAZeroOrNegativeLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            BoundedInputReader.read(ByteArrayInputStream(byteArrayOf(1)), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoundedInputReader.read(ByteArrayInputStream(byteArrayOf(1)), -1)
        }
    }
    @Test
    fun readsHindiAndEmojiFromUtf8ResponseText() {
        val expected = "नमस्ते QUANTUM AI 🚀"
        val input = ByteArrayInputStream(expected.toByteArray(Charsets.UTF_8))
        assertEquals(expected, BoundedInputReader.readText(input, 128, "too large"))
    }

    @Test
    fun rejectsOversizedHttpResponseText() {
        val input = ByteArrayInputStream(ByteArray(33))
        val error = assertThrows(IOException::class.java) {
            BoundedInputReader.readText(input, 32, "response too large")
        }
        assertEquals("response too large", error.message)
    }
}

