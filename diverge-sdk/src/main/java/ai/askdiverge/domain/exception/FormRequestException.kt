package ai.askdiverge.domain.exception

/**
 * Failures of the form calls, `POST /actions` and `GET /forms/{form_id}`. Messages are fixed SDK
 * copy, so logging one never carries what the visitor typed.
 */
internal sealed class FormRequestException(message: String) : Exception(message) {

    /** A 409: the action conflicts with server state, for example a form already submitted. */
    class Conflict : FormRequestException(message = "Action conflicts with server state")

    /** A 404: the backend has no form or part with that id. */
    class NotFound : FormRequestException(message = "Resource not found")

    /**
     * The request body was rejected: a 422, any status whose error envelope lists [params], or an
     * action the SDK refused to send because the contract rejects it (no [params]).
     */
    class Validation(val params: List<Param>) : FormRequestException(message = "Request failed validation") {

        /**
         * A single field-level validation failure from `error.params`.
         *
         * @property field path of the invalid value in the request body, as the API reports it
         *   (for example `action.fields.email`).
         * @property message human-readable description of what is wrong with the value.
         */
        data class Param(
            val field: String,
            val message: String
        )
    }
}
