package ai.askdiverge.ui.compose.components.message.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.form.FormBannerError
import ai.askdiverge.ui.form.FormFieldError
import ai.askdiverge.ui.form.FormSubmissionModel

@Composable
internal fun formTitle(form: ConversationForm): String {
    val name = form.displayName
    if (!name.isNullOrEmpty()) return name
    return when (form.kind) {
        ConversationForm.Kind.Contact,
        is ConversationForm.Kind.Custom -> stringResource(R.string.chat_form_contact_title)
        is ConversationForm.Kind.SupportTicket -> stringResource(R.string.chat_form_ticket_title)
    }
}

@Composable
internal fun formFieldErrorText(error: FormFieldError): String = when (error) {
    FormFieldError.REQUIRED -> stringResource(R.string.chat_form_required)
    FormFieldError.INVALID_EMAIL -> stringResource(R.string.chat_form_invalid_email)
    FormFieldError.REJECTED -> stringResource(R.string.chat_form_field_rejected)
}

@Composable
internal fun formBannerErrorText(error: FormBannerError): String = when (error) {
    is FormBannerError.MinFilled -> pluralStringResource(
        R.plurals.chat_form_min_filled,
        error.count,
        error.count
    )
    FormBannerError.Unavailable -> stringResource(R.string.chat_form_unavailable)
    FormBannerError.SubmitFailed -> stringResource(R.string.chat_form_submit_failed)
    FormBannerError.LoadFailed -> stringResource(R.string.chat_form_load_failed)
    FormBannerError.PhotoFailed -> stringResource(R.string.chat_form_photo_failed)
}

@Composable
internal fun formConfirmationText(raw: String): String = when {
    raw == FormSubmissionModel.ALREADY_SENT_TOKEN ->
        stringResource(R.string.chat_form_already_sent)
    raw.isBlank() -> stringResource(R.string.chat_form_submitted_default)
    else -> raw
}
