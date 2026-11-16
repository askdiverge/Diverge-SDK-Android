package ai.askdiverge.ui.compose.components.message

import ai.askdiverge.ui.compose.animation.loadingBorder
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.model.SenderAvatar

@Composable
internal fun BotMessageBubble(
    sessionAvatarUrl: String,
    modifier: Modifier = Modifier,
    senderAvatar: SenderAvatar = SenderAvatar.Session,
    isStreaming: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(fraction = 0.85f),
        verticalAlignment = Alignment.Top
    ) {
        BotAvatar(
            senderAvatar = senderAvatar,
            sessionAvatarUrl = sessionAvatarUrl
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .animateContentSize()
                .then(
                    if (isStreaming) {
                        Modifier.loadingBorder()
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
    BotMessageBubble(sessionAvatarUrl = "", isStreaming = false) {
        Text(
            text = "I've found several black boots for you, including the classic 2976 Ys Smooth from Dr. Martens.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
