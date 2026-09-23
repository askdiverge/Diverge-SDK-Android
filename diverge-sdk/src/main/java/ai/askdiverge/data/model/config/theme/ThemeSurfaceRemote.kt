package ai.askdiverge.data.model.config.theme

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Mirrors the API response as-is. It is currently unclear which part of the UI this surface
 * refers to; usage will be clarified when the theming ticket is implemented.
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeSurfaceRemote(
    val background_gradient_color: String?,
    val background_color: String?,
    val muted_text_color: String?
)
