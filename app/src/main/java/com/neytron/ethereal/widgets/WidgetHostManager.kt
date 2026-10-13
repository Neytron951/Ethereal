package com.neytron.ethereal.widgets

import android.appwidget.AppWidgetManager
import android.content.Context

/**
 * Синглтон-обёртка для доступа к хосту из Compose.
 */
object WidgetHostManager {

    private var host: EtherealAppWidgetHost? = null

    fun getHost(context: Context): EtherealAppWidgetHost {
        if (host == null) {
            host = EtherealAppWidgetHost(context.applicationContext)
        }
        return host!!
    }

    /**
     * Стартует слушание изменений виджетов.
     */
    fun startListening(context: Context) {
        try {
            getHost(context).startListening()
        } catch (_: Throwable) {
        }
    }

    /**
     * Останавливает слушание.
     */
    fun stopListening(context: Context) {
        try {
            getHost(context).stopListening()
        } catch (_: Throwable) {
        }
    }

    /**
     * Биндит виджет — «привязывает» его к хосту, чтобы можно было показать.
     * Нужно вызывать после получения appWidgetId.
     */
    fun bindWidget(context: Context, appWidgetId: Int) {
        try {
            getHost(context).startListening()
            getHost(context).allocateAppWidgetId()
        } catch (_: Throwable) {
        }
    }

    /**
     * Удаляет виджет.
     */
    fun removeWidget(context: Context, appWidgetId: Int) {
        try {
            getHost(context).removeWidget(appWidgetId)
        } catch (_: Throwable) {
        }
    }

    /**
     * Проверяет, забинден ли виджет.
     */
    fun isBound(context: Context, appWidgetId: Int): Boolean {
        return try {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetInfo(appWidgetId) != null
        } catch (_: Throwable) {
            false
        }
    }
}