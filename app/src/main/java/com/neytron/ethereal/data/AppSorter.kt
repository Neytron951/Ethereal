package com.neytron.ethereal.data

import android.content.Context

object AppSorter {

    private const val HOUR_MS = 60L * 60L * 1000L
    private const val DAY_MS = 24L * HOUR_MS

    fun sortByScore(
        context: Context,
        apps: List<AppInfo>,
        query: String = ""
    ): List<AppInfo> {
        val now = System.currentTimeMillis()

        // 1) Фильтр по поисковому запросу (если есть)
        val filtered = if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }

        // 2) Считаем score для каждого
        val scored = filtered.map { app ->
            val count = SettingsRepository.getLaunchCount(context, app.packageName)
            val last = SettingsRepository.getLastLaunch(context, app.packageName)

            val recencyBonus = when {
                last == 0L -> 0.0
                now - last < DAY_MS -> 5.0
                now - last < 7 * DAY_MS -> 2.0
                else -> 0.0
            }

            val score = count * 2.0 + recencyBonus
            app to score
        }

        // 3) Сортировка: сначала с score > 0 (по убыванию), потом без score (по алфавиту)
        val withScore = scored.filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .map { it.first }

        val withoutScore = scored.filter { it.second == 0.0 }
            .map { it.first }
            .sortedBy { it.label.lowercase() }

        return withScore + withoutScore
    }
}