package com.neytron.ethereal.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Log
import android.view.ViewGroup

class EtherealAppWidgetHost(private val appContext: Context) : AppWidgetHost(appContext, HOST_ID) {

    companion object {
        const val HOST_ID = 1024
        const val CONFIG_PREFIX = "appwidget:"

        fun encodeWidgetId(id: Int): String = "$CONFIG_PREFIX$id"

        fun decodeWidgetId(config: String?): Int? {
            if (config.isNullOrBlank()) return null
            if (!config.startsWith(CONFIG_PREFIX)) return null
            return config.removePrefix(CONFIG_PREFIX).toIntOrNull()
        }
    }

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidgetInfo: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return super.onCreateView(context, appWidgetId, appWidgetInfo)
    }

    fun createHostView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        return try {
            val info = getProviderInfo(appWidgetId)
            val view = createView(context, appWidgetId, info)
            (view?.parent as? ViewGroup)?.removeView(view)
            view
        } catch (_: Throwable) {
            null
        }
    }

    fun removeWidget(appWidgetId: Int) {
        try {
            deleteAppWidgetId(appWidgetId)
        } catch (_: Throwable) {
        }
    }

    fun getProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return try {
            AppWidgetManager.getInstance(appContext).getAppWidgetInfo(appWidgetId)
        } catch (_: Throwable) {
            null
        }
    }
}