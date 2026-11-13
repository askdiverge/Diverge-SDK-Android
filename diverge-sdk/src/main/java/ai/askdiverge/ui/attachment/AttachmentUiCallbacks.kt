package ai.askdiverge.ui.attachment

/**
 * Attachment open actions and the `request_image_upload` Add-photo CTA the conversation list
 * needs.
 */
internal data class AttachmentUiCallbacks(
    val onOpen: (
        displayUrl: String,
        attachmentId: String?,
        urlExpiresAt: String?
    ) -> Unit = { _, _, _ -> },
    /** Whether the Add-photo CTA on an upload prompt is currently usable. */
    val canAddPromptPhoto: () -> Boolean = { false },
    /**
     * Visitor tapped Add photo on a `request_image_upload` card. The host of the list launches the
     * shared Photo Picker when this returns after the ViewModel has armed the marker budget.
     */
    val onAddPromptPhoto: (
        acceptedTypes: List<String>,
        maxSizeBytes: Int
    ) -> Unit = { _, _ -> }
) {
    companion object {
        val None = AttachmentUiCallbacks()
    }
}
