package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.banner.ChatBannerListRemote
import ai.askdiverge.data.model.banner.ChatBannerRemote
import ai.askdiverge.domain.model.banner.ChatBanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ChatBannerMapperTest {

    @Test
    fun `a complete banner maps with link CTA style`() {
        val banner = ChatBannerRemote(
            id = "b1",
            message = "Free shipping",
            background_color = "#112233",
            text_color = "#ffffff",
            cta_url = "https://shop.example/sale",
            cta_label = "Shop",
            cta_style = "link",
            dismissible = true
        ).mapToDomain()

        assertEquals("b1", banner?.id)
        assertEquals(ChatBanner.CtaStyle.LINK, banner?.ctaStyle)
        assertTrue(banner!!.showsCta)
        assertEquals("https://shop.example/sale", banner.sanitizedCtaUrl)
    }

    @Test
    fun `unknown cta_style becomes null`() {
        val banner = ChatBannerRemote(
            id = "b2",
            message = "Hello",
            cta_style = "pill"
        ).mapToDomain()

        assertNull(banner?.ctaStyle)
    }

    @Test
    fun `blank message banners are dropped`() {
        val list = ChatBannerListRemote(
            banners = listOf(
                ChatBannerRemote(id = "keep", message = "Visible"),
                ChatBannerRemote(id = "drop", message = "   ")
            )
        ).mapToDomain()

        assertEquals(listOf("keep"), list.map { it.id })
    }

    @Test
    fun `relative and javascript CTAs do not show`() {
        val relative = ChatBannerRemote(
            id = "r",
            message = "Go",
            cta_url = "/sale",
            cta_label = "Open"
        ).mapToDomain()
        val script = ChatBannerRemote(
            id = "j",
            message = "Go",
            cta_url = "javascript:alert(1)",
            cta_label = "Open"
        ).mapToDomain()

        assertFalse(relative!!.showsCta)
        assertFalse(script!!.showsCta)
    }

    @Test
    fun `content fingerprint changes when the message changes`() {
        val a = ChatBanner(id = "1", message = "A", dismissible = true)
        val b = a.copy(message = "B")

        assertTrue(a.contentFingerprint != b.contentFingerprint)
    }
}
