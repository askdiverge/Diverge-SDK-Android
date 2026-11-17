package ai.askdiverge

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Host-app hooks the chatbot calls into. Implement this once and pass it to
 * ChatbotViewModel.factory().
 */
interface ChatbotCallbacks {
    /** Returns the visitor's session token, issuing one on the host backend if there is none yet. */
    suspend fun fetchToken(): Result<String>

    /**
     * Issues a fresh session token on the host backend, replacing the current session.
     * The chatbot adopts the returned token and reconnects with it, so the conversation restarts empty.
     */
    suspend fun createNewToken(): Result<String>

    /** Deletes the visitor's chat data on the host backend. */
    suspend fun deleteChatData(): Result<Unit>

    /**
     * Emits when the token [fetchToken] returns may have changed, such as after a login or logout.
     * The chatbot drops its current token and fetches it again; unlike [createNewToken], nothing is reset on the backend.
     */
    fun sessionInvalidations(): Flow<Unit> = emptyFlow()

    /**
     * Called on the main thread when the livechat status or the connected agent's name changes,
     * while the chat sheet is on screen and started. Use it for host badges or to offer reopening a
     * chat the visitor closed mid-session. Default is a no-op so hosts that do not need it leave
     * this unimplemented.
     *
     * After the sheet is dismissed, the last [info] delivered is the session as it stood then; the
     * next open delivers the current one. The poller stays parked while the sheet is hidden.
     */
    fun onLivechatSessionChange(info: LivechatSessionInfo) = Unit

}
