package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.OutgoingAttachment

/**
 * A visitor-picked photo ready for a form file field or ticket attachment chip.
 *
 * Holds the wire [OutgoingAttachment] plus a short [displayName] for the UI; thumbnails are not
 * kept so drafts stay cheap to copy in [FormSubmissionModel].
 */
internal data class PendingFormAttachment(
    val attachment: OutgoingAttachment,
    val displayName: String
)
