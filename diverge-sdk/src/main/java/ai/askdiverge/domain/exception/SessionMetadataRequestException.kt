package ai.askdiverge.domain.exception

/**
 * A `PATCH /chat/session` the backend answered with [status], outside 2xx. The status is passed on
 * unmapped, so a 401 here neither ends the chat nor asks the host for a token; the session metadata
 * queue decides from it whether the patch goes again.
 */
internal class SessionMetadataRequestException(val status: Int) :
    Exception("Session metadata patch failed with HTTP $status")
