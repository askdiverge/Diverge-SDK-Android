package ai.askdiverge

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
}
