package com.neytron.ethereal.utils

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.neytron.ethereal.ui.theme.EtherealPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /**
     * Версия кэша. Увеличивается при сбросе, чтобы композиция гарантированно
     * пересоздала иконки: одного очищения LRU недостаточно — состояние
     * `produceState` в плитке продолжает держать старый битмап.
     */
    var epoch by mutableIntStateOf(0)
        private set

    fun key(packageName: String, themed: Boolean, paletteId: String, foregroundOnly: Boolean): String =
        "$packageName|$themed|$paletteId|$foregroundOnly"

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    fun clear() {
        cache.evictAll()
        epoch++
    }

    /**
     * Готовит иконку заранее — в тот же кэш, который читают плитки.
     *
     * Нужно для миниатюр пространств: без прогрева рамка сначала рисуется
     * без иконок, и они заметно «подгружаются» уже на глазах.
     * Ключ считается ровно так же, как в TileCell, иначе прогрев бесполезен.
     */
    suspend fun prewarm(
        packageName: String,
        icon: Drawable,
        themed: Boolean,
        palette: EtherealPalette,
        foregroundOnly: Boolean
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        val cacheKey = key(packageName, themed, palette.id, foregroundOnly) + "#" + epoch
        get(cacheKey)?.let { return@withContext it }

        val rendered: ImageBitmap? = when {
            themed -> renderThemedIcon(icon, palette, drawBackground = !foregroundOnly).asImageBitmap()
            foregroundOnly -> icon.toForegroundOnlyBitmap()
            else -> icon.toImageBitmap()
        }
        if (rendered != null) put(cacheKey, rendered)
        rendered
    }
}
