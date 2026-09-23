package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.config.display.AvatarRemote
import ai.askdiverge.data.model.config.display.DisplayRemote
import ai.askdiverge.data.model.config.display.subtitle.SubtitleLinkValueRemote
import ai.askdiverge.data.model.config.display.subtitle.SubtitleRemote
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConfigMapperTest {

    @Test
    fun `maps display fields correctly`() {
        val remote = configRemote(
            subtitle = SubtitleRemote(
                text = "Read our policy",
                link = SubtitleLinkValueRemote(text = "policy", url = "https://example.com/policy")
            )
        )

        val result = remote.mapToDomain()

        assertEquals("Bot", result.display.name)
        assertEquals("https://example.com/avatar.png", result.display.avatarUrl)
        assertEquals("Hello!", result.display.welcomeMessage)
        val subtitle = assertNotNull(result.display.subtitle)
        assertEquals("Read our policy", subtitle.text)
        assertEquals("policy", subtitle.link?.text)
        assertEquals("https://example.com/policy", subtitle.link?.url)
        assertEquals("https://example.com/privacy-policy", result.display.privacyPolicyUrl)
    }

    @Test
    fun `maps text-only subtitle`() {
        val remote = configRemote(subtitle = SubtitleRemote(text = "You are chatting with an AI"))

        val result = remote.mapToDomain()

        val subtitle = assertNotNull(result.display.subtitle)
        assertEquals("You are chatting with an AI", subtitle.text)
        assertNull(subtitle.link)
    }

    @Test
    fun `maps link-only subtitle`() {
        val remote = configRemote(
            subtitle = SubtitleRemote(link = SubtitleLinkValueRemote(text = "Contact support", url = "https://example.com/support"))
        )

        val result = remote.mapToDomain()

        val subtitle = assertNotNull(result.display.subtitle)
        assertNull(subtitle.text)
        assertEquals("Contact support", subtitle.link?.text)
        assertEquals("https://example.com/support", subtitle.link?.url)
    }

    @Test
    fun `maps theme brand color`() {
        val remote = configRemote()

        val result = remote.mapToDomain()

        assertEquals("#4F46E5", result.theme.brand.primaryColor)
    }

    private fun configRemote(subtitle: SubtitleRemote? = null) = ConfigRemote(
        display = DisplayRemote(
            name = "Bot",
            avatar = AvatarRemote(url = "https://example.com/avatar.png"),
            welcome_message = "Hello!",
            subtitle = subtitle,
            privacy_policy_url = "https://example.com/privacy-policy"
        ),
        theme = ThemeRemote(
            brand = BrandRemote(primary_color = "#4F46E5"),
            surface = ThemeSurfaceRemote(
                background_gradient_color = null,
                background_color = null,
                muted_text_color = null
            ),
            header = ThemeHeaderRemote(
                alignment = HeaderAlignmentRemote.CENTER,
                logo = ThemeLogoRemote(url = null),
                button = ThemeHeaderButtonRemote(background_color = null, icon_color = null)
            ),
            messages = ThemeMessagesRemote(
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
            input = ThemeInputRemote(
                text_color = null,
                placeholder_color = null,
                background_color = null,
                border_color = null,
                send_button = ThemeSendButtonRemote(icon_color = null)
            ),
            product_card = ThemeProductCardRemote(discount_price_color = null),
            font = ThemeFontRemote(
                android = ThemeAndroidFontRemote(asset_url = "https://example.com/suisse-intl.ttc")
            )
        )
    )
}
