package ai.askdiverge.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AttachmentUrlTest {

    @Test
    fun `absolute urls pass through`() {
        assertEquals(
            "https://cdn.example/a.pdf",
            AttachmentUrl.resolve("https://cdn.example/a.pdf", apiBaseUrl = null)
        )
    }

    @Test
    fun `relative paths resolve against the api base`() {
        assertEquals(
            "https://api.example/v1/files/a",
            AttachmentUrl.resolve("/v1/files/a", apiBaseUrl = "https://api.example/")
        )
        assertEquals(
            "https://api.example/v1/files/a",
            AttachmentUrl.resolve("v1/files/a", apiBaseUrl = "https://api.example")
        )
    }

    @Test
    fun `data urls pass without a base`() {
        assertEquals(
            "data:image/png;base64,abc",
            AttachmentUrl.resolve("data:image/png;base64,abc", apiBaseUrl = null)
        )
    }

    @Test
    fun `a relative path without a base is unusable`() {
        assertNull(AttachmentUrl.resolve("/v1/files/a", apiBaseUrl = null))
        assertNull(AttachmentUrl.resolve("/v1/files/a", apiBaseUrl = "  "))
    }

    @Test
    fun `expiry is true only when the timestamp is in the past`() {
        assertFalse(AttachmentUrl.isExpired(null))
        assertFalse(AttachmentUrl.isExpired("not-a-date"))
        assertTrue(
            AttachmentUrl.isExpired(
                urlExpiresAt = "2000-01-01T00:00:00Z",
                nowMs = 1_700_000_000_000L
            )
        )
        assertFalse(
            AttachmentUrl.isExpired(
                urlExpiresAt = "2099-01-01T00:00:00Z",
                nowMs = 1_700_000_000_000L
            )
        )
    }
}
