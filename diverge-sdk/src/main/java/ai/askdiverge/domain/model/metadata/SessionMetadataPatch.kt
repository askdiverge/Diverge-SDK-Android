package ai.askdiverge.domain.model.metadata

/** One change to the visitor's session metadata, sent as its own `PATCH /chat/session`. */
internal sealed interface SessionMetadataPatch {

    /** Values the backend deep-merges into the session's `metadata`. Never empty. */
    data class Metadata(val values: Map<String, JsonValue>) : SessionMetadataPatch

    /**
     * A JWT the host's backend signed, trimmed and never blank. The backend verifies it and merges
     * its claims into the session's `verified_metadata`.
     */
    data class Signed(val token: String) : SessionMetadataPatch
}
