package ai.askdiverge.data.model.message.incoming.part.product

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ProductCardRemote(
    val id: String,
    val title: String,
    val description: String?,
    val image_url: String,
    val original_price: ProductPriceRemote?,
    val price: ProductPriceRemote,
    val url: String
)
