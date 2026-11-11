package ai.askdiverge.domain.livechat

/** What [LivechatSession.bootstrap] found. */
internal enum class LivechatBootstrapOutcome {
    /** Queued or talking to an agent; polling has started. */
    IN_SESSION,

    /** No open session, or its state couldn't be read yet; polling may retry the read. */
    NOT_IN_SESSION,

    /** The visitor session ended (401). */
    SESSION_EXPIRED
}
