package br.com.espectra.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherDay(val date: String, val maxTempF: Double)

data class WeatherInfo(
    val tempF: Double,
    val condition: String,
    val humidity: Int,
    val windKph: Double,
    /** Dias seguintes (como forecastDays() do web: pula o dia atual). */
    val nextDays: List<WeatherDay>,
)

/**
 * Espelha WeatherService do web (mesma chave/local). Fora do Retrofit
 * porque é outra origem e não usa JWT.
 */
object WeatherRepository {
    private const val API_URL = "https://api.weatherapi.com/v1/forecast.json"
    private const val API_KEY = "3ad93257bb3e4e3db1c195559260208"

    suspend fun getForecast(location: String = "Sao Paulo", days: Int = 5): WeatherInfo? =
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(8000) {
                try {
                    val q = URLEncoder.encode(location, "UTF-8")
                    val url = URL("$API_URL?key=$API_KEY&q=$q&days=$days")
                    val conn = url.openConnection() as HttpURLConnection
                    try {
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        conn.requestMethod = "GET"
                        conn.connect()
                        if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withTimeoutOrNull null
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        parse(JSONObject(body))
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

    private fun parse(root: JSONObject): WeatherInfo {
        val current = root.getJSONObject("current")
        val forecastDays = root.getJSONObject("forecast").getJSONArray("forecastday")
        val nextDays = mutableListOf<WeatherDay>()
        for (i in 1 until forecastDays.length()) {
            val d = forecastDays.getJSONObject(i)
            nextDays += WeatherDay(
                date = d.getString("date"),
                maxTempF = d.getJSONObject("day").getDouble("maxtemp_f"),
            )
        }
        return WeatherInfo(
            tempF = current.getDouble("temp_f"),
            condition = current.getJSONObject("condition").getString("text"),
            humidity = current.getInt("humidity"),
            windKph = current.getDouble("wind_kph"),
            nextDays = nextDays,
        )
    }
}
