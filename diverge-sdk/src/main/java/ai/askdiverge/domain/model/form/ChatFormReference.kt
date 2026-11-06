package ai.askdiverge.domain.model.form

/**
 * Lightweight form row from `GET /api/v1/chat/config` → `forms[]`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatformreference)
 */
internal data class ChatFormReference(
    val formId: String,
    /** Form name rendered to the visitor, trimmed. `null` when [formId] is the display label. */
    val name: String?,
    val trigger: Trigger
) {

    /**
     * The moment in the conversation the form belongs to. A case is added when the backend
     * introduces a new moment and the SDK ships UI for it.
     */
    internal enum class Trigger {
        SESSION_START,
        LIVECHAT_WAITING,
        LLM,
        UNKNOWN
    }
}
