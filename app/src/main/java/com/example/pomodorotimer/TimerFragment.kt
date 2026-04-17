package com.example.pomodorotimer

import android.Manifest
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
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

                    if (timeLeftMs <= 10000 && !isPulsing) {
                        startPulseAnimation()
                    }
                    if (timeLeftMs <= 10000) {
                        val msInSecond = timeLeftMs % 1000
                        if (msInSecond in 0..550) {
                            playTick()
                        }
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
                        addPomodoroToStats()
                        updateStats()
                        view?.postDelayed({
                            testMode = false
                            switchMode(Mode.BREAK)
                        }, 1500)
                    } else {
                        view?.postDelayed({
                            testMode = false
                            switchMode(Mode.WORK)
                        }, 1500)
                    }
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

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (_: Exception) {}

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        setupViews(view)
        setupControls()
        setupChips(view)
        updateTimerDisplay()
        updateStats()

        // Register receiver
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

        // Restore state if service is running
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

        timerCenterText.setOnLongClickListener {
            toggleTestMode()
            true
        }
    }

    private fun toggleTestMode() {
        if (isRunning) return
        testMode = !testMode
        if (testMode) {
            timeLeftMs = TEST_DURATION
            totalTimeMs = TEST_DURATION
            Toast.makeText(requireContext(), "Тестовый режим: 10 сек", Toast.LENGTH_SHORT).show()
        } else {
            timeLeftMs = workDuration
            totalTimeMs = workDuration
            Toast.makeText(requireContext(), "Обычный режим", Toast.LENGTH_SHORT).show()
        }
        timerRingView.progress = 1f
        updateTimerDisplay()
    }

    private fun setupControls() {
        btnStartPause.setOnClickListener {
            if (!isRunning) {
                startTimer()
            } else if (!isPaused) {
                pauseTimer()
            } else {
                resumeTimer()
            }
        }
        btnReset.setOnClickListener { resetTimer() }
        btnSkip.setOnClickListener { skipToNext() }
    }

    private fun startTimer() {
        if (timeLeftMs <= 0) return
        isRunning = true
        isPaused = false
        updateButtonState()
        timerRingView.isTimerRunning = true

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_LEFT, timeLeftMs)
            putExtra(TimerService.EXTRA_TOTAL_TIME, totalTimeMs)
            putExtra(TimerService.EXTRA_IS_BREAK, currentMode == Mode.BREAK)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun pauseTimer() {
        isPaused = true
        timerRingView.isTimerRunning = false
        updateButtonState()
        stopPulseAnimation()

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        requireContext().startService(intent)
    }

    private fun resumeTimer() {
        isPaused = false
        timerRingView.isTimerRunning = true
        updateButtonState()

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        requireContext().startService(intent)
    }

    private fun resetTimer() {
        isRunning = false
        isPaused = false
        isPulsing = false
        testMode = false
        stopPulseAnimation()
        timerRingView.isTimerRunning = false

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        }
        requireContext().startService(intent)

        timeLeftMs = if (currentMode == Mode.WORK) workDuration else breakDuration
        totalTimeMs = timeLeftMs
        timerRingView.progress = 1f
        updateTimerDisplay()
        updateButtonState()
    }

    private fun skipToNext() {
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        }
        requireContext().startService(intent)

        isRunning = false
        isPaused = false
        testMode = false
        stopPulseAnimation()
        timerRingView.isTimerRunning = false

        if (currentMode == Mode.WORK) switchMode(Mode.BREAK) else switchMode(Mode.WORK)
    }

    private fun switchMode(mode: Mode) {
        currentMode = mode
        isRunning = false
        isPaused = false
        isPulsing = false
        timerRingView.isTimerRunning = false

        if (mode == Mode.WORK) {
            timeLeftMs = workDuration
            totalTimeMs = workDuration
            timerRingView.isBreakMode = false
            tvTimerLabel.text = "ФОКУС"
            tvTimerLabel.setTextColor(Color.parseColor("#F97316"))
            view?.findViewById<View>(R.id.dotIndicator)?.setBackgroundColor(Color.parseColor("#F97316"))
        } else {
            timeLeftMs = breakDuration
            totalTimeMs = breakDuration
            timerRingView.isBreakMode = true
            tvTimerLabel.text = "ОТДЫХ"
            tvTimerLabel.setTextColor(Color.parseColor("#22C55E"))
            view?.findViewById<View>(R.id.dotIndicator)?.setBackgroundColor(Color.parseColor("#22C55E"))
        }
        timerRingView.progress = 1f
        updateTimerDisplay()
        updateButtonState()
    }

    private fun updateTimerDisplay() {
        val totalSeconds = (timeLeftMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun updateButtonState() {
        if (isRunning && !isPaused) {
            tvBtnLabel.text = "Пауза"
            ivBtnIcon.setImageResource(R.drawable.ic_pause)
        } else {
            tvBtnLabel.text = if (isPaused) "Продолжить" else "Старт"
            ivBtnIcon.setImageResource(R.drawable.ic_play)
        }
    }

    private fun startPulseAnimation() {
        isPulsing = true
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.06f).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                val scale = it.animatedValue as Float
                timerCenterText.scaleX = scale
                timerCenterText.scaleY = scale
                timerRingView.glowIntensity = (scale - 1f) / 0.06f
            }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        isPulsing = false
        timerCenterText.scaleX = 1f
        timerCenterText.scaleY = 1f
        timerRingView.glowIntensity = 0f
    }

    private fun playTick() {
        try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50) } catch (_: Exception) {}
    }

    private fun playCompletionSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            view?.postDelayed({ toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200) }, 300)
            view?.postDelayed({ toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400) }, 600)
        } catch (_: Exception) {}
    }

    private fun doVibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
            }
        } catch (_: Exception) {}
    }

    // Chips
    private fun setupChips(view: View) {
        val chipWork15 = view.findViewById<TextView>(R.id.chipWork15)
        val chipWork25 = view.findViewById<TextView>(R.id.chipWork25)
        val chipWork45 = view.findViewById<TextView>(R.id.chipWork45)
        val chipBreak5 = view.findViewById<TextView>(R.id.chipBreak5)
        val chipBreak10 = view.findViewById<TextView>(R.id.chipBreak10)
        val chipBreak15 = view.findViewById<TextView>(R.id.chipBreak15)

        val workChips = mapOf(chipWork15 to 15, chipWork25 to 25, chipWork45 to 45)
        val breakChips = mapOf(chipBreak5 to 5, chipBreak10 to 10, chipBreak15 to 15)

        workChips.forEach { (chip, min) ->
            chip.setOnClickListener {
                if (isRunning) return@setOnClickListener
                selectedWorkMin = min
                workDuration = min * 60 * 1000L
                setActiveChip(workChips, chip)
                if (currentMode == Mode.WORK) resetTimer()
                updateStats()
            }
        }
        breakChips.forEach { (chip, min) ->
            chip.setOnClickListener {
                if (isRunning) return@setOnClickListener
                selectedBreakMin = min
                breakDuration = min * 60 * 1000L
                setActiveChip(breakChips, chip)
                if (currentMode == Mode.BREAK) resetTimer()
            }
        }
    }

    private fun setActiveChip(chips: Map<TextView, Int>, active: TextView) {
        chips.keys.forEach { c ->
            if (c == active) {
                c.setBackgroundResource(R.drawable.bg_chip_active)
                c.setTextColor(Color.WHITE)
            } else {
                c.setBackgroundResource(R.drawable.bg_chip_inactive)
                c.setTextColor(Color.parseColor("#999999"))
            }
        }
    }

    // Stats
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
        for (i in 0..6) {
            keys.add("pomo_${sdf.format(cal.time)}")
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return keys
    }

    private fun addPomodoroToStats() {
        val key = getTodayKey()
        val prefs = getPrefs()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    private fun updateStats() {
        tvTodayCount.text = getPrefs().getInt(getTodayKey(), 0).toString()
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
            val params = LinearLayout.LayoutParams(
                (8 * density).toInt(),
                (barH * density).toInt()
            ).apply {
                marginStart = if (i > 0) (2 * density).toInt() else 0
                gravity = android.view.Gravity.BOTTOM
            }
            bar.layoutParams = params
            bar.setBackgroundResource(
                if (keys[i] == todayKey) R.drawable.bg_bar_fill else R.drawable.bg_bar_fill_dim
            )
            miniChart.addView(bar)
        }

        val labelsContainer = view?.findViewById<LinearLayout>(R.id.miniChartLabels)
        labelsContainer?.removeAllViews()
        val dayNames = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        keys.forEachIndexed { i, key ->
            val tv = TextView(requireContext()).apply {
                text = dayNames[i]
                textSize = 8f
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
