package com.neytron.ethereal.utils

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Глобальный кэш растровых иконок приложений.
 *
 * Раньше иконка растеризовалась (`renderThemedIcon` / `toImageBitmap`) прямо в композиции
 * каждой плитки — при переключении пространств это давало заметный лаг, потому что
 * работа выполнялась на главном потоке заново для каждого экрана.
 *
 * Кэш живёт в процессе: повторные входы в пространство берут готовые битмапы.
 * Ключ включает пакет, режим темы, палитру и режим «только foreground».
 */
object AppIconBitmaps {

    // ~96 иконок по 256×256×4 байта ≈ 24 МБ верхняя граница, LruCache сам вытесняет
    private val cache = LruCache<String, ImageBitmap>(96)

    fun key(packageName: String, themed: Boolean, paletteId: String, foregroundOnly: Boolean): String =
        "$packageName|$themed|$paletteId|$foregroundOnly"

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
}
