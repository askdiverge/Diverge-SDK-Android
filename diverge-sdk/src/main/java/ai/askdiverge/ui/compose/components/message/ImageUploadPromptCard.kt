package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.theme.chatbotPrimary

@Composable
internal fun ImageUploadPromptCard(
    enabled: Boolean,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    PromptCard(
        text = stringResource(R.string.chat_image_upload_prompt),
        modifier = modifier
    ) {
        TextButton(
            onClick = onAddPhoto,
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.chatbotPrimary
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(text = stringResource(R.string.chat_form_add_photo))
        }
    }
}
