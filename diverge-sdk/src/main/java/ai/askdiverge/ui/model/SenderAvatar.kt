package ai.askdiverge.ui.model

import androidx.compose.runtime.Immutable

/**
 * Who sent a bot text bubble, which decides the face beside it: the assistant ([Session]) or a
 * livechat agent ([Agent]).
 */
@Immutable
internal sealed interface SenderAvatar {

    /** Use the chat's session avatar from `/config` `display.avatar`. */
    data object Session : SenderAvatar

    /**
     * Livechat agent identity for `role: agent` turns.
     *
     * @param imageUrl absolute `http`/`https` URL or `data:` URL to load; null when the API sent
     * none, or one that doesn't resolve to either.
     * @param displayName the agent's trimmed name, or null when the API sent none or a blank one.
     */
    data class Agent(
        val imageUrl: String?,
        val displayName: String?
    ) : SenderAvatar
}
