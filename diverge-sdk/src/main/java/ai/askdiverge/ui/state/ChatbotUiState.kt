package ai.askdiverge.ui.state

import androidx.compose.runtime.Immutable
import ai.askdiverge.domain.model.banner.ChatBanner
import ai.askdiverge.domain.model.config.theme.Theme
import ai.askdiverge.ui.attachment.PendingComposerAttachment
import ai.askdiverge.ui.form.FormSubmissionModel
import ai.askdiverge.ui.livechat.LivechatRatingModel
import ai.askdiverge.ui.model.ChatBubbleUiModel

@Immutable
internal data class ChatbotUiState(
    val session: SessionState = SessionState(),
    val pendingBotMessage: PendingBotMessageState = PendingBotMessageState.None,
    val conversation: List<ChatBubbleUiModel> = emptyList(),
    /** Drafts keyed by form `partId`, so recycling a list item keeps the visitor's answers. */
    val formModels: Map<String, FormSubmissionModel> = emptyMap(),
    /** True while a form or composer photo is JPEG-encoding off the main thread. */
    val isEncodingAttachment: Boolean = false,
    /** Composer photo chips waiting to send with the next message. */
    val pendingAttachments: List<PendingComposerAttachment> = emptyList(),
    /** Ids of conversation entries a reloaded history will show; dropped once its newest page loads. */
    val conversationIdsAwaitingHistory: Set<String> = emptySet(),
    val lastMessageId: String? = null,
    val messageToScrollTo: String? = null,
    val inputMessage: String = "",
    val activeSheet: SheetType? = null,
    val isDeletingChatData: Boolean = false,
    /** True while `GET /chat/export` is in flight from Privacy & Data. */
    val isExportingChatData: Boolean = false,
    /** The message under Download my data after the last export; null when there is none. */
    val privacyExportNotice: PrivacyExportNotice? = null,
    val isResettingConversation: Boolean = false,
    val snackbarState: SnackbarState? = null,
    val theme: Theme? = null,
    val livechat: LivechatUiState = LivechatUiState(),
    /** Conversation close-rating draft; null when no prompt. Separate from [LivechatUiState.rating]. */
    val conversationRating: LivechatRatingModel? = null,
    /** In-chat promo banners from `GET /chat/banners` (empty when none / soft-fail). */
    val banners: List<ChatBanner> = emptyList(),
    /** Content fingerprints dismissed for this ViewModel lifetime. */
    val dismissedBannerFingerprints: Set<String> = emptySet()
) {
    /** Banners still visible after in-memory dismiss. */
    val visibleBanners: List<ChatBanner>
        get() = banners.filter { it.contentFingerprint !in dismissedBannerFingerprints }

    /**
     * Whether a livechat handover or close can start: the session is ready, not being reset or
     * deleted, and no other handover or close is running.
     */
    val canRunLivechatAction: Boolean
        get() = session.connectionState is ChatConnectionState.Ready &&
            !isResettingConversation &&
            !isDeletingChatData &&
            !livechat.isBusy

    /**
     * Whether an assistant's `request_human_agent` card can be accepted, unless the visitor declined
     * that card: no livechat session is open and no reply is in flight.
     */
    val canRequestHumanAgent: Boolean
        get() = canRunLivechatAction &&
            !livechat.status.isInSession &&
            pendingBotMessage == PendingBotMessageState.None
}
