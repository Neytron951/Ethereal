package com.neytron.ethereal.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.utils.renderThemedIcon
import com.neytron.ethereal.utils.toImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Кэш иконок приложений в виде готовых [ImageBitmap].
 *
 * Зачем: раньше каждая строка ESearch растрировала Drawable прямо в композиции
 * (`toImageBitmap()` / `renderThemedIcon()` на main thread) — при открытии и быстром
 * свайпе это давало просадку fps. Теперь иконки декодируются в фоне (2 потока),
 * кэшируются по ключу «пакет + тема + палитра» и переиспользуются между открытиями.
 *
 * Кэш — Compose SnapshotStateMap, поэтому строка сама перерисуется, когда иконка готова.
 */
object AppIconStore {

    /** Размер растрирования: 128 px достаточно и для строк (28 dp), и для сетки (44 dp). */
    const val SIZE_PX = 128

    /** Больше — не держим: старые вытесняются (иконка догрузится заново в фоне). */
    private const val MAX_ENTRIES = 320

    val bitmaps = mutableStateMapOf<String, ImageBitmap>()

    private val inFlight: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val order = ArrayDeque<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(2))

    fun key(packageName: String, themed: Boolean, paletteId: String, sizePx: Int = SIZE_PX): String =
        "$packageName|$themed|$paletteId|$sizePx"

    fun cached(packageName: String, themed: Boolean, paletteId: String): ImageBitmap? =
        bitmaps[key(packageName, themed, paletteId)]

    /** Асинхронно декодирует иконку, если её ещё нет в кэше. Повторные вызовы игнорируются. */
    fun load(key: String, produce: () -> ImageBitmap?) {
        if (bitmaps.containsKey(key)) return
        if (!inFlight.add(key)) return
        scope.launch {
            val bitmap = try {
                produce()
            } catch (_: Throwable) {
                null
            }
            withContext(Dispatchers.Main.immediate) {
                inFlight.remove(key)
                if (bitmap != null && !bitmaps.containsKey(key)) {
                    put(key, bitmap)
                }
            }
        }
    }

    /**
     * Прогревает кэш для всего списка приложений — делает открытие ESearch мгновенным.
     * Вызывать после загрузки списка приложений и при смене палитры/пакета иконок.
     */
    fun prefetch(apps: List<AppInfo>, themed: Boolean, palette: EtherealPalette) {
        for (app in apps) {
            val k = key(app.packageName, themed, palette.id)
            if (bitmaps.containsKey(k)) continue
            load(k) {
                if (themed) {
                    renderThemedIcon(app.icon, palette, SIZE_PX).asImageBitmap()
                } else {
                    app.icon.toImageBitmap(SIZE_PX)
                }
            }
        }
    }

    /** Сброс при смене пакета иконок — иконки могли измениться. */
    fun clear() {
        bitmaps.clear()
        order.clear()
        inFlight.clear()
    }

    private fun put(key: String, bitmap: ImageBitmap) {
        bitmaps[key] = bitmap
        order.addLast(key)
        while (order.size > MAX_ENTRIES) {
            val oldest = order.removeFirst()
            if (oldest != key) bitmaps.remove(oldest)
        }
    }
}
