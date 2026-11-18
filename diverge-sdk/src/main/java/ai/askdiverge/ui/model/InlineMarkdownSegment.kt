package ai.askdiverge.ui.model

import java.net.URI
import java.net.URISyntaxException

/**
 * One run of the constrained inline Markdown that `/config` text such as the header subtitle
 * carries: `**bold**` and `[label](url)`, read the way the web widget reads it. Links are found
 * first and their labels taken literally; bold is read in the text between links. A link whose
 * URL is not an absolute http(s) URL becomes its label as plain text.
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
            if (candidate.isEmpty() || candidate.any { it.isWhitespace() || it.isISOControl() }) {
                return null
            }
            val uri = try {
                URI(candidate)
            } catch (e: URISyntaxException) {
                return null
            }
            val isWeb = uri.scheme?.lowercase() in WEB_SCHEMES && !uri.host.isNullOrEmpty()
            return candidate.takeIf { isWeb }
        }
    }
}
