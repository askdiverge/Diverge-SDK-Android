package ai.askdiverge.domain.model.form

/**
 * Request body for `POST /api/v1/chat/actions`. Field-keyed dictionaries keep the keys the form
 * defines.
 *
 * [API ref](https://docs.askdiverge.ai/api#tag/visitor-actions/POST/api/v1/chat/actions)
 */
internal data class SubmitActionRequest(
    /** The `part_id` of the marker that opened the form. */
    val partId: String,
    /**
     * The host's description of the screen the visitor submitted from, forwarded verbatim as
     * `current_page_url` for the handover context. `null` when the host supplies none.
     */
    val currentPage: String?,
    /** What the visitor submitted. */
    val action: SubmitAction
)
