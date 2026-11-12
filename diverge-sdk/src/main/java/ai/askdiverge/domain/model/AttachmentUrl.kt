package ai.askdiverge.domain.model

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Shared rules for attachment (`image` / `file`) URL strings and signed-URL expiry.
 *
 * Attachment URLs are normally absolute, but when the server cannot resolve its own public host it
 * falls back to a bare path. Relative paths resolve against the API base; `data:` URLs pass as-is.
 */
internal object AttachmentUrl {

    /**
     * Resolves [raw] to an absolute URL string. Relative paths need [apiBaseUrl]; `data:` URLs
     * pass without a base. Null when the result still has no host or [raw] is blank.
     */
    fun resolve(
        raw: String,
        apiBaseUrl: String?
    ): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (isDataUrl(trimmed)) return trimmed
        if (hasHost(trimmed)) return trimmed
        val base = apiBaseUrl?.trim().orEmpty()
        if (base.isEmpty()) return null
        val baseNormalized = if (base.endsWith("/")) base else "$base/"
        val path = trimmed.removePrefix("/")
        return baseNormalized + path
    }

    /** True when [urlExpiresAt] is present, parseable as ISO-8601, and already in the past. */
    fun isExpired(
        urlExpiresAt: String?,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val expiry = expiryEpochMs(urlExpiresAt) ?: return false
        return expiry <= nowMs
    }

    /** Parses [urlExpiresAt]; null when absent or unparseable (treated as never expires). */
    fun expiryEpochMs(urlExpiresAt: String?): Long? {
        val raw = urlExpiresAt?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun isDataUrl(raw: String): Boolean =
        raw.length >= 5 && raw.regionMatches(0, "data:", 0, 5, ignoreCase = true)

    private fun hasHost(raw: String): Boolean {
        val schemeEnd = raw.indexOf("://")
        if (schemeEnd <= 0) return false
        val afterScheme = schemeEnd + 3
        if (afterScheme >= raw.length) return false
        val pathStart = raw.indexOf('/', afterScheme)
        val host = if (pathStart < 0) raw.substring(afterScheme) else raw.substring(afterScheme, pathStart)
        return host.isNotEmpty()
    }
}
