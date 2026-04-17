package com.example.pomodorotimer

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private val PREFS = "pomodoro_settings"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val switchVibration = view.findViewById<Switch>(R.id.switchVibration)
        val switchSound = view.findViewById<Switch>(R.id.switchSound)
        val switchNotifications = view.findViewById<Switch>(R.id.switchNotifications)
        val switchAutoStart = view.findViewById<Switch>(R.id.switchAutoStart)
        val btnClearStats = view.findViewById<View>(R.id.btnClearStats)

        // Daily goal spinner
        val spinnerGoal = view.findViewById<Spinner>(R.id.spinnerDailyGoal)
        val goalOptions = listOf(4, 6, 8, 10, 12)
        val currentGoal = prefs.getInt("daily_goal", 8)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, goalOptions.map { "$it сессий" })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoal.adapter = adapter
        spinnerGoal.setSelection(goalOptions.indexOf(currentGoal).coerceAtLeast(0))
        spinnerGoal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.edit().putInt("daily_goal", goalOptions[pos]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Long break spinner
        val spinnerLongBreak = view.findViewById<Spinner>(R.id.spinnerLongBreak)
        val longBreakOptions = listOf(10, 15, 20, 30)
        val currentLongBreak = prefs.getInt("long_break_min", 15)
        val lbAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, longBreakOptions.map { "$it мин" })
        lbAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLongBreak.adapter = lbAdapter
        spinnerLongBreak.setSelection(longBreakOptions.indexOf(currentLongBreak).coerceAtLeast(0))
        spinnerLongBreak.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.edit().putInt("long_break_min", longBreakOptions[pos]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchVibration.isChecked = prefs.getBoolean("vibration", true)
        switchSound.isChecked = prefs.getBoolean("sound", true)
        switchNotifications.isChecked = prefs.getBoolean("notifications", true)
        switchAutoStart.isChecked = prefs.getBoolean("auto_start", true)

        switchVibration.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("vibration", checked).apply() }
        switchSound.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("sound", checked).apply() }
        switchNotifications.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("notifications", checked).apply() }
        switchAutoStart.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("auto_start", checked).apply() }

        btnClearStats.setOnClickListener {
            requireContext().getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE).edit().clear().apply()
            requireContext().getSharedPreferences("achievements", Context.MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(requireContext(), "Статистика и достижения очищены", Toast.LENGTH_SHORT).show()
        }

        styleSwitches(switchVibration, switchSound, switchNotifications, switchAutoStart)
    }

    private fun styleSwitches(vararg switches: Switch) {
        switches.forEach { sw -> sw.setTextColor(Color.parseColor("#F5F5F5")) }
    }
}
