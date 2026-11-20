package ai.askdiverge.ui.compose.components.conversation.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.InlineMarkdownSegment
import ai.askdiverge.ui.model.SubtitleLinkUiModel
import ai.askdiverge.ui.model.SubtitleUiModel

internal fun LazyListScope.subtitleBanner(
    uiModel: SubtitleUiModel,
    onUrlClick: ((String) -> Unit)? = null
) = item {
    val text = remember(uiModel, onUrlClick) {
        buildAnnotatedString {
            uiModel.text?.let { appendInlineMarkdown(text = it, onUrlClick = onUrlClick) }
            uiModel.link?.let { link ->
                appendLink(
                    link = link,
                    prefixWithSpace = uiModel.text != null,
                    onUrlClick = onUrlClick
                )
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.mutedText
        )
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    onUrlClick: ((String) -> Unit)?
) {
    InlineMarkdownSegment.parse(text).forEach { segment ->
        when (segment) {
            is InlineMarkdownSegment.Plain -> append(segment.text)

            is InlineMarkdownSegment.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(segment.text)
            }

            is InlineMarkdownSegment.Link -> appendLink(
                link = SubtitleLinkUiModel(text = segment.text, url = segment.url),
                prefixWithSpace = false,
                onUrlClick = onUrlClick
            )
        }
    }
}

private fun AnnotatedString.Builder.appendLink(
    link: SubtitleLinkUiModel,
    prefixWithSpace: Boolean,
    onUrlClick: ((String) -> Unit)?
) {
    val linkStyles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))
    if (prefixWithSpace) {
        append(" ")
    }
    withLink(
        if (onUrlClick != null) {
            LinkAnnotation.Clickable(
                tag = link.url,
                styles = linkStyles,
                linkInteractionListener = { onUrlClick(link.url) }
            )
        } else {
            LinkAnnotation.Url(
                url = link.url,
                styles = linkStyles
            )
        }
    ) { append(link.text) }
}

@Preview(showBackground = true)
@Composable
private fun SubtitleBannerPreview() {
    LazyColumn {
        subtitleBanner(
            uiModel = SubtitleUiModel(
                text = "You are communicating with an artificial intelligence. Errors may occur and the " +
                    "conversation may be saved and processed. Read more in our",
                link = SubtitleLinkUiModel(
                    text = "privacy policy",
                    url = "https://example.com/privacy"
                )
            )
        )
    }
}
