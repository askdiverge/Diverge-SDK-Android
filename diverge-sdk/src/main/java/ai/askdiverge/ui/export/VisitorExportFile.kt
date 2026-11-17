package ai.askdiverge.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * One cache file for the GDPR export share sheet. Overwritten on each download; deleted when the
 * privacy sheet dismisses or after the chooser is launched.
 */
internal object VisitorExportFile {

    const val FILE_NAME = "diverge-export.json"

    fun authority(context: Context): String =
        "${context.packageName}.ai.askdiverge.fileprovider"

    fun file(context: Context): File = File(context.cacheDir, FILE_NAME)

    fun write(
        context: Context,
        bytes: ByteArray
    ): Uri {
        val export = file(context)
        export.writeBytes(bytes)
        return FileProvider.getUriForFile(context, authority(context), export)
    }

    fun discard(context: Context) {
        file(context).delete()
    }

    fun shareIntent(
        context: Context,
        uri: Uri
    ): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null
        )
}
