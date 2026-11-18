package ai.askdiverge.ui.compose.components.message.attachment

import android.content.Context
import coil3.request.CachePolicy
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
}
