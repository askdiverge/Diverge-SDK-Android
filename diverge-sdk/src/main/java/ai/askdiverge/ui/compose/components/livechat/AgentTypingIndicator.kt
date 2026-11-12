package ai.askdiverge.ui.compose.components.livechat

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R

/**
 * Shows when the active livechat agent is typing. The row keeps its height while they aren't;
 * the spinner is left out when the system asks to remove animations.
 */
@Composable
internal fun AgentTypingIndicator(
    isTyping: Boolean,
    agentDisplayName: String?,
    modifier: Modifier = Modifier
) {
    val copy = if (!agentDisplayName.isNullOrBlank()) {
        stringResource(R.string.livechat_agent_typing_named, agentDisplayName)
    } else {
        stringResource(R.string.livechat_agent_typing)
    }
    val contentResolver = LocalContext.current.contentResolver
    val reduceMotion = try {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Throwable) {
        false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .alpha(if (isTyping) 1f else 0f)
            .semantics {
                contentDescription = if (isTyping) copy else ""
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTyping && !reduceMotion) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
            }
            if (isTyping) {
                Text(
                    text = copy,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
