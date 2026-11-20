package ai.askdiverge.ui.compose.components.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.banner.ChatBanner
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.chatbotPrimary
import ai.askdiverge.ui.compose.theme.sendButtonIcon

// The CTA wraps within this width, so the message always keeps most of the row.
private val CTA_MAX_WIDTH = 120.dp

@Composable
internal fun ChatBannerCard(
    banner: ChatBanner,
    onCtaClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Without banner colours, the card takes the accent and the colour drawn on it elsewhere.
    val (fill, foreground) = ChatBannerChrome.colors(
        backgroundHex = banner.backgroundColor,
        textHex = banner.textColor,
        fallbackFill = MaterialTheme.colorScheme.chatbotPrimary,
        fallbackText = MaterialTheme.colorScheme.sendButtonIcon
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(end = if (banner.dismissible) 44.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = banner.message,
                color = foreground,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )

            if (banner.showsCta) {
                val label = banner.trimmedCtaLabel.orEmpty()
                val url = banner.sanitizedCtaUrl.orEmpty()
                BannerCta(
                    label = label,
                    style = banner.ctaStyle,
                    foreground = foreground,
                    onClick = { onCtaClick(url) },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (banner.dismissible) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = ChatbotIcons.Close,
                    contentDescription = stringResource(R.string.chat_banner_dismiss),
                    tint = foreground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BannerCta(
    label: String,
    style: ChatBanner.CtaStyle?,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isButton = style == ChatBanner.CtaStyle.BUTTON
    val ctaDescription = stringResource(R.string.chat_banner_cta_hint)
    val capsule = RoundedCornerShape(percent = 50)
    Text(
        text = label,
        color = foreground,
        style = MaterialTheme.typography.labelMedium,
        textDecoration = if (isButton) TextDecoration.None else TextDecoration.Underline,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            // Reserves a 48 dp touch target for the pill or link without resizing it. The 8 dp that
            // keeps it off the card's top and bottom edges counts towards the target.
            .minimumInteractiveComponentSize()
            .padding(vertical = 8.dp)
            .widthIn(max = CTA_MAX_WIDTH)
            .then(
                if (isButton) {
                    Modifier
                        .clip(capsule)
                        .border(1.dp, foreground, capsule)
                        .background(foreground.copy(alpha = 0.2f))
                } else {
                    Modifier
                }
            )
            // Before the pill's padding, so the whole pill responds and ripples inside its shape.
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (isButton) Modifier.padding(horizontal = 8.dp, vertical = 4.dp) else Modifier)
            .semantics { contentDescription = "$label. $ctaDescription" }
    )
}
