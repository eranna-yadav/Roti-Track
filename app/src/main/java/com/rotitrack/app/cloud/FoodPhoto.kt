package com.rotitrack.app.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Shrinks a food photo to a small, upright JPEG: enough detail for the scanner, quick to upload. */
object FoodPhoto {
    private const val MAX_EDGE = 1024
    private const val QUALITY = 80

    /** Reads, rotates and shrinks the photo at [uri]; null if it can't be read. */
    fun prepare(context: Context, uri: Uri): ByteArray? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Decode at roughly twice the target size, then scale down smoothly.
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE * 2) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val degrees = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = MAX_EDGE.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (degrees != 0f) postRotate(degrees)
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
    }.getOrNull()
}
