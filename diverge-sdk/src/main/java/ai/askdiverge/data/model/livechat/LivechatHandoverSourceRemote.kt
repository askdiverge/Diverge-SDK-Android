package ai.askdiverge.data.model.livechat

/**
 * Wire `source` values on `POST /livechat/handover`: what made the visitor ask for an agent. The
 * backend records it with the session for reporting. A value is added when the API adds a source.
 */
internal object LivechatHandoverSourceRemote {
    /** The visitor tapped the header livechat control. */
    const val MANUAL_BUTTON = "manual_button"

    /** The visitor accepted a `request_human_agent` marker in an assistant reply. */
    const val ASSISTANT_MARKER = "assistant_marker"
}
