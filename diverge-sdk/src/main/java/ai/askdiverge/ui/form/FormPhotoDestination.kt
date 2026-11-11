package ai.askdiverge.ui.form

/** Where a picked photo should land after encoding. */
internal sealed interface FormPhotoDestination {
    data class FormField(
        val partId: String,
        val key: String
    ) : FormPhotoDestination

    data class TicketAttachment(
        val partId: String
    ) : FormPhotoDestination
}
