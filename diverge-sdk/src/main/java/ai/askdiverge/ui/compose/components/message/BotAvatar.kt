package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.SenderAvatar
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest

private val AVATAR_SIZE = 32.dp

/**
 * The face beside a bot text bubble.
 *
 * @param avatarUrl the `/config` avatar, drawn for [SenderAvatar.Session].
 */
@Composable
internal fun BotAvatar(
    senderAvatar: SenderAvatar,
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    when (senderAvatar) {
        SenderAvatar.Session -> AsyncImage(
            modifier = modifier.size(AVATAR_SIZE),
            model = avatarUrl,
            contentDescription = null,
            imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
            contentScale = ContentScale.Fit
        )

        is SenderAvatar.Agent -> AgentAvatar(
            imageUrl = senderAvatar.imageUrl,
            contentDescription = senderAvatar.displayName
                ?: stringResource(R.string.livechat_agent_avatar_content_description),
            modifier = modifier
        )
    }
}

@Composable
private fun AgentAvatar(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier
) {
    val context = LocalPlatformContext.current
    // An agent's photo arrives with the conversation, so it is cached in memory only, never on disk.
    val request = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }
    // The person icon shows while the photo loads, when there is none, and when it fails to load.
    val personIcon = rememberPersonIconPainter()
    AsyncImage(
        modifier = modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.assistantBubbleBorder,
                shape = CircleShape
            ),
        model = request,
        contentDescription = contentDescription,
        imageLoader = SingletonImageLoader.get(context),
        placeholder = personIcon,
        error = personIcon,
        fallback = personIcon,
        contentScale = ContentScale.Crop
    )
}

/** [ChatbotIcons.Person] as a painter, tinted in the muted text colour. */
@Composable
private fun rememberPersonIconPainter(): Painter {
    val icon = ChatbotIcons.Person
    return rememberVectorPainter(
        defaultWidth = icon.defaultWidth,
        defaultHeight = icon.defaultHeight,
        viewportWidth = icon.viewportWidth,
        viewportHeight = icon.viewportHeight,
        tintColor = MaterialTheme.colorScheme.mutedText
    ) { _, _ -> RenderVectorGroup(group = icon.root) }
}
