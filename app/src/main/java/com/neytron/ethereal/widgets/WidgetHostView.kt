package com.neytron.ethereal.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Контейнер для виджета, умеющий перехватывать тапы в режиме редактора (isFrozen = true).
 *
 * onInterceptTouchEvent = true предотвращает передачу касаний к дочерним View
 * системного виджета (кнопкам, приложениям и т.д.).
 *
 * onTouchEvent = false гарантирует, что сам FrameLayout не поглотит событие,
 * благодаря чему клики и долгие нажатия уходят Compose-родителю (TileCell).
 */
@SuppressLint("ClickableViewAccessibility")
class InterceptingFrameLayout(context: Context) : FrameLayout(context) {
    var isFrozen: Boolean = false
    var appWidgetId: Int = 0
    var widgetView: AppWidgetHostView? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (isFrozen) true else super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (isFrozen) false else super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && appWidgetId != 0 && widgetView != null) {
            updateWidgetOptions(context, appWidgetId, widgetView!!, w, h)
        }
    }

    companion object {
        fun updateWidgetOptions(
            context: Context,
            appWidgetId: Int,
            widgetView: AppWidgetHostView,
            widthPx: Int,
            heightPx: Int
        ) {
            try {
                val density = context.resources.displayMetrics.density
                if (density <= 0f) return
                val widthDp = (widthPx / density).toInt()
                val heightDp = (heightPx / density).toInt()

                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                }

                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
                widgetView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp)
            } catch (e: Exception) {
                Log.e("EtherealWidget", "Error updating widget options for id=$appWidgetId", e)
            }
        }
    }
}

@Composable
fun WidgetHostView(
    appWidgetId: Int,
    frozen: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    key(appWidgetId) {
        AndroidView(
            modifier = modifier,
            factory = {
                Log.d("EtherealWidget", "WidgetHostView factory: constructing container for appWidgetId=$appWidgetId")
                val host = WidgetHostManager.getHost(context)
                val widgetView = host.createHostView(context, appWidgetId)

                val container = InterceptingFrameLayout(context).apply {
                    this.appWidgetId = appWidgetId
                    this.widgetView = widgetView
                }

                if (widgetView != null) {
                    (widgetView.parent as? ViewGroup)?.removeView(widgetView)
                    container.addView(
                        widgetView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    Log.d("EtherealWidget", "WidgetHostView factory: widgetView attached successfully for appWidgetId=$appWidgetId")
                } else {
                    Log.e("EtherealWidget", "WidgetHostView factory: widgetView is NULL for appWidgetId=$appWidgetId")
                }

                container
            },
            update = { container ->
                container.isFrozen = frozen
                container.appWidgetId = appWidgetId
                if (container.width > 0 && container.height > 0 && container.widgetView != null) {
                    InterceptingFrameLayout.updateWidgetOptions(
                        context,
                        appWidgetId,
                        container.widgetView!!,
                        container.width,
                        container.height
                    )
                }
            }
        )
    }
}