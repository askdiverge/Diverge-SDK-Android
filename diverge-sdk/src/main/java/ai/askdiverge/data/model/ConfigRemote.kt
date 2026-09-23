package ai.askdiverge.data.model

import androidx.annotation.Keep
import ai.askdiverge.data.model.config.display.DisplayRemote
import ai.askdiverge.data.model.config.theme.ThemeRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ConfigRemote(
    val display: DisplayRemote,
    val theme: ThemeRemote
)
