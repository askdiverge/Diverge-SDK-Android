package ai.askdiverge

/**
 * Whether the chat may open the system photo picker: in the composer, where the visitor sends
 * photos to the assistant or to a live agent, and for form file fields and support-ticket
 * attachments. The SDK owns the picker and encoding; the host only chooses whether photos are
 * offered.
 *
 * Defaults to [PHOTO_LIBRARY] on [ai.askdiverge.ui.compose.ChatbotBottomSheet], so the composer
 * offers photos unless the host passes [DISABLED]. Pass [DISABLED] when the host must not access
 * the visitor's photos.
 */
enum class ChatbotAttachments {

    /**
     * No photo controls in the composer or on forms; required file fields make the form
     * unavailable.
     */
    DISABLED,

    /**
     * The visitor can attach a photo from the device library (Android Photo Picker). No storage
     * permission is required for the one-shot picker the SDK uses.
     */
    PHOTO_LIBRARY
}
