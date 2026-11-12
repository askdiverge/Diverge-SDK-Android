package ai.askdiverge.ui.compose.components.message.attachment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.AttachmentUrl
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import java.util.Locale

@Composable
internal fun MessageFileRow(
    file: MessagePartUiModel.File,
    onOpen: (displayUrl: String, attachmentId: String?, urlExpiresAt: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val canOpen = AttachmentUrl.isWebUrl(file.displayUrl)
    val detail = when {
        !canOpen -> stringResource(R.string.media_unavailable)
        file.sizeBytes != null -> formatFileSize(file.sizeBytes)
        else -> null
    }
    val a11y = if (detail != null) "${file.filename}. $detail" else file.filename

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.assistantBubbleBackground)
            .border(1.dp, MaterialTheme.colorScheme.assistantBubbleBorder)
            .clickable(enabled = canOpen) {
                onOpen(file.displayUrl, file.attachmentId, file.urlExpiresAt)
            }
            .padding(12.dp)
            .semantics { contentDescription = a11y },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.filename,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.mutedText
                )
            }
        }
        if (canOpen) {
            Icon(
                imageVector = ChatbotIcons.ArrowUpRight,
                contentDescription = stringResource(R.string.media_open_hint),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.mutedText
            )
        }
    }
}

private fun formatFileSize(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return String.format(Locale.US, "%.1f KB", kib)
    val mib = kib / 1024.0
    return String.format(Locale.US, "%.1f MB", mib)
}
