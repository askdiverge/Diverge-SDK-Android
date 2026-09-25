package ai.askdiverge.data.model.message.incoming.part.product

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ProductPriceRemote(
    val amount: Float,
    val currency: String
)
