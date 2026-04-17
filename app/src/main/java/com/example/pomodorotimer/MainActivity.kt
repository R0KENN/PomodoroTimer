package com.example.pomodorotimer

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private var currentTab = 0
    private val timerFragment = TimerFragment()
    private val statsFragment = StatsFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showFragment(timerFragment)
        updateNav(0)

        findViewById<View>(R.id.navTimer).setOnClickListener {
            if (currentTab != 0) { showFragment(timerFragment); updateNav(0); currentTab = 0 }
        }
        findViewById<View>(R.id.navStats).setOnClickListener {
            if (currentTab != 1) { showFragment(statsFragment); updateNav(1); currentTab = 1 }
        }
        findViewById<View>(R.id.navSettings).setOnClickListener {
            if (currentTab != 2) { showFragment(settingsFragment); updateNav(2); currentTab = 2 }
        }
    }

    private fun showFragment(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, f).commit()
    }

    private fun updateNav(active: Int) {
        val bgs = listOf(
            findViewById<View>(R.id.navTimerBg),
            findViewById<View>(R.id.navStatsBg),
            findViewById<View>(R.id.navSettingsBg)
        )
        val icons = listOf(
            findViewById<ImageView>(R.id.navTimerIcon),
            findViewById<ImageView>(R.id.navStatsIcon),
            findViewById<ImageView>(R.id.navSettingsIcon)
        )
        val activeColor = Color.parseColor("#F97316")
        val inactiveColor = Color.parseColor("#66FFFFFF")

        bgs.forEachIndexed { i, bg ->
            bg.visibility = if (i == active) View.VISIBLE else View.INVISIBLE
        }
        icons.forEachIndexed { i, icon ->
            icon.setColorFilter(if (i == active) activeColor else inactiveColor)
        }
    }
}
