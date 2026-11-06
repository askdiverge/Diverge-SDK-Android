package ai.askdiverge.domain.model.form

/**
 * Success body for `POST /api/v1/chat/actions`.
 *
 * [API ref](https://docs.askdiverge.ai/api#tag/visitor-actions/POST/api/v1/chat/actions)
 */
internal data class SubmitActionResponse(
    /** The backend's identifier for the stored submission. */
    val submissionId: String,
    /**
     * Copy shown to the visitor after the submission. `null` when the form sets none; the SDK then
     * shows its own confirmation copy.
     */
    val confirmationText: String?
)
