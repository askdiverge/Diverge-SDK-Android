package ai.askdiverge.ui.compose.components.message.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.form.FormSubmissionModel
import ai.askdiverge.ui.form.FormValidator

/**
 * In-conversation form card for contact / support-ticket / custom markers.
 */
@Composable
internal fun ConversationFormCard(
    form: ConversationForm,
    model: FormSubmissionModel?,
    isEditable: Boolean,
    isBusy: Boolean,
    onEnsure: (ConversationForm) -> Unit,
    onValueChange: (partId: String, key: String, value: String) -> Unit,
    onSubmit: (partId: String) -> Unit,
    onReload: (partId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(form.partId) { onEnsure(form) }
    val draft = model ?: return

    when (val phase = draft.phase) {
        is FormSubmissionModel.Phase.Submitted -> ConfirmationCard(
            text = formConfirmationText(phase.confirmation),
            modifier = modifier
        )
        else -> EditingCard(
            draft = draft,
            isEditable = isEditable,
            isBusy = isBusy,
            onValueChange = onValueChange,
            onSubmit = onSubmit,
            onReload = onReload,
            modifier = modifier
        )
    }
}

@Composable
private fun ConfirmationCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.assistantBubbleBorder)
            .background(MaterialTheme.colorScheme.assistantBubbleBackground)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "✓",
            color = MaterialTheme.colorScheme.chatbotPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.assistantMessageText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditingCard(
    draft: FormSubmissionModel,
    isEditable: Boolean,
    isBusy: Boolean,
    onValueChange: (partId: String, key: String, value: String) -> Unit,
    onSubmit: (partId: String) -> Unit,
    onReload: (partId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = formTitle(draft.form)
    val controlsEnabled = isEditable && !draft.isSubmitting && !isBusy
    val visible = FormValidator.visibleFields(draft.form, draft.values)
    val banner = draft.formError
        ?: (draft.phase as? FormSubmissionModel.Phase.Failed)?.message

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.assistantBubbleBorder)
            .background(MaterialTheme.colorScheme.assistantBubbleBackground)
            .padding(12.dp)
            .semantics {
                contentDescription = title
                heading()
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.assistantMessageText
        )

        when {
            draft.isHydrating -> CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally),
                strokeWidth = 2.dp
            )
            draft.hydrationFailed -> LoadFailedRow(
                isEditable = isEditable,
                onReload = { onReload(draft.form.partId) }
            )
            !draft.isCompletable -> Text(
                text = stringResource(R.string.chat_form_unavailable),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.mutedText
            )
        }

        visible.forEach { field ->
            FormFieldControl(
                field = field,
                value = draft.values[field.key].orEmpty(),
                error = draft.fieldErrors[field.key],
                isEnabled = controlsEnabled,
                onValueChange = { value ->
                    onValueChange(draft.form.partId, field.key, value)
                }
            )
        }

        if (banner != null) {
            Text(
                text = formBannerErrorText(banner),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isEditable) {
            // A form with nothing to fill in, or one that cannot be completed here, has no Submit.
            if (draft.hasFields && draft.isCompletable) {
                Button(
                    onClick = { onSubmit(draft.form.partId) },
                    enabled = controlsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (draft.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.chat_form_submit))
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.chat_form_read_only),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.mutedText
            )
        }
    }
}

@Composable
private fun LoadFailedRow(
    isEditable: Boolean,
    onReload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.chat_form_load_failed),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onReload, enabled = isEditable) {
            Text(stringResource(R.string.chat_initialization_error_retry))
        }
    }
}
