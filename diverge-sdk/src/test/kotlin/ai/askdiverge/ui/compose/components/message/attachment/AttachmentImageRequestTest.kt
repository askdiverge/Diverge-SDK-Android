package ai.askdiverge.ui.compose.components.message.attachment

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.NullRequestData
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName

@DisplayName("attachmentImageRequest")
internal class AttachmentImageRequestTest {

    @Test
    fun `conversation images never touch the disk cache`() {
        val request = attachmentImageRequest(
            context = mockk<Context>(relaxed = true),
            url = "https://cdn.example/photo.jpg"
        )

        assertEquals(CachePolicy.DISABLED, request.diskCachePolicy)
        assertEquals("https://cdn.example/photo.jpg", request.data)
    }

    @Test
    fun `a web link is requested with its scheme lowercased and a data link as it is`() {
        val context = mockk<Context>(relaxed = true)

        assertEquals(
            "https://cdn.example/Photo.jpg",
            attachmentImageRequest(context, "HTTPS://cdn.example/Photo.jpg").data
        )
        assertEquals(
            "data:image/jpeg;base64,/9j/4AAQ",
            attachmentImageRequest(context, "data:image/jpeg;base64,/9j/4AAQ").data
        )
    }

    @Test
    fun `a file, content or resource link gives a request with no data`() {
        val context = mockk<Context>(relaxed = true)

        assertEquals(NullRequestData, attachmentImageRequest(context, "file:///sdcard/photo.jpg").data)
        assertEquals(
            NullRequestData,
            attachmentImageRequest(context, "content://media/external/images/media/1").data
        )
        assertEquals(
            NullRequestData,
            attachmentImageRequest(context, "android.resource://ai.askdiverge.sample/1").data
        )
    }
}
