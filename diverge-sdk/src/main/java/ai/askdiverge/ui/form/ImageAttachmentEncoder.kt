package ai.askdiverge.ui.form

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import ai.askdiverge.domain.model.form.OutgoingAttachment
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Downsamples a picked photo and JPEG-encodes it for `POST /actions`, matching the iOS
 * `ImageAttachment` budgets (1024 px long edge at quality 0.7, one retry at 512 px).
 */
internal object ImageAttachmentEncoder {

    const val MAX_PIXEL_SIZE = 1024
    const val RETRY_PIXEL_SIZE = 512
    const val JPEG_QUALITY = 70

    /**
     * Encodes [bytes] into a [PendingFormAttachment], or `null` when the blob is not a decodable
     * image or still exceeds the action size budget after the 512 px retry.
     */
    fun encode(
        bytes: ByteArray,
        filename: String,
        maxDecodedBytes: Int
    ): PendingFormAttachment? {
        encodeOnce(bytes, MAX_PIXEL_SIZE, filename, maxDecodedBytes)?.let { return it }
        return encodeOnce(bytes, RETRY_PIXEL_SIZE, filename, maxDecodedBytes)
    }

    /** Base64 character budget for a decoded-byte cap (4 chars per 3 bytes, floors), capped. */
    fun encodedLengthBudget(maxDecodedBytes: Int): Int {
        val uncapped = (maxDecodedBytes.toLong() / 3L) * 4L
        return minOf(uncapped, OutgoingAttachment.MAX_ACTION_ENCODED_LENGTH.toLong()).toInt()
    }

    private fun encodeOnce(
        bytes: ByteArray,
        maxPixelSize: Int,
        filename: String,
        maxDecodedBytes: Int
    ): PendingFormAttachment? {
        val bitmap = decodeSampled(bytes, maxPixelSize) ?: return null
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
        bytes: ByteArray,
        maxPixelSize: Int
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxPixelSize)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val longEdge = max(decoded.width, decoded.height)
        if (longEdge <= maxPixelSize) return decoded

        val scale = maxPixelSize.toFloat() / longEdge
        val width = max(1, (decoded.width * scale).roundToInt())
        val height = max(1, (decoded.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
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
