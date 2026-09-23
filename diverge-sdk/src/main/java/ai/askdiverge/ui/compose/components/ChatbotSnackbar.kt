package ai.askdiverge.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.previewprovider.ChatbotSnackbarPreviewProvider

@Composable
internal fun ChatbotSnackbar(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = if (isError) {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    } else {
        SnackbarDefaults.color to SnackbarDefaults.contentColor
    }

    Snackbar(
        modifier = modifier.padding(16.dp),
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isError) {
                Icon(
                    imageVector = ChatbotIcons.Error,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatbotSnackbarPreview(@PreviewParameter(ChatbotSnackbarPreviewProvider::class) isError: Boolean) {
    ChatbotSnackbar(
        message = "Det lykkedes ikke at generere et svar. Prøv venligst igen.",
        isError = isError
    )
}
