package com.neytron.ethereal.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Репозиторий проверки обновлений через GitHub Releases API.
 *
 * Особенности:
 *  - Работает анонимно (публичный репо, без токена).
 *  - Кэширует последний успешный ответ в SharedPreferences.
 *  - TTL кэша — 6 часов (см. UpdateInfo.DEFAULT_TTL_MS).
 *  - Сетевые ошибки не бросает — возвращает null.
 *
 * GitHub лимит без токена: 60 запросов/час на IP.
 * Мы дёргаем API не чаще раза в 6 часов — с огромным запасом.
 */
object UpdateRepository {

    private const val TAG = "UpdateRepo"

    // === GitHub ===
    private const val GITHUB_OWNER = "Neytron951"
    private const val GITHUB_REPO = "Ethereal"
    private const val API_LATEST_RELEASE =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    // === Кэш ===
    private const val PREFS = "ethereal_update"
    private const val KEY_CACHE = "update_cache_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ============================================================
    // Публичный API
    // ============================================================

    /**
     * Возвращает информацию об обновлении.
     *
     * @param forceRefresh true — игнорировать кэш и сделать запрос.
     * @param currentVersion текущая версия приложения (обычно BuildConfig.VERSION_NAME).
     *
     * Логика:
     *  1. Если forceRefresh == false и кэш свежий — вернуть кэш.
     *  2. Иначе — сделать запрос к GitHub, сохранить в кэш, вернуть результат.
     *  3. Если запрос провалился, но есть кэш (пусть и устаревший) — вернуть кэш.
     *  4. Если и кэша нет — вернуть null.
     */
    fun getUpdateInfo(
        context: Context,
        currentVersion: String,
        forceRefresh: Boolean = false
    ): UpdateInfo? {
        val current = Version.parse(currentVersion)
        if (current == null) {
            Log.w(TAG, "Cannot parse current version: $currentVersion")
            return null
        }

        // 1. Кэш свежий?
        if (!forceRefresh) {
            val cached = readCache(context)
            if (cached != null && !UpdateInfo.isCacheExpired(cached.checkedAt)) {
                Log.d(TAG, "Using fresh cache")
                return cached
            }
        }

        // 2. Свежий запрос
        val fetched = fetchFromGitHub(current)
        if (fetched != null) {
            writeCache(context, fetched)
            return fetched
        }

        // 3. Фолбэк на устаревший кэш
        val stale = readCache(context)
        if (stale != null) {
            Log.d(TAG, "Network failed, using stale cache")
            return stale
        }

        Log.w(TAG, "No data available")
        return null
    }

    /**
     * Очищает кэш. Например, при выходе из аккаунта или для отладки.
     */
    fun clearCache(context: Context) {
        prefs(context).edit().remove(KEY_CACHE).apply()
    }

    // ============================================================
    // HTTP
    // ============================================================

    private fun fetchFromGitHub(current: Version): UpdateInfo? {
        return try {
            val url = URL(API_LATEST_RELEASE)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            conn.setRequestProperty(
                "User-Agent",
                "Ethereal-Launcher/${current}"
            )

            val code = conn.responseCode
            Log.d(TAG, "GitHub API response code: $code")

            if (code != 200) {
                conn.disconnect()
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            parseRelease(current, response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching release: ${e.message}", e)
            null
        }
    }

    private fun parseRelease(current: Version, json: String): UpdateInfo? {
        return try {
            val obj = JSONObject(json)

            val tagName = obj.optString("tag_name", "")
            val latest = Version.parse(tagName) ?: run {
                Log.w(TAG, "Cannot parse tag_name: '$tagName'")
                return null
            }

            val assets = obj.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            UpdateInfo(
                currentVersion = current,
                latestVersion = latest,
                changelog = obj.optString("body", "").trim(),
                htmlUrl = obj.optString("html_url", ""),
                apkUrl = apkUrl,
                publishedAt = obj.optString("published_at", ""),
                checkedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing release JSON: ${e.message}", e)
            null
        }
    }

    // ============================================================
    // Кэш (SharedPreferences + JSON)
    // ============================================================

    private fun readCache(context: Context): UpdateInfo? {
        val raw = prefs(context).getString(KEY_CACHE, null) ?: return null
        return try {
            val obj = JSONObject(raw)

            val currentRaw = obj.optString("currentVersion", "")
            val latestRaw = obj.optString("latestVersion", "")

            val current = Version.parse(currentRaw) ?: return null
            val latest = Version.parse(latestRaw) ?: return null

            UpdateInfo(
                currentVersion = current,
                latestVersion = latest,
                changelog = obj.optString("changelog", ""),
                htmlUrl = obj.optString("htmlUrl", ""),
                apkUrl = obj.optString("apkUrl", ""),
                publishedAt = obj.optString("publishedAt", ""),
                checkedAt = obj.optLong("checkedAt", 0L)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cache: ${e.message}", e)
            null
        }
    }

    private fun writeCache(context: Context, info: UpdateInfo) {
        try {
            val obj = JSONObject().apply {
                put("currentVersion", info.currentVersion.toString())
                put("latestVersion", info.latestVersion.toString())
                put("changelog", info.changelog)
                put("htmlUrl", info.htmlUrl)
                put("apkUrl", info.apkUrl)
                put("publishedAt", info.publishedAt)
                put("checkedAt", info.checkedAt)
            }
            prefs(context).edit().putString(KEY_CACHE, obj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing cache: ${e.message}", e)
        }
    }
}