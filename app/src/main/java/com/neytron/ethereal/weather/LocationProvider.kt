package com.neytron.ethereal.weather

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val city: String
)

data class CityResult(
    val name: String,
    val country: String,
    val admin1: String,
    val latitude: Double,
    val longitude: Double
) {
    fun displayName(): String {
        return buildString {
            append(name)
            if (admin1.isNotBlank() && admin1 != name) {
                append(", ")
                append(admin1)
            }
            if (country.isNotBlank()) {
                append(", ")
                append(country)
            }
        }
    }

    fun toConfig(): String {
        return "$name|$latitude|$longitude"
    }

    companion object {
        fun parseConfig(config: String?): CityResult? {
            if (config.isNullOrBlank()) return null
            val parts = config.split("|")
            if (parts.size != 3) return null
            return try {
                CityResult(
                    name = parts[0],
                    country = "",
                    admin1 = "",
                    latitude = parts[1].toDouble(),
                    longitude = parts[2].toDouble()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

object LocationProvider {

    private const val TAG = "WeatherLocation"

    fun getCoordinatesByIp(context: Context): Coordinates? {
        val services = listOf(
            "https://ipwho.is/",
            "http://ip-api.com/json/"
        )

        for (urlStr in services) {
            val result = tryFetch(urlStr)
            if (result != null) return result
        }
        return null
    }

    private fun tryFetch(urlStr: String): Coordinates? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            Log.d(TAG, "Response code from $urlStr: $code")

            if (code != 200) {
                conn.disconnect()
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(response)

            if (urlStr.contains("ipwho.is")) {
                val success = json.optBoolean("success", true)
                if (!success) return null

                val lat = json.optDouble("latitude", Double.NaN)
                val lon = json.optDouble("longitude", Double.NaN)
                val city = json.optString("city", "")
                if (lat.isNaN() || lon.isNaN()) return null
                return Coordinates(lat, lon, city)
            }

            if (urlStr.contains("ip-api.com")) {
                val status = json.optString("status", "")
                if (status != "success") return null

                val lat = json.optDouble("lat", Double.NaN)
                val lon = json.optDouble("lon", Double.NaN)
                val city = json.optString("city", "")
                if (lat.isNaN() || lon.isNaN()) return null
                return Coordinates(lat, lon, city)
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching $urlStr: ${e.message}", e)
            null
        }
    }

    fun getCoordinatesByCity(cityName: String): Coordinates? {
        if (cityName.isBlank()) return null
        return try {
            val encoded = java.net.URLEncoder.encode(cityName, "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=ru&format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(response)
            val results = json.optJSONArray("results") ?: return null
            if (results.length() == 0) return null

            val first = results.getJSONObject(0)
            Coordinates(
                latitude = first.getDouble("latitude"),
                longitude = first.getDouble("longitude"),
                city = first.optString("name", cityName)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding $cityName: ${e.message}", e)
            null
        }
    }

    fun searchCities(query: String, limit: Int = 5): List<CityResult> {
        if (query.length < 2) return emptyList()
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=$limit&language=ru&format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(response)
            val results = json.optJSONArray("results") ?: return emptyList()

            val list = mutableListOf<CityResult>()
            for (i in 0 until results.length()) {
                val obj = results.getJSONObject(i)
                list.add(
                    CityResult(
                        name = obj.optString("name", ""),
                        country = obj.optString("country", ""),
                        admin1 = obj.optString("admin1", ""),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error searching cities: ${e.message}", e)
            emptyList()
        }
    }
}