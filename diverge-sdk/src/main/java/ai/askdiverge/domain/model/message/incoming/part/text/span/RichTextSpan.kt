package ai.askdiverge.domain.model.message.incoming.part.text.span

internal data class RichTextSpan(
    val text: String,
    val type: RichTextSpanType,
    val url: String? = null
)
