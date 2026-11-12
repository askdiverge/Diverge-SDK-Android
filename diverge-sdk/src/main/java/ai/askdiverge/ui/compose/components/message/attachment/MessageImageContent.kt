package ai.askdiverge.ui.compose.components.message.attachment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import ai.askdiverge.domain.model.AttachmentUrl
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

private const val IMAGE_RATIO = 4f / 3f

@Composable
internal fun MessageImageContent(
    image: MessagePartUiModel.Image,
    onOpen: (displayUrl: String, attachmentId: String?, urlExpiresAt: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val canOpen = AttachmentUrl.isWebUrl(image.displayUrl)
    val loadUrl = image.thumbnailUrl ?: image.displayUrl
    val label = image.caption?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.media_image_content_description)
    val context = LocalPlatformContext.current
    // Chat images are conversation data, so they are cached in memory only, never on disk.
    val request = remember(loadUrl, context) {
        ImageRequest.Builder(context)
            .data(loadUrl)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_RATIO)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (canOpen) {
                        Modifier.clickable {
                            onOpen(image.displayUrl, image.attachmentId, image.urlExpiresAt)
                        }
                    } else {
                        Modifier
                    }
                )
                .semantics { contentDescription = label },
            model = request,
            contentDescription = label,
            imageLoader = SingletonImageLoader.get(context),
            contentScale = ContentScale.Crop
        )
        image.caption?.takeIf { it.isNotBlank() }?.let { caption ->
            Text(
                text = caption,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.mutedText
            )
        }
    }
}
