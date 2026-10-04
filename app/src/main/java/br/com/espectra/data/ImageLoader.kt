package br.com.espectra.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Carregador simples de imagens autenticadas (sem Coil, que não está no
 * cache offline). Avatares/mídias usam o mesmo Bearer do jwt.interceptor.
 */
object ImageLoader {

    private val cache: LruCache<String, Bitmap> = run {
        val maxMem = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        object : LruCache<String, Bitmap>(maxMem / 8) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        }
    }

    fun resolve(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        if (pathOrUrl.startsWith("http")) return pathOrUrl
        val path = if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl"
        return ApiClient.API_ORIGIN + path
    }

    /** Remove do cache (usado após troca de avatar/capa). */
    fun evict(url: String?) {
        if (url == null) return
        cache.remove(url)
        cache.remove(url.substringBefore("?"))
    }

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }
        try {
            val request = Request.Builder().url(url).get().build()
            ApiClient.httpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                // OkHttp 3.x (transitivo do Retrofit): body() é método, não propriedade.
                val bytes = response.body()?.bytes() ?: return@withContext null
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext null
                cache.put(url, bitmap)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }
}
