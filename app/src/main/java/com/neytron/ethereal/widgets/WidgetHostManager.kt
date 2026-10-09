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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Останавливает слушание.
     */
    fun stopListening(context: Context) {
        try {
            getHost(context).stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Удаляет виджет.
     */
    fun removeWidget(context: Context, appWidgetId: Int) {
        try {
            getHost(context).removeWidget(appWidgetId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Проверяет, забинден ли виджет.
     */
    fun isBound(context: Context, appWidgetId: Int): Boolean {
        return try {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetInfo(appWidgetId) != null
        } catch (e: Exception) {
            false
        }
    }
}