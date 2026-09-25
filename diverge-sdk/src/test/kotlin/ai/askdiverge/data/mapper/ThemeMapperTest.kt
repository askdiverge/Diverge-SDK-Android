package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.config.theme.BrandRemote
import ai.askdiverge.data.model.config.theme.ThemeProductCardRemote
import ai.askdiverge.data.model.config.theme.ThemeRemote
import ai.askdiverge.data.model.config.theme.ThemeSurfaceRemote
import ai.askdiverge.data.model.config.theme.font.ThemeAndroidFontRemote
import ai.askdiverge.data.model.config.theme.font.ThemeFontRemote
import ai.askdiverge.data.model.config.theme.header.HeaderAlignmentRemote
import ai.askdiverge.data.model.config.theme.header.ThemeHeaderButtonRemote
import ai.askdiverge.data.model.config.theme.header.ThemeHeaderRemote
import ai.askdiverge.data.model.config.theme.header.ThemeLogoRemote
import ai.askdiverge.data.model.config.theme.input.ThemeInputRemote
import ai.askdiverge.data.model.config.theme.input.ThemeSendButtonRemote
import ai.askdiverge.data.model.config.theme.messages.AssistantMessageStyleRemote
import ai.askdiverge.data.model.config.theme.messages.ThemeMessagesRemote
import ai.askdiverge.data.model.config.theme.messages.UserMessageStyleRemote
import ai.askdiverge.domain.model.config.theme.header.HeaderAlignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ThemeMapperTest {

    @Test
    fun `when a theme is mapped expect its brand colour`() {
        assertEquals(
            expected = "#4F46E5",
            actual = sampleThemeRemote().mapToDomain().brand.primaryColor
        )
    }

    @Test
    fun `when a theme is mapped expect its surface colours`() {
        val surface = sampleThemeRemote(
            surface = ThemeSurfaceRemote(
                background_gradient_color = "#111111",
                background_color = "#222222",
                muted_text_color = "#333333"
            )
        ).mapToDomain().surface

        assertEquals(
            expected = "#111111",
            actual = surface.backgroundGradientColor
        )
        assertEquals(
            expected = "#222222",
            actual = surface.backgroundColor
        )
        assertEquals(
            expected = "#333333",
            actual = surface.mutedTextColor
        )
    }

    @Test
    fun `when the backend sends a header alignment expect the matching one`() {
        val alignments = listOf(
            HeaderAlignmentRemote.LEFT to HeaderAlignment.LEFT,
            HeaderAlignmentRemote.CENTER to HeaderAlignment.CENTER,
            HeaderAlignmentRemote.RIGHT to HeaderAlignment.RIGHT,
            HeaderAlignmentRemote.UNKNOWN to HeaderAlignment.UNKNOWN
        )

        alignments.forEach { (remote, domain) ->
            assertEquals(
                expected = domain,
                actual = sampleThemeRemote(header = sampleHeaderRemote(alignment = remote)).mapToDomain().header.alignment
            )
        }
    }

    @Test
    fun `when a theme is mapped expect its message styles`() {
        val messages = sampleThemeRemote(
            messages = ThemeMessagesRemote(
                assistant = AssistantMessageStyleRemote(
                    background_color = "#F3F4F6",
                    text_color = "#111827",
                    border_color = "#666666",
                    thinking_border_gradient = listOf("#AAAAAA", "#BBBBBB")
                ),
                user = UserMessageStyleRemote(
                    background_color = "#E0E7FF",
                    text_color = "#FFFFFF",
                    border_color = "#777777"
                )
            )
        ).mapToDomain().messages

        assertEquals(
            expected = "#F3F4F6",
            actual = messages.assistant.backgroundColor
        )
        assertEquals(
            expected = "#111827",
            actual = messages.assistant.textColor
        )
        assertEquals(
            expected = "#666666",
            actual = messages.assistant.borderColor
        )
        assertEquals(
            expected = listOf("#AAAAAA", "#BBBBBB"),
            actual = messages.assistant.thinkingBorderGradient
        )
        assertEquals(
            expected = "#E0E7FF",
            actual = messages.user.backgroundColor
        )
        assertEquals(
            expected = "#FFFFFF",
            actual = messages.user.textColor
        )
        assertEquals(
            expected = "#777777",
            actual = messages.user.borderColor
        )
    }

    @Test
    fun `when a theme is mapped expect its product card colour`() {
        assertEquals(
            expected = "#DDDDDD",
            actual = sampleThemeRemote(
                productCard = ThemeProductCardRemote(discount_price_color = "#DDDDDD")
            ).mapToDomain().productCard.discountPriceColor
        )
    }

    @Test
    fun `when a theme is mapped expect its downloadable font`() {
        assertEquals(
            expected = "https://example.com/suisse-intl.ttc",
            actual = sampleThemeRemote().mapToDomain().font.android.assetUrl
        )
    }

    @Test
    fun `when the backend leaves a theme blank expect nothing to apply`() {
        val theme = sampleThemeRemote(
            surface = ThemeSurfaceRemote(
                background_gradient_color = null,
                background_color = null,
                muted_text_color = null
            ),
            input = ThemeInputRemote(
                text_color = null,
                placeholder_color = null,
                background_color = null,
                border_color = null,
                send_button = ThemeSendButtonRemote(icon_color = null)
            ),
            productCard = ThemeProductCardRemote(discount_price_color = null)
        ).mapToDomain()

        assertNull(theme.surface.backgroundColor)
        assertNull(theme.input.textColor)
        assertNull(theme.input.sendButtonIconColor)
        assertNull(theme.productCard.discountPriceColor)
        assertTrue(theme.messages.assistant.thinkingBorderGradient.isEmpty())
    }

    private fun sampleHeaderRemote(alignment: HeaderAlignmentRemote = HeaderAlignmentRemote.LEFT) = ThemeHeaderRemote(
        alignment = alignment,
        logo = ThemeLogoRemote(url = null),
        button = ThemeHeaderButtonRemote(
            background_color = null,
            icon_color = null
        )
    )

    private fun sampleThemeRemote(
        surface: ThemeSurfaceRemote = ThemeSurfaceRemote(
            background_gradient_color = null,
            background_color = null,
            muted_text_color = null
        ),
        header: ThemeHeaderRemote = sampleHeaderRemote(),
        messages: ThemeMessagesRemote = ThemeMessagesRemote(
            assistant = AssistantMessageStyleRemote(
                background_color = "#F3F4F6",
                text_color = "#111827",
                border_color = null,
                thinking_border_gradient = emptyList()
            ),
            user = UserMessageStyleRemote(
                background_color = "#E0E7FF",
                text_color = "#FFFFFF",
                border_color = null
            )
        ),
        input: ThemeInputRemote = ThemeInputRemote(
            text_color = null,
            placeholder_color = null,
            background_color = null,
            border_color = null,
            send_button = ThemeSendButtonRemote(icon_color = null)
        ),
        productCard: ThemeProductCardRemote = ThemeProductCardRemote(discount_price_color = null)
    ) = ThemeRemote(
        brand = BrandRemote(primary_color = "#4F46E5"),
        surface = surface,
        header = header,
        messages = messages,
        input = input,
        product_card = productCard,
        font = ThemeFontRemote(
            android = ThemeAndroidFontRemote(asset_url = "https://example.com/suisse-intl.ttc")
        )
    )
}
