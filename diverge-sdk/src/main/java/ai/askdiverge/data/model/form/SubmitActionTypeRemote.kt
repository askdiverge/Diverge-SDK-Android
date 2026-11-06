package ai.askdiverge.data.model.form

/**
 * Wire `type` values of [SubmitActionRemote], fixed by the TypeSpec `ActionContent` union. The
 * backend routes each submission to the service for its type. An entry is added when the contract
 * adds an action.
 */
internal object SubmitActionTypeRemote {

    /** Sent for a submitted `show_contact_form`; the backend delivers the fields to the owner. */
    const val CONTACT_FORM = "contact_form"

    /** Sent for a submitted `show_support_ticket`; the backend creates a helpdesk ticket. */
    const val SUPPORT_TICKET = "support_ticket"

    /** Sent for a submitted `show_form`; the backend runs the form's configured actions. */
    const val FORM_SUBMISSION = "form_submission"
}
