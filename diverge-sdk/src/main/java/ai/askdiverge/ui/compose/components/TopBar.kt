package ai.askdiverge.ui.compose.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.screenBackground
import ai.askdiverge.sdk.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatbotTopBar(
    assistantName: String,
    hasNavigateBackIcon: Boolean,
    showReset: Boolean,
    isResetting: Boolean,
    isResetEnabled: Boolean,
    onReset: () -> Unit,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        windowInsets = WindowInsets(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.screenBackground
        ),
        title = {
            if (assistantName.isNotEmpty()) {
                Text(
                    text = assistantName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        navigationIcon = {
            if (hasNavigateBackIcon) {
                IconButton(
                    onClick = onNavigateBack
                ) {
                    Icon(
                        imageVector = ChatbotIcons.ChevronLeft,
                        contentDescription = null // TODO: add content description
                    )
                }
            }
        },
        actions = {
            if (showReset) {
                IconButton(
                    onClick = onReset,
                    enabled = isResetEnabled && !isResetting
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Icon(
                            imageVector = ChatbotIcons.Refresh,
                            contentDescription = stringResource(R.string.chat_reset_conversation_content_description)
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ChatbotTopBarPreview() {
    ChatbotTopBar(
        assistantName = "AI Assistant",
        hasNavigateBackIcon = true,
        showReset = true,
        isResetting = false,
        isResetEnabled = true,
        onReset = {},
        onNavigateBack = {}
    )
}
