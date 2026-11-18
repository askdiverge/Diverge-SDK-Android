package ai.askdiverge.ui.attachment

/**
 * Attachment open actions the conversation list needs for file and image parts.
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
    ) -> Unit = { _, _, _ -> }
) {
    companion object {
        val None = AttachmentUiCallbacks()
    }
}
