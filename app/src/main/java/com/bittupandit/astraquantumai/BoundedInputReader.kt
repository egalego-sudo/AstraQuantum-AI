package com.bittupandit.astraquantumai

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Reads an input stream without ever buffering more than [maxBytes].
 * The caller owns and closes the stream.
 */
internal object BoundedInputReader {
    fun read(
        input: InputStream,
        maxBytes: Int,
        tooLargeMessage: String = "फ़ाइल निर्धारित आकार सीमा से बड़ी है।"
    ): ByteArray {
        require(maxBytes > 0) { "maxBytes must be positive." }

        val output = ByteArrayOutputStream(minOf(maxBytes, BUFFER_SIZE))
        val buffer = ByteArray(minOf(BUFFER_SIZE, maxBytes))
        var total = 0

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val single = input.read()
                if (single < 0) break
                if (total == maxBytes) throw IOException(tooLargeMessage)
                output.write(single)
                total++
                continue
            }
            if (count > maxBytes - total) throw IOException(tooLargeMessage)
            output.write(buffer, 0, count)
            total += count
        }
        return output.toByteArray()
    }

    private const val BUFFER_SIZE = 8 * 1024
}
