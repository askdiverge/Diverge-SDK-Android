package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ConversationForm

/** Callbacks the conversation list forwards into form cards. */
internal data class FormUiCallbacks(
    val formModels: Map<String, FormSubmissionModel>,
    val isReplyBusy: Boolean,
    val onEnsure: (ConversationForm) -> Unit,
    val onValueChange: (partId: String, key: String, value: String) -> Unit,
    val onSubmit: (partId: String) -> Unit,
    val onReload: (partId: String) -> Unit
) {
    companion object {
        val None = FormUiCallbacks(
            formModels = emptyMap(),
            isReplyBusy = false,
            onEnsure = {},
            onValueChange = { _, _, _ -> },
            onSubmit = {},
            onReload = {}
        )
    }
}
