package com.neytron.ethereal.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val activityName: String? = null,
    // Законсервировано: используется для виртуальных приложений (Ethereal Phone и др.)
    // Чтобы включить обратно — вернуть VirtualAppsProvider.getVirtualApps() в AppRepository.
    val isVirtual: Boolean = false,
    val virtualAction: String? = null
)