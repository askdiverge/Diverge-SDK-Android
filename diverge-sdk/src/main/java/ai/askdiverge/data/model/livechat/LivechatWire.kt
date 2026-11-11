package ai.askdiverge.data.model.livechat

/**
 * Query item and body literals for the visitor livechat endpoints. Tests pin these strings.
 *
 * [API ref](https://docs.askdiverge.ai/api#tag/visitor-livechat)
 */
internal object LivechatWire {

    /** `GET /livechat/sync` — highest message sequence already applied. */
    const val AFTER_SEQUENCE_NUMBER = "after_sequence_number"

    /** `GET /livechat/sync` — how long the server may hold for a change (ms). */
    const val WAIT_MS = "wait_ms"

    /** `GET /livechat/sync` — change hint from the previous response. */
    const val SYNC_CURSOR = "sync_cursor"

}

