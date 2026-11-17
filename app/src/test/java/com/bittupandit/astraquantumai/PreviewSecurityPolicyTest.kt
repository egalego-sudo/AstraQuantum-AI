package com.bittupandit.astraquantumai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSecurityPolicyTest {
    @Test fun allowsOnlyTheSyntheticHttpsPreviewOrigin() {
        assertTrue(PreviewSecurityPolicy.allowsNavigation("https://astra.local/"))
        assertTrue(PreviewSecurityPolicy.allowsNavigation("https://ASTRA.LOCAL/app/page?tab=home"))
        assertTrue(PreviewSecurityPolicy.allowsNavigation("https://astra.local:443/#section"))
    }

    @Test fun blocksExternalSchemesAndHosts() {
        assertFalse(PreviewSecurityPolicy.allowsNavigation("http://astra.local/"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://example.com/"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://astra.local.evil.test/"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("javascript:alert(1)"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("file:///etc/passwd"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("data:text/html,hello"))
    }

    @Test fun blocksOriginConfusionAndNonstandardPorts() {
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://astra.local@evil.test/"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://evil.test@astra.local/"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://astra.local:8443/"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://astra.local:80/"))
    }

    @Test fun blocksMalformedAndOversizedUrls() {
        assertFalse(PreviewSecurityPolicy.allowsNavigation("not a URL"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://"))
        assertFalse(PreviewSecurityPolicy.allowsNavigation("https://astra.local/" + "x".repeat(2_100)))
    }
}
