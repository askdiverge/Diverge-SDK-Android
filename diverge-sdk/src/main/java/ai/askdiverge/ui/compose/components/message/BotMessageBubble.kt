package ai.askdiverge.ui.compose.components.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import ai.askdiverge.ui.compose.animation.rainbowBorder
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder

@Composable
internal fun BotMessageBubble(
    avatarUrl: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(fraction = 0.85f),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            modifier = Modifier.size(24.dp),
            model = avatarUrl,
            contentDescription = null,
            imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .animateContentSize()
                .then(
                    if (isStreaming) {
                        Modifier.rainbowBorder()
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.assistantBubbleBorder
                        )
                    }
                )
                .background(MaterialTheme.colorScheme.assistantBubbleBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BotMessageBubblePreview() {
    BotMessageBubble(avatarUrl = "", isStreaming = false) {
        Text(
            text = "I've found several black boots for you, including the classic 2976 Ys Smooth from Dr. Martens.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
