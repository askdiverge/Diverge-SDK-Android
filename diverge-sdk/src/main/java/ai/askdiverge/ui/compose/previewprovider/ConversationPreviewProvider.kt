package ai.askdiverge.ui.compose.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel
import ai.askdiverge.ui.model.ChatBubbleSender
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.SenderAvatar

internal class ConversationPreviewProvider : PreviewParameterProvider<List<ChatBubbleUiModel>> {

    private val richTextValues = RichTextPreviewProvider().values.toList()

    override val values: Sequence<List<ChatBubbleUiModel>>
        get() = sequenceOf(
            listOf(
                ChatBubbleUiModel(
                    sender = ChatBubbleSender.User,
                    part = userText("Can you help me find black boots?"),
                    id = "preview-user-1"
                ),
                ChatBubbleUiModel(
                    sender = ChatBubbleSender.Bot(SenderAvatar.Session),
                    part = richTextValues[0],
                    id = "preview-bot-1"
                ),
                ChatBubbleUiModel(
                    sender = ChatBubbleSender.User,
                    part = userText("Do you have more options?"),
                    id = "preview-user-2"
                ),
                ChatBubbleUiModel(
                    sender = ChatBubbleSender.Bot(SenderAvatar.Session),
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
