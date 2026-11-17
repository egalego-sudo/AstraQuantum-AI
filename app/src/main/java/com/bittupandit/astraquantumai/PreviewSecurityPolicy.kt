package com.bittupandit.astraquantumai

import java.net.URI

/** Allow only the synthetic HTTPS origin used by the generated-code preview. */
internal object PreviewSecurityPolicy {
    private const val PREVIEW_HOST = "astra.local"

    fun allowsNavigation(rawUrl: String): Boolean {
        if (rawUrl.length > 2_048) return false
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        return uri.isAbsolute &&
            uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(PREVIEW_HOST, ignoreCase = true) &&
            uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443)
    }
}
