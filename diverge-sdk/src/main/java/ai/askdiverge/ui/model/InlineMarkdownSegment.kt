package ai.askdiverge.ui.model

import java.util.Locale

/**
 * One run of the constrained inline Markdown that `/config` text such as the header subtitle
 * carries: `**bold**` and `[label](url)`, read the way the web widget reads it. Links are found
 * first and their labels taken literally; bold is read in the text between links. A link opens
 * only an absolute http(s) URL with a host, written in ASCII as RFC 3986 allows it (non-ASCII
 * text, spaces, `|`, `{`, `}`, `^` or a `%` that does not start an escape are not), and with no
 * quote, angle bracket or backtick, as the iOS SDK accepts it; its scheme is lowercased. Any other
 * link becomes its label as plain text.
 */
internal sealed interface InlineMarkdownSegment {

    val text: String

    data class Plain(override val text: String) : InlineMarkdownSegment

    data class Bold(override val text: String) : InlineMarkdownSegment

    data class Link(override val text: String, val url: String) : InlineMarkdownSegment

    companion object {

        private val LINK = Regex("""\[([^\]]+)]\(([^)]+)\)""")
        private val BOLD = Regex("""\*\*([^*]+)\*\*""")
        private val WEB_SCHEMES = setOf("http", "https")
        private const val AUTHORITY_START = "//"
        private val AUTHORITY_END = charArrayOf('/', '?', '#')
        private const val REFUSED_URL_CHARS = "\"'<>`"

        // RFC 3986 character sets. Every other character, including any non-ASCII one, is refused.
        private const val UNRESERVED_MARKS = "-._~"
        private const val SUB_DELIMS = "!$&'()*+,;="
        private const val USERINFO_EXTRA = ":"
        private const val PATH_EXTRA = ":@/"
        private const val QUERY_EXTRA = ":@/?"

        /** Splits [text] into segments in reading order; markup that does not match stays plain. */
        fun parse(text: String): List<InlineMarkdownSegment> {
            val segments = mutableListOf<InlineMarkdownSegment>()
            var start = 0
            LINK.findAll(text).forEach { match ->
                segments += parseBold(text.substring(start, match.range.first))
                val label = match.groupValues[1]
                val url = webUrlOrNull(match.groupValues[2])
                segments += if (url == null) Plain(label) else Link(text = label, url = url)
                start = match.range.last + 1
            }
            segments += parseBold(text.substring(start))
            return segments
        }

        private fun parseBold(text: String): List<InlineMarkdownSegment> {
            val segments = mutableListOf<InlineMarkdownSegment>()
            var start = 0
            BOLD.findAll(text).forEach { match ->
                if (match.range.first > start) {
                    segments += Plain(text.substring(start, match.range.first))
                }
                segments += Bold(match.groupValues[1])
                start = match.range.last + 1
            }
            if (start < text.length) {
                segments += Plain(text.substring(start))
            }
            return segments
        }

        private fun webUrlOrNull(raw: String): String? {
            val candidate = raw.trim()
            if (candidate.any { it in REFUSED_URL_CHARS }) return null
            val schemeEnd = candidate.indexOf(':')
            if (schemeEnd < 0) return null
            val scheme = candidate.substring(0, schemeEnd).lowercase(Locale.ROOT)
            if (scheme !in WEB_SCHEMES || !candidate.startsWith(AUTHORITY_START, schemeEnd + 1)) return null
            val afterScheme = candidate.substring(schemeEnd + 1 + AUTHORITY_START.length)
            val authorityEnd = afterScheme.indexOfAny(AUTHORITY_END).takeIf { it >= 0 } ?: afterScheme.length
            if (!isValidAuthority(afterScheme.substring(0, authorityEnd))) return null
            val rest = afterScheme.substring(authorityEnd)
            val beforeFragment = rest.substringBefore('#')
            val fragment = rest.substringAfter('#', missingDelimiterValue = "")
            val isValidRest = isValid(beforeFragment.substringBefore('?'), PATH_EXTRA) &&
                isValid(beforeFragment.substringAfter('?', missingDelimiterValue = ""), QUERY_EXTRA) &&
                isValid(fragment, QUERY_EXTRA)
            return (scheme + candidate.substring(schemeEnd)).takeIf { isValidRest }
        }

        // `[userinfo@]host[:port]`, where the host is a registered name or a bracketed IP literal and
        // must not be empty.
        private fun isValidAuthority(authority: String): Boolean {
            val userInfoEnd = authority.indexOf('@')
            if (userInfoEnd >= 0 && !isValid(authority.substring(0, userInfoEnd), USERINFO_EXTRA)) {
                return false
            }
            val hostAndPort = authority.substring(userInfoEnd + 1)
            val host: String
            val port: String
            if (hostAndPort.startsWith('[')) {
                val literalEnd = hostAndPort.indexOf(']')
                if (literalEnd < 0) return false
                val literal = hostAndPort.substring(1, literalEnd)
                if (literal.isEmpty() || !isValid(literal, USERINFO_EXTRA)) return false
                host = hostAndPort.substring(0, literalEnd + 1)
                val afterLiteral = hostAndPort.substring(literalEnd + 1)
                port = when {
                    afterLiteral.isEmpty() -> ""
                    afterLiteral.startsWith(':') -> afterLiteral.substring(1)
                    else -> return false
                }
            } else {
                host = hostAndPort.substringBefore(':')
                port = hostAndPort.substringAfter(':', missingDelimiterValue = "")
                if (!isValid(host, extra = "")) return false
            }
            return host.isNotEmpty() && port.all { it in '0'..'9' }
        }

        // True when every character of [part] is ASCII alphanumeric, an unreserved mark, a
        // sub-delimiter or one of [extra], or starts a `%` escape of two hex digits.
        private fun isValid(
            part: String,
            extra: String
        ): Boolean {
            var index = 0
            while (index < part.length) {
                val char = part[index]
                if (char == '%') {
                    val isEscape = index + 2 < part.length &&
                        part[index + 1].isHexDigit() &&
                        part[index + 2].isHexDigit()
                    if (!isEscape) return false
                    index += 3
                    continue
                }
                val isAllowed = char.isAsciiLetterOrDigit() || char in UNRESERVED_MARKS ||
                    char in SUB_DELIMS || char in extra
                if (!isAllowed) return false
                index++
            }
            return true
        }

        private fun Char.isAsciiLetterOrDigit(): Boolean =
            this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

        private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
