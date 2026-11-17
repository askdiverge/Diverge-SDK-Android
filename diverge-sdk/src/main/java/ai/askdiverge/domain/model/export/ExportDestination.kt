package ai.askdiverge.domain.model.export

import java.io.OutputStream

/**
 * The document the visitor picked to save their data export in. Both calls block, so callers make
 * them off the main thread.
 */
internal interface ExportDestination {

    /** A stream that writes the document's contents; the caller closes it. */
    fun openOutputStream(): OutputStream

    /** Removes the document, so a failed or cancelled export leaves no partial file behind. */
    fun delete()
}
