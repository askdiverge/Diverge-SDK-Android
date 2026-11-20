package ai.askdiverge.ui.livechat

/**
 * What the header livechat control offers. It shows only while livechat is enabled with its logo,
 * and always during a session so the visitor can end it.
 */
internal enum class LivechatControl {
    HIDDEN,
    START,
    END,
    OFFLINE
}
