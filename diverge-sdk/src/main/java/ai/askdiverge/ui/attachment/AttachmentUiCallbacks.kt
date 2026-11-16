package ai.askdiverge.ui.attachment

/**
 * Attachment open actions and the `request_image_upload` Add-photo CTA the conversation list
 * needs.
 */
internal data class AttachmentUiCallbacks(
    /**
     * A freshly signed link for the attachment with this id, or null when there is none. An image
     * asks for one instead of loading a signed link that is past its expiry or fails to load.
     */
    val freshUrl: suspend (attachmentId: String) -> String? = { null },
    val onOpen: (
        displayUrl: String,
        attachmentId: String?,
        urlExpiresAt: String?
    ) -> Unit = { _, _, _ -> },
    /** Whether the Add-photo CTA on an upload prompt is currently usable. */
    val canAddPromptPhoto: Boolean = false,
    /**
     * Visitor tapped Add photo on a `request_image_upload` card. The host of the list launches the
     * shared Photo Picker when this returns after the ViewModel has armed the marker budget.
     */
    val onAddPromptPhoto: (maxSizeBytes: Int) -> Unit = {}
) {
    companion object {
        val None = AttachmentUiCallbacks()
    }
}
