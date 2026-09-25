package ai.askdiverge.ui.model.message.part.text

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class RichTextBlockUiModel {
    data class Paragraph(val spans: List<RichTextSpanUiModel>) : RichTextBlockUiModel()
    data class BulletList(val items: List<BulletListItemUiModel>) : RichTextBlockUiModel()
}
