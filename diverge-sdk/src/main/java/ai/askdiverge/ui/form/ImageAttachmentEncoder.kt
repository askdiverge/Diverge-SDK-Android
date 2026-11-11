package ai.askdiverge.ui.form

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import ai.askdiverge.domain.model.form.OutgoingAttachment
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Downsamples a picked photo and JPEG-encodes it for `POST /actions`, matching the iOS
 * `ImageAttachment` budgets (1024 px long edge at quality 0.7, one retry at 512 px). The EXIF
 * orientation is applied to the pixels, since the re-encoded JPEG carries no EXIF.
 */
internal object ImageAttachmentEncoder {

    const val MAX_PIXEL_SIZE = 1024
    const val RETRY_PIXEL_SIZE = 512
    const val JPEG_QUALITY = 70

    /**
     * Encodes the image [open] streams into a [PendingFormAttachment], or `null` when it cannot be
     * read or decoded, or still exceeds the action size budget after the 512 px retry. [open] is
     * called once per pass, and each stream it returns is closed here. Blocks; call it off the
     * main thread.
     */
    fun encode(
        open: () -> InputStream?,
        filename: String,
        maxDecodedBytes: Int
    ): PendingFormAttachment? = try {
        val orientation = readOrientation(open)
        encodeOnce(open, MAX_PIXEL_SIZE, orientation, filename, maxDecodedBytes)
            ?: encodeOnce(open, RETRY_PIXEL_SIZE, orientation, filename, maxDecodedBytes)
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }

    /** Base64 character budget for a decoded-byte cap (4 chars per 3 bytes, floors), capped. */
    fun encodedLengthBudget(maxDecodedBytes: Int): Int {
        val uncapped = (maxDecodedBytes.toLong() / 3L) * 4L
        return minOf(uncapped, OutgoingAttachment.MAX_ACTION_ENCODED_LENGTH.toLong()).toInt()
    }

    private fun encodeOnce(
        open: () -> InputStream?,
        maxPixelSize: Int,
        orientation: Int,
        filename: String,
        maxDecodedBytes: Int
    ): PendingFormAttachment? {
        val bitmap = decodeSampled(open, maxPixelSize, orientation) ?: return null
        val jpeg = try {
            ByteArrayOutputStream().use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    return null
                }
                stream.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }

        val base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
        if (base64.length > encodedLengthBudget(maxDecodedBytes)) return null
        val attachment = OutgoingAttachment.create(
            data = base64,
            mime = "image/jpeg",
            filename = filename,
            maxDecodedBytes = maxDecodedBytes
        ) ?: return null
        return PendingFormAttachment(attachment = attachment, displayName = filename)
    }

    private fun decodeSampled(
        open: () -> InputStream?,
        maxPixelSize: Int,
        orientation: Int
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (open() ?: return null).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxPixelSize)
        }
        val decoded = open()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val matrix = orientationMatrix(orientation)
        val longEdge = max(decoded.width, decoded.height)
        if (longEdge > maxPixelSize) {
            val scale = maxPixelSize.toFloat() / longEdge
            matrix.preScale(scale, scale)
        }
        if (matrix.isIdentity) return decoded

        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed !== decoded) decoded.recycle()
        return transformed
    }

    /** The EXIF orientation of the image, or [ExifInterface.ORIENTATION_NORMAL] when it has none. */
    private fun readOrientation(open: () -> InputStream?): Int = try {
        open()?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    /** The transform that turns the stored pixels upright for an EXIF [orientation]. */
    private fun orientationMatrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
        }
    }

    private fun sampleSizeFor(
        width: Int,
        height: Int,
        maxPixelSize: Int
    ): Int {
        var sample = 1
        var w = width
        var h = height
        while (max(w, h) / 2 >= maxPixelSize) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }
}
