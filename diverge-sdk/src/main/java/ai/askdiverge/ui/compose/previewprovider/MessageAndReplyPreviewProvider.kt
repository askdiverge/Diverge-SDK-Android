package ai.askdiverge.ui.compose.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel

internal class MessageAndReplyPreviewProvider : PreviewParameterProvider<List<ChatBubbleUiModel>> {

    override val values: Sequence<List<ChatBubbleUiModel>>
        get() = sequenceOf(
            listOf(
                ChatBubbleUiModel(
                    role = MessageRoleUiModel.BOT,
                    part = RichTextPreviewProvider().values.first(),
                    id = "preview-bot"
                ),
                ChatBubbleUiModel(
                    role = MessageRoleUiModel.USER,
                    part = userText("Can you help me find black boots?"),
                    id = "preview-user"
                )
            )
        )

    private fun userText(text: String) = MessagePartUiModel.RichText(
        blocks = listOf(
            RichTextBlockUiModel.Paragraph(
                spans = listOf(RichTextSpanUiModel(text = text, type = RichTextSpanTypeUiModel.TEXT))
            )
        )
    )
}
