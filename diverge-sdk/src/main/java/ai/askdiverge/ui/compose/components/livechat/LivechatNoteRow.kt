package ai.askdiverge.ui.compose.components.livechat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.message.part.LivechatNoteKind
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

@Composable
internal fun LivechatNoteRow(
    note: MessagePartUiModel.LivechatNote,
    modifier: Modifier = Modifier
) {
    val text = when (note.kind) {
        LivechatNoteKind.QUEUED -> stringResource(R.string.livechat_queued_note)
        LivechatNoteKind.AGENT_JOINED -> stringResource(
            R.string.livechat_agent_joined_note,
            note.agentDisplayName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.livechat_agent_fallback)
        )
        LivechatNoteKind.ENDED -> stringResource(R.string.livechat_ended_note)
        LivechatNoteKind.ENDED_FOR_INACTIVITY -> stringResource(R.string.livechat_ended_inactivity_note)
    }
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.mutedText,
        textAlign = TextAlign.Center
    )
}
