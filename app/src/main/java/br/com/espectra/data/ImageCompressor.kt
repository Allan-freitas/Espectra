package br.com.espectra.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Reduz fotos da câmera/galeria antes do upload: o web envia os bytes crus
 * num array JSON, e uma foto de celular (~4MB) viraria ~15MB de payload.
 * Redimensiona para no máx. 1024px (JPEG 85).
 */
object ImageCompressor {

    fun compress(bytes: ByteArray, maxDim: Int = 1024, quality: Int = 85): ByteArray {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return bytes

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                ?: return bytes

            val w = bitmap.width
            val h = bitmap.height
            val longest = max(w, h).toFloat()
            if (longest > maxDim) {
                val scale = maxDim / longest
                val scaled = Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
                if (scaled !== bitmap) bitmap.recycle()
                bitmap = scaled
            }

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bitmap.recycle()
            val compressed = out.toByteArray()
            return if (compressed.isEmpty()) bytes else compressed
        } catch (e: Exception) {
            return bytes
        }
    }
}
