package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.product.ProductCardRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ProductPartRemote(
    val part_id: String,
    val products: List<ProductCardRemote>
) : MessagePartRemote
