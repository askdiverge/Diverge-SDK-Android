package ai.askdiverge.ui.compose.components.banner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import ai.askdiverge.domain.model.banner.ChatBanner
import kotlin.math.roundToInt

/**
 * Sticky stack of in-chat promo banners under the top bar. It takes at most a quarter of the
 * height it is offered and scrolls beyond that, so the conversation keeps most of the screen in
 * landscape, with the keyboard up and at large font scales.
 */
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
            .maxHeightFraction(MAX_HEIGHT_FRACTION)
            .verticalScroll(rememberScrollState())
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

private const val MAX_HEIGHT_FRACTION = 0.25f

// Measures the content with at most [fraction] of the bounded height on offer.
private fun Modifier.maxHeightFraction(fraction: Float): Modifier =
    layout { measurable, constraints ->
        val maxHeight = if (constraints.hasBoundedHeight) {
            (constraints.maxHeight * fraction).roundToInt().coerceAtLeast(constraints.minHeight)
        } else {
            constraints.maxHeight
        }
        val placeable = measurable.measure(constraints.copy(maxHeight = maxHeight))
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
