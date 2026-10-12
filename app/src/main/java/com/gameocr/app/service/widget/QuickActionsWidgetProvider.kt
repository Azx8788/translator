package com.gameocr.app.service.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gameocr.app.R
import com.gameocr.app.capture.CaptureStartRequestActivity
import com.gameocr.app.service.CaptureService
import com.gameocr.app.ui.MainActivity

/**
 * 桌面快捷小部件（P4）：翻译 / 停止 / 打开应用。
 *
 * 用传统 RemoteViews（不引入 Glance 依赖，规避版本兼容风险；专家 C2 建议）。
 * 翻译按钮与首页开始按钮同链路（CaptureStartRequestActivity 处理授权+启动）。
 */
class QuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)
            views.setOnClickPendingIntent(
                R.id.widget_btn_translate,
                pendingActivity(context, REQUEST_TRANSLATE, CaptureStartRequestActivity.newIntent(context)),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_stop,
                pendingService(context, REQUEST_STOP, CaptureService.stopIntent(context)),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_open,
                pendingActivity(context, REQUEST_OPEN, Intent(context, MainActivity::class.java)),
            )
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun pendingActivity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun pendingService(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val REQUEST_TRANSLATE = 1001
        private const val REQUEST_STOP = 1002
        private const val REQUEST_OPEN = 1003
    }
}
