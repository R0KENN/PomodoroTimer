package com.example.pomodorotimer

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildStats(view)
    }

    override fun onResume() { super.onResume(); view?.let { buildStats(it) } }

    private fun getPrefs() = requireContext().getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)

    private fun buildStats(view: View) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = "pomo_${sdf.format(Date())}"
        val todayCount = getPrefs().getInt(todayKey, 0)
        val weekKeys = getWeekKeys()
        val weekCounts = weekKeys.map { getPrefs().getInt(it, 0) }
        val weekTotal = weekCounts.sum()
        val streak = calculateStreak()

        // Extended stats
        val allPomos = AchievementManager.getTotalPomos(getPrefs())
        val bestDay = AchievementManager.getMaxDailyPomos(getPrefs())
        val daysWithPomos = getPrefs().all.filter { it.key.startsWith("pomo_") && (it.value as? Int ?: 0) > 0 }.size
        val avgPerDay = if (daysWithPomos > 0) allPomos.toFloat() / daysWithPomos else 0f

        // Используем реальные накопленные минуты
        val totalMinutes = AchievementManager.getTotalWorkMinutes(getPrefs())

        view.findViewById<TextView>(R.id.tvStatsToday).text = todayCount.toString()
        view.findViewById<TextView>(R.id.tvStatsWeek).text = weekTotal.toString()
        view.findViewById<TextView>(R.id.tvStatsTotalMin).text = totalMinutes.toString()
        view.findViewById<TextView>(R.id.tvStatsStreak).text = streak.toString()

        view.findViewById<TextView>(R.id.tvStatsTotalAll)?.text = allPomos.toString()
        view.findViewById<TextView>(R.id.tvStatsBestDay)?.text = bestDay.toString()
        view.findViewById<TextView>(R.id.tvStatsAvgDay)?.text = String.format("%.1f", avgPerDay)

        buildWeekChart(view.findViewById(R.id.weekChartContainer), weekKeys, weekCounts)
    }

    private fun buildWeekChart(container: LinearLayout, keys: List<String>, counts: List<Int>) {
        container.removeAllViews()
        val dayNames = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
        val d = resources.displayMetrics.density
        val todayKey = "pomo_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"

        counts.forEachIndexed { i, count ->
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    marginStart = (3 * d).toInt(); marginEnd = (3 * d).toInt()
                }
            }
            val trackH = 120
            val track = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams((16 * d).toInt(), (trackH * d).toInt())
                setBackgroundResource(R.drawable.bg_bar_track)
            }
            val fillH = if (count == 0) 0 else ((count.toFloat() / max) * trackH).toInt().coerceAtLeast(8)
            val fill = View(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (fillH * d).toInt()).apply {
                    gravity = Gravity.BOTTOM
                }
                setBackgroundResource(if (keys[i] == todayKey) R.drawable.bg_bar_fill else R.drawable.bg_bar_fill_dim)
            }
            track.addView(fill)
            val label = TextView(requireContext()).apply {
                text = dayNames[i]; textSize = 10f
                setTextColor(if (keys[i] == todayKey) Color.parseColor("#66F97316") else Color.parseColor("#40FFFFFF"))
                gravity = Gravity.CENTER; setPadding(0, (8 * d).toInt(), 0, 0); letterSpacing = 0.1f
            }
            col.addView(track); col.addView(label); container.addView(col)
        }
    }

    private fun getWeekKeys(): List<String> {
        val keys = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val fromMon = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -fromMon)
        for (i in 0..6) { keys.add("pomo_${sdf.format(cal.time)}"); cal.add(Calendar.DAY_OF_YEAR, 1) }
        return keys
    }

    private fun calculateStreak(): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var s = 0
        for (i in 0..365) {
            if (getPrefs().getInt("pomo_${sdf.format(cal.time)}", 0) > 0) {
                s++; cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return s
    }
}
