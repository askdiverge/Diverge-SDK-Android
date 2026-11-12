package ai.askdiverge.ui.compose.components.livechat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.chatbotPrimary

@Composable
internal fun HumanAgentPromptCard(
    enabled: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.assistantBubbleBorder, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.assistantBubbleBackground,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.livechat_human_agent_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.assistantMessageText
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onAccept,
                    enabled = enabled,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics {
                            role = Role.Button
                        }
                ) {
                    Text(
                        text = stringResource(R.string.livechat_talk_to_person),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.chatbotPrimary
                        } else {
                            MaterialTheme.colorScheme.assistantMessageText.copy(alpha = 0.38f)
                        }
                    )
                }
                if (enabled) {
                    TextButton(
                        onClick = onDecline,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.livechat_no_thanks))
                    }
                }
            }
        }
    }
}
