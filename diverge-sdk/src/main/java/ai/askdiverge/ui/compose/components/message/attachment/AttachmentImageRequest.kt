package ai.askdiverge.ui.compose.components.message.attachment

import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import ai.askdiverge.domain.model.AttachmentUrl

/**
 * The Coil request for an image in the conversation. It neither reads nor writes Coil's disk
 * cache, so conversation images are held in memory only and nothing outlives a data deletion.
 * Only a link [AttachmentUrl.loadableOrNull] accepts is requested, with its scheme lowercased; any
 * other, such as `file:` or `content:`, gives a request with no data, which fails without reading
 * the device.
 */
internal fun attachmentImageRequest(
    context: PlatformContext,
    url: String
): ImageRequest = ImageRequest.Builder(context)
    .data(AttachmentUrl.loadableOrNull(url))
    .diskCachePolicy(CachePolicy.DISABLED)
    .build()
