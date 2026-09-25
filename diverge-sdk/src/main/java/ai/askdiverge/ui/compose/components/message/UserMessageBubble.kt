package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.theme.userBubbleBackground
import ai.askdiverge.ui.compose.theme.userBubbleBorder
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel

@Composable
internal fun UserMessageBubble(
    part: MessagePartUiModel,
    modifier: Modifier = Modifier,
    onUrlClick: ((String) -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.85f)
                .wrapContentWidth(Alignment.End)
                .background(MaterialTheme.colorScheme.userBubbleBackground)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.userBubbleBorder)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
        ) {
            // In the future, more types will be added (like images)
            when (part) {
                is MessagePartUiModel.RichText -> TextContent(
                    content = part,
                    role = MessageRoleUiModel.USER,
                    onUrlClick = onUrlClick
                )

                // A user message can only contain rich text, never a products carousel or a table.
                is MessagePartUiModel.Products,
                is MessagePartUiModel.Table -> Unit
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserMessageBubblePreview() {
    UserMessageBubble(
        part = MessagePartUiModel.RichText(
            blocks = listOf(
                RichTextBlockUiModel.Paragraph(
                    spans = listOf(
                        RichTextSpanUiModel(
                            text = "Can you help me find black boots?",
                            type = RichTextSpanTypeUiModel.TEXT
                        )
                    )
                )
            )
        )
    )
}
