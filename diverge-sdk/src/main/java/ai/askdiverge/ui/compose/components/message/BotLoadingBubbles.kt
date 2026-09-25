package ai.askdiverge.ui.compose.components.message

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import kotlin.math.PI
import kotlin.math.sin

private const val DOT_COUNT = 3
private const val WAVE_DURATION_MILLIS = 1000
private const val STAGGER_DELAY_MILLIS = 150
private val DOT_SIZE = 8.dp
private val DOT_SPACING = 8.dp
private val DOT_TRAVEL_Y = 4.dp

@Composable
internal fun BotLoadingBubbles(
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    BotMessageBubble(
        avatarUrl = avatarUrl,
        modifier = modifier,
        isStreaming = true
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = 24.dp,
                    vertical = 12.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(DOT_SPACING)
        ) {
            repeat(DOT_COUNT) { index ->
                BotLoadingBubbleDot(index = index)
            }
        }
    }
}

@Composable
private fun BotLoadingBubbleDot(index: Int) {
    // Creating a "sine wave" animation
    val transition = rememberInfiniteTransition(label = "botLoadingBubbleDotTransition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(offsetMillis = index * STAGGER_DELAY_MILLIS)
        ),
        label = "botLoadingBubbleDotOffsetY"
    )

    val offsetY = DOT_TRAVEL_Y.value * sin(phase)

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(DOT_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.chatbotPrimary)
    )
}

@Preview(showBackground = true)
@Composable
private fun BotLoadingBubblesPreview() {
    BotLoadingBubbles(avatarUrl = "")
}
