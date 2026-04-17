package com.example.pomodorotimer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TimerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_START = "WIDGET_START"
        const val ACTION_WIDGET_PAUSE = "WIDGET_PAUSE"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TimerWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, TimerWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_START -> {
                if (TimerService.isRunning && !TimerService.isPaused) {
                    // Pause
                    context.startService(Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_PAUSE
                    })
                } else if (TimerService.isRunning && TimerService.isPaused) {
                    // Resume
                    context.startService(Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_RESUME
                    })
                } else {
                    // Start new — open app
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(openIntent)
                }
            }
        }
        // Refresh widget
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, TimerWidget::class.java))
        for (id in ids) updateWidget(context, mgr, id)
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_timer)

        val timeText: String
        val btnText: String

        if (TimerService.isRunning) {
            val secs = (TimerService.currentTimeLeft / 1000).toInt()
            timeText = String.format("%02d:%02d", secs / 60, secs % 60)
            btnText = if (TimerService.isPaused) "▶" else "⏸"
        } else {
            val prefs = context.getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
            val todayCount = context.getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)
                .getInt("pomo_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}", 0)
            timeText = "25:00"
            btnText = "▶"
        }

        views.setTextViewText(R.id.widget_time, timeText)
        views.setTextViewText(R.id.widget_btn, btnText)

        // Stats
        val statsPrefs = context.getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)
        val todayKey = "pomo_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}"
        val todayCount = statsPrefs.getInt(todayKey, 0)
        val settingsPrefs = context.getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        val dailyGoal = settingsPrefs.getInt("daily_goal", 8)
        views.setTextViewText(R.id.widget_stats, "🔥 $todayCount / $dailyGoal")

        val modeText = if (TimerService.currentIsBreak) "🌿 Отдых" else "🍅 Фокус"
        views.setTextViewText(R.id.widget_mode, modeText)

        // Play/Pause action
        val actionIntent = Intent(context, TimerWidget::class.java).apply {
            action = ACTION_WIDGET_START
        }
        val actionPending = PendingIntent.getBroadcast(context, 0, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_btn, actionPending)

        // Open app on tap
        val openIntent = PendingIntent.getActivity(context, 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_root, openIntent)

        manager.updateAppWidget(widgetId, views)
    }
}
