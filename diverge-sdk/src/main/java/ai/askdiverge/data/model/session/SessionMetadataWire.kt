package ai.askdiverge.data.model.session

/**
 * Path and body literals for `PATCH /api/v1/chat/session` (`Session.patchSession` in the Chatbot
 * API TypeSpec), the visitor's own counterpart of the API-key `PATCH /session/metadata`. Tests pin
 * these strings.
 */
internal object SessionMetadataWire {

    /**
     * Retrofit path relative to the API base (`…/api/`). The authenticated visitor session; the
     * session comes from the bearer token. A new path needs a backend change first.
     */
    const val PATH = "v1/chat/session"

    /**
     * Body field the host's `setMetadata` values go in. The backend deep-merges it into the
     * session's untrusted `metadata`. Omitted from a body that carries a signed token.
     */
    const val METADATA = "metadata"

    /**
     * Body field for the JWT the host's backend signed. The backend verifies it against the
     * chatbot's public key and merges its claims into `verified_metadata`. Omitted from a body that
     * carries metadata.
     */
    const val SIGNED_METADATA = "signed_metadata"
}
