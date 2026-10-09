package com.neytron.ethereal.data

import android.content.Context

/**
 * Провайдер виртуальных приложений.
 *
 * ЗАКОНСЕРВИРОВАНО.
 *
 * Изначально использовался для подмешивания "Ethereal Phone" в Dock и ESearch.
 * Сейчас отключён — возвращает пустой список.
 *
 * Чтобы включить обратно:
 *  1. Раскомментировать в AndroidManifest.xml блок звонков.
 *  2. Раскомментировать в AppRepository вызов getVirtualApps().
 *  3. Раскомментировать код ниже.
 */
object VirtualAppsProvider {

    const val ACTION_DIALER = "dialer"
    const val DIALER_PACKAGE = "com.example.ethereal.virtual.dialer"

    fun getVirtualApps(context: Context): List<AppInfo> {
        // ============================================================
        // ЗАКОНСЕРВИРОВАНО
        // ============================================================
        // val list = mutableListOf<AppInfo>()
        //
        // if (SettingsRepository.isVirtualDialerEnabled(context)) {
        //     val icon = ContextCompat.getDrawable(context, R.drawable.ic_dialer)
        //     if (icon != null) {
        //         list.add(
        //             AppInfo(
        //                 label = "Ethereal Phone",
        //                 packageName = DIALER_PACKAGE,
        //                 icon = icon,
        //                 isVirtual = true,
        //                 virtualAction = ACTION_DIALER
        //             )
        //         )
        //     }
        // }
        //
        // return list
        // ============================================================

        return emptyList()
    }

    fun launch(context: Context, action: String) {
        // ============================================================
        // ЗАКОНСЕРВИРОВАНО
        // ============================================================
        // when (action) {
        //     ACTION_DIALER -> {
        //         val intent = Intent(context, DialerActivity::class.java).apply {
        //             addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        //         }
        //         context.startActivity(intent)
        //     }
        // }
        // ============================================================
    }
}