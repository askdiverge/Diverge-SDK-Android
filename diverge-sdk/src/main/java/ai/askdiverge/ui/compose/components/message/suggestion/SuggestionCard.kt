package ai.askdiverge.ui.compose.components.message.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.model.message.part.suggestion.SuggestionCardUiModel
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext

private const val SUGGESTION_IMAGE_RATIO = 0.7f
private const val DISABLED_ALPHA = 0.38f

@Composable
internal fun SuggestionCard(
    suggestion: SuggestionCardUiModel,
    enabled: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = stringResource(R.string.suggestion_card_accessibility_hint)
    val label = buildString {
        append(suggestion.title)
        suggestion.description?.takeIf { it.isNotBlank() }?.let {
            append(". ")
            append(it)
        }
    }
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button) { onClick(suggestion.promptText) }
            // One TalkBack element that reads the card's text once; the hint only while a tap
            // can send.
            .clearAndSetSemantics {
                contentDescription = if (enabled) "$label. $hint" else label
            }
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = SUGGESTION_IMAGE_RATIO)
                .background(color = MaterialTheme.colorScheme.surfaceVariant),
            model = suggestion.imageUrl.takeIf { it.isNotBlank() },
            contentDescription = null,
            imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
            contentScale = ContentScale.Crop
        )

        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = suggestion.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.assistantMessageText
        )

        suggestion.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
