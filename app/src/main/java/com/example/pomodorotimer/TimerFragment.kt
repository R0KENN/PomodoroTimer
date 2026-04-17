package com.example.pomodorotimer

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class TimerFragment : Fragment() {

    private lateinit var timerRingView: TimerRingView
    private lateinit var tvTimer: TextView
    private lateinit var tvTimerLabel: TextView
    private lateinit var tvBtnLabel: TextView
    private lateinit var ivBtnIcon: ImageView
    private lateinit var tvTodayCount: TextView
    private lateinit var btnStartPause: View
    private lateinit var btnReset: View
    private lateinit var btnSkip: View
    private lateinit var timerCenterText: View
    private lateinit var miniChart: LinearLayout

    private var workDuration = 25 * 60 * 1000L
    private var breakDuration = 5 * 60 * 1000L
    private var longBreakDuration = 15 * 60 * 1000L
    private var currentMode = Mode.WORK
    private var timeLeftMs = 25 * 60 * 1000L
    private var totalTimeMs = 25 * 60 * 1000L
    private var isRunning = false
    private var isPaused = false
    private var pulseAnimator: ValueAnimator? = null
    private var isPulsing = false
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    private var testMode = false
    private val TEST_DURATION = 10 * 1000L

    private var selectedWorkMin = 25
    private var selectedBreakMin = 5

    // Pomodoro counter for long break
    private var pomodoroSessionCount = 0

    // Auto-start setting
    private var autoStartEnabled = true

    enum class Mode { WORK, BREAK }

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerService.ACTION_TICK -> {
                    timeLeftMs = intent.getLongExtra(TimerService.EXTRA_TIME_LEFT, 0)
                    totalTimeMs = intent.getLongExtra(TimerService.EXTRA_TOTAL_TIME, totalTimeMs)
                    isRunning = true
                    isPaused = false
                    timerRingView.progress = timeLeftMs.toFloat() / totalTimeMs.toFloat()
                    timerRingView.isTimerRunning = true
                    updateTimerDisplay()
                    updateButtonState()

                    try { TimerWidget.updateAllWidgets(requireContext()) } catch (_: Exception) {}

                    if (timeLeftMs <= 10000 && !isPulsing) startPulseAnimation()
                    if (timeLeftMs <= 10000) {
                        val msInSecond = timeLeftMs % 1000
                        if (msInSecond in 0..550) playTick()
                    }
                }
                TimerService.ACTION_FINISHED -> {
                    timeLeftMs = 0
                    timerRingView.progress = 0f
                    updateTimerDisplay()
                    isRunning = false
                    isPaused = false
                    timerRingView.isTimerRunning = false
                    updateButtonState()
                    stopPulseAnimation()
                    playCompletionSound()
                    doVibrate()

                    if (currentMode == Mode.WORK) {
                        pomodoroSessionCount++
                        // Сохраняем счётчик сессий
                        requireContext().getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
                            .edit().putInt("session_count", pomodoroSessionCount).apply()

                        addPomodoroToStats()
                        updateStats()
                        checkAchievements()
                        showMotivationalQuote(true)

                        view?.postDelayed({
                            testMode = false
                            if (pomodoroSessionCount % 4 == 0) {
                                switchMode(Mode.BREAK, useLongBreak = true)
                            } else {
                                switchMode(Mode.BREAK)
                            }
                            if (autoStartEnabled) {
                                view?.postDelayed({ startTimer() }, 500)
                            }
                        }, 2000)
                    } else {
                        showMotivationalQuote(false)
                        view?.postDelayed({
                            testMode = false
                            switchMode(Mode.WORK)
                            if (autoStartEnabled) {
                                view?.postDelayed({ startTimer() }, 500)
                            }
                        }, 2000)
                    }
                    try { TimerWidget.updateAllWidgets(requireContext()) } catch (_: Exception) {}
                }
                "ACTION_SKIP_FROM_NOTIF" -> {
                    isRunning = false
                    isPaused = false
                    timerRingView.isTimerRunning = false
                    stopPulseAnimation()
                    if (currentMode == Mode.WORK) switchMode(Mode.BREAK) else switchMode(Mode.WORK)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        try { toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) } catch (_: Exception) {}

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Load settings
        loadSettings()

        setupViews(view)
        setupControls()
        setupChips(view)
        updateTimerDisplay()
        updateStats()

        val filter = IntentFilter().apply {
            addAction(TimerService.ACTION_TICK)
            addAction(TimerService.ACTION_FINISHED)
            addAction("ACTION_SKIP_FROM_NOTIF")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(tickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(tickReceiver, filter)
        }

        if (TimerService.isRunning) {
            isRunning = true
            isPaused = TimerService.isPaused
            timeLeftMs = TimerService.currentTimeLeft
            totalTimeMs = TimerService.currentTotalTime
            timerRingView.progress = if (totalTimeMs > 0) timeLeftMs.toFloat() / totalTimeMs.toFloat() else 1f
            timerRingView.isTimerRunning = !isPaused
            updateTimerDisplay()
            updateButtonState()
        }
    }

    override fun onResume() {
        super.onResume()
        // Перечитываем настройки при возврате к фрагменту
        loadSettings()
        updateStats()
    }

    private fun loadSettings() {
        val settingsPrefs = requireContext().getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        autoStartEnabled = settingsPrefs.getBoolean("auto_start", true)
        val longBreakMin = settingsPrefs.getInt("long_break_min", 15)
        longBreakDuration = longBreakMin * 60 * 1000L
        selectedWorkMin = settingsPrefs.getInt("work_minutes", 25)
        workDuration = selectedWorkMin * 60 * 1000L
        // Восстанавливаем счётчик сессий
        pomodoroSessionCount = settingsPrefs.getInt("session_count", 0)
    }

    private fun setupViews(view: View) {
        tvTimer = view.findViewById(R.id.tvTimer)
        tvTimerLabel = view.findViewById(R.id.tvTimerLabel)
        tvBtnLabel = view.findViewById(R.id.tvBtnLabel)
        ivBtnIcon = view.findViewById(R.id.ivBtnIcon)
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
        btnStartPause = view.findViewById(R.id.btnStartPause)
        btnReset = view.findViewById(R.id.btnReset)
        btnSkip = view.findViewById(R.id.btnSkip)
        timerCenterText = view.findViewById(R.id.timerCenterText)
        miniChart = view.findViewById(R.id.miniChart)

        timerRingView = TimerRingView(requireContext())
        val container = view.findViewById<FrameLayout>(R.id.timerCanvasContainer)
        container.addView(timerRingView)

        timerCenterText.setOnLongClickListener { toggleTestMode(); true }
    }

    private fun toggleTestMode() {
        if (isRunning) return
        testMode = !testMode
        if (testMode) {
            timeLeftMs = TEST_DURATION; totalTimeMs = TEST_DURATION
            Toast.makeText(requireContext(), "Тест: 10 сек", Toast.LENGTH_SHORT).show()
        } else {
            timeLeftMs = workDuration; totalTimeMs = workDuration
            Toast.makeText(requireContext(), "Обычный режим", Toast.LENGTH_SHORT).show()
        }
        timerRingView.progress = 1f; updateTimerDisplay()
    }

    private fun setupControls() {
        btnStartPause.setOnClickListener {
            if (!isRunning) startTimer()
            else if (!isPaused) pauseTimer()
            else resumeTimer()
        }
        btnReset.setOnClickListener { resetTimer() }
        btnSkip.setOnClickListener { skipToNext() }
    }

    private fun startTimer() {
        if (timeLeftMs <= 0) return
        isRunning = true; isPaused = false
        updateButtonState()
        timerRingView.isTimerRunning = true

        // Check time-based achievement triggers
        AchievementManager(requireContext()).checkTimeTriggers(requireContext())

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_LEFT, timeLeftMs)
            putExtra(TimerService.EXTRA_TOTAL_TIME, totalTimeMs)
            putExtra(TimerService.EXTRA_IS_BREAK, currentMode == Mode.BREAK)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun pauseTimer() {
        isPaused = true; timerRingView.isTimerRunning = false
        updateButtonState(); stopPulseAnimation()
        requireContext().startService(Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        })
    }

    private fun resumeTimer() {
        isPaused = false; timerRingView.isTimerRunning = true; updateButtonState()
        requireContext().startService(Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        })
    }

    private fun resetTimer() {
        isRunning = false; isPaused = false; isPulsing = false; testMode = false
        stopPulseAnimation(); timerRingView.isTimerRunning = false
        requireContext().startService(Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        })
        timeLeftMs = if (currentMode == Mode.WORK) workDuration else breakDuration
        totalTimeMs = timeLeftMs
        timerRingView.progress = 1f; updateTimerDisplay(); updateButtonState()
    }

    private fun skipToNext() {
        requireContext().startService(Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        })
        isRunning = false; isPaused = false; testMode = false
        stopPulseAnimation(); timerRingView.isTimerRunning = false
        if (currentMode == Mode.WORK) switchMode(Mode.BREAK) else switchMode(Mode.WORK)
    }

    private fun switchMode(mode: Mode, useLongBreak: Boolean = false) {
        currentMode = mode
        isRunning = false; isPaused = false; isPulsing = false
        timerRingView.isTimerRunning = false

        if (mode == Mode.WORK) {
            timeLeftMs = workDuration; totalTimeMs = workDuration
            timerRingView.isBreakMode = false
            tvTimerLabel.text = "ФОКУС"
            tvTimerLabel.setTextColor(Color.parseColor("#F97316"))
            view?.findViewById<View>(R.id.dotIndicator)?.setBackgroundColor(Color.parseColor("#F97316"))
        } else {
            val dur = if (useLongBreak) longBreakDuration else breakDuration
            timeLeftMs = dur; totalTimeMs = dur
            timerRingView.isBreakMode = true
            tvTimerLabel.text = if (useLongBreak) "ДЛИННЫЙ ОТДЫХ" else "ОТДЫХ"
            tvTimerLabel.setTextColor(Color.parseColor("#22C55E"))
            view?.findViewById<View>(R.id.dotIndicator)?.setBackgroundColor(Color.parseColor("#22C55E"))
        }
        timerRingView.progress = 1f; updateTimerDisplay(); updateButtonState()
    }

    private fun updateTimerDisplay() {
        val totalSeconds = (timeLeftMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun updateButtonState() {
        if (isRunning && !isPaused) {
            tvBtnLabel.text = "Пауза"; ivBtnIcon.setImageResource(R.drawable.ic_pause)
        } else {
            tvBtnLabel.text = if (isPaused) "Далее" else "Старт"
            ivBtnIcon.setImageResource(R.drawable.ic_play)
        }
    }

    // --- Motivational Quotes ---
    private fun showMotivationalQuote(isWorkComplete: Boolean) {
        val quote = if (isWorkComplete) MotivationalQuotes.getWorkQuote() else MotivationalQuotes.getBreakQuote()
        val ctx = context ?: return
        Toast.makeText(ctx, quote, Toast.LENGTH_LONG).show()
    }

    // --- Achievements ---
    private fun checkAchievements() {
        val ctx = context ?: return
        val manager = AchievementManager(ctx)
        manager.checkCompletionTriggers(ctx)
        val newlyUnlocked = manager.checkAndUnlockNew()
        for (achievement in newlyUnlocked) {
            showAchievementUnlocked(achievement)
        }
    }

    private fun showAchievementUnlocked(achievement: AchievementManager.Achievement) {
        val ctx = context ?: return
        val d = resources.displayMetrics.density

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((32 * d).toInt(), (24 * d).toInt(), (32 * d).toInt(), (24 * d).toInt())
        }

        val emoji = TextView(ctx).apply {
            text = achievement.icon; textSize = 48f; gravity = Gravity.CENTER
        }
        val title = TextView(ctx).apply {
            text = "Достижение!"; textSize = 20f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#F97316"))
            setPadding(0, (12 * d).toInt(), 0, (8 * d).toInt())
        }
        val name = TextView(ctx).apply {
            text = achievement.title; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        val desc = TextView(ctx).apply {
            text = achievement.description; textSize = 13f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#99FFFFFF"))
            setPadding(0, (4 * d).toInt(), 0, 0)
        }

        container.addView(emoji); container.addView(title); container.addView(name); container.addView(desc)

        AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(container)
            .setPositiveButton("Круто!") { dlg, _ -> dlg.dismiss() }
            .show()
    }

    // --- Pulse Animation ---
    private fun startPulseAnimation() {
        isPulsing = true
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.06f).apply {
            duration = 500; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                val scale = it.animatedValue as Float
                timerCenterText.scaleX = scale; timerCenterText.scaleY = scale
                timerRingView.glowIntensity = (scale - 1f) / 0.06f
            }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel(); isPulsing = false
        timerCenterText.scaleX = 1f; timerCenterText.scaleY = 1f
        timerRingView.glowIntensity = 0f
    }

    // --- Sound ---
    private fun playTick() {
        val prefs = requireContext().getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound", true)) return
        try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50) } catch (_: Exception) {}
    }

    private fun playCompletionSound() {
        val prefs = requireContext().getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound", true)) return
        try {
            val toneType = if (currentMode == Mode.WORK)
                ToneGenerator.TONE_PROP_BEEP2
            else
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD

            toneGenerator?.startTone(toneType, 200)
            view?.postDelayed({ toneGenerator?.startTone(toneType, 200) }, 300)
            view?.postDelayed({ toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400) }, 600)
        } catch (_: Exception) {}
    }

    private fun doVibrate() {
        val prefs = requireContext().getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("vibration", true)) return
        try {
            val pattern = if (currentMode == Mode.WORK)
                longArrayOf(0, 200, 100, 200, 100, 400)
            else
                longArrayOf(0, 100, 80, 100)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION") vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    // --- Chips ---
    private fun setupChips(view: View) {
        val chipWork15 = view.findViewById<TextView>(R.id.chipWork15)
        val chipWork25 = view.findViewById<TextView>(R.id.chipWork25)
        val chipWork45 = view.findViewById<TextView>(R.id.chipWork45)
        val chipBreak5 = view.findViewById<TextView>(R.id.chipBreak5)
        val chipBreak10 = view.findViewById<TextView>(R.id.chipBreak10)
        val chipBreak15 = view.findViewById<TextView>(R.id.chipBreak15)

        val workChips = mapOf(chipWork15 to 15, chipWork25 to 25, chipWork45 to 45)
        val breakChips = mapOf(chipBreak5 to 5, chipBreak10 to 10, chipBreak15 to 15)

        // Восстанавливаем визуальное состояние чипов по сохранённым настройкам
        workChips.forEach { (chip, min) ->
            if (min == selectedWorkMin) {
                chip.setBackgroundResource(R.drawable.bg_chip_active); chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive); chip.setTextColor(Color.parseColor("#999999"))
            }
        }
        breakChips.forEach { (chip, min) ->
            if (min == selectedBreakMin) {
                chip.setBackgroundResource(R.drawable.bg_chip_active); chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive); chip.setTextColor(Color.parseColor("#999999"))
            }
        }

        workChips.forEach { (chip, min) ->
            chip.setOnClickListener {
                if (isRunning) return@setOnClickListener
                selectedWorkMin = min; workDuration = min * 60 * 1000L
                // Сохраняем выбранную длительность фокуса
                requireContext().getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
                    .edit().putInt("work_minutes", min).apply()
                setActiveChip(workChips, chip)
                if (currentMode == Mode.WORK) resetTimer()
                updateStats()
            }
        }
        breakChips.forEach { (chip, min) ->
            chip.setOnClickListener {
                if (isRunning) return@setOnClickListener
                selectedBreakMin = min; breakDuration = min * 60 * 1000L
                setActiveChip(breakChips, chip)
                if (currentMode == Mode.BREAK) resetTimer()
            }
        }
    }

    private fun setActiveChip(chips: Map<TextView, Int>, active: TextView) {
        chips.keys.forEach { c ->
            if (c == active) {
                c.setBackgroundResource(R.drawable.bg_chip_active); c.setTextColor(Color.WHITE)
            } else {
                c.setBackgroundResource(R.drawable.bg_chip_inactive); c.setTextColor(Color.parseColor("#999999"))
            }
        }
    }

    // --- Stats ---
    private fun getPrefs() = requireContext().getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)

    private fun getTodayKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return "pomo_${sdf.format(java.util.Date())}"
    }

    private fun getWeekKeys(): List<String> {
        val keys = mutableListOf<String>()
        val cal = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val fromMon = if (dow == java.util.Calendar.SUNDAY) 6 else dow - java.util.Calendar.MONDAY
        cal.add(java.util.Calendar.DAY_OF_YEAR, -fromMon)
        for (i in 0..6) { keys.add("pomo_${sdf.format(cal.time)}"); cal.add(java.util.Calendar.DAY_OF_YEAR, 1) }
        return keys
    }

    private fun addPomodoroToStats() {
        val key = getTodayKey(); val prefs = getPrefs()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()

        // Сохраняем реальные отработанные минуты
        val totalMin = prefs.getInt("total_work_minutes", 0) + selectedWorkMin
        prefs.edit().putInt("total_work_minutes", totalMin).apply()
    }

    private fun updateStats() {
        val settingsPrefs = requireContext().getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        val dailyGoal = settingsPrefs.getInt("daily_goal", 8)
        val todayCount = getPrefs().getInt(getTodayKey(), 0)
        tvTodayCount.text = todayCount.toString()

        view?.findViewById<TextView>(R.id.tvDailyGoal)?.text = " / $dailyGoal сессий"

        buildMiniChart()
    }

    private fun buildMiniChart() {
        miniChart.removeAllViews()
        val keys = getWeekKeys()
        val counts = keys.map { getPrefs().getInt(it, 0) }
        val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
        val density = resources.displayMetrics.density
        val todayKey = getTodayKey()

        counts.forEachIndexed { i, count ->
            val barH = if (count == 0) 4 else ((count.toFloat() / max) * 40).toInt().coerceAtLeast(4)
            val bar = View(requireContext())
            val params = LinearLayout.LayoutParams((8 * density).toInt(), (barH * density).toInt()).apply {
                marginStart = if (i > 0) (2 * density).toInt() else 0
                gravity = android.view.Gravity.BOTTOM
            }
            bar.layoutParams = params
            bar.setBackgroundResource(if (keys[i] == todayKey) R.drawable.bg_bar_fill else R.drawable.bg_bar_fill_dim)
            miniChart.addView(bar)
        }

        val labelsContainer = view?.findViewById<LinearLayout>(R.id.miniChartLabels)
        labelsContainer?.removeAllViews()
        val dayNames = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        keys.forEachIndexed { i, key ->
            val tv = TextView(requireContext()).apply {
                text = dayNames[i]; textSize = 8f
                setTextColor(if (key == todayKey) Color.parseColor("#99F97316") else Color.parseColor("#40FFFFFF"))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            labelsContainer?.addView(tv)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { requireContext().unregisterReceiver(tickReceiver) } catch (_: Exception) {}
        pulseAnimator?.cancel()
        toneGenerator?.release()
    }
}
