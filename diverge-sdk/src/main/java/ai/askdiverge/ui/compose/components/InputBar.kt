package ai.askdiverge.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import ai.askdiverge.ui.compose.theme.inputBackground
import ai.askdiverge.ui.compose.theme.inputBorder
import ai.askdiverge.ui.compose.theme.inputPlaceholder
import ai.askdiverge.ui.compose.theme.inputText
import ai.askdiverge.ui.compose.theme.sendButtonIcon
import ai.askdiverge.sdk.R

@Composable
internal fun ChatbotInputBar(
    inputMessage: String,
    isGenerating: Boolean,
    isEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.inputBorder)
            .background(MaterialTheme.colorScheme.inputBackground)
            .padding(top = 8.dp)
    ) {
        BasicTextField(
            value = inputMessage,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            minLines = 1,
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.inputText),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
            decorationBox = { innerTextField ->
                Box {
                    if (inputMessage.isEmpty()) {
                        Text(
                            text = stringResource(R.string.chat_input_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.inputPlaceholder
                        )
                    }
                    innerTextField()
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = onPrivacyPolicyClick
            ) {
                Icon(
                    imageVector = ChatbotIcons.PrivacyPolicy,
                    contentDescription = stringResource(R.string.chat_privacy_policy_icon_description),
                    tint = MaterialTheme.colorScheme.inputText
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FilledIconButton(
                modifier = Modifier.size(48.dp).padding(8.dp),
                enabled = isEnabled && inputMessage.isNotBlank() && !isGenerating,
                onClick = onSendMessage,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.chatbotPrimary,
                    contentColor = MaterialTheme.colorScheme.sendButtonIcon
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = ChatbotIcons.Send,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatbotInputBarPreview() {
    ChatbotInputBar(
        inputMessage = "",
        isGenerating = false,
        isEnabled = true,
        onInputChange = {},
        onSendMessage = {},
        onPrivacyPolicyClick = {}
    )
}
