package ai.askdiverge.ui.compose.components.livechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import ai.askdiverge.ui.compose.theme.inputBackground
import ai.askdiverge.ui.compose.theme.inputBorder
import ai.askdiverge.ui.compose.theme.inputPlaceholder
import ai.askdiverge.ui.compose.theme.inputText
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.livechat.LivechatRatingModel

/**
 * @param onDismissRequest back press or a tap beside the dialog; Skip unless the caller keeps
 * something open behind it.
 */
@Composable
internal fun LivechatRatingDialog(
    model: LivechatRatingModel,
    onScore: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSend: () -> Unit,
    onSkip: () -> Unit,
    onDismissRequest: () -> Unit = onSkip
) {
    val title = stringResource(R.string.livechat_rating_title)
    AlertDialog(
        // A rating already sent finishes in the background, so the dialog can always be dismissed.
        onDismissRequest = onDismissRequest,
        // A tap beside the dialog would throw away the comment being typed.
        properties = DialogProperties(dismissOnClickOutside = !model.needsComment),
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
                    .verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                }
                // Stays composed so the failure is announced when it appears inside it.
                Box(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        liveRegion = LiveRegionMode.Polite
                    }
                ) {
                    if (model.phase is LivechatRatingModel.Phase.Failed) {
                        Text(
                            text = stringResource(R.string.livechat_rating_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (model.needsComment) {
                Button(
                    onClick = onSend,
                    enabled = !model.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.chatbotPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    if (model.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current
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
    val primary = MaterialTheme.colorScheme.chatbotPrimary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (score in LivechatRatingModel.MIN_SCORE..LivechatRatingModel.MAX_SCORE) {
            val description = stringResource(R.string.livechat_rating_score, score)
            val selected = selectedScore == score
            val contentColor = if (selected) onPrimary else primary
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .then(if (selected) Modifier.background(primary, shape) else Modifier)
                    .border(width = 1.dp, color = primary, shape = shape)
                    .selectable(
                        selected = selected,
                        enabled = !isSubmitting,
                        role = Role.RadioButton,
                        onClick = { onScore(score) }
                    )
                    .semantics { contentDescription = description }
            ) {
                if (isSubmitting && selected) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
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
    val prompt = stringResource(R.string.livechat_rating_feedback_prompt)
    Text(
        text = prompt,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.mutedText
    )
    OutlinedTextField(
        value = model.comment,
        onValueChange = onCommentChange,
        enabled = !model.isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .semantics { contentDescription = prompt },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.inputText
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.livechat_rating_feedback_placeholder),
                color = MaterialTheme.colorScheme.inputPlaceholder
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.inputBackground,
            unfocusedContainerColor = MaterialTheme.colorScheme.inputBackground,
            disabledContainerColor = MaterialTheme.colorScheme.inputBackground,
            focusedBorderColor = MaterialTheme.colorScheme.chatbotPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.inputBorder,
            cursorColor = MaterialTheme.colorScheme.chatbotPrimary
        ),
        minLines = 3,
        maxLines = 6
    )
}
