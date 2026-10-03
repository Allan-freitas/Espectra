package br.com.espectra.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Equivalente mobile do DeviceDetectorService + IpService do web.
 * A API aceita DeviceInfo nulo, mas o web sempre envia — fazemos o mesmo
 * para preencher LoginHistory (ver AuthService.LoginAsync).
 */
object DeviceInfoProvider {

    suspend fun build(context: Context): DeviceInfo {
        val orientation = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            Configuration.ORIENTATION_PORTRAIT -> "portrait"
            else -> "unknown"
        }
        return DeviceInfo(
            browser = "Espectra Android",
            browserVersion = null,
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            device = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            deviceType = "mobile",
            orientation = orientation,
            userAgent = System.getProperty("http.agent"),
            isDesktopMode = false,
            ipAddress = getPublicIp(),
        )
    }

    /** Mesmo provedor do web (api.ipify.org). Timeout curto + fallback nulo. */
    private suspend fun getPublicIp(): String? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(5000) {
            try {
                val url = URL("https://api.ipify.org?format=json")
                val conn = url.openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    conn.connect()
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withTimeoutOrNull null
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(body).optString("ip").takeIf { it.isNotBlank() }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
