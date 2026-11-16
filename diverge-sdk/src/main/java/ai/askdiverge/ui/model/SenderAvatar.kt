package ai.askdiverge.ui.model

import androidx.compose.runtime.Immutable

/**
 * Which face sits beside a bot text bubble: the session `/config` avatar, or a livechat agent's
 * own photo. A person glyph stands in while the photo loads, when the URL is missing, and when it
 * fails to load.
 */
@Immutable
internal sealed interface SenderAvatar {

    /** Use the chat's session avatar from `/config` `display.avatar`. */
    data object Session : SenderAvatar

    /**
     * Livechat agent identity for `role: agent` turns.
     *
     * @param imageUrl absolute URL to load, or null when the API sent none / resolve failed.
     * @param displayName the agent's trimmed name, or null when the API sent none or a blank one.
     */
    data class Agent(
        val imageUrl: String?,
        val displayName: String?
    ) : SenderAvatar
}
