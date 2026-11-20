package ai.askdiverge.ui.compose.components.message.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.inputBackground
import ai.askdiverge.ui.compose.theme.inputBorder
import ai.askdiverge.ui.compose.theme.inputPlaceholder
import ai.askdiverge.ui.compose.theme.inputText
import ai.askdiverge.ui.form.FormFieldError
import ai.askdiverge.ui.form.PendingFormAttachment

@Composable
internal fun FormFieldControl(
    field: FormField,
    value: String,
    error: FormFieldError?,
    announcesError: Boolean,
    isEnabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    offersAttachments: Boolean = false,
    pickedFile: PendingFormAttachment? = null,
    onPickFile: () -> Unit = {},
    onRemoveFile: () -> Unit = {}
) {
    val borderColor = if (error != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.inputBorder
    }
    val errorText = error?.let { formFieldErrorText(it) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.assistantMessageText
            )
            if (field.required) {
                val requiredLabel = stringResource(R.string.chat_form_required)
                Text(
                    text = "*",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = requiredLabel }
                )
            }
        }

        when (field.type) {
            FormField.FieldType.DROPDOWN -> FormDropdownField(
                label = field.label,
                options = field.options,
                required = field.required,
                selection = value,
                isEnabled = isEnabled,
                onSelect = onValueChange,
                errorText = errorText
            )

            FormField.FieldType.FILE -> FormFileField(
                fieldLabel = field.label,
                offersAttachments = offersAttachments,
                pickedFile = pickedFile,
                isEnabled = isEnabled,
                onPickFile = onPickFile,
                onRemoveFile = onRemoveFile,
                errorText = errorText
            )

            FormField.FieldType.TEXTAREA -> FormTextInput(
                field = field,
                value = value,
                isEnabled = isEnabled,
                borderColor = borderColor,
                errorText = errorText,
                singleLine = false,
                minLines = 3,
                onValueChange = onValueChange
            )

            FormField.FieldType.TEXT,
            FormField.FieldType.EMAIL,
            FormField.FieldType.TEL -> FormTextInput(
                field = field,
                value = value,
                isEnabled = isEnabled,
                borderColor = borderColor,
                errorText = errorText,
                singleLine = true,
                minLines = 1,
                onValueChange = onValueChange
            )
        }

        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics {
                    if (announcesError) {
                        // Announced as "label: error" when it appears or becomes the first error.
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = accessibilityLabel(errorText, field.label)
                    }
                }
            )
        }
    }
}

@Composable
private fun FormTextInput(
    field: FormField,
    value: String,
    isEnabled: Boolean,
    borderColor: Color,
    errorText: String?,
    singleLine: Boolean,
    minLines: Int,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = isEnabled,
        singleLine = singleLine,
        minLines = minLines,
        placeholder = {
            field.placeholder?.let {
                Text(text = it, color = MaterialTheme.colorScheme.inputPlaceholder)
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.inputText
        ),
        keyboardOptions = keyboardOptionsFor(field),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.inputBackground,
            unfocusedContainerColor = MaterialTheme.colorScheme.inputBackground,
            disabledContainerColor = MaterialTheme.colorScheme.inputBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .border(1.dp, borderColor)
            .background(MaterialTheme.colorScheme.inputBackground)
            .semantics {
                contentDescription = field.label
                if (errorText != null) error(errorText)
            }
            .padding(0.dp)
    )
}

private fun keyboardOptionsFor(field: FormField): KeyboardOptions = when (field.type) {
    FormField.FieldType.EMAIL -> KeyboardOptions(
        keyboardType = KeyboardType.Email,
        capitalization = KeyboardCapitalization.None
    )
    FormField.FieldType.TEL -> KeyboardOptions(keyboardType = KeyboardType.Phone)
    else -> KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
}
