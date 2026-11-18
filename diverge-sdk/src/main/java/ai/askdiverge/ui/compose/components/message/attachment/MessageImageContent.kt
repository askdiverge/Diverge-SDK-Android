package ai.askdiverge.ui.compose.components.message.attachment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import ai.askdiverge.domain.model.AttachmentUrl
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

private const val IMAGE_RATIO = 4f / 3f

@Composable
internal fun MessageImageContent(
    image: MessagePartUiModel.Image,
    onOpen: (displayUrl: String, attachmentId: String?, urlExpiresAt: String?) -> Unit,
    freshUrl: suspend (attachmentId: String) -> String?,
    modifier: Modifier = Modifier
) {
    val canOpen = AttachmentUrl.isWebUrl(image.displayUrl)
    val storedUrl = image.thumbnailUrl ?: image.displayUrl
    // A signed link is swapped, once, for a freshly signed one when it is past its expiry or fails
    // to load, as when the device clock runs behind the server's.
    val signedId = image.attachmentId?.takeIf { !image.urlExpiresAt.isNullOrBlank() }
    var resigned by remember(storedUrl) { mutableStateOf(false) }
    var loadUrl by remember(storedUrl) {
        mutableStateOf(
            storedUrl.takeUnless { signedId != null && AttachmentUrl.isExpired(image.urlExpiresAt) }
        )
    }
    LaunchedEffect(storedUrl, loadUrl == null) {
        if (loadUrl == null && signedId != null && !resigned) {
            resigned = true
            loadUrl = freshUrl(signedId) ?: storedUrl
        }
    }
    val label = image.caption?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.media_image_content_description)
    val context = LocalPlatformContext.current
    val request = remember(context, loadUrl) { loadUrl?.let { attachmentImageRequest(context, it) } }
    var failed by remember(storedUrl) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
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
                .semantics { contentDescription = label }
        ) {
            request?.let { model ->
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = model,
                    contentDescription = label,
                    imageLoader = SingletonImageLoader.get(context),
                    contentScale = ContentScale.Crop,
                    onSuccess = { failed = false },
                    onError = {
                        if (signedId != null && !resigned) loadUrl = null else failed = true
                    }
                )
            }
            if (failed) {
                Text(
                    text = stringResource(R.string.media_unavailable),
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.mutedText
                )
            }
        }
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
