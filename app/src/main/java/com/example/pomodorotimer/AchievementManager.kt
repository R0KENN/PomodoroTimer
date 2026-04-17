package com.example.pomodorotimer

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class AchievementManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
    private val statsPrefs: SharedPreferences = context.getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)

    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val icon: String,
        val category: String,
        val checkUnlocked: (SharedPreferences, SharedPreferences) -> Boolean
    )

    val achievements = listOf(

        // ═══════════════════════════════════════
        // 🌱 НАЧАЛО ПУТИ (первые шаги)
        // ═══════════════════════════════════════
        Achievement("first_pomo", "Первый шаг", "Завершите первый помодоро", "🌱", "Начало") { stats, _ ->
            getTotalPomos(stats) >= 1
        },
        Achievement("three_pomos", "Тройка", "Завершите 3 помодоро", "🌿", "Начало") { stats, _ ->
            getTotalPomos(stats) >= 3
        },
        Achievement("five_pomos", "Разогрев", "Завершите 5 помодоро", "🔥", "Начало") { stats, _ ->
            getTotalPomos(stats) >= 5
        },
        Achievement("ten_pomos", "Десятка", "Завершите 10 помодоро", "🎯", "Начало") { stats, _ ->
            getTotalPomos(stats) >= 10
        },

        // ═══════════════════════════════════════
        // ⭐ КОЛИЧЕСТВО (всего помодоро)
        // ═══════════════════════════════════════
        Achievement("twenty_five", "Четверть сотни", "Завершите 25 помодоро", "⭐", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 25
        },
        Achievement("fifty_total", "Полтинник", "Завершите 50 помодоро", "💎", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 50
        },
        Achievement("hundred_total", "Сотня", "Завершите 100 помодоро", "🏆", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 100
        },
        Achievement("two_hundred", "Двести!", "Завершите 200 помодоро", "💫", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 200
        },
        Achievement("three_hundred", "Триста спартанцев", "Завершите 300 помодоро", "⚔️", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 300
        },
        Achievement("five_hundred", "Полтысячи", "Завершите 500 помодоро", "🌟", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 500
        },
        Achievement("seven_fifty", "750", "Завершите 750 помодоро", "🔮", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 750
        },
        Achievement("thousand", "Тысячник", "Завершите 1000 помодоро", "👑", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 1000
        },
        Achievement("fifteen_hundred", "Полторы тысячи", "Завершите 1500 помодоро", "🗿", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 1500
        },
        Achievement("two_thousand", "Двухтысячник", "Завершите 2000 помодоро", "🏔️", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 2000
        },
        Achievement("five_thousand", "Легенда", "Завершите 5000 помодоро", "🐉", "Количество") { stats, _ ->
            getTotalPomos(stats) >= 5000
        },

        // ═══════════════════════════════════════
        // 📅 СЕРИЯ ДНЕЙ (стрик)
        // ═══════════════════════════════════════
        Achievement("streak_2", "Два дня подряд", "Серия из 2 дней", "📅", "Серия") { stats, _ ->
            calculateStreak(stats) >= 2
        },
        Achievement("streak_3", "Три дня подряд", "Серия из 3 дней", "📆", "Серия") { stats, _ ->
            calculateStreak(stats) >= 3
        },
        Achievement("streak_5", "Пять дней", "Серия из 5 дней", "🗓️", "Серия") { stats, _ ->
            calculateStreak(stats) >= 5
        },
        Achievement("streak_7", "Неделя огня", "7 дней подряд", "🔥", "Серия") { stats, _ ->
            calculateStreak(stats) >= 7
        },
        Achievement("streak_14", "Двухнедельный марафон", "14 дней подряд", "🚀", "Серия") { stats, _ ->
            calculateStreak(stats) >= 14
        },
        Achievement("streak_21", "Три недели", "21 день подряд — привычка!", "🧠", "Серия") { stats, _ ->
            calculateStreak(stats) >= 21
        },
        Achievement("streak_30", "Месяц дисциплины", "30 дней подряд", "💪", "Серия") { stats, _ ->
            calculateStreak(stats) >= 30
        },
        Achievement("streak_50", "50 дней!", "Полсотни дней подряд", "🦁", "Серия") { stats, _ ->
            calculateStreak(stats) >= 50
        },
        Achievement("streak_60", "Два месяца", "60 дней подряд", "🐺", "Серия") { stats, _ ->
            calculateStreak(stats) >= 60
        },
        Achievement("streak_90", "Квартал", "90 дней подряд", "⚡", "Серия") { stats, _ ->
            calculateStreak(stats) >= 90
        },
        Achievement("streak_100", "Сотня дней", "100 дней подряд", "🏅", "Серия") { stats, _ ->
            calculateStreak(stats) >= 100
        },
        Achievement("streak_180", "Полгода!", "180 дней подряд", "🌍", "Серия") { stats, _ ->
            calculateStreak(stats) >= 180
        },
        Achievement("streak_365", "Год дисциплины", "365 дней подряд!", "🏆", "Серия") { stats, _ ->
            calculateStreak(stats) >= 365
        },

        // ═══════════════════════════════════════
        // 💥 РЕКОРД ЗА ДЕНЬ
        // ═══════════════════════════════════════
        Achievement("day_3", "Тройная фокусировка", "3 помодоро за день", "✊", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 3
        },
        Achievement("day_5", "Пятёрочка", "5 помодоро за день", "✋", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 5
        },
        Achievement("day_8", "Полный день", "8 помодоро за день", "💼", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 8
        },
        Achievement("day_10", "Мастер фокуса", "10 помодоро за день", "🎯", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 10
        },
        Achievement("day_12", "Дюжина", "12 помодоро за день", "🔋", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 12
        },
        Achievement("day_15", "Марафонец", "15 помодоро за день", "🏃", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 15
        },
        Achievement("day_20", "Сверхфокус", "20 помодоро за день", "⚡", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 20
        },
        Achievement("day_25", "Безумный день", "25 помодоро за день", "🤯", "За день") { stats, _ ->
            getMaxDailyPomos(stats) >= 25
        },

        // ═══════════════════════════════════════
        // ⏰ ВРЕМЯ (часы в фокусе)
        // ═══════════════════════════════════════
        Achievement("hours_1", "Первый час", "1 час в фокусе всего", "⏰", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 60
        },
        Achievement("hours_5", "5 часов", "5 часов в фокусе всего", "⏱️", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 300
        },
        Achievement("hours_10", "10 часов", "10 часов в фокусе", "🕐", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 600
        },
        Achievement("hours_24", "Сутки фокуса", "24 часа в фокусе", "🌍", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 1440
        },
        Achievement("hours_50", "50 часов", "50 часов в фокусе", "🕰️", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 3000
        },
        Achievement("hours_100", "100 часов", "100 часов чистого фокуса", "⌛", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 6000
        },
        Achievement("hours_250", "250 часов", "250 часов мастерства", "🧘", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 15000
        },
        Achievement("hours_500", "500 часов", "500 часов — ты эксперт", "🎓", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 30000
        },
        Achievement("hours_1000", "1000 часов", "1000 часов — путь мастера", "🐉", "Время") { stats, _ ->
            getTotalPomos(stats) * 25 >= 60000
        },

        // ═══════════════════════════════════════
        // 📊 НЕДЕЛЯ
        // ═══════════════════════════════════════
        Achievement("week_10", "Продуктивная неделя", "10 помодоро за неделю", "📊", "Неделя") { stats, _ ->
            getWeekTotal(stats) >= 10
        },
        Achievement("week_20", "Ударная неделя", "20 помодоро за неделю", "📈", "Неделя") { stats, _ ->
            getWeekTotal(stats) >= 20
        },
        Achievement("week_35", "Полная рабочая неделя", "35 помодоро за неделю (5 в день)", "💼", "Неделя") { stats, _ ->
            getWeekTotal(stats) >= 35
        },
        Achievement("week_50", "Суперпродуктивность", "50 помодоро за неделю", "🚀", "Неделя") { stats, _ ->
            getWeekTotal(stats) >= 50
        },
        Achievement("week_all_days", "Без выходных", "Помодоро каждый день недели", "🗓️", "Неделя") { stats, _ ->
            getWeekDaysActive(stats) >= 7
        },
        Achievement("week_workdays", "Рабочая пятидневка", "5 рабочих дней с помодоро", "📋", "Неделя") { stats, _ ->
            getWeekDaysActive(stats) >= 5
        },

        // ═══════════════════════════════════════
        // 🌙 ОСОБЫЕ / СКРЫТЫЕ
        // ═══════════════════════════════════════
        Achievement("early_bird", "Ранняя пташка", "Запустите помодоро до 7:00", "🐦", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("early_bird_triggered", false)
        },
        Achievement("night_owl", "Ночная сова", "Запустите помодоро после 23:00", "🦉", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("night_owl_triggered", false)
        },
        Achievement("weekend_warrior", "Воин выходных", "5+ помодоро в субботу или воскресенье", "⚔️", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("weekend_warrior_triggered", false)
        },
        Achievement("perfect_week", "Идеальная неделя", "Достигните дневной цели каждый день недели", "✨", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("perfect_week_triggered", false)
        },
        Achievement("comeback", "Камбэк", "Вернитесь после 3+ дней без помодоро", "🔄", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("comeback_triggered", false)
        },
        Achievement("double_goal", "Двойная норма", "Сделайте 2x дневной цели за день", "🎰", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("double_goal_triggered", false)
        },
        Achievement("first_monday", "Понедельник — старт", "Первое помодоро в понедельник", "📌", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("first_monday_triggered", false)
        },
        Achievement("triple_goal", "Тройная норма", "Сделайте 3x дневной цели за день", "💣", "Особые") { _, achPrefs ->
            achPrefs.getBoolean("triple_goal_triggered", false)
        },

        // ═══════════════════════════════════════
        // 🧩 КОЛЛЕКЦИОНЕР
        // ═══════════════════════════════════════
        Achievement("collect_10", "Коллекционер", "Откройте 10 достижений", "🧩", "Мета") { _, achPrefs ->
            (achPrefs.getStringSet("unlocked", emptySet())?.size ?: 0) >= 10
        },
        Achievement("collect_25", "Заядлый коллекционер", "Откройте 25 достижений", "🎪", "Мета") { _, achPrefs ->
            (achPrefs.getStringSet("unlocked", emptySet())?.size ?: 0) >= 25
        },
        Achievement("collect_40", "Охотник за трофеями", "Откройте 40 достижений", "🏹", "Мета") { _, achPrefs ->
            (achPrefs.getStringSet("unlocked", emptySet())?.size ?: 0) >= 40
        },
        Achievement("collect_all", "Перфекционист", "Откройте все достижения", "🌈", "Мета") { _, achPrefs ->
            // -1 because this one itself shouldn't count
            (achPrefs.getStringSet("unlocked", emptySet())?.size ?: 0) >= 59
        }
    )

    fun getUnlockedIds(): Set<String> = prefs.getStringSet("unlocked", emptySet()) ?: emptySet()

    fun checkAndUnlockNew(): List<Achievement> {
        val unlocked = getUnlockedIds().toMutableSet()
        val newlyUnlocked = mutableListOf<Achievement>()
        for (a in achievements) {
            if (a.id !in unlocked && a.checkUnlocked(statsPrefs, prefs)) {
                unlocked.add(a.id)
                newlyUnlocked.add(a)
            }
        }
        if (newlyUnlocked.isNotEmpty()) {
            prefs.edit().putStringSet("unlocked", unlocked).apply()
        }
        return newlyUnlocked
    }

    fun isUnlocked(id: String): Boolean = id in getUnlockedIds()

    // Call this when a pomodoro starts to check time-based achievements
    fun checkTimeTriggers(context: Context) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        // Early bird (before 7:00)
        if (hour < 7) {
            prefs.edit().putBoolean("early_bird_triggered", true).apply()
        }

        // Night owl (after 23:00)
        if (hour >= 23) {
            prefs.edit().putBoolean("night_owl_triggered", true).apply()
        }

        // Monday
        if (dayOfWeek == Calendar.MONDAY) {
            prefs.edit().putBoolean("first_monday_triggered", true).apply()
        }

        // Weekend warrior (Sat/Sun with 5+ pomos)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayCount = statsPrefs.getInt("pomo_${sdf.format(Date())}", 0)
            if (todayCount >= 5) {
                prefs.edit().putBoolean("weekend_warrior_triggered", true).apply()
            }
        }
    }

    // Call after completing a pomodoro to check special triggers
    fun checkCompletionTriggers(context: Context) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCount = statsPrefs.getInt("pomo_${sdf.format(Date())}", 0)
        val settingsPrefs = context.getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        val dailyGoal = settingsPrefs.getInt("daily_goal", 8)

        // Double goal
        if (todayCount >= dailyGoal * 2) {
            prefs.edit().putBoolean("double_goal_triggered", true).apply()
        }

        // Triple goal
        if (todayCount >= dailyGoal * 3) {
            prefs.edit().putBoolean("triple_goal_triggered", true).apply()
        }

        // Weekend warrior
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if ((dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) && todayCount >= 5) {
            prefs.edit().putBoolean("weekend_warrior_triggered", true).apply()
        }

        // Comeback: check if there's a gap of 3+ days before today
        checkComeback()

        // Perfect week
        checkPerfectWeek(context)
    }

    private fun checkComeback() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Yesterday
        cal.add(Calendar.DAY_OF_YEAR, -1)
        var gapDays = 0
        for (i in 0..30) {
            if (statsPrefs.getInt("pomo_${sdf.format(cal.time)}", 0) == 0) {
                gapDays++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        if (gapDays >= 3) {
            prefs.edit().putBoolean("comeback_triggered", true).apply()
        }
    }

    private fun checkPerfectWeek(context: Context) {
        val settingsPrefs = context.getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)
        val dailyGoal = settingsPrefs.getInt("daily_goal", 8)
        val weekKeys = getWeekKeysList()
        val allMet = weekKeys.all { statsPrefs.getInt(it, 0) >= dailyGoal }
        if (allMet && weekKeys.size == 7) {
            prefs.edit().putBoolean("perfect_week_triggered", true).apply()
        }
    }

    companion object {
        fun getTotalPomos(prefs: SharedPreferences): Int {
            return prefs.all.filter { it.key.startsWith("pomo_") }
                .values.sumOf { (it as? Int) ?: 0 }
        }

        fun getMaxDailyPomos(prefs: SharedPreferences): Int {
            return prefs.all.filter { it.key.startsWith("pomo_") }
                .values.maxOfOrNull { (it as? Int) ?: 0 } ?: 0
        }

        fun calculateStreak(prefs: SharedPreferences): Int {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            var streak = 0
            for (i in 0..365) {
                if (prefs.getInt("pomo_${sdf.format(cal.time)}", 0) > 0) {
                    streak++; cal.add(Calendar.DAY_OF_YEAR, -1)
                } else break
            }
            return streak
        }

        fun getWeekTotal(prefs: SharedPreferences): Int {
            return getWeekKeysList().sumOf { prefs.getInt(it, 0) }
        }

        fun getWeekDaysActive(prefs: SharedPreferences): Int {
            return getWeekKeysList().count { prefs.getInt(it, 0) > 0 }
        }

        private fun getWeekKeysList(): List<String> {
            val keys = mutableListOf<String>()
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val fromMon = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
            cal.add(Calendar.DAY_OF_YEAR, -fromMon)
            for (i in 0..6) {
                keys.add("pomo_${sdf.format(cal.time)}")
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return keys
        }
    }
}
