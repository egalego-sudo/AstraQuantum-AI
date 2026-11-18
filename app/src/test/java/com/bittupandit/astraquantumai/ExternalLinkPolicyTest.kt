package com.bittupandit.astraquantumai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalLinkPolicyTest {
    @Test
    fun allowsOnlyOrdinaryHttpsSourceLinks() {
        assertTrue(ExternalLinkPolicy.allows("https://example.org/paper?id=123"))
        assertTrue(ExternalLinkPolicy.allows("https://example.org:443/paper"))
        assertFalse(ExternalLinkPolicy.allows("http://example.org/paper"))
        assertFalse(ExternalLinkPolicy.allows("javascript:alert(1)"))
        assertFalse(ExternalLinkPolicy.allows("file:///etc/passwd"))
        assertFalse(ExternalLinkPolicy.allows("https://user:pass@example.org/paper"))
        assertFalse(ExternalLinkPolicy.allows("https://example.org:444/paper"))
        assertFalse(ExternalLinkPolicy.allows("https://localhost/"))
        assertFalse(ExternalLinkPolicy.allows("https://printer.local/"))
        assertFalse(ExternalLinkPolicy.allows("https://metadata.google.internal/"))
        assertFalse(ExternalLinkPolicy.allows("https://127.0.0.1/"))
        assertFalse(ExternalLinkPolicy.allows("https://192.168.1.10/"))
        assertFalse(ExternalLinkPolicy.allows("https://[::1]/"))
        assertFalse(ExternalLinkPolicy.allows("https://2130706433/"))
        assertFalse(ExternalLinkPolicy.allows("https:///paper"))
        assertFalse(ExternalLinkPolicy.allows("x".repeat(2_049)))
    }
}
