package com.neytron.ethereal.weather

data class WeatherData(
    val temperature: Int,
    val weatherCode: Int,
    val city: String
) {
    companion object {
        val EMPTY = WeatherData(
            temperature = 0,
            weatherCode = -1,
            city = ""
        )
    }
}