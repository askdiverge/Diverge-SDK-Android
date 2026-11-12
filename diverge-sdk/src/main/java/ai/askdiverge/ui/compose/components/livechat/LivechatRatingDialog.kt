package ai.askdiverge.ui.compose.components.livechat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.livechat.LivechatRatingModel

@Composable
internal fun LivechatRatingDialog(
    model: LivechatRatingModel,
    onScore: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSend: () -> Unit,
    onSkip: () -> Unit
) {
    val title = stringResource(R.string.livechat_rating_title)
    AlertDialog(
        onDismissRequest = {
            if (!model.isSubmitting) onSkip()
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (model.needsComment) {
                    FeedbackStep(
                        model = model,
                        onCommentChange = onCommentChange
                    )
                } else {
                    ScoreRow(
                        isSubmitting = model.isSubmitting,
                        selectedScore = model.score,
                        onScore = onScore
                    )
                }
                if (model.phase is LivechatRatingModel.Phase.Failed) {
                    Text(
                        text = stringResource(R.string.chat_send_message_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (model.needsComment) {
                Button(
                    onClick = onSend,
                    enabled = !model.isSubmitting,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    if (model.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text = stringResource(R.string.livechat_rating_send))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !model.isSubmitting,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(text = stringResource(R.string.livechat_rating_skip))
            }
        }
    )
}

@Composable
private fun ScoreRow(
    isSubmitting: Boolean,
    selectedScore: Int?,
    onScore: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (score in 1..5) {
            val description = stringResource(R.string.livechat_rating_score, score)
            TextButton(
                onClick = { onScore(score) },
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = description }
            ) {
                if (isSubmitting && selectedScore == score) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.chatbotPrimary
                    )
                } else {
                    Text(
                        text = score.toString(),
                        color = MaterialTheme.colorScheme.chatbotPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackStep(
    model: LivechatRatingModel,
    onCommentChange: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.livechat_rating_feedback_prompt),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.mutedText
    )
    OutlinedTextField(
        value = model.comment,
        onValueChange = onCommentChange,
        enabled = !model.isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        placeholder = {
            Text(text = stringResource(R.string.livechat_rating_feedback_placeholder))
        },
        minLines = 3,
        maxLines = 6
    )
}
