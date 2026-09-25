package ai.askdiverge.domain.model.config.theme.messages

internal data class AssistantMessageStyle(
    val backgroundColor: String,
    val textColor: String,
    val borderColor: String?,
    val thinkingBorderGradient: List<String>
)
