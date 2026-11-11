package ai.askdiverge.domain.model

import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Shared decode rules for attachment (`image` / `file`) and agent avatar URLs, and for
 * signed-URL expiry.
 *
 * Attachment URLs are minted by the API and normally absolute, but when the server cannot
 * resolve its own public host it falls back to a bare path
 * (`/api/v1/chat/livechat/attachments/…`). A host-less URL is resolved against the API base; it
 * is rejected when no base is available. `data:` URLs pass without a base.
 */
internal object AttachmentUrl {

    /**
     * Resolves [raw] against [baseUrl] when it carries no host. Returns `null` when the result
     * still has no host or the string is not a URI at all.
     */
    fun resolve(
        raw: String,
        baseUrl: String? = null
    ): String? {
        if (isDataUrl(raw)) return raw
        return try {
            val uri = URI(raw)
            if (uri.host != null) return uri.toString()
            val base = baseUrl?.let { URI(it) } ?: return null
            val resolved = base.resolve(uri)
            if (resolved.host == null) null else resolved.toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Case-insensitive `data:` scheme check on the first five characters only — a data URL can
     * be megabytes long.
     */
    fun isDataUrl(raw: String): Boolean {
        if (raw.length < 5) return false
        return raw.regionMatches(0, "data:", 0, 5, ignoreCase = true)
    }

    /**
     * Parses the wire's `url_expires_at` (ISO 8601, with or without fractional seconds).
     * `null` when absent or unparseable — callers treat that as "does not expire".
     */
    fun expiry(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso))
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(iso)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /** True when `url_expires_at` is present, parseable, and already in the past. */
    fun isExpired(
        iso: String?,
        at: Instant = Instant.now()
    ): Boolean {
        val expiry = expiry(iso) ?: return false
        return !expiry.isAfter(at)
    }
}
