package com.neytron.ethereal.data

/**
 * Информация об обновлении, полученная из GitHub Releases.
 *
 * Хранится в кэше (SharedPreferences) в виде JSON, чтобы не дёргать API
 * при каждом открытии настроек.
 */
data class UpdateInfo(
    /** Версия текущей установки, например v1.0.0 */
    val currentVersion: Version,

    /** Последняя доступная версия из GitHub Releases */
    val latestVersion: Version,

    /** Changelog (markdown), взятый из body релиза */
    val changelog: String,

    /** Ссылка на страницу релиза (html_url) — открывается в браузере */
    val htmlUrl: String,

    /** Прямая ссылка на APK файл в релизе (asset browser_download_url) */
    val apkUrl: String,

    /** Дата публикации релиза (ISO 8601 из GitHub) */
    val publishedAt: String,

    /** Когда мы последний раз проверяли (System.currentTimeMillis) */
    val checkedAt: Long
) {
    /** Есть ли доступное обновление */
    val hasUpdate: Boolean get() = latestVersion > currentVersion

    companion object {
        /**
         * Проверяет, устарел ли кэш.
         * TTL по умолчанию — 6 часов.
         */
        fun isCacheExpired(checkedAt: Long, ttlMs: Long = DEFAULT_TTL_MS): Boolean {
            if (checkedAt <= 0L) return true
            return System.currentTimeMillis() - checkedAt > ttlMs
        }

        const val DEFAULT_TTL_MS: Long = 6L * 60L * 60L * 1000L
    }
}
