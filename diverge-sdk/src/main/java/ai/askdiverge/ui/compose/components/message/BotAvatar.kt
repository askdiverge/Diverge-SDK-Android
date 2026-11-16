package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.icons.Person
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.SenderAvatar
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter

private val AVATAR_SIZE = 32.dp

@Composable
internal fun BotAvatar(
    senderAvatar: SenderAvatar,
    sessionAvatarUrl: String,
    modifier: Modifier = Modifier
) {
    when (senderAvatar) {
        SenderAvatar.Session -> {
            Image(
                painter = rememberAsyncImagePainter(
                    model = sessionAvatarUrl,
                    imageLoader = SingletonImageLoader.get(LocalPlatformContext.current)
                ),
                contentDescription = null,
                modifier = modifier.size(AVATAR_SIZE),
                contentScale = ContentScale.Fit
            )
        }

        is SenderAvatar.Agent -> {
            val description = senderAvatar.displayName.ifBlank {
                stringResource(R.string.livechat_agent_fallback)
            }
            AgentAvatar(
                imageUrl = senderAvatar.imageUrl,
                contentDescription = description,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AgentAvatar(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) {
        AgentPersonIcon(
            contentDescription = contentDescription,
            modifier = modifier
        )
        return
    }
    val painter = rememberAsyncImagePainter(
        model = imageUrl,
        imageLoader = SingletonImageLoader.get(LocalPlatformContext.current)
    )
    val state by painter.state.collectAsState()
    when (state) {
        is AsyncImagePainter.State.Error,
        AsyncImagePainter.State.Empty -> {
            AgentPersonIcon(
                contentDescription = contentDescription,
                modifier = modifier
            )
        }

        else -> {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = modifier.size(AVATAR_SIZE),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun AgentPersonIcon(
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Person,
        contentDescription = contentDescription,
        modifier = modifier.size(AVATAR_SIZE),
        tint = MaterialTheme.colorScheme.mutedText
    )
}
