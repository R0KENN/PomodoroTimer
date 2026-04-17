package com.example.pomodorotimer

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class AchievementsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_achievements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildAchievements(view)
    }

    override fun onResume() { super.onResume(); view?.let { buildAchievements(it) } }

    private fun buildAchievements(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.achievementsContainer)
        container.removeAllViews()
        val manager = AchievementManager(requireContext())
        val unlocked = manager.getUnlockedIds()
        val d = resources.displayMetrics.density
        val total = manager.achievements.size
        val unlockedCount = unlocked.size

        // Progress header
        val progressText = TextView(requireContext()).apply {
            text = "$unlockedCount / $total достижений"
            textSize = 14f
            setTextColor(Color.parseColor("#99FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (16 * d).toInt())
        }
        container.addView(progressText)

        // Overall progress bar
        val progressBarContainer = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (6 * d).toInt()).apply {
                bottomMargin = (28 * d).toInt()
            }
            setBackgroundResource(R.drawable.bg_bar_track)
        }
        val progressFill = View(requireContext()).apply {
            val fillFraction = if (total > 0) unlockedCount.toFloat() / total else 0f
            layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                width = (fillFraction * (resources.displayMetrics.widthPixels - 32 * d)).toInt()
            }
            setBackgroundResource(R.drawable.bg_bar_fill)
        }
        progressBarContainer.addView(progressFill)
        container.addView(progressBarContainer)

        // Group by category
        val categories = manager.achievements.map { it.category }.distinct()

        for (category in categories) {
            val categoryAchievements = manager.achievements.filter { it.category == category }
            val categoryUnlocked = categoryAchievements.count { it.id in unlocked }

            // Category header
            val header = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (8 * d).toInt(), 0, (12 * d).toInt())
            }
            val headerTitle = TextView(requireContext()).apply {
                text = category.uppercase()
                textSize = 11f
                setTextColor(Color.parseColor("#66FFFFFF"))
                letterSpacing = 0.2f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val headerCount = TextView(requireContext()).apply {
                text = "$categoryUnlocked/${categoryAchievements.size}"
                textSize = 11f
                setTextColor(if (categoryUnlocked == categoryAchievements.size) Color.parseColor("#F97316") else Color.parseColor("#40FFFFFF"))
            }
            header.addView(headerTitle)
            header.addView(headerCount)
            container.addView(header)

            // Achievement cards — 2 columns
            var row: LinearLayout? = null
            categoryAchievements.forEachIndexed { i, achievement ->
                if (i % 2 == 0) {
                    row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            bottomMargin = (8 * d).toInt()
                        }
                    }
                    container.addView(row)
                }

                val isUnlocked = achievement.id in unlocked
                val card = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding((12 * d).toInt(), (16 * d).toInt(), (12 * d).toInt(), (16 * d).toInt())
                    setBackgroundResource(if (isUnlocked) R.drawable.bg_glass_panel_active else R.drawable.bg_glass_panel)
                    alpha = if (isUnlocked) 1f else 0.35f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = if (i % 2 == 1) (4 * d).toInt() else 0
                        marginEnd = if (i % 2 == 0) (4 * d).toInt() else 0
                    }
                }

                val emoji = TextView(requireContext()).apply {
                    text = if (isUnlocked) achievement.icon else "🔒"
                    textSize = 28f
                    gravity = Gravity.CENTER
                }

                val title = TextView(requireContext()).apply {
                    text = achievement.title
                    textSize = 12f
                    setTextColor(if (isUnlocked) Color.parseColor("#E6FFFFFF") else Color.parseColor("#66FFFFFF"))
                    gravity = Gravity.CENTER
                    setPadding(0, (6 * d).toInt(), 0, (3 * d).toInt())
                    maxLines = 1
                }

                val desc = TextView(requireContext()).apply {
                    text = achievement.description
                    textSize = 9f
                    setTextColor(Color.parseColor("#55FFFFFF"))
                    gravity = Gravity.CENTER
                    maxLines = 2
                }

                card.addView(emoji)
                card.addView(title)
                card.addView(desc)
                row?.addView(card)

                // If last item in category is odd, add empty spacer
                if (i == categoryAchievements.lastIndex && i % 2 == 0) {
                    val spacer = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    }
                    row?.addView(spacer)
                }
            }
        }
    }
}
