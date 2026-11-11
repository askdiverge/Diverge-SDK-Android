package ai.askdiverge.domain.model.livechat

/**
 * Body for `POST /api/v1/chat/livechat/handover`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechathandoverrequest)
 */
internal data class LivechatHandoverRequest(
    val source: Source,
    /** The `request_human_agent` marker that prompted the handover, if one did. */
    val partId: String?,
    val clientContext: LivechatClientContext?
) {
    /** Always [NATIVE_PLATFORM] for this SDK. */
    val platform: String get() = NATIVE_PLATFORM

    /** What triggered the handover. */
    enum class Source {
        /** The visitor tapped the handover control. */
        MANUAL_BUTTON,

        /** The visitor accepted a `request_human_agent` marker. */
        ASSISTANT_MARKER
    }

    companion object {
        /**
         * `platform` wire value. Native Android reports as a mobile app; the backend records it
         * beside `mobile_web` and `website`.
         */
        const val NATIVE_PLATFORM = "mobile_app"
    }
}
