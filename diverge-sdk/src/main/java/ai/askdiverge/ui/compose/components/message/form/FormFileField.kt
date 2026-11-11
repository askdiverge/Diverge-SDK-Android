package ai.askdiverge.ui.compose.components.message.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.form.PendingFormAttachment

/**
 * A file-field control: an "Add photo" button, or a chip with remove once picked. When
 * [offersAttachments] is false, shows the attachments-disabled copy instead.
 */
@Composable
internal fun FormFileField(
    fieldLabel: String?,
    offersAttachments: Boolean,
    pickedFile: PendingFormAttachment?,
    isEnabled: Boolean,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!offersAttachments) {
        Text(
            text = stringResource(R.string.chat_form_attachments_disabled),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.mutedText,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    if (pickedFile != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = pickedFile.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.assistantMessageText,
                modifier = Modifier.weight(1f)
            )
            val removeLabel = stringResource(R.string.chat_form_remove_photo)
            TextButton(
                onClick = onRemoveFile,
                enabled = isEnabled,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = accessibilityLabel(removeLabel, fieldLabel)
                    }
            ) {
                Text(stringResource(R.string.chat_form_remove_photo))
            }
        }
    } else {
        val addLabel = stringResource(R.string.chat_form_add_photo)
        TextButton(
            onClick = onPickFile,
            enabled = isEnabled,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics {
                    contentDescription = accessibilityLabel(addLabel, fieldLabel)
                }
        ) {
            Text(addLabel)
        }
    }
}

internal fun accessibilityLabel(
    action: String,
    fieldLabel: String?
): String = if (fieldLabel.isNullOrBlank()) action else "$fieldLabel: $action"
