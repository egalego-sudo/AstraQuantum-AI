package com.bittupandit.astraquantumai

import java.net.URI

/** Only open ordinary HTTPS source links in an external browser. */
internal object ExternalLinkPolicy {
    fun allows(rawUrl: String): Boolean {
        if (rawUrl.length !in 1..2_048) return false
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        return uri.isAbsolute &&
            uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port == 443)
    }
}
