package ai.askdiverge.ui.compose.components.bottomsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R

@Composable
internal fun ChatDataDeletionConfirmation(
    isChecked: Boolean,
    showError: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // toggleable merges its descendants into a single accessibility node, so the checkbox
            // and the label are announced together as one checkbox row.
            modifier = Modifier.toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
        ) {
            // onCheckedChange is null: the whole row is the toggle target, so the checkbox
            // itself must not be independently clickable (one merged accessibility node).
            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                colors = if (showError) {
                    CheckboxDefaults.colors(uncheckedColor = MaterialTheme.colorScheme.error)
                } else {
                    CheckboxDefaults.colors()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.chat_delete_data_checkbox_label),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedVisibility(visible = showError) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.chat_delete_data_confirm_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatDataDeletionConfirmationPreview() {
    ChatDataDeletionConfirmation(
        isChecked = false,
        showError = true,
        onCheckedChange = {}
    )
}
