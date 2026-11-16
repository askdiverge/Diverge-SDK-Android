package ai.askdiverge

/**
 * Whether the chat may ask for a conversation rating when the visitor dismisses the sheet after
 * sending at least one message. The SDK owns the prompt and `POST /chat/rate`; the host only
 * chooses whether that close gate runs.
 *
 * Defaults to [ENABLED] on [ai.askdiverge.ui.compose.ChatbotBottomSheet]. Pass [DISABLED] when the
 * host must dismiss immediately without offering a rating.
 */
enum class ChatbotRating {

    /**
     * No conversation-rating prompt on dismiss; the sheet's `onDismiss` runs as soon as the sheet
     * is dismissed.
     */
    DISABLED,

    /**
     * After the visitor has sent a message this session, dismissing the sheet may show a 1–5
     * rating (optional comment for 1–3) before `onDismiss`. Livechat CSAT still owns the close when
     * that prompt applies.
     */
    ENABLED
}
