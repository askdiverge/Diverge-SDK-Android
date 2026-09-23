package ai.askdiverge.ui.compose.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel
import ai.askdiverge.ui.model.ChatBubbleUiModel

internal class ConversationPreviewProvider : PreviewParameterProvider<List<ChatBubbleUiModel>> {

    private val richTextValues = RichTextPreviewProvider().values.toList()

    override val values: Sequence<List<ChatBubbleUiModel>>
        get() = sequenceOf(
            listOf(
                ChatBubbleUiModel(
                    role = MessageRoleUiModel.USER,
                    part = userText("Can you help me find black boots?"),
                    id = "preview-user-1"
                ),
                ChatBubbleUiModel(
                    role = MessageRoleUiModel.BOT,
                    part = richTextValues[0],
                    id = "preview-bot-1"
                ),
                ChatBubbleUiModel(
                    role = MessageRoleUiModel.USER,
                    part = userText("Do you have more options?"),
                    id = "preview-user-2"
                ),
                ChatBubbleUiModel(
                    role = MessageRoleUiModel.BOT,
                    part = richTextValues[1],
                    id = "preview-bot-2"
                )
            ),
            emptyList()
        )

    private fun userText(text: String) = MessagePartUiModel.RichText(
        blocks = listOf(
            RichTextBlockUiModel.Paragraph(
                spans = listOf(RichTextSpanUiModel(text = text, type = RichTextSpanTypeUiModel.TEXT))
            )
        )
    )
}
