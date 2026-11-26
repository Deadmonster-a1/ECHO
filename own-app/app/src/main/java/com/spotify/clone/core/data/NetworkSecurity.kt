package com.spotify.clone.core.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest

/**
 * Shared network and integrity validation helpers used across API clients,
 * media stream resolvers, offline downloaders, and the OTA updater.
 */
object NetworkSecurity {

    private const val DEFAULT_MAX_RESPONSE_BYTES = 4 * 1024 * 1024 // 4 MB cap

    private val UPGRADABLE_CDN_HOSTS = listOf(
        "saavncdn.com",
        "jiosaavn.com",
        "googleusercontent.com",
        "mzstatic.com",
        "dzcdn.net",
        "archive.org",
        "audius.co"
    )

    private val TRUSTED_UPDATE_BASE_HOSTS = listOf(
        "github.com",
        "githubusercontent.com",
        "githubassets.com",
        "cdn.jsdelivr.net"
    )

    private val TRUSTED_UPDATE_CDN_HOSTS = listOf(
        "s3.amazonaws.com",
        "blob.core.windows.net",
        "fastly.net",
        "cloudfront.net"
    )

    private fun hostMatches(host: String, suffix: String): Boolean {
        return host.equals(suffix, ignoreCase = true) ||
            host.endsWith(".$suffix", ignoreCase = true)
    }

    /**
     * Enforces HTTPS for network URLs. Upgrades known media CDN URLs from HTTP -> HTTPS
     * and rejects untrusted schemes (e.g., file://, javascript:, content://) or plain HTTP hosts.
     */
    fun enforceHttpsOrUpgradeKnownCdn(urlStr: String?): String? {
        val trimmed = urlStr?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }

        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return null

        return when (scheme) {
            "https" -> trimmed
            "http" -> {
                if (UPGRADABLE_CDN_HOSTS.any { hostMatches(host, it) }) {
                    "https://" + trimmed.substringAfter("://")
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /**
     * Validates that an OTA update metadata or APK download URL uses HTTPS and points
     * to a trusted release host (optionally allowing official GitHub release CDN redirects).
     */
    fun isTrustedUpdateUrl(urlStr: String?, allowCdnRedirects: Boolean = false): Boolean {
        val trimmed = urlStr?.trim().orEmpty()
        if (trimmed.isBlank()) return false

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return false
        }

        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return false

        if (TRUSTED_UPDATE_BASE_HOSTS.any { hostMatches(host, it) }) return true
        if (allowCdnRedirects && TRUSTED_UPDATE_CDN_HOSTS.any { hostMatches(host, it) }) return true
        return false
    }

    /**
     * Reads UTF-8 text from an HTTP response stream up to [maxBytes] to guard against
     * oversized payloads or memory exhaustion from captive portals / malformed responses.
     */
    @Throws(IOException::class)
    fun readBoundedText(input: InputStream, maxBytes: Int = DEFAULT_MAX_RESPONSE_BYTES): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) {
                throw IOException("HTTP response exceeded $maxBytes byte safety limit")
            }
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    /**
     * Computes the lowercase SHA-256 hex digest of a local file.
     */
    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32768)
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
