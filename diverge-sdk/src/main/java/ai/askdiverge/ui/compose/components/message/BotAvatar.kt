package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.AttachmentUrl
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.SenderAvatar
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext

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
            model = AttachmentUrl.loadableOrNull(avatarUrl),
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
    val personIcon = rememberPersonIconPainter()
    val circle = modifier
        .size(AVATAR_SIZE)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.assistantBubbleBackground)
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.assistantBubbleBorder,
            shape = CircleShape
        )
    if (imageUrl == null) {
        // No photo to load, so the icon is drawn without starting a request.
        Image(
            painter = personIcon,
            contentDescription = contentDescription,
            modifier = circle,
            contentScale = ContentScale.Crop
        )
    } else {
        AsyncImage(
            modifier = circle,
            model = rememberMemoryOnlyImageRequest(imageUrl),
            contentDescription = contentDescription,
            imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
            placeholder = personIcon,
            error = personIcon,
            contentScale = ContentScale.Crop
        )
    }
}

/** [ChatbotIcons.Person] tinted in the muted text colour, the way `Icon` tints it. */
@Composable
private fun rememberPersonIconPainter(): Painter {
    val icon = rememberVectorPainter(ChatbotIcons.Person)
    val tint = MaterialTheme.colorScheme.mutedText
    return remember(icon, tint) { TintedPainter(painter = icon, tint = ColorFilter.tint(tint)) }
}

/** Draws [painter] through [tint], at the alpha this painter is drawn with. */
private class TintedPainter(
    private val painter: Painter,
    private val tint: ColorFilter
) : Painter() {

    private var drawAlpha = DefaultAlpha

    override val intrinsicSize: Size
        get() = painter.intrinsicSize

    override fun applyAlpha(alpha: Float): Boolean {
        drawAlpha = alpha
        return true
    }

    override fun DrawScope.onDraw() {
        with(painter) { draw(size, drawAlpha, tint) }
    }
}
