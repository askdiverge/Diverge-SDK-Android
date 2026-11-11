package ai.askdiverge.ui.form

/** Per-field validation outcome resolved to copy in the form card. */
internal enum class FormFieldError {
    REQUIRED,
    INVALID_EMAIL,
    REJECTED
}

/** Form-level banner error resolved to copy in the form card. */
internal sealed interface FormBannerError {
    data class MinFilled(val count: Int) : FormBannerError

    data object Unavailable : FormBannerError

    data object SubmitFailed : FormBannerError

    data object LoadFailed : FormBannerError

    data object PhotoFailed : FormBannerError
}
