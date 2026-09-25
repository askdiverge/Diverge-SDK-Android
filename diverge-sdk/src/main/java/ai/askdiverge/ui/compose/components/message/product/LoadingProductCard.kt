package ai.askdiverge.ui.compose.components.message.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.extension.shimmer

/** Shimmer skeleton mirroring [ProductCard], shown while more products are still streaming in. */
@Composable
internal fun LoadingProductCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Image placeholder.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = PRODUCT_IMAGE_RATIO)
                .shimmer()
        )
        // Title placeholder line.
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(height = 16.dp)
                .shimmer()
        )
        // Description placeholder line.
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(fraction = 0.7f)
                .height(height = 14.dp)
                .shimmer()
        )
        // Price placeholder line.
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(fraction = 0.5f)
                .height(height = 16.dp)
                .shimmer()
        )
    }
}
