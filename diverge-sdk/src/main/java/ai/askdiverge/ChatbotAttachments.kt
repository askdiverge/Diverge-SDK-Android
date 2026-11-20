package ai.askdiverge

/**
 * Whether the chat may open the system photo picker: in the composer, where the visitor sends
 * photos to the assistant or to a live agent, on the card shown when the assistant asks for a
 * photo, and for form file fields and support-ticket attachments. The SDK owns the picker and
 * encoding; the host only chooses whether photos are offered.
 *
 * Defaults to [DISABLED] on [ai.askdiverge.ui.compose.ChatbotBottomSheet]. Pass [PHOTO_LIBRARY]
 * to offer photo attach.
 */
enum class ChatbotAttachments {

    /**
     * No photo controls in the composer or on forms; required file fields make the form
     * unavailable, and the assistant's request for a photo shows no card.
     */
    DISABLED,

    /**
     * The visitor can attach a photo from the device library (Android Photo Picker). No storage
     * permission is required for the one-shot picker the SDK uses.
     */
    PHOTO_LIBRARY
}
