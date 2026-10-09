package com.neytron.ethereal.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Тонкая обёртка над Firebase Analytics.
 *
 * Все события проходят через проверку isEnabled(context) — если пользователь
 * отключил статистику в настройках, ни одно событие не улетит.
 *
 * События намеренно минимальные и агрегированные:
 *  - какие темы и шрифты выбирают (топ палитр/шрифтов)
 *  - какие фичи вообще используются (edit mode, spaces, widgets)
 *  - воронка онбординга
 *
 * НЕ отправляем: имена пакетов, поисковые запросы, местоположение,
 * содержимое плиток, модель устройства и что-либо, что можно привязать к пользователю.
 */
object AnalyticsHelper {

    // === Имена событий ===
    private const val EVENT_SELECT_CONTENT = FirebaseAnalytics.Event.SELECT_CONTENT
    private const val EVENT_TUTORIAL_BEGIN = FirebaseAnalytics.Event.TUTORIAL_BEGIN
    private const val EVENT_TUTORIAL_COMPLETE = FirebaseAnalytics.Event.TUTORIAL_COMPLETE
    private const val EVENT_UNLOCK_ACHIEVEMENT = FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT
    private const val EVENT_SCREEN_VIEW = FirebaseAnalytics.Event.SCREEN_VIEW

    // === Параметры ===
    private const val PARAM_CONTENT_TYPE = FirebaseAnalytics.Param.CONTENT_TYPE
    private const val PARAM_ITEM_ID = FirebaseAnalytics.Param.ITEM_ID
    private const val PARAM_ACHIEVEMENT_ID = FirebaseAnalytics.Param.ACHIEVEMENT_ID
    private const val PARAM_SCREEN_NAME = FirebaseAnalytics.Param.SCREEN_NAME

    // === content_type значения ===
    private const val TYPE_PALETTE = "palette"
    private const val TYPE_FONT = "font"
    private const val TYPE_THEMED_ICONS = "themed_icons"

    // === achievement_id значения ===
    private const val FEATURE_EDIT_MODE = "edit_mode"
    private const val FEATURE_CREATE_SPACE = "create_space"
    private const val FEATURE_ADD_WIDGET = "add_widget"
    private const val FEATURE_CHANGE_WALLPAPER = "change_wallpaper"
    private const val FEATURE_ESEARCH_OPEN = "esearch_open"

    private fun analytics(context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context.applicationContext)

    private fun isEnabled(context: Context): Boolean =
        SettingsRepository.getAnalyticsEnabled(context)

    private fun log(context: Context, event: String, params: Bundle? = null) {
        if (!isEnabled(context)) return
        try {
            analytics(context).logEvent(event, params)
        } catch (_: Exception) {
            // Firebase не критичен для работы лаунчера
        }
    }

    // ============================================================
    // Онбординг
    // ============================================================

    fun logOnboardingBegin(context: Context) {
        log(context, EVENT_TUTORIAL_BEGIN)
    }

    fun logOnboardingComplete(context: Context) {
        log(context, EVENT_TUTORIAL_COMPLETE)
    }

    /**
     * Шаг онбординга. Вызывать при выходе с шага (вперёд или назад),
     * а также при завершении.
     *
     * @param step номер шага 1..4
     * @param completed true — пользователь прошёл шаг, false — ушёл с него
     */
    fun logOnboardingStep(context: Context, step: Int, completed: Boolean) {
        val params = Bundle().apply {
            putInt("step", step)
            putString("status", if (completed) "completed" else "left")
        }
        log(context, "onboarding_step", params)
    }

    // ============================================================
    // Внешний вид
    // ============================================================

    fun logPaletteSelected(context: Context, paletteId: String) {
        val params = Bundle().apply {
            putString(PARAM_CONTENT_TYPE, TYPE_PALETTE)
            putString(PARAM_ITEM_ID, paletteId)
        }
        log(context, EVENT_SELECT_CONTENT, params)
    }

    fun logFontSelected(context: Context, fontName: String) {
        val params = Bundle().apply {
            putString(PARAM_CONTENT_TYPE, TYPE_FONT)
            putString(PARAM_ITEM_ID, fontName)
        }
        log(context, EVENT_SELECT_CONTENT, params)
    }

    fun logThemedIconsToggled(context: Context, enabled: Boolean) {
        val params = Bundle().apply {
            putString(PARAM_CONTENT_TYPE, TYPE_THEMED_ICONS)
            putString(PARAM_ITEM_ID, if (enabled) "on" else "off")
        }
        log(context, EVENT_SELECT_CONTENT, params)
    }

    // ============================================================
    // Использование функций
    // ============================================================

    fun logEditModeEntered(context: Context) {
        logFeatureUsed(context, FEATURE_EDIT_MODE)
    }

    fun logSpaceCreated(context: Context) {
        logFeatureUsed(context, FEATURE_CREATE_SPACE)
    }

    fun logWidgetAdded(context: Context) {
        logFeatureUsed(context, FEATURE_ADD_WIDGET)
    }

    fun logWallpaperChanged(context: Context) {
        logFeatureUsed(context, FEATURE_CHANGE_WALLPAPER)
    }

    fun logESearchOpened(context: Context) {
        logFeatureUsed(context, FEATURE_ESEARCH_OPEN)
    }

    private fun logFeatureUsed(context: Context, featureId: String) {
        val params = Bundle().apply {
            putString(PARAM_ACHIEVEMENT_ID, featureId)
        }
        log(context, EVENT_UNLOCK_ACHIEVEMENT, params)
    }

    // ============================================================
    // Навигация (опционально)
    // ============================================================

    fun logScreen(context: Context, screenName: String) {
        val params = Bundle().apply {
            putString(PARAM_SCREEN_NAME, screenName)
        }
        log(context, EVENT_SCREEN_VIEW, params)
    }
}