package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateApiTokenPolicyTest {
    @Test
    fun acceptsOnly64LowercaseHexCharacters() {
        val valid = "a1".repeat(32)
        assertTrue(PrivateApiTokenPolicy.isValid(valid))
        assertFalse(PrivateApiTokenPolicy.isValid(""))
        assertFalse(PrivateApiTokenPolicy.isValid("g1".repeat(32)))
        assertFalse(PrivateApiTokenPolicy.isValid("A1".repeat(32)))
        assertFalse(PrivateApiTokenPolicy.isValid("a1".repeat(31)))
        assertFalse(PrivateApiTokenPolicy.isValid("a1".repeat(33)))
    }

    @Test
    fun buildsBearerHeaderOnlyForValidToken() {
        val token = "ab".repeat(32)
        assertEquals("Bearer $token", PrivateApiTokenPolicy.authorizationHeader(token))
        assertFalse(runCatching { PrivateApiTokenPolicy.authorizationHeader("wrong") }.isSuccess)
    }
}
