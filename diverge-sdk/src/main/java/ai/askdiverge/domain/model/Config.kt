package ai.askdiverge.domain.model

import ai.askdiverge.domain.model.config.display.Display
import ai.askdiverge.domain.model.config.theme.Theme
import ai.askdiverge.domain.model.form.ChatFormReference
import ai.askdiverge.domain.model.livechat.LivechatConfig

internal data class Config(
    val display: Display,
    val theme: Theme,
    val livechat: LivechatConfig,
    /** Form rows from `/config` → `forms[]`. Empty when the server omits them. */
    val forms: List<ChatFormReference> = emptyList()
) {
    /**
     * The first configured `livechat_waiting` form id, or null when none is configured.
     * Later rows with the same trigger are ignored.
     */
    val livechatWaitingFormId: String?
        get() = forms.firstOrNull { it.trigger == ChatFormReference.Trigger.LIVECHAT_WAITING }
            ?.formId
            ?.takeIf { it.isNotBlank() }
}
