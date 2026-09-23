package ai.askdiverge.data.model.config.theme

import androidx.annotation.Keep
import ai.askdiverge.data.model.config.theme.font.ThemeFontRemote
import ai.askdiverge.data.model.config.theme.header.ThemeHeaderRemote
import ai.askdiverge.data.model.config.theme.input.ThemeInputRemote
import ai.askdiverge.data.model.config.theme.messages.ThemeMessagesRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeRemote(
    val brand: BrandRemote,
    val surface: ThemeSurfaceRemote,
    val header: ThemeHeaderRemote,
    val messages: ThemeMessagesRemote,
    val input: ThemeInputRemote,
    val product_card: ThemeProductCardRemote,
    val font: ThemeFontRemote
)
