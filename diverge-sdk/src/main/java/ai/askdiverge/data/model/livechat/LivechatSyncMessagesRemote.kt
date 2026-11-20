package ai.askdiverge.data.model.livechat

/**
 * The `messages` array of a `GET /livechat/sync` page, decoded by `LivechatSyncMessagesAdapter`.
 */
internal data class LivechatSyncMessagesRemote(
    /** The messages that decoded; one that fails to decode is logged and left out. */
    val messages: List<LivechatMessageRemote>,
    /**
     * The highest `sequence_number` in the array, a message left out of [messages] included, so
     * the next read starts after it. `null` when the array is empty.
     */
    val lastSequenceNumber: Long?
)
