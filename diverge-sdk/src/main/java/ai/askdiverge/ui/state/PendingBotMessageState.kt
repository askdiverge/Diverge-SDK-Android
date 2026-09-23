package ai.askdiverge.ui.state

import ai.askdiverge.ui.model.message.part.MessagePartUiModel

/**
 * The reply the bot is producing, if there is one.
 *
 * A reply arrives in parts. Each finished part is committed to the conversation and dropped from
 * here, so [Streaming] holds only what has not been committed yet and is empty in the gap between
 * one part finishing and the next one starting.
 */
internal sealed class PendingBotMessageState {

    /** No reply in flight. */
    data object None : PendingBotMessageState()

    /** The message is sent and nothing has come back yet, so the typing bubble shows. */
    data object AwaitingFirstPart : PendingBotMessageState()

    /** The parts streamed so far, newest last. */
    data class Streaming(val parts: List<MessagePartUiModel>) : PendingBotMessageState()
}
