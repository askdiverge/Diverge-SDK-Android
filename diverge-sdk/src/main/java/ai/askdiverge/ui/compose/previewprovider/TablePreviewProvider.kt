package ai.askdiverge.ui.compose.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellBlockUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellUiModel
import ai.askdiverge.ui.model.message.part.table.TableColumnAlignmentUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel

internal class TablePreviewProvider : PreviewParameterProvider<MessagePartUiModel.Table> {
    override val values: Sequence<MessagePartUiModel.Table>
        get() = sequenceOf(
            productComparison(),
            deliveryOptions()
        )
}

private fun productComparison() = MessagePartUiModel.Table(
    caption = null,
    headers = listOf(
        imageCell(image = "dunk-low-metro", alt = "Nike Dunk Low Metro"),
        imageCell(image = "dunk-low-panda", alt = "Nike Dunk Low Panda")
    ),
    rows = listOf(
        labelledRow(label = "NIKE", first = "Dunk Low Metro", second = "Dunk Low Panda"),
        labelledRow(label = "Price", first = "749.00 SEK", second = "1 149.00 SEK"),
        labelledRow(label = "Color", first = "Yellow", second = "Black/White"),
        labelledRow(label = "Material", first = "100% rubber", second = "100% leather")
    )
)

private fun deliveryOptions() = MessagePartUiModel.Table(
    caption = "Delivery options",
    headers = listOf(
        textCell(text = "Carrier"),
        textCell(text = "Price", alignment = TableColumnAlignmentUiModel.RIGHT),
        textCell(text = "Days", alignment = TableColumnAlignmentUiModel.CENTER)
    ),
    rows = listOf(
        listOf(
            textCell(text = "PostNord"),
            textCell(text = "49.00 SEK", alignment = TableColumnAlignmentUiModel.RIGHT),
            textCell(text = "2-4", alignment = TableColumnAlignmentUiModel.CENTER)
        ),
        listOf(textCell(text = "Instabox"))
    )
)

private fun labelledRow(
    label: String,
    first: String,
    second: String
) = listOf(
    TableCellUiModel(
        blocks = listOf(
            paragraph(text = label, type = RichTextSpanTypeUiModel.BOLD),
            paragraph(text = first)
        ),
        alignment = TableColumnAlignmentUiModel.LEFT
    ),
    TableCellUiModel(
        blocks = listOf(
            paragraph(text = label, type = RichTextSpanTypeUiModel.BOLD),
            paragraph(text = second)
        ),
        alignment = TableColumnAlignmentUiModel.LEFT
    )
)

private fun textCell(
    text: String,
    alignment: TableColumnAlignmentUiModel = TableColumnAlignmentUiModel.LEFT
) = TableCellUiModel(
    blocks = listOf(paragraph(text = text)),
    alignment = alignment
)

private fun imageCell(
    image: String,
    alt: String
) = TableCellUiModel(
    blocks = listOf(
        TableCellBlockUiModel.Image(
            url = "https://example.com/$image.jpg",
            alt = alt
        )
    ),
    alignment = TableColumnAlignmentUiModel.LEFT
)

private fun paragraph(
    text: String,
    type: RichTextSpanTypeUiModel = RichTextSpanTypeUiModel.TEXT
) = TableCellBlockUiModel.Text(
    content = MessagePartUiModel.RichText(
        blocks = listOf(
            RichTextBlockUiModel.Paragraph(spans = listOf(RichTextSpanUiModel(text = text, type = type)))
        )
    )
)
