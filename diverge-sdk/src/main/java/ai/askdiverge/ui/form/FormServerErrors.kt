package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.FormRequestException

/**
 * Splits a 422 `error.params` list onto visible fields vs the card banner. Backend English
 * messages are never shown — the SDK owns the copy.
 */
internal object FormServerErrors {

    data class Applied(
        val fieldErrors: Map<String, FormFieldError>,
        val formError: FormBannerError?
    )

    fun apply(
        params: List<FormRequestException.Validation.Param>,
        knownFieldKeys: Set<String>
    ): Applied {
        val fieldErrors = mutableMapOf<String, FormFieldError>()
        var hasUnmatched = params.isEmpty()
        for (param in params) {
            val key = fieldKey(param.field)
            if (key in knownFieldKeys) {
                fieldErrors[key] = FormFieldError.REJECTED
            } else {
                hasUnmatched = true
            }
        }
        return Applied(
            fieldErrors = fieldErrors,
            formError = if (hasUnmatched) FormBannerError.SubmitFailed else null
        )
    }

    /**
     * The form field a server `field` path names. The API reports body paths such as
     * `action.fields.email`, `action.values.email`, `action.files.photo`, `action.email`, and
     * (for values PATCH) `values.email`. Longer prefixes are matched first.
     */
    fun fieldKey(path: String): String {
        for (prefix in listOf(
            "action.fields.",
            "action.values.",
            "action.files.",
            "action.",
            "values."
        )) {
            if (path.startsWith(prefix)) return path.removePrefix(prefix)
        }
        return path
    }
}
