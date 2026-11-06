package ai.askdiverge.ui.compose.components.message.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.inputBackground
import ai.askdiverge.ui.compose.theme.inputBorder
import ai.askdiverge.ui.compose.theme.inputPlaceholder
import ai.askdiverge.ui.compose.theme.inputText
import ai.askdiverge.ui.compose.theme.mutedText

@Composable
internal fun FormDropdownField(
    label: String,
    options: List<String>,
    required: Boolean,
    selection: String,
    isEnabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val display = if (selection.isEmpty()) {
        stringResource(R.string.chat_form_select_option)
    } else {
        selection
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(1.dp, MaterialTheme.colorScheme.inputBorder)
                .background(MaterialTheme.colorScheme.inputBackground)
                .clickable(
                    enabled = isEnabled,
                    role = Role.DropdownList,
                    onClick = { expanded = true }
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selection.isEmpty()) {
                    MaterialTheme.colorScheme.inputPlaceholder
                } else {
                    MaterialTheme.colorScheme.inputText
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "▾",
                color = MaterialTheme.colorScheme.mutedText,
                style = MaterialTheme.typography.labelMedium
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (!required && selection.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.chat_form_clear_selection),
                            color = MaterialTheme.colorScheme.assistantMessageText
                        )
                    },
                    onClick = {
                        onSelect("")
                        expanded = false
                    }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = MaterialTheme.colorScheme.assistantMessageText
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
