package ai.askdiverge.ui.state

/** The message Privacy & Data shows under Download my data after an export attempt. */
internal enum class PrivacyExportNotice {

    /** The export was written to the visitor's document. */
    Saved,

    /** The export failed for a reason other than session expiry; the visitor can try again. */
    Failed,

    /** The device has no system save dialog, so the export cannot start. */
    Unavailable
}
