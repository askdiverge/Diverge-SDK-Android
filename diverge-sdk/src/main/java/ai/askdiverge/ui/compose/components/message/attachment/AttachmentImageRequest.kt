package ai.askdiverge.ui.compose.components.message.attachment

import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest

/**
 * The Coil request for an image in the conversation. It neither reads nor writes Coil's disk
 * cache, so conversation images are held in memory only and nothing outlives a data deletion.
 */
internal fun attachmentImageRequest(
    context: PlatformContext,
    url: String
): ImageRequest = ImageRequest.Builder(context)
    .data(url)
    .diskCachePolicy(CachePolicy.DISABLED)
    .build()
