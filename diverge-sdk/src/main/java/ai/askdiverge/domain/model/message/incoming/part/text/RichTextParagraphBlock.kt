package ai.askdiverge.domain.model.message.incoming.part.text

import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan

internal data class RichTextParagraphBlock(val spans: List<RichTextSpan>) :
    RichTextBlock,
    TableCellBlock
