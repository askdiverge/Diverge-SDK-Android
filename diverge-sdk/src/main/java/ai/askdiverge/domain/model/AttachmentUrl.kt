package ai.askdiverge.domain.model

import java.net.URI
import java.net.URISyntaxException
import java.util.Calendar
import java.util.TimeZone

/**
 * Shared rules for attachment (`image` / `file`) URL strings and signed-URL expiry.
 *
 * Attachment URLs are normally absolute, but when the server cannot resolve its own public host it
 * falls back to a bare path. Relative paths resolve against the API base; `data:` URLs pass as-is.
 */
internal object AttachmentUrl {

    /**
     * Resolves [raw] to an absolute URL string. Relative paths need [apiBaseUrl]: a path starting
     * with `/` resolves against its origin, any other against its path. `data:` URLs pass without
     * a base. Null when the result still has no host or [raw] is blank.
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
        return try {
            URI(baseNormalized).resolve(trimmed).toString()
        } catch (_: URISyntaxException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** True when [url] can be handed to a browser: an absolute `http` or `https` URL. */
    fun isWebUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

    /** True when [urlExpiresAt] is present, parseable as ISO-8601, and already in the past. */
    fun isExpired(
        urlExpiresAt: String?,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val expiry = expiryEpochMs(urlExpiresAt) ?: return false
        return expiry <= nowMs
    }

    /**
     * Parses [urlExpiresAt] as an ISO-8601 date-time with a `Z` or `±hh:mm` offset; null when
     * absent or unparseable (treated as never expires).
     */
    fun expiryEpochMs(urlExpiresAt: String?): Long? {
        val raw = urlExpiresAt?.trim().orEmpty()
        val match = ISO_DATE_TIME.matchEntire(raw) ?: return null
        val (year, month, day, hour, minute, second, fraction, offset) = match.destructured
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(year.toInt(), month.toInt() - 1, day.toInt(), hour.toInt(), minute.toInt(), second.toInt())
        }
        val utcMs = try {
            calendar.timeInMillis
        } catch (_: IllegalArgumentException) {
            return null
        }
        val fractionMs = fraction.removePrefix(".").take(3).padEnd(3, '0').toLongOrNull() ?: 0L
        return utcMs + fractionMs - offsetMs(offset)
    }

    private fun offsetMs(offset: String): Long {
        if (offset.equals("Z", ignoreCase = true)) return 0L
        val sign = if (offset.startsWith("-")) -1L else 1L
        val hours = offset.substring(1, 3).toLong()
        val minutes = offset.substring(4, 6).toLong()
        return sign * (hours * 60 + minutes) * 60_000L
    }

    private val ISO_DATE_TIME = Regex(
        """(\d{4})-(\d{2})-(\d{2})[Tt](\d{2}):(\d{2}):(\d{2})(\.\d+)?([Zz]|[+-]\d{2}:\d{2})"""
    )

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
