package com.neytron.ethereal.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Кэш системных обоев в виде битмапа.
 *
 * Нужен для «жидкого стекла»: раньше стекло размывало только обои, заданные
 * файлом внутри лаунчера. Если пользователь ставит обычные системные обои,
 * `LocalWallpaperPath` пуст и панели становились просто полупрозрачной заливкой —
 * никакого размытия не было.
 */
object SystemWallpaperCache {

    @Volatile
    private var cached: ImageBitmap? = null

    @Volatile
    private var loaded = false

    fun get(context: Context): ImageBitmap? {
        if (loaded) return cached
        synchronized(this) {
            if (loaded) return cached
            cached = runCatching {
                val manager = WallpaperManager.getInstance(context)
                val drawable = manager.drawable ?: return@runCatching null
                val metrics = DisplayMetrics()
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(metrics)
                val width = (metrics.widthPixels / 2).coerceIn(360, 1080)
                val height = (metrics.heightPixels / 2).coerceIn(640, 2400)
                drawable.toBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }.getOrNull()
            loaded = true
            return cached
        }
    }

    fun invalidate() {
        synchronized(this) {
            cached = null
            loaded = false
        }
    }
}
