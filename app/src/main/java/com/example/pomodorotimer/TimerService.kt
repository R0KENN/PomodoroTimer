package com.example.pomodorotimer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_RESET = "ACTION_RESET"
        const val ACTION_TICK = "ACTION_TICK"
        const val ACTION_FINISHED = "ACTION_FINISHED"
        const val EXTRA_TIME_LEFT = "time_left"
        const val EXTRA_TOTAL_TIME = "total_time"
        const val EXTRA_IS_BREAK = "is_break"

        var isRunning = false; private set
        var isPaused = false; private set
        var currentTimeLeft = 0L; private set
        var currentTotalTime = 0L; private set
        var currentIsBreak = false; private set
    }

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftMs = 0L
    private var totalTimeMs = 0L
    private var isBreakMode = false
    private var tickCounter = 0

    override fun onCreate() { super.onCreate(); createNotificationChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                timeLeftMs = intent.getLongExtra(EXTRA_TIME_LEFT, 25 * 60 * 1000L)
                totalTimeMs = intent.getLongExtra(EXTRA_TOTAL_TIME, 25 * 60 * 1000L)
                isBreakMode = intent.getBooleanExtra(EXTRA_IS_BREAK, false)
                currentTotalTime = totalTimeMs; currentIsBreak = isBreakMode
                startForeground(NOTIFICATION_ID, buildNotification(timeLeftMs, totalTimeMs, true))
                startCountdown()
            }
            ACTION_PAUSE -> pauseCountdown()
            ACTION_RESUME -> startCountdown()
            ACTION_RESET -> resetCountdown()
        }
        return START_NOT_STICKY
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        isRunning = true; isPaused = false; tickCounter = 0

        countDownTimer = object : CountDownTimer(timeLeftMs, 500) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished; currentTimeLeft = timeLeftMs
                updateNotification(timeLeftMs, totalTimeMs, true)
                sendTickBroadcast(timeLeftMs)
                // Update widget every 4 ticks (~2 seconds)
                tickCounter++
                if (tickCounter % 4 == 0) {
                    try { TimerWidget.updateAllWidgets(this@TimerService) } catch (_: Exception) {}
                }
            }
            override fun onFinish() {
                timeLeftMs = 0; currentTimeLeft = 0; isRunning = false; isPaused = false
                sendFinishedBroadcast()
                try { TimerWidget.updateAllWidgets(this@TimerService) } catch (_: Exception) {}
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }.start()
    }

    private fun pauseCountdown() {
        countDownTimer?.cancel(); isRunning = true; isPaused = true
        updateNotification(timeLeftMs, totalTimeMs, false)
        try { TimerWidget.updateAllWidgets(this) } catch (_: Exception) {}
    }

    private fun resetCountdown() {
        countDownTimer?.cancel(); isRunning = false; isPaused = false; currentTimeLeft = 0
        try { TimerWidget.updateAllWidgets(this) } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    private fun sendTickBroadcast(timeLeft: Long) {
        sendBroadcast(Intent(ACTION_TICK).apply {
            setPackage(packageName)
            putExtra(EXTRA_TIME_LEFT, timeLeft); putExtra(EXTRA_TOTAL_TIME, totalTimeMs)
        })
    }

    private fun sendFinishedBroadcast() { sendBroadcast(Intent(ACTION_FINISHED).apply { setPackage(packageName) }) }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Помодоро таймер", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Уведомление таймера"; setShowBadge(false); setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(timeLeft: Long, total: Long, playing: Boolean): Notification {
        val totalSec = (timeLeft / 1000).toInt()
        val timeText = String.format("%02d:%02d", totalSec / 60, totalSec % 60)
        val progress = if (total > 0) ((total - timeLeft) * 100 / total).toInt() else 0
        val modeEmoji = if (isBreakMode) "\uD83C\uDF3F" else "\uD83C\uDF45"
        val modeText = if (isBreakMode) "Отдых" else "Фокус"
        val statusText = if (playing && !isPaused) "Таймер идёт" else "На паузе"

        // Today's count
        val statsPrefs = getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)
        val todayKey = "pomo_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}"
        val todayCount = statsPrefs.getInt(todayKey, 0)
        val settingsPrefs = getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        val dailyGoal = settingsPrefs.getInt("daily_goal", 8)

        val openPending = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pauseResumeAction = if (playing && !isPaused) "NOTIF_PAUSE" else "NOTIF_RESUME"
        val pauseResumeLabel = if (playing && !isPaused) "⏸ Пауза" else "▶ Продолжить"
        val pauseResumePending = PendingIntent.getBroadcast(this, 1,
            Intent(this, TimerActionReceiver::class.java).apply { action = pauseResumeAction },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val skipPending = PendingIntent.getBroadcast(this, 3,
            Intent(this, TimerActionReceiver::class.java).apply { action = "NOTIF_SKIP" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val resetPending = PendingIntent.getBroadcast(this, 2,
            Intent(this, TimerActionReceiver::class.java).apply { action = "NOTIF_RESET" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_small)
            .setContentTitle("$modeEmoji $modeText — $timeText")
            .setContentText("$statusText  •  🔥 $todayCount/$dailyGoal")
            .setSubText("Sola")
            .setProgress(100, progress, false)
            .setContentIntent(openPending)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setColor(if (isBreakMode) 0xFF22C55E.toInt() else 0xFFF97316.toInt())
            .addAction(0, pauseResumeLabel, pauseResumePending)
            .addAction(0, "⏭ Пропустить", skipPending)
            .addAction(0, "↺ Сброс", resetPending)
            .build()
    }

    private fun updateNotification(timeLeft: Long, total: Long, playing: Boolean) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(timeLeft, total, playing))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() { countDownTimer?.cancel(); isRunning = false; isPaused = false; super.onDestroy() }
}
