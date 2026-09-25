package ai.askdiverge.domain.model.message.incoming.part

import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBlock

internal data class RichTextPart(
    val id: String,
    val blocks: List<RichTextBlock>
) : MessagePart
