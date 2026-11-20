package ai.askdiverge.ui.compose.components.message.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import ai.askdiverge.domain.model.AttachmentUrl
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.compose.theme.productDiscountPrice
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel

internal const val PRODUCT_IMAGE_RATIO = 0.765f

@Composable
internal fun ProductCard(
    product: ProductCardUiModel,
    modifier: Modifier = Modifier,
    onUrlClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val locale = LocalConfiguration.current.locales[0]
    val priceText = remember(product.price, locale) { product.price.mapToUi(locale) }
    val originalPriceText = remember(product.originalPrice, locale) {
        product.originalPrice?.mapToUi(locale)
    }
    Column(
        modifier = modifier.clickable {
            if (onUrlClick != null) {
                onUrlClick(product.url)
            } else {
                uriHandler.openUri(product.url)
            }
        }
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = PRODUCT_IMAGE_RATIO)
                .background(color = MaterialTheme.colorScheme.surfaceVariant),
            model = AttachmentUrl.loadableOrNull(product.imageUrl),
            contentDescription = product.description,
            imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
            contentScale = ContentScale.Crop
        )

        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = product.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.assistantMessageText
        )

        product.description?.let { description ->
            Text(
                text = description,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // The original price moves to its own line when the two do not fit side by side.
        FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            itemVerticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = priceText,
                style = MaterialTheme.typography.titleMedium,
                color = if (product.originalPrice != null) {
                    MaterialTheme.colorScheme.productDiscountPrice
                } else {
                    MaterialTheme.colorScheme.assistantMessageText
                }
            )

            originalPriceText?.let { originalPrice ->
                Text(
                    text = originalPrice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.mutedText,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
    }
}
