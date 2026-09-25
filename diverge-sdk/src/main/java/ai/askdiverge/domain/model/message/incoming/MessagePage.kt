package ai.askdiverge.domain.model.message.incoming

internal data class MessagePage(
    val hasMore: Boolean,
    val messages: List<Message>,
    val nextCursor: String?
)
