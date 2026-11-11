package ai.askdiverge.ui.compose.components.livechat

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.sdk.R

@Composable
internal fun LivechatLeaveDialog(
    status: LivechatState.Status,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val active = status == LivechatState.Status.ACTIVE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (active) R.string.livechat_end_title else R.string.livechat_leave_title
                )
            )
        },
        text = {
            Text(
                text = stringResource(
                    if (active) R.string.livechat_end_message else R.string.livechat_leave_message
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(
                        if (active) R.string.livechat_end_confirm else R.string.livechat_leave_confirm
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(
                        if (active) R.string.livechat_end_cancel else R.string.livechat_leave_cancel
                    )
                )
            }
        }
    )
}
