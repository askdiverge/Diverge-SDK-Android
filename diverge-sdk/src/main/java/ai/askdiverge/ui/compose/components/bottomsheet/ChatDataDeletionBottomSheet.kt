package ai.askdiverge.ui.compose.components.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.extension.hideWithAnimation
import ai.askdiverge.sdk.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatDataDeletionBottomSheet(
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChecked by remember { mutableStateOf(false) }
    var isDeleteClicked by remember { mutableStateOf(false) }
    val showError = isDeleteClicked && !isChecked
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDeleting) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.chat_privacy_policy_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ChatDataDeletionHeader()

            Spacer(modifier = Modifier.height(24.dp))

            ChatDataDeletionConfirmation(
                isChecked = isChecked,
                showError = showError,
                onCheckedChange = { isChecked = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            ChatDataDeletionActions(
                isDeleting = isDeleting,
                onCancelClick = {
                    sheetState.hideWithAnimation(
                        scope = scope,
                        onHidden = onCancel
                    )
                },
                onDeleteClick = {
                    isDeleteClicked = true
                    if (isChecked) {
                        onDelete()
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Preview
@Composable
private fun ChatDataDeletionBottomSheetPreview() {
    ChatDataDeletionBottomSheet(
        isDeleting = false,
        onDelete = {},
        onCancel = {},
        onDismiss = {}
    )
}
