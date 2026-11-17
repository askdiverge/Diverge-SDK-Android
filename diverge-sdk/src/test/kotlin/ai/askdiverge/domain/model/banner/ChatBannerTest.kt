package ai.askdiverge.domain.model.banner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ChatBannerTest {

    @Test
    fun `an https CTA with a label shows`() {
        val banner = makeSUT(ctaUrl = "https://shop.example/sale")

        assertTrue(banner.showsCta)
        assertEquals("https://shop.example/sale", banner.sanitizedCtaUrl)
    }

    @Test
    fun `a data CTA does not show`() {
        val banner = makeSUT(ctaUrl = "data:text/html,<p>Sale</p>")

        assertFalse(banner.showsCta)
        assertNull(banner.sanitizedCtaUrl)
    }

    private fun makeSUT(ctaUrl: String): ChatBanner = ChatBanner(
        id = "1",
        message = "Sale today",
        ctaUrl = ctaUrl,
        ctaLabel = "Shop now"
    )
}
