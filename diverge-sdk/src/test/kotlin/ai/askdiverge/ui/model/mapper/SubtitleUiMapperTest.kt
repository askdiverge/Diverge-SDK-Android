package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.config.display.subtitle.Subtitle
import ai.askdiverge.domain.model.config.display.subtitle.SubtitleLink
import ai.askdiverge.ui.model.SubtitleLinkUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class SubtitleUiMapperTest {

    @Test
    fun `when a subtitle has text and a link expect both kept`() {
        val subtitle = Subtitle(
            text = "Powered by AI",
            link = SubtitleLink(
                text = "Learn more",
                url = "https://example.com/ai"
            )
        ).mapToUi()

        assertEquals(
            expected = "Powered by AI",
            actual = subtitle.text
        )
        assertEquals(
            expected = SubtitleLinkUiModel(
                text = "Learn more",
                url = "https://example.com/ai"
            ),
            actual = subtitle.link
        )
    }

    @Test
    fun `when a subtitle has no link expect only its text`() {
        val subtitle = Subtitle(
            text = "Powered by AI",
            link = null
        ).mapToUi()

        assertEquals(
            expected = "Powered by AI",
            actual = subtitle.text
        )
        assertNull(subtitle.link)
    }

    @Test
    fun `when a subtitle is only a link expect no text`() {
        val subtitle = Subtitle(
            text = null,
            link = SubtitleLink(
                text = "Learn more",
                url = "https://example.com/ai"
            )
        ).mapToUi()

        assertNull(subtitle.text)
        assertEquals(
            expected = "Learn more",
            actual = subtitle.link?.text
        )
    }
}
