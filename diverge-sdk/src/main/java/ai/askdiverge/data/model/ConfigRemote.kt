package ai.askdiverge.data.model

import androidx.annotation.Keep
import ai.askdiverge.data.model.config.display.DisplayRemote
import ai.askdiverge.data.model.config.theme.ThemeRemote
import ai.askdiverge.data.model.livechat.LivechatConfigRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ConfigRemote(
    val display: DisplayRemote,
    val theme: ThemeRemote,
    /** Absent on older deployments; maps to [ai.askdiverge.domain.model.livechat.LivechatConfig.serverDefaults]. */
    val livechat: LivechatConfigRemote? = null
)
