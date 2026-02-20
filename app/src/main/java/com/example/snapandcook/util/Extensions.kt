package com.example.snapandcook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.text.HtmlCompat
import java.io.IOException

/** Show a short toast message. */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/** Make a view visible. */
fun View.show() { visibility = View.VISIBLE }

/** Make a view invisible (still takes up space). */
fun View.hide() { visibility = View.INVISIBLE }

/** Make a view gone (no space taken). */
fun View.gone() { visibility = View.GONE }

/**
 * Decode a content [Uri] to a [Bitmap] safely, with down-sampling to avoid OOM.
 *
 * @param maxWidth Maximum output width in pixels (default 1024).
 * @param maxHeight Maximum output height in pixels (default 1024).
 */
fun Context.decodeBitmapFromUri(uri: Uri, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: IOException) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/** Strip HTML tags from a string (used to clean Spoonacular summary text). */
fun String.stripHtml(): String =
    HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()

/** Format minutes as a human-readable string, e.g. "1h 30m" or "45m". */
fun Int.formatMinutes(): String {
    return if (this >= 60) {
        val h = this / 60
        val m = this % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else {
        "${this}m"
    }
}
