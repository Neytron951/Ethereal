package com.neytron.ethereal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.neytron.ethereal.data.AppIconStore
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.utils.renderThemedIcon
import com.neytron.ethereal.utils.toImageBitmap

/**
 * Иконка приложения из общего кэша.
 *
 * Возвращает `null`, пока иконка декодируется в фоне — вызывающая сторона показывает
 * плейсхолдер того же размера, поэтому список не «прыгает» и кадры не проседают.
 */
@Composable
fun rememberAppIcon(
    app: AppInfo,
    themed: Boolean,
    palette: EtherealPalette,
    sizePx: Int = AppIconStore.SIZE_PX
): ImageBitmap? {
    val key = remember(app.packageName, themed, palette.id, sizePx) {
        AppIconStore.key(app.packageName, themed, palette.id, sizePx)
    }

    // Чтение из SnapshotStateMap подписывает композицию на появление иконки
    val bitmap = AppIconStore.bitmaps[key]

    LaunchedEffect(key) {
        if (bitmap == null) {
            AppIconStore.load(key) {
                if (themed) {
                    renderThemedIcon(app.icon, palette, sizePx).asImageBitmap()
                } else {
                    app.icon.toImageBitmap(sizePx)
                }
            }
        }
    }

    return bitmap
}
