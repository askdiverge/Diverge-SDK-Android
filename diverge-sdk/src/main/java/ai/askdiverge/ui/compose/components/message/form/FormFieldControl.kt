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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.form.FormField
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
                Text(
                    text = "*",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
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
                onSelect = onValueChange
            )

            FormField.FieldType.FILE -> FormFileField(
                fieldLabel = field.label,
                offersAttachments = offersAttachments,
                pickedFile = pickedFile,
                isEnabled = isEnabled,
                onPickFile = onPickFile,
                onRemoveFile = onRemoveFile
            )

            FormField.FieldType.TEXTAREA -> FormTextInput(
                field = field,
                value = value,
                isEnabled = isEnabled,
                borderColor = borderColor,
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
                singleLine = true,
                minLines = 1,
                onValueChange = onValueChange
            )
        }

        if (error != null) {
            Text(
                text = formFieldErrorText(error),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
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
            .semantics { contentDescription = field.label }
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
