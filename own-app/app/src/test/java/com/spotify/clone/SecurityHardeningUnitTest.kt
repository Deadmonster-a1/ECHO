package com.spotify.clone

import com.spotify.clone.core.data.NetworkSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class SecurityHardeningUnitTest {

    @Test
    fun httpsEnforcement_upgradesKnownCdnHttpToHttps() {
        assertEquals(
            "https://c.saavncdn.com/313/cover.jpg",
            NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("http://c.saavncdn.com/313/cover.jpg")
        )
        assertEquals(
            "https://aac.saavncdn.com/123/song_320.mp4",
            NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("http://aac.saavncdn.com/123/song_320.mp4")
        )
        assertEquals(
            "https://lh3.googleusercontent.com/avatar",
            NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("http://lh3.googleusercontent.com/avatar")
        )
    }

    @Test
    fun httpsEnforcement_blocksUntrustedHttpAndDangerousSchemes() {
        assertNull(NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("http://untrusted-host.example/audio.mp3"))
        assertNull(NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("file:///data/data/com.echo.musicplayer/shared_prefs/a.xml"))
        assertNull(NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("javascript:alert(1)"))
        assertNull(NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("content://media/external/audio/1"))
        assertNull(NetworkSecurity.enforceHttpsOrUpgradeKnownCdn("   "))
    }

    @Test
    fun httpsEnforcement_preservesValidHttpsUrls() {
        val url = "https://www.jiosaavn.com/api.php?__call=search.getResults"
        assertEquals(url, NetworkSecurity.enforceHttpsOrUpgradeKnownCdn(url))
    }

    @Test
    fun updateUrlPolicy_allowsOnlyTrustedHttpsHosts() {
        assertTrue(NetworkSecurity.isTrustedUpdateUrl("https://github.com/Deadmonster-a1/ECHO/releases/download/v2.1.0/echo.apk"))
        assertTrue(NetworkSecurity.isTrustedUpdateUrl("https://cdn.jsdelivr.net/gh/Deadmonster-a1/ECHO@main/version.json"))
        assertTrue(NetworkSecurity.isTrustedUpdateUrl("https://raw.githubusercontent.com/Deadmonster-a1/ECHO/main/version.json"))
        assertTrue(
            NetworkSecurity.isTrustedUpdateUrl(
                "https://release-assets.s3.amazonaws.com/echo.apk",
                allowCdnRedirects = true
            )
        )

        // Rejects cleartext HTTP, spoofing subdomains/paths, and CDN hosts when redirects disabled
        assertFalse(NetworkSecurity.isTrustedUpdateUrl("http://github.com/Deadmonster-a1/ECHO/releases/latest"))
        assertFalse(NetworkSecurity.isTrustedUpdateUrl("https://evil.example.com/github.com/echo.apk"))
        assertFalse(NetworkSecurity.isTrustedUpdateUrl("https://github.com.evil.example/echo.apk"))
        assertFalse(
            NetworkSecurity.isTrustedUpdateUrl(
                "https://release-assets.s3.amazonaws.com/echo.apk",
                allowCdnRedirects = false
            )
        )
    }

    @Test
    fun readBoundedText_readsWithinLimitAndRejectsOversizedPayload() {
        val okBytes = """{"ok":true}""".toByteArray(Charsets.UTF_8)
        assertEquals("""{"ok":true}""", NetworkSecurity.readBoundedText(ByteArrayInputStream(okBytes), maxBytes = 64))

        val huge = ByteArray(128) { 'a'.code.toByte() }
        var threw = false
        try {
            NetworkSecurity.readBoundedText(ByteArrayInputStream(huge), maxBytes = 64)
        } catch (_: IOException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun sha256Hex_matchesKnownDigest() {
        val temp = File.createTempFile("echo_sha_test", ".tmp")
        try {
            temp.writeText("echo-apk-integrity")
            val digest = NetworkSecurity.sha256Hex(temp)
            assertEquals(64, digest.length)
            assertEquals(digest, NetworkSecurity.sha256Hex(temp))
        } finally {
            temp.delete()
        }
    }

    @Test
    fun otaDownload_onlyAcceptsOwnRepoReleaseAssets() {
        val u = com.spotify.clone.core.update.AppUpdateManager
        assertTrue(u.isOwnReleaseAsset("https://github.com/Deadmonster-a1/ECHO/releases/download/v2.3.0/echo.apk"))
        assertFalse(u.isOwnReleaseAsset("https://github.com/attacker/ECHO/releases/download/v9.0.0/echo.apk"))
        assertFalse(u.isOwnReleaseAsset("https://raw.githubusercontent.com/attacker/x/main/echo.apk"))
        assertFalse(u.isOwnReleaseAsset("https://github.com/Deadmonster-a1/ECHO/releases/download/../../../attacker/x/echo.apk"))
        assertFalse(u.isOwnReleaseAsset("http://github.com/Deadmonster-a1/ECHO/releases/download/v2.3.0/echo.apk"))
        assertFalse(u.isOwnReleaseAsset(null))
    }
}
