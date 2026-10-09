package com.neytron.ethereal.weather

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WeatherRepository {

    private const val TAG = "WeatherRepo"

    private const val PREFS = "ethereal_weather"
    private const val KEY_TEMP = "temp"
    private const val KEY_CODE = "code"
    private const val KEY_CITY = "city"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_LAST_CONFIG = "last_config"

    private const val CACHE_TTL_MS = 60L * 60L * 1000L
    private const val WEATHER_CODE = -1

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // === Кэш ===

    fun getCached(context: Context): WeatherData? {
        val p = prefs(context)
        val ts = p.getLong(KEY_TIMESTAMP, 0L)
        if (ts == 0L) return null
        if (System.currentTimeMillis() - ts > CACHE_TTL_MS) return null

        return WeatherData(
            temperature = p.getInt(KEY_TEMP, 0),
            weatherCode = p.getInt(KEY_CODE, WEATHER_CODE),
            city = p.getString(KEY_CITY, "") ?: ""
        )
    }

    fun saveCache(context: Context, data: WeatherData, config: String?) {
        prefs(context).edit()
            .putInt(KEY_TEMP, data.temperature)
            .putInt(KEY_CODE, data.weatherCode)
            .putString(KEY_CITY, data.city)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putString(KEY_LAST_CONFIG, config ?: "")
            .apply()
    }

    fun clearCache(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun getLastConfig(context: Context): String {
        return prefs(context).getString(KEY_LAST_CONFIG, "") ?: ""
    }

    // === Основная логика ===

    /**
     * Получает погоду с учётом config.
     *  - Если config изменился с прошлого раза — принудительно новый запрос.
     *  - Иначе — кэш, если свежий.
     */
    fun getWeather(context: Context, config: String?): WeatherData? {
        val configKey = config ?: ""
        val lastConfig = getLastConfig(context)

        // Если config изменился — сбрасываем кэш и делаем новый запрос
        if (configKey != lastConfig) {
            Log.d(TAG, "Config changed: '$lastConfig' → '$configKey'. Refetching.")
            clearCache(context)
            return fetchWeather(context, config)
        }

        // Config тот же — смотрим кэш
        val cached = getCached(context)
        if (cached != null) {
            Log.d(TAG, "Using cache for config='$configKey'")
            return cached
        }

        // Кэш пустой/старый — запрос
        return fetchWeather(context, config)
    }

    /**
     * Принудительный запрос (игнорирует кэш).
     */
    fun fetchWeather(context: Context, config: String?): WeatherData? {
        val coords: Coordinates? = if (!config.isNullOrBlank()) {
            // config = "Москва|55.75|37.62"
            val city = CityResult.parseConfig(config)
            if (city != null) {
                Log.d(TAG, "Using manual city: ${city.name} (${city.latitude}, ${city.longitude})")
                Coordinates(city.latitude, city.longitude, city.name)
            } else {
                Log.d(TAG, "Config malformed, falling back to IP")
                LocationProvider.getCoordinatesByIp(context)
            }
        } else {
            Log.d(TAG, "No config, using IP")
            LocationProvider.getCoordinatesByIp(context)
        }

        if (coords == null) {
            Log.e(TAG, "No coordinates available")
            return null
        }

        return try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=${coords.latitude}" +
                        "&longitude=${coords.longitude}" +
                        "&current=temperature_2m,weather_code" +
                        "&timezone=auto"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val code = conn.responseCode
            Log.d(TAG, "Open-Meteo code: $code")
            if (code != 200) {
                conn.disconnect()
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(response)
            val current = json.optJSONObject("current") ?: return null

            val temp = current.optDouble("temperature_2m", 0.0).toInt()
            val wcode = current.optInt("weather_code", WEATHER_CODE)

            val data = WeatherData(
                temperature = temp,
                weatherCode = wcode,
                city = coords.city
            )

            saveCache(context, data, config)
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather: ${e.message}", e)
            null
        }
    }
}