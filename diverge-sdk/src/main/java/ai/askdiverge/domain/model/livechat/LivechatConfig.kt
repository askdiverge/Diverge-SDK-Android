package ai.askdiverge.domain.model.livechat

/**
 * Livechat block from `GET /api/v1/chat/config`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatconfig)
 */
internal data class LivechatConfig(
    /** Whether the visitor can request a human agent right now. */
    val enabled: Boolean,
    /** Whether livechat is configured for this chatbot at all. */
    val configured: Boolean,
    val availabilityStatus: AvailabilityStatus,
    val availabilityReason: AvailabilityReason,
    /** Whether the header livechat control should render when idle. */
    val showLivechatLogo: Boolean,
    /** Whether participants may send attachments during an active session. */
    val attachmentsEnabled: Boolean,
    /** Max attachment size in decoded bytes for an active-session upload. */
    val maxAttachmentSizeBytes: Int
) {

    /** Livechat is on and currently accepting handovers. */
    val isAvailable: Boolean
        get() = enabled && availabilityStatus == AvailabilityStatus.LIVE

    /**
     * Whether a visitor can hand over to a live agent right now. The backend decides it from the
     * chatbot's livechat settings, working hours and manual overrides; the client offers the
     * handover control only while it is [AvailabilityStatus.LIVE].
     *
     * [API ref](https://docs.askdiverge.ai/api#model/livechatavailabilitystatus)
     */
    enum class AvailabilityStatus {
        LIVE,
        OFFLINE,
        UNKNOWN
    }

    /**
     * Why the backend set [availabilityStatus]. The client picks its unavailable copy from it.
     *
     * [API ref](https://docs.askdiverge.ai/api#model/livechatavailabilityreason)
     */
    enum class AvailabilityReason {
        DISABLED,
        MANUAL_LIVE,
        MANUAL_OFFLINE,
        ALWAYS_ON,
        WITHIN_WORKING_HOURS,
        OUTSIDE_WORKING_HOURS,
        UNKNOWN
    }

    companion object {
        /** Default attachment size the TypeSpec documents when the block omits the field (5 MiB). */
        const val DEFAULT_MAX_ATTACHMENT_SIZE_BYTES = 5_242_880

        /**
         * Every field at its server default, as a deployment without a livechat block describes
         * it. Mapping fills an absent field from here too.
         */
        val serverDefaults = LivechatConfig(
            enabled = false,
            configured = false,
            availabilityStatus = AvailabilityStatus.OFFLINE,
            availabilityReason = AvailabilityReason.DISABLED,
            showLivechatLogo = true,
            attachmentsEnabled = false,
            maxAttachmentSizeBytes = DEFAULT_MAX_ATTACHMENT_SIZE_BYTES
        )
    }
}
