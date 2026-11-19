package ai.askdiverge.ui.model

import ai.askdiverge.ui.model.InlineMarkdownSegment.Bold
import ai.askdiverge.ui.model.InlineMarkdownSegment.Link
import ai.askdiverge.ui.model.InlineMarkdownSegment.Plain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InlineMarkdownSegmentTest {

    @Nested
    @DisplayName("When the text has links")
    inner class LinksTest {

        @Test
        fun `when a link sits inside text expect the text around it kept`() {
            assertEquals(
                expected = listOf(
                    Plain("Read our "),
                    Link(text = "privacy policy", url = "https://example.com/privacy"),
                    Plain(" first.")
                ),
                actual = InlineMarkdownSegment.parse(
                    "Read our [privacy policy](https://example.com/privacy) first."
                )
            )
        }

        @Test
        fun `when a subtitle carries two links expect both, with the text between them`() {
            assertEquals(
                expected = listOf(
                    Plain("Read more in our "),
                    Link(text = "privatlivspolitik", url = "https://www.boozt.com/dk/da/privatliv"),
                    Plain(". "),
                    Link(text = "Discounts need 2 items.", url = "https://www.boozt.com/dk/da/vilkaar")
                ),
                actual = InlineMarkdownSegment.parse(
                    "Read more in our [privatlivspolitik](https://www.boozt.com/dk/da/privatliv). " +
                        "[Discounts need 2 items.](https://www.boozt.com/dk/da/vilkaar)"
                )
            )
        }

        @Test
        fun `when the url has spaces around it expect them trimmed`() {
            assertEquals(
                expected = listOf(Link(text = "Terms", url = "https://example.com/terms")),
                actual = InlineMarkdownSegment.parse("[Terms]( https://example.com/terms )")
            )
        }

        @Test
        fun `when the scheme is written in capitals expect the link kept as written`() {
            assertEquals(
                expected = listOf(Link(text = "Terms", url = "HTTPS://Example.com/Terms")),
                actual = InlineMarkdownSegment.parse("[Terms](HTTPS://Example.com/Terms)")
            )
        }

        @Test
        fun `when the host is an internationalised domain expect the link kept as written`() {
            assertEquals(
                expected = listOf(Link(text = "Privatliv", url = "https://www.skønhed.dk/privatliv")),
                actual = InlineMarkdownSegment.parse("[Privatliv](https://www.skønhed.dk/privatliv)")
            )
        }

        @Test
        fun `when the host holds an underscore expect the link kept as written`() {
            assertEquals(
                expected = listOf(Link(text = "Shop", url = "https://my_shop.example.com/terms")),
                actual = InlineMarkdownSegment.parse("[Shop](https://my_shop.example.com/terms)")
            )
        }

        @Test
        fun `when the query holds a pipe expect the link kept as written`() {
            assertEquals(
                expected = listOf(Link(text = "Søg", url = "https://example.com/search?q=a|b")),
                actual = InlineMarkdownSegment.parse("[Søg](https://example.com/search?q=a|b)")
            )
        }

        @Test
        fun `when the path holds braces expect the link kept as written`() {
            assertEquals(
                expected = listOf(Link(text = "Vare", url = "https://example.com/p/{id}")),
                actual = InlineMarkdownSegment.parse("[Vare](https://example.com/p/{id})")
            )
        }

        @Test
        fun `when the path holds a bare percent sign expect the link kept as written`() {
            assertEquals(
                expected = listOf(Link(text = "Rabat", url = "https://example.com/100%")),
                actual = InlineMarkdownSegment.parse("[Rabat](https://example.com/100%)")
            )
        }

        @Test
        fun `when the url has user info and a port expect the link kept as written`() {
            assertEquals(
                expected = listOf(Link(text = "Terms", url = "https://user@example.com:8443/terms")),
                actual = InlineMarkdownSegment.parse("[Terms](https://user@example.com:8443/terms)")
            )
        }

        @Test
        fun `when a link label holds bold markers expect the label taken literally`() {
            assertEquals(
                expected = listOf(Link(text = "**Terms**", url = "https://example.com/terms")),
                actual = InlineMarkdownSegment.parse("[**Terms**](https://example.com/terms)")
            )
        }
    }

    @Nested
    @DisplayName("When the text has bold")
    inner class BoldTest {

        @Test
        fun `when a word is wrapped in double asterisks expect it bold`() {
            assertEquals(
                expected = listOf(Plain("You chat with "), Bold("an AI"), Plain(".")),
                actual = InlineMarkdownSegment.parse("You chat with **an AI**.")
            )
        }

        @Test
        fun `when bold and a link are mixed expect each kept in order`() {
            assertEquals(
                expected = listOf(
                    Bold("Note:"),
                    Plain(" see "),
                    Link(text = "terms", url = "https://example.com/terms")
                ),
                actual = InlineMarkdownSegment.parse("**Note:** see [terms](https://example.com/terms)")
            )
        }
    }

    @Nested
    @DisplayName("When a link's url is not a web address")
    inner class UnsafeLinkTest {

        @Test
        fun `when the scheme is not http or https expect only the label`() {
            listOf(
                "javascript:void",
                "javascript://example.com/%0Avoid",
                "mailto:help@example.com",
                "tel:+4512345678",
                "intent://scan",
                "file:///etc/hosts",
                "content://media/external/1",
                "ftp://example.com/terms"
            )
                .forEach { url ->
                    assertEquals(
                        expected = listOf(Plain("Open")),
                        actual = InlineMarkdownSegment.parse("[Open]($url)"),
                        message = url
                    )
                }
        }

        @Test
        fun `when the url is relative expect only the label`() {
            listOf("/terms", "./terms", "terms", "?page=2", "#terms", "//example.com/terms", "www.example.com")
                .forEach { url ->
                    assertEquals(
                        expected = listOf(Plain("Terms")),
                        actual = InlineMarkdownSegment.parse("[Terms]($url)"),
                        message = url
                    )
                }
        }

        @Test
        fun `when the url contains whitespace expect only the label`() {
            assertEquals(
                expected = listOf(Plain("Terms")),
                actual = InlineMarkdownSegment.parse("[Terms](https://example.com/a b)")
            )
        }

        @Test
        fun `when the url contains a tab, a no-break space or a control character expect only the label`() {
            listOf("https://example.com/a\tb", "https://example.com/a\u00A0b", "https://example.com/a\u0000b")
                .forEach { url ->
                    assertEquals(
                        expected = listOf(Plain("Terms")),
                        actual = InlineMarkdownSegment.parse("[Terms]($url)"),
                        message = url
                    )
                }
        }

        @Test
        fun `when the url contains a quote, an angle bracket or a backtick expect only the label`() {
            listOf(
                "https://example.com/it's",
                "https://example.com/\"a\"",
                "https://example.com/<a>",
                "https://example.com/`a`"
            )
                .forEach { url ->
                    assertEquals(
                        expected = listOf(Plain("Terms")),
                        actual = InlineMarkdownSegment.parse("[Terms]($url)"),
                        message = url
                    )
                }
        }

        @Test
        fun `when the url has no host expect only the label`() {
            listOf(
                "https:///terms",
                "https://",
                "https://?page=2",
                "https://:8443/terms",
                "https://user@/terms",
                "https:example.com/terms"
            )
                .forEach { url ->
                    assertEquals(
                        expected = listOf(Plain("Terms")),
                        actual = InlineMarkdownSegment.parse("[Terms]($url)"),
                        message = url
                    )
                }
        }
    }

    @Nested
    @DisplayName("When the markup is incomplete")
    inner class MalformedTest {

        @Test
        fun `when the text has no markup expect it unchanged`() {
            assertEquals(
                expected = listOf(Plain("You are chatting with an AI.")),
                actual = InlineMarkdownSegment.parse("You are chatting with an AI.")
            )
        }

        @Test
        fun `when a link is missing its url expect the brackets kept`() {
            assertEquals(
                expected = listOf(Plain("See [terms] and [policy]()")),
                actual = InlineMarkdownSegment.parse("See [terms] and [policy]()")
            )
        }

        @Test
        fun `when bold is not closed expect the asterisks kept`() {
            assertEquals(
                expected = listOf(Plain("Save **20% today")),
                actual = InlineMarkdownSegment.parse("Save **20% today")
            )
        }

        @Test
        fun `when the text is empty expect no segments`() {
            assertEquals(
                expected = emptyList(),
                actual = InlineMarkdownSegment.parse("")
            )
        }
    }
}
