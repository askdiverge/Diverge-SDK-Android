package ai.askdiverge.ui.model.message.part.text

internal data class RichTextSpanUiModel(
    val text: String,
    val type: RichTextSpanTypeUiModel,
    val url: String? = null
)
