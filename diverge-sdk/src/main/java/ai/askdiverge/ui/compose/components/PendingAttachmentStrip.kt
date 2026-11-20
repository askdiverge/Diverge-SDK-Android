package ai.askdiverge.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.attachment.PendingComposerAttachment
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import ai.askdiverge.ui.compose.theme.sendButtonIcon
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext

@Composable
internal fun PendingAttachmentStrip(
    attachments: List<PendingComposerAttachment>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    val imageLabel = stringResource(R.string.media_image_content_description)
    val removeLabel = stringResource(R.string.chat_form_remove_photo)
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(attachments, key = { _, pending -> pending.id }) { index, pending ->
            Box(
                modifier = Modifier
                    .size(64.dp)
                    // One TalkBack element per chip: the photo, its position, and removal as a
                    // named action, so each chip says which photo it removes.
                    .clearAndSetSemantics {
                        contentDescription = imageLabel
                        stateDescription = (index + 1).toString()
                        customActions = listOf(
                            CustomAccessibilityAction(removeLabel) {
                                onRemove(pending.id)
                                true
                            }
                        )
                    }
            ) {
                AsyncImage(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    model = pending.previewUrl,
                    contentDescription = null,
                    imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
                    contentScale = ContentScale.Crop
                )
                // Small glyph, full-size target: a 48 dp square inside the chip's top-end corner,
                // so the whole target sits within this chip's bounds.
                val removeInteractions = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clickable(
                            interactionSource = removeInteractions,
                            indication = null,
                            role = Role.Button,
                            onClick = { onRemove(pending.id) }
                        ),
                    contentAlignment = Alignment.TopEnd
                ) {
                    // The send button's colour pair, so the glyph stands out on any photo.
                    Icon(
                        imageVector = ChatbotIcons.Trash,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.sendButtonIcon,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .indication(removeInteractions, ripple())
                            .background(MaterialTheme.colorScheme.chatbotPrimary, CircleShape)
                            .padding(5.dp)
                    )
                }
            }
        }
    }
}
