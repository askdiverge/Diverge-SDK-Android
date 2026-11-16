package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.compose.theme.assistantMessageText

/** The assistant-styled card an in-conversation prompt is drawn on: [text] above [actions]. */
@Composable
internal fun PromptCard(
    text: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit
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
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.assistantMessageText
            )
            actions()
        }
    }
}
