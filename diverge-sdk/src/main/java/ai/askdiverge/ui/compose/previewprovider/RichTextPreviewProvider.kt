package ai.askdiverge.ui.compose.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.BulletListItemUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel

internal class RichTextPreviewProvider : PreviewParameterProvider<MessagePartUiModel.RichText> {
    override val values: Sequence<MessagePartUiModel.RichText>
        get() = sequenceOf(
            MessagePartUiModel.RichText(
                blocks = listOf(
                    RichTextBlockUiModel.Paragraph(
                        spans = listOf(
                            RichTextSpanUiModel(
                                text = "I've found several black boots for you, including the classic 2976 Ys Smooth from Dr. Martens.",
                                type = RichTextSpanTypeUiModel.TEXT
                            )
                        )
                    )
                )
            ),
            MessagePartUiModel.RichText(
                blocks = listOf(
                    RichTextBlockUiModel.Paragraph(
                        spans = listOf(
                            RichTextSpanUiModel(
                                text = "Do you need shoes for",
                                type = RichTextSpanTypeUiModel.TEXT
                            )
                        )
                    ),
                    RichTextBlockUiModel.BulletList(
                        items = listOf(
                            BulletListItemUiModel(
                                spans = listOf(
                                    RichTextSpanUiModel(
                                        text = "Men",
                                        type = RichTextSpanTypeUiModel.TEXT
                                    ),
                                    RichTextSpanUiModel(
                                        text = "Women",
                                        type = RichTextSpanTypeUiModel.TEXT
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
}
