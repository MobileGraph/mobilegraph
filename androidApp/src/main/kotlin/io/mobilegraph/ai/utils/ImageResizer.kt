package io.mobilegraph.ai.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

object ImageResizer {
    /**
     * Resizes the image to fit within maxW x maxH while maintaining aspect ratio.
     * Only downscales; if the image is smaller than maxW/maxH, it is left as is.
     */
    fun downscaleToMax(
        input: ByteArray,
        maxW: Int = 2048,
        maxH: Int = 2048,
    ): ByteArray {
        val options =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeByteArray(input, 0, input.size, options)

        val srcW = options.outWidth
        val srcH = options.outHeight

        if (srcW <= maxW && srcH <= maxH) return input // No resize needed

        val ratio = srcW.toFloat() / srcH.toFloat()
        var targetW = maxW
        var targetH = (targetW / ratio).toInt()

        if (targetH > maxH) {
            targetH = maxH
            targetW = (targetH * ratio).toInt()
        }

        val bitmap = BitmapFactory.decodeByteArray(input, 0, input.size) ?: return input
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        scaled.recycle()
        if (scaled != bitmap) bitmap.recycle()

        return stream.toByteArray()
    }
}
