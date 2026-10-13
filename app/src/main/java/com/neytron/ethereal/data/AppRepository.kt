package com.neytron.ethereal.data

import android.content.Context
import android.content.Intent

object AppRepository {

    fun getInstalledApps(context: Context, iconPackPackage: String? = null): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val realApps = pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                try {
                    val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                    val label = resolveInfo.loadLabel(pm).toString()
                    val packageName = activityInfo.packageName
                    val activityName = activityInfo.name
                    val icon = iconPackPackage?.let {
                        IconPackRepository.loadIcon(context, it, packageName, activityName)
                    } ?: resolveInfo.loadIcon(pm) ?: return@mapNotNull null

                    // Пропускаем сам лаунчер
                    if (packageName == context.packageName) return@mapNotNull null

                    AppInfo(
                        label = label,
                        packageName = packageName,
                        icon = icon,
                        activityName = activityName
                    )
                } catch (_: Throwable) {
                    null
                }
            }

        // ============================================================
        // ЗАКОНСЕРВИРОВАНО: виртуальные приложения (Ethereal Phone и др.)
        // ============================================================
        // Чтобы включить обратно — раскомментировать блок ниже:
        //
        // val virtualApps = VirtualAppsProvider.getVirtualApps(context)
        // return (realApps + virtualApps).sortedBy { it.label.lowercase() }
        //
        // ============================================================

        return realApps.sortedBy { it.label.lowercase() }
    }

    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}