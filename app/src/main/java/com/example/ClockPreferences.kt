package com.example

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class ClockPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    enum class ThemeMode(val id: String) {
        DARK("dark"),
        LIGHT("light"),
        OLED("oled")
    }

    enum class ClockStyle(val id: String) {
        MINIMAL("minimal"),
        CLASSIC("classic")
    }

    var theme: ThemeMode
        get() {
            val value = prefs.getString(KEY_THEME, ThemeMode.DARK.id) ?: ThemeMode.DARK.id
            return ThemeMode.values().find { it.id == value } ?: ThemeMode.DARK
        }
        set(value) {
            prefs.edit().putString(KEY_THEME, value.id).apply()
            notifyWidgets()
        }

    var clockStyle: ClockStyle
        get() {
            val value = prefs.getString(KEY_STYLE, ClockStyle.MINIMAL.id) ?: ClockStyle.MINIMAL.id
            return ClockStyle.values().find { it.id == value } ?: ClockStyle.MINIMAL
        }
        set(value) {
            prefs.edit().putString(KEY_STYLE, value.id).apply()
            notifyWidgets()
        }

    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_SECONDS, value).apply()
            notifyWidgets()
        }

    fun notifyWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, UKClockWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(context, UKClockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    companion object {
        private const val PREFS_NAME = "uk_clock_widget_prefs"
        private const val KEY_THEME = "pref_theme"
        private const val KEY_STYLE = "pref_style"
        private const val KEY_SHOW_SECONDS = "pref_show_seconds"
    }
}
