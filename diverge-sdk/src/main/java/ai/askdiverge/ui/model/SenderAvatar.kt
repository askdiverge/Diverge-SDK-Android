package ai.askdiverge.ui.model

import androidx.compose.runtime.Immutable

/**
 * Which face sits beside a bot text bubble: the session `/config` avatar, or a livechat agent's
 * own photo (with a person glyph when the URL is missing or fails to load).
 */
@Immutable
internal sealed interface SenderAvatar {

    /** Use the chat's session avatar from `/config` `display.avatar`. */
    data object Session : SenderAvatar

    /**
     * Livechat agent identity for `role: agent` turns.
     *
     * @param imageUrl absolute URL to load, or null when the API sent none / resolve failed.
     * @param displayName trimmed agent name; blank → generic agent a11y string in Compose.
     */
    data class Agent(
        val imageUrl: String?,
        val displayName: String
    ) : SenderAvatar
}
