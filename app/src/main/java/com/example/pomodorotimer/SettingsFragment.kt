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
        val btnClearStats = view.findViewById<View>(R.id.btnClearStats)

        switchVibration.isChecked = prefs.getBoolean("vibration", true)
        switchSound.isChecked = prefs.getBoolean("sound", true)
        switchNotifications.isChecked = prefs.getBoolean("notifications", true)

        switchVibration.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("vibration", checked).apply()
        }
        switchSound.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("sound", checked).apply()
        }
        switchNotifications.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("notifications", checked).apply()
        }

        btnClearStats.setOnClickListener {
            requireContext().getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)
                .edit().clear().apply()
            Toast.makeText(requireContext(), "Статистика очищена", Toast.LENGTH_SHORT).show()
        }

        // Style switches
        styleSwitches(switchVibration, switchSound, switchNotifications)
    }

    private fun styleSwitches(vararg switches: Switch) {
        switches.forEach { sw ->
            sw.setTextColor(Color.parseColor("#F5F5F5"))
        }
    }
}
