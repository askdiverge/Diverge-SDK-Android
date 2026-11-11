package ai.askdiverge.domain.model

import ai.askdiverge.domain.model.config.display.Display
import ai.askdiverge.domain.model.config.theme.Theme
import ai.askdiverge.domain.model.form.ChatFormReference
import ai.askdiverge.domain.model.livechat.LivechatConfig

internal data class Config(
    val display: Display,
    val theme: Theme,
    val livechat: LivechatConfig = LivechatConfig.serverDefaults,
    /** Dynamic form references (`session_start` / `livechat_waiting` / `llm`). Empty when absent. */
    val forms: List<ChatFormReference> = emptyList()
) {
    /** The first `livechat_waiting` form id, if any — used when the visitor is queued. */
    val livechatWaitingFormId: String?
        get() = forms.firstOrNull { it.trigger == ChatFormReference.Trigger.LIVECHAT_WAITING }?.formId
}
