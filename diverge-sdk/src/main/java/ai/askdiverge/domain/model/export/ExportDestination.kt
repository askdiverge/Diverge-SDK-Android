package ai.askdiverge.domain.model.export

import java.io.OutputStream

/**
 * The document the visitor picked to save their data export in. Both calls block, so callers make
 * them off the main thread.
 */
internal interface ExportDestination {

    /** A stream that writes the document's contents; the caller closes it. */
    fun openOutputStream(): OutputStream

    /**
     * Removes what a failed or cancelled export leaves behind: the document once [openOutputStream]
     * has opened it, or the empty document the save dialog created. A document that already had
     * content and was never opened is kept.
     */
    fun delete()
}
