package ai.askdiverge.domain.model.form

/**
 * Display-oriented form — the three wire markers collapse here so the UI and the submit builder
 * each switch on one enum instead of three near-identical paths.
 *
 * `submit_actions` / `submit_rules` stay on the server; the client only echoes [partId].
 */
internal data class ConversationForm(
    val partId: String,
    val kind: Kind,
    val fields: List<FormField>
) {

    internal sealed interface Kind {
        data object Contact : Kind

        data class SupportTicket(
            val attachmentsAccepted: Boolean,
            val maxAttachmentSizeBytes: Int
        ) : Kind

        data class Custom(
            val formId: String,
            val name: String?,
            val confirmationText: String?,
            val minFilledFields: Int
        ) : Kind
    }

    /**
     * Title shown above the fields — a custom form's name, or its `form_id` when it has none, as
     * the contract displays it; `null` for contact and support-ticket forms.
     */
    val displayName: String?
        get() = (kind as? Kind.Custom)?.let { it.name ?: it.formId }

    /** Optional confirmation the server may return; custom forms may also carry one inline. */
    val confirmationText: String?
        get() = (kind as? Kind.Custom)?.confirmationText

    /** Minimum filled visible fields required before submit — only custom forms set this. */
    val minFilledFields: Int
        get() = (kind as? Kind.Custom)?.minFilledFields ?: 0

    companion object {

        /** The marker field keys a `support_ticket` action carries. */
        val supportTicketFieldKeys: Set<String> = setOf("name", "email", "message")

        fun contact(marker: ShowContactForm) = ConversationForm(
            partId = marker.partId,
            kind = Kind.Contact,
            fields = marker.fields
        )

        /**
         * Normalises a support-ticket marker to the fields the action carries. `null` when the
         * marker lacks one of them; the turn then renders without the card.
         */
        fun supportTicket(marker: ShowSupportTicket): ConversationForm? {
            val fields = marker.fields.filter { it.key in supportTicketFieldKeys }
            if (fields.map { it.key }.toSet() != supportTicketFieldKeys) return null
            return ConversationForm(
                partId = marker.partId,
                kind = Kind.SupportTicket(
                    attachmentsAccepted = marker.attachmentsAccepted,
                    maxAttachmentSizeBytes = marker.maxAttachmentSizeBytes
                ),
                fields = fields
            )
        }

        fun custom(marker: ShowForm) = ConversationForm(
            partId = marker.partId,
            kind = Kind.Custom(
                formId = marker.formId,
                name = marker.name,
                confirmationText = marker.confirmationText,
                minFilledFields = marker.minFilledFields
            ),
            fields = marker.fields
        )
    }
}
