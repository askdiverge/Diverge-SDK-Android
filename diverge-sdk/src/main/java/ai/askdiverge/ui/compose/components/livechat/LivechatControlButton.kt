package ai.askdiverge.ui.compose.components.livechat

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import ai.askdiverge.ui.livechat.LivechatControl

@Composable
internal fun LivechatControlButton(
    control: LivechatControl,
    status: LivechatState.Status,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (control == LivechatControl.HIDDEN) return

    val label = when (control) {
        LivechatControl.END -> stringResource(R.string.livechat_end)
        LivechatControl.OFFLINE -> stringResource(R.string.livechat_offline)
        LivechatControl.START,
        LivechatControl.HIDDEN -> stringResource(R.string.livechat_start)
    }
    val statusValue = when (status) {
        LivechatState.Status.WAITING -> stringResource(R.string.livechat_waiting_status)
        LivechatState.Status.ACTIVE -> stringResource(R.string.livechat_active_status)
        else -> ""
    }
    val tint = when {
        control == LivechatControl.END -> MaterialTheme.colorScheme.chatbotPrimary
        control == LivechatControl.OFFLINE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier.semantics {
            contentDescription = if (statusValue.isEmpty()) label else "$label, $statusValue"
        }
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = tint,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = ChatbotIcons.Person,
                contentDescription = null,
                tint = tint
            )
        }
    }
}
