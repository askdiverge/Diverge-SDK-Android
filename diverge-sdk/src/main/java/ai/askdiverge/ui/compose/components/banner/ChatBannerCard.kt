package ai.askdiverge.ui.compose.components.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.banner.ChatBanner
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.chatbotPrimary

@Composable
internal fun ChatBannerCard(
    banner: ChatBanner,
    onCtaClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fill = ChatBannerChrome.fill(
        hex = banner.backgroundColor,
        accent = MaterialTheme.colorScheme.chatbotPrimary
    )
    val foreground = ChatBannerChrome.foreground(banner.textColor)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(end = if (banner.dismissible) 44.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = banner.message,
                color = foreground,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
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
                    modifier = Modifier.size(12.dp)
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
    Text(
        text = label,
        color = foreground,
        style = MaterialTheme.typography.labelMedium,
        textDecoration = if (isButton) TextDecoration.None else TextDecoration.Underline,
        modifier = modifier
            .then(
                if (isButton) {
                    val capsule = RoundedCornerShape(percent = 50)
                    Modifier
                        .border(1.dp, foreground, capsule)
                        .background(Color.White.copy(alpha = 0.2f), capsule)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label. $ctaDescription" }
    )
}
