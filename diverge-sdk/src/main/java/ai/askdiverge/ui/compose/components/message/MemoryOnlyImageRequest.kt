package ai.askdiverge.ui.compose.components.message

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import ai.askdiverge.ui.compose.components.message.attachment.attachmentImageRequest

/**
 * A request for an image that arrives with the conversation. Such images are conversation data, so
 * they are cached in memory only, never on disk; while that cache is cold, each request for the
 * same image goes to the network.
 */
@Composable
internal fun rememberMemoryOnlyImageRequest(url: String): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(url, context) { attachmentImageRequest(context, url) }
}
