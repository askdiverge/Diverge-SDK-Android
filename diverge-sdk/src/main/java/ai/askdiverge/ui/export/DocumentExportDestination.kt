package ai.askdiverge.ui.export

import ai.askdiverge.domain.model.export.ExportDestination
import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.FileNotFoundException
import java.io.OutputStream

/**
 * The document the visitor created in the system save dialog
 * (`ActivityResultContracts.CreateDocument`), written through [contentResolver].
 */
internal class DocumentExportDestination(
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : ExportDestination {

    override fun openOutputStream(): OutputStream =
        contentResolver.openOutputStream(uri)
            ?: throw FileNotFoundException("The export document could not be opened")

    override fun delete() {
        DocumentsContract.deleteDocument(contentResolver, uri)
    }

    internal companion object {
        /** The type of document the save dialog creates. */
        const val MIME_TYPE = "application/json"

        /** The file name the save dialog suggests; the visitor can change it. */
        const val SUGGESTED_FILE_NAME = "diverge-export.json"
    }
}
