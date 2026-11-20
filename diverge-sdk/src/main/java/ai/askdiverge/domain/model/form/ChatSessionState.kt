package ai.askdiverge.domain.model.form

/**
 * Materialized visitor session values from `GET /session` or a values PATCH.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatsessionstate)
 */
internal data class ChatSessionState(
    val values: List<ChatSessionValue>
)

/**
 * One session value. The waiting form uses [key] and [value] only.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatsessionvalue)
 */
internal data class ChatSessionValue(
    val key: String,
    val value: String
)
