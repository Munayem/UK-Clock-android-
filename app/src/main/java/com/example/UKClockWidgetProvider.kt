package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews

/**
 * Android AppWidgetProvider for the UK London Clock Widget.
 * Responsively renders the analog clock and London time attributes onto RemoteViews.
 */
class UKClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = ClockPreferences(context)
        val timeInfo = ClockTimeHelper.getCurrentLondonTime()

        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val views = buildRemoteViews(context, appWidgetId, options, timeInfo, prefs)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Schedule next update for the beginning of the next minute
        scheduleNextAlarm(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val prefs = ClockPreferences(context)
        val timeInfo = ClockTimeHelper.getCurrentLondonTime()
        val views = buildRemoteViews(context, appWidgetId, newOptions, timeInfo, prefs)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_UPDATE_UK_CLOCK,
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_SCREEN_ON -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, UKClockWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, ids)
                }
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        options: Bundle,
        timeInfo: ClockTimeHelper.LondonTimeInfo,
        prefs: ClockPreferences
    ): RemoteViews {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)

        // Select responsive layout based on widget dimension
        val layoutRes = when {
            minWidth >= 260 && minHeight >= 220 -> R.layout.widget_uk_clock_large
            minWidth >= 220 && minHeight < 200 -> R.layout.widget_uk_clock_medium
            minWidth >= 200 && minHeight >= 180 -> R.layout.widget_uk_clock_large
            minWidth >= 180 -> R.layout.widget_uk_clock_medium
            else -> R.layout.widget_uk_clock_small
        }

        val views = RemoteViews(context.packageName, layoutRes)

        // Generate crisp analog clock face bitmap
        val bitmapSize = when (layoutRes) {
            R.layout.widget_uk_clock_large -> 512
            R.layout.widget_uk_clock_medium -> 420
            else -> 360
        }

        val clockBitmap = ClockBitmapRenderer.renderClockBitmap(
            size = bitmapSize,
            timeInfo = timeInfo,
            theme = prefs.theme,
            clockStyle = prefs.clockStyle,
            showSeconds = prefs.showSeconds
        )

        views.setImageViewBitmap(R.id.widget_clock_face, clockBitmap)
        views.setContentDescription(R.id.widget_clock_face, timeInfo.accessibilityDescription)

        // Update Sun / Moon Icon and AM / PM Indicator
        val dayNightIconRes = if (timeInfo.isDayTime) R.drawable.ic_sun else R.drawable.ic_moon
        views.setImageViewResource(R.id.widget_icon_day_night, dayNightIconRes)
        views.setContentDescription(
            R.id.widget_icon_day_night,
            if (timeInfo.isDayTime) "Daytime (Sun)" else "Nighttime (Moon)"
        )
        views.setTextViewText(R.id.widget_text_ampm, timeInfo.amPmString)

        // Apply theme-specific styling
        val isLight = prefs.theme == ClockPreferences.ThemeMode.LIGHT
        val bgDrawable = when (prefs.theme) {
            ClockPreferences.ThemeMode.LIGHT -> R.drawable.widget_bg_light
            ClockPreferences.ThemeMode.OLED -> R.drawable.widget_bg_oled
            else -> R.drawable.widget_bg_dark
        }
        val pillBgDrawable = if (isLight) R.drawable.badge_pill_light else R.drawable.badge_pill_dark
        val textColorPrimary = if (isLight) Color.parseColor("#0F172A") else Color.parseColor("#FFFFFF")
        val textColorSecondary = if (isLight) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")

        views.setInt(R.id.widget_container, "setBackgroundResource", bgDrawable)
        views.setTextColor(R.id.widget_text_ampm, textColorPrimary)
        views.setTextColor(R.id.widget_text_region, textColorSecondary)
        views.setInt(R.id.widget_text_dst, "setBackgroundResource", pillBgDrawable)

        // Region text
        views.setTextViewText(R.id.widget_text_region, context.getString(R.string.label_uk_london))

        // Daylight saving text
        val dstBadgeText = if (layoutRes == R.layout.widget_uk_clock_large) {
            "${timeInfo.tzCode} • ${timeInfo.offsetString}"
        } else {
            timeInfo.tzCode
        }
        views.setTextViewText(R.id.widget_text_dst, dstBadgeText)

        // Layout specific bindings (Digital time & Date for Medium and Large widgets)
        if (layoutRes != R.layout.widget_uk_clock_small) {
            views.setTextViewText(R.id.widget_text_digital_time, timeInfo.digitalTime12)
            views.setTextColor(R.id.widget_text_digital_time, textColorPrimary)

            val dateText = if (layoutRes == R.layout.widget_uk_clock_large) {
                timeInfo.dateLong
            } else {
                timeInfo.dateShort
            }
            views.setTextViewText(R.id.widget_text_date, dateText)
            views.setTextColor(R.id.widget_text_date, textColorSecondary)
        }

        // Tap action: Launch MainActivity and request immediate refresh
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        return views
    }

    companion object {
        const val ACTION_UPDATE_UK_CLOCK = "com.example.ACTION_UPDATE_UK_CLOCK"
        private const val ALARM_REQUEST_CODE = 8012

        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val nextMillis = ClockTimeHelper.getNextMinuteMillis()

            val intent = Intent(context, UKClockWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_UK_CLOCK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC,
                            nextMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC,
                            nextMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        nextMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                Log.w("UKClockWidget", "Exact alarm permission restricted, falling back", e)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC,
                    nextMillis,
                    pendingIntent
                )
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, UKClockWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_UK_CLOCK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
