package ai.askdiverge.domain.model.event

/**
 * A recoverable error emitted mid-stream, e.g. when generation fails.
 *
 * @property message Localized message describing the error, meant for direct display.
 * @property retryable Whether the caller can recover by resending the last user message.
 */
internal data class StreamErrorEvent(
    val message: String,
    val retryable: Boolean
) : StreamEvent
