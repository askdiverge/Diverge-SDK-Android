package ai.askdiverge.ui.attachment

/**
 * Attachment open actions the conversation list needs for file and image parts.
 */
internal data class AttachmentUiCallbacks(
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
