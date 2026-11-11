package ai.askdiverge.ui.form

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
 * orientation is applied to the pixels, since the re-encoded JPEG carries no EXIF, and
 * transparent pixels are flattened onto white.
 */
internal object ImageAttachmentEncoder {

    const val MAX_PIXEL_SIZE = 1024
    const val RETRY_PIXEL_SIZE = 512
    const val JPEG_QUALITY = 70

    /**
     * How to turn stored pixels upright: rotate clockwise by [degrees], then mirror horizontally
     * when [flipHorizontal].
     */
    data class UprightTransform(
        val degrees: Float,
        val flipHorizontal: Boolean
    )

    /**
     * Encodes the image [open] streams into a [PendingFormAttachment], or `null` when it cannot be
     * read or decoded, or still exceeds the action size budget after the 512 px retry. [open] is
     * called for the EXIF read, the bounds read and the decode, and each stream it returns is
     * closed here. Blocks; call it off the main thread.
     */
    fun encode(
        open: () -> InputStream?,
        filename: String,
        maxDecodedBytes: Int
    ): PendingFormAttachment? = try {
        val orientation = readOrientation(open)
        val upright = decodeUpright(open, orientation)
        if (upright == null) {
            null
        } else {
            try {
                encodeBitmap(upright, filename, maxDecodedBytes) ?: run {
                    val smaller = scaleToLongEdge(upright, RETRY_PIXEL_SIZE)
                    try {
                        encodeBitmap(smaller, filename, maxDecodedBytes)
                    } finally {
                        if (smaller !== upright) smaller.recycle()
                    }
                }
            } finally {
                upright.recycle()
            }
        }
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

    /** The transform for an EXIF [orientation]; unknown values leave the pixels as stored. */
    fun uprightTransform(orientation: Int): UprightTransform = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> UprightTransform(0f, flipHorizontal = true)
        ExifInterface.ORIENTATION_ROTATE_180 -> UprightTransform(180f, flipHorizontal = false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> UprightTransform(180f, flipHorizontal = true)
        ExifInterface.ORIENTATION_TRANSPOSE -> UprightTransform(90f, flipHorizontal = true)
        ExifInterface.ORIENTATION_ROTATE_90 -> UprightTransform(90f, flipHorizontal = false)
        ExifInterface.ORIENTATION_TRANSVERSE -> UprightTransform(270f, flipHorizontal = true)
        ExifInterface.ORIENTATION_ROTATE_270 -> UprightTransform(270f, flipHorizontal = false)
        else -> UprightTransform(0f, flipHorizontal = false)
    }

    private fun encodeBitmap(
        bitmap: Bitmap,
        filename: String,
        maxDecodedBytes: Int
    ): PendingFormAttachment? {
        val jpeg = ByteArrayOutputStream().use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) return null
            stream.toByteArray()
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

    /** Decodes at most [MAX_PIXEL_SIZE] on the long edge, upright and opaque. */
    private fun decodeUpright(
        open: () -> InputStream?,
        orientation: Int
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (open() ?: return null).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_PIXEL_SIZE)
        }
        val decoded = open()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val transform = uprightTransform(orientation)
        val matrix = Matrix().apply {
            postRotate(transform.degrees)
            if (transform.flipHorizontal) postScale(-1f, 1f)
        }
        val longEdge = max(decoded.width, decoded.height)
        if (longEdge > MAX_PIXEL_SIZE) {
            val scale = MAX_PIXEL_SIZE.toFloat() / longEdge
            matrix.preScale(scale, scale)
        }
        val transformed = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        }
        return flattenOnWhite(transformed)
    }

    /** JPEG has no alpha, so transparent pixels are composited onto white rather than black. */
    private fun flattenOnWhite(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        bitmap.recycle()
        return opaque
    }

    /** [bitmap] scaled so its long edge is at most [maxPixelSize]; the same instance when it fits. */
    private fun scaleToLongEdge(
        bitmap: Bitmap,
        maxPixelSize: Int
    ): Bitmap {
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge <= maxPixelSize) return bitmap
        val scale = maxPixelSize.toFloat() / longEdge
        return Bitmap.createScaledBitmap(
            bitmap,
            max(1, (bitmap.width * scale).toInt()),
            max(1, (bitmap.height * scale).toInt()),
            true
        )
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
