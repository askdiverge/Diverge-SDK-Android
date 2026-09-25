package ai.askdiverge

/**
 * Host-app hooks the chatbot calls into. Implement this once and pass it to
 * [ChatbotScreen][ai.askdiverge.ui.compose.ChatbotScreen] or
 * [ChatbotBottomSheet][ai.askdiverge.ui.compose.ChatbotBottomSheet].
 *
 * A chat keeps the instance it was first composed with for as long as its `ViewModelStoreOwner`
 * (the activity, fragment or navigation entry it is composed in) lives; an instance passed on a
 * later recomposition is not used.
 */
interface ChatbotCallbacks {
    /**
     * Returns the user's session token, issuing one on the host backend if there is none yet.
     *
     * The chat calls it whenever it needs a token and holds none: when it first connects, when the
     * user retries after this call failed or after Diverge rejected the token
     * ([ChatbotException.SessionExpired][ai.askdiverge.domain.exception.ChatbotException.SessionExpired]),
     * and after the user's chat data is deleted. It keeps the token in memory and sends it with every
     * call to the Diverge API until the chat replaces it. On failure the chat shows its retry screen.
     *
     * Called on the main thread; move network and disk work off it. It can be called again before an
     * earlier call returns, for example by a second chat sharing this instance. If the chat's
     * `ViewModelStoreOwner` is destroyed first, the call is cancelled and the token it would have
     * returned is not used.
     */
    suspend fun fetchToken(): Result<String>

    /**
     * Issues a fresh session token on the host backend, replacing the current session.
     * The chatbot adopts the returned token and reconnects with it, so the conversation restarts empty.
     *
     * The chat calls it when the user resets the conversation. On failure the chat keeps the current
     * session and conversation and tells the user the reset failed.
     *
     * Called on the main thread; move network and disk work off it. It can run while another call on
     * this instance is still running, for example one from a second chat sharing it. If the chat's
     * `ViewModelStoreOwner` is destroyed first, the call is cancelled and the token it would have
     * returned is not used.
     */
    suspend fun createNewToken(): Result<String>

    /**
     * Deletes the user's chat data on the host backend.
     *
     * The chat calls it when the user confirms deleting their chat data. On success the chat discards
     * its token, starts a new session through [fetchToken] and shows an empty conversation; on
     * failure it keeps the session and conversation and tells the user the deletion failed.
     *
     * Called on the main thread; move network and disk work off it. It can run while another call on
     * this instance is still running, for example one from a second chat sharing it. If the chat's
     * `ViewModelStoreOwner` is destroyed first, the call is cancelled and the chat does not start a
     * new session.
     */
    suspend fun deleteChatData(): Result<Unit>
}
