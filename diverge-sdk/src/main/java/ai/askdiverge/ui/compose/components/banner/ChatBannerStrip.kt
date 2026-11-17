package ai.askdiverge.ui.compose.components.banner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.banner.ChatBanner

/** Sticky stack of in-chat promo banners under the top bar / above the conversation list. */
@Composable
internal fun ChatBannerStrip(
    banners: List<ChatBanner>,
    onCtaClick: (String) -> Unit,
    onDismiss: (ChatBanner) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        banners.forEach { banner ->
            ChatBannerCard(
                banner = banner,
                onCtaClick = onCtaClick,
                onDismiss = { onDismiss(banner) }
            )
        }
    }
}
