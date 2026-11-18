package com.bittupandit.astraquantumai

import java.net.IDN
import java.net.URI

/** Only open ordinary public HTTPS source links in an external browser. */
internal object ExternalLinkPolicy {
    private val localSuffixes = setOf(
        "localhost", "local", "localdomain", "internal", "test", "invalid", "example",
        "lan", "home", "home.arpa"
    )

    fun allows(rawUrl: String): Boolean {
        if (rawUrl.length !in 1..2_048) return false
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        val rawHost = uri.host ?: return false
        if (!isPublicDnsHost(rawHost)) return false
        return uri.isAbsolute &&
            uri.scheme.equals("https", ignoreCase = true) &&
            uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port == 443)
    }

    private fun isPublicDnsHost(rawHost: String): Boolean {
        if (rawHost.contains(':')) return false // Reject IPv6 literals, including loopback and ULA.
        val host = runCatching {
            IDN.toASCII(rawHost.trimEnd('.'), IDN.USE_STD3_ASCII_RULES).lowercase()
        }.getOrNull() ?: return false
        if (host.isBlank() || host.length > 253 || !host.contains('.')) return false
        if (host.matches(Regex("^[0-9.]+$"))) return false // Reject IPv4 literals.
        val labels = host.split('.')
        if (labels.any { it.isBlank() || it.length > 63 }) return false
        if (labels.all { it.matches(Regex("(?i)(?:0x[0-9a-f]+|[0-9]+)")) }) return false
        if (localSuffixes.any { host == it || host.endsWith(".$it") }) return false
        return labels.all { it.matches(Regex("(?i)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) }
    }
}
