package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.previewprovider.RichTextPreviewProvider
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.userMessageText
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

private const val TYPEWRITER_CHAR_DELAY_MILLIS = 12L

@Composable
internal fun TextContent(
    content: MessagePartUiModel.RichText,
    role: MessageRoleUiModel,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    onUrlClick: ((String) -> Unit)? = null
) {
    val textColor = when (role) {
        MessageRoleUiModel.USER -> MaterialTheme.colorScheme.userMessageText
        MessageRoleUiModel.BOT -> MaterialTheme.colorScheme.assistantMessageText
    }

    // One shared cursor drives every block/bullet in document order, so the whole message types
    // out as a single continuous stream instead of each line animating in parallel.
    val totalChars = rememberTotalChars(content.blocks)

    var revealedChars by remember { mutableIntStateOf(0) }
    if (isStreaming) {
        LaunchedEffect(totalChars) {
            while (revealedChars < totalChars) {
                delay(TYPEWRITER_CHAR_DELAY_MILLIS.milliseconds)
                revealedChars++
            }
        }
    }
    val visibleChars = if (isStreaming) revealedChars.coerceAtMost(totalChars) else totalChars

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        content.blocks
            .zip(content.blocks.revealCounts(visibleChars))
            .forEach { (block, lineCounts) ->
                when (block) {
                    is RichTextBlockUiModel.Paragraph -> {
                        lineCounts.first().takeIf { it > 0 }?.let { visibleInBlock ->
                            Paragraph(
                                spans = block.spans,
                                textColor = textColor,
                                visibleChars = visibleInBlock,
                                onUrlClick = onUrlClick
                            )
                        }
                    }

                    is RichTextBlockUiModel.BulletList -> {
                        block.items.zip(lineCounts).forEach { (item, visibleInItem) ->
                            if (visibleInItem > 0) {
                                BulletRow(
                                    spans = item.spans,
                                    textColor = textColor,
                                    visibleChars = visibleInItem,
                                    onUrlClick = onUrlClick
                                )
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun Paragraph(
    spans: List<RichTextSpanUiModel>,
    textColor: Color,
    visibleChars: Int,
    onUrlClick: ((String) -> Unit)? = null
) {
    val fullText = remember(spans, onUrlClick) {
        buildText(
            spans = spans,
            onUrlClick = onUrlClick
        )
    }

    val style = MaterialTheme.typography.bodyMedium
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Reserve the box at the size of the currently known (possibly still-growing) text up front,
    // and reveal along its stable line breaks — so the typewriter fills in characters without the
    // bubble itself resizing in step with every keystroke. The box only grows when new text
    // actually streams in, not while already-known text is being typed out.
    BoxWithConstraints {
        val maxWidthPx = with(density) { maxWidth.roundToPx() }
        val fullLayout = remember(fullText, maxWidthPx, style) {
            textMeasurer.measure(
                text = fullText,
                style = style,
                constraints = Constraints(maxWidth = maxWidthPx)
            )
        }

        val revealedText = remember(fullText, visibleChars, fullLayout) {
            buildRevealedText(
                fullText = fullText,
                visibleChars = visibleChars,
                fullLayout = fullLayout
            )
        }

        Text(
            modifier = Modifier.size(
                width = with(density) { fullLayout.size.width.toDp() },
                height = with(density) { fullLayout.size.height.toDp() }
            ),
            color = textColor,
            text = revealedText,
            style = style
        )
    }
}

@Composable
private fun BulletRow(
    spans: List<RichTextSpanUiModel>,
    textColor: Color,
    visibleChars: Int,
    onUrlClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.chat_bullet_point),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
        Paragraph(
            spans = spans,
            textColor = textColor,
            visibleChars = visibleChars,
            onUrlClick = onUrlClick
        )
    }
}

@Composable
private fun rememberTotalChars(models: List<RichTextBlockUiModel>) = remember(models) {
    models.sumOf { block -> block.charCount }
}

private fun buildText(
    spans: List<RichTextSpanUiModel>,
    onUrlClick: ((String) -> Unit)?
): AnnotatedString {
    val linkStyles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        spans.forEach { span ->
            when (span.type) {
                RichTextSpanTypeUiModel.TEXT -> append(span.text)

                RichTextSpanTypeUiModel.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(span.text)
                }

                RichTextSpanTypeUiModel.STRIKE -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(span.text)
                }

                RichTextSpanTypeUiModel.LINK -> {
                    val url = span.url.orEmpty()
                    withLink(
                        if (onUrlClick != null) {
                            LinkAnnotation.Clickable(
                                tag = url,
                                styles = linkStyles,
                                linkInteractionListener = { onUrlClick(url) }
                            )
                        } else {
                            LinkAnnotation.Url(url = url, styles = linkStyles)
                        }
                    ) { append(span.text) }
                }
            }
        }
    }
}

private fun buildRevealedText(
    fullText: AnnotatedString,
    visibleChars: Int,
    fullLayout: TextLayoutResult
): AnnotatedString {
    val clampedChars = visibleChars.coerceIn(0, fullText.length)
    val currentLine = fullLayout.getLineForOffset(clampedChars)
    return buildAnnotatedString {
        for (line in 0 until currentLine) {
            append(
                fullText.subSequence(
                    startIndex = fullLayout.getLineStart(line),
                    endIndex = fullLayout.getLineEnd(line, visibleEnd = true)
                )
            )
            append('\n')
        }
        append(
            fullText.subSequence(
                startIndex = fullLayout.getLineStart(currentLine),
                endIndex = clampedChars
            )
        )
    }
}

private fun List<RichTextBlockUiModel>.revealCounts(visibleChars: Int): List<List<Int>> {
    var consumed = 0
    return map { block ->
        val lineLengths = when (block) {
            is RichTextBlockUiModel.Paragraph -> listOf(block.spans.charCount())
            is RichTextBlockUiModel.BulletList -> block.items.map { it.spans.charCount() }
        }
        lineLengths.map { length ->
            val visible = (visibleChars - consumed).coerceIn(0, length)
            consumed += length
            visible
        }
    }
}

private fun List<RichTextSpanUiModel>.charCount(): Int = sumOf { it.text.length }

private val RichTextBlockUiModel.charCount: Int
    get() = when (this) {
        is RichTextBlockUiModel.Paragraph -> spans.charCount()
        is RichTextBlockUiModel.BulletList -> items.sumOf { it.spans.charCount() }
    }

@Preview(showBackground = true)
@Composable
private fun TextContentPreview(@PreviewParameter(RichTextPreviewProvider::class) content: MessagePartUiModel.RichText) {
    TextContent(content = content, role = MessageRoleUiModel.BOT)
}
