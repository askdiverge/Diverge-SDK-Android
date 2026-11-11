package ai.askdiverge

/**
 * Whether the chat may open the system photo picker for form file fields and support-ticket
 * attachments. The SDK owns the picker and encoding; the host only chooses whether photos are
 * offered.
 *
 * Defaults to [PHOTO_LIBRARY] on [ai.askdiverge.ui.compose.ChatbotBottomSheet]. Pass [DISABLED]
 * when the host must not access the visitor's photos.
 */
enum class ChatbotAttachments {

    /** No photo controls on forms; required file fields make the form unavailable. */
    DISABLED,

    /**
     * The visitor can attach a photo from the device library (Android Photo Picker). No storage
     * permission is required for the one-shot picker the SDK uses.
     */
    PHOTO_LIBRARY
}
