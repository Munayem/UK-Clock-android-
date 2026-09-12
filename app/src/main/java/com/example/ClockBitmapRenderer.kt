package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders an anti-aliased, mathematically accurate analog clock face onto a Bitmap.
 * Used by RemoteViews for the Android App Widget and available for previews.
 */
object ClockBitmapRenderer {

    fun renderClockBitmap(
        size: Int = 512,
        timeInfo: ClockTimeHelper.LondonTimeInfo,
        theme: ClockPreferences.ThemeMode = ClockPreferences.ThemeMode.DARK,
        clockStyle: ClockPreferences.ClockStyle = ClockPreferences.ClockStyle.MINIMAL,
        showSeconds: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val radius = (size / 2f) * 0.92f

        val isLight = theme == ClockPreferences.ThemeMode.LIGHT
        val isOled = theme == ClockPreferences.ThemeMode.OLED

        // Theme-specific colors
        val dialBgColor = when {
            isLight -> Color.parseColor("#F8FAFC")
            isOled -> Color.parseColor("#000000")
            else -> Color.parseColor("#0F172A")
        }
        val bezelColor = when {
            isLight -> Color.parseColor("#E2E8F0")
            isOled -> Color.parseColor("#27272A")
            else -> Color.parseColor("#1E293B")
        }
        val outerRingColor = when {
            isLight -> Color.parseColor("#CBD5E1")
            isOled -> Color.parseColor("#3F3F46")
            else -> Color.parseColor("#334155")
        }
        val majorTickColor = when {
            isLight -> Color.parseColor("#0F172A")
            else -> Color.parseColor("#FFFFFF")
        }
        val minorTickColor = when {
            isLight -> Color.parseColor("#94A3B8")
            else -> Color.parseColor("#475569")
        }
        val hourHandColor = when {
            isLight -> Color.parseColor("#0F172A")
            else -> Color.parseColor("#FFFFFF")
        }
        val minuteHandColor = when {
            isLight -> Color.parseColor("#1E293B")
            else -> Color.parseColor("#E2E8F0")
        }
        val secondHandColor = Color.parseColor("#F43F5E")
        val accentPillColor = Color.parseColor("#38BDF8")

        // 1. Draw Dial Base Circle
        val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.FILL
            color = dialBgColor
        }
        canvas.drawCircle(cx, cy, radius, dialPaint)

        // 2. Draw Bezel Ring
        val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.022f
            color = bezelColor
        }
        canvas.drawCircle(cx, cy, radius - (bezelPaint.strokeWidth / 2f), bezelPaint)

        // 3. Draw Minute Track Outer Ring
        val trackRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.003f
            color = outerRingColor
        }
        canvas.drawCircle(cx, cy, radius * 0.88f, trackRingPaint)

        // 4. Draw Dial Text ("LONDON" placed cleanly above the large central Sun/Moon disc)
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = minorTickColor
            textSize = size * 0.040f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.22f
        }
        canvas.drawText("LONDON", cx, cy - (radius * 0.63f), brandPaint)

        // 5. Draw Ticks / Hour Markers
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = majorTickColor
            textSize = size * 0.075f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()

        for (i in 0 until 60) {
            val angleDeg = i * 6.0
            val angleRad = Math.toRadians(angleDeg)
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            val isHour = i % 5 == 0
            val hourNum = if (i == 0) 12 else i / 5

            if (isHour) {
                if (clockStyle == ClockPreferences.ClockStyle.CLASSIC && (hourNum == 12 || hourNum == 3 || hourNum == 6 || hourNum == 9)) {
                    // Draw numbers for 12, 3, 6, 9 in classic mode
                    val textDist = radius * 0.72f
                    val tx = cx + sinA * textDist
                    val ty = cy - cosA * textDist
                    val str = hourNum.toString()
                    textPaint.getTextBounds(str, 0, str.length, textBounds)
                    canvas.drawText(str, tx, ty + (textBounds.height() / 2f), textPaint)
                } else {
                    // Draw bold hour batons
                    val startR = radius * 0.74f
                    val endR = radius * 0.86f
                    val isTop12 = i == 0

                    tickPaint.strokeWidth = if (isTop12) size * 0.024f else size * 0.018f
                    tickPaint.color = if (isTop12) accentPillColor else majorTickColor

                    val startX = cx + sinA * startR
                    val startY = cy - cosA * startR
                    val endX = cx + sinA * endR
                    val endY = cy - cosA * endR

                    canvas.drawLine(startX, startY, endX, endY, tickPaint)

                    // In minimal style, draw double baton for 12 o'clock
                    if (clockStyle == ClockPreferences.ClockStyle.MINIMAL && isTop12) {
                        val offset = size * 0.015f
                        canvas.drawLine(startX - offset, startY, endX - offset, endY, tickPaint)
                        canvas.drawLine(startX + offset, startY, endX + offset, endY, tickPaint)
                    }
                }
            } else {
                // Subtle minute ticks
                val startR = radius * 0.82f
                val endR = radius * 0.86f
                tickPaint.strokeWidth = size * 0.005f
                tickPaint.color = minorTickColor

                val startX = cx + sinA * startR
                val startY = cy - cosA * startR
                val endX = cx + sinA * endR
                val endY = cy - cosA * endR

                canvas.drawLine(startX, startY, endX, endY, tickPaint)
            }
        }

        // 5b. Draw Day / Night Complication (Large Centered Celestial Sun & Moon Medallion)
        val compX = cx
        val compY = cy
        val compR = radius * 0.56f

        // Center Celestial Disc Background
        val apertureBgColor = when {
            isLight -> Color.parseColor("#E4EBF4")
            isOled -> Color.parseColor("#05070B")
            else -> Color.parseColor("#0A1128")
        }
        val apertureBorderColor = when {
            isLight -> Color.parseColor("#94A3B8")
            isOled -> Color.parseColor("#52525B")
            else -> Color.parseColor("#475569")
        }

        val apertureBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = apertureBgColor
        }
        canvas.drawCircle(compX, compY, compR, apertureBgPaint)

        val apertureBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.007f
            color = apertureBorderColor
        }
        canvas.drawCircle(compX, compY, compR, apertureBorderPaint)

        if (timeInfo.isDayTime) {
            // --- DAY: Extra-Large Radiant Golden Sun ---
            val sunCenterY = compY - (compR * 0.06f)
            val sunRadius = compR * 0.38f

            // 16 Radiant Sun Rays extending across the large central disc
            val sunRayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = size * 0.013f
                color = Color.parseColor("#F59E0B")
            }
            val rayInner = sunRadius * 1.20f
            val rayOuter = sunRadius * 1.96f
            for (r in 0 until 16) {
                val rAngle = Math.toRadians(r * 22.5)
                val cosR = cos(rAngle).toFloat()
                val sinR = sin(rAngle).toFloat()
                canvas.drawLine(
                    compX + cosR * rayInner,
                    sunCenterY + sinR * rayInner,
                    compX + cosR * rayOuter,
                    sunCenterY + sinR * rayOuter,
                    sunRayPaint
                )
            }

            // Outer glowing golden corona
            val sunCoronaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#FBBF24")
            }
            canvas.drawCircle(compX, sunCenterY, sunRadius, sunCoronaPaint)

            // Core warm amber sun
            val sunCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#F59E0B")
            }
            canvas.drawCircle(compX, sunCenterY, sunRadius * 0.76f, sunCorePaint)

            // Micro-label "DAY"
            val dayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = if (isLight) Color.parseColor("#B45309") else Color.parseColor("#FBBF24")
                textSize = size * 0.040f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.20f
            }
            canvas.drawText("DAY", compX, compY + (compR * 0.76f), dayLabelPaint)
        } else {
            // --- NIGHT: Extra-Large Luminous Crescent Moon & Stars ---
            val moonCenterY = compY - (compR * 0.06f)
            val moonRadius = compR * 0.48f

            // Multiple large twinkling celestial stars
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#38BDF8")
            }
            canvas.drawCircle(compX + (compR * 0.50f), compY - (compR * 0.44f), size * 0.014f, starPaint)
            starPaint.color = Color.parseColor("#FDE047")
            canvas.drawCircle(compX + (compR * 0.65f), compY - (compR * 0.08f), size * 0.011f, starPaint)
            starPaint.color = Color.parseColor("#F8FAFC")
            canvas.drawCircle(compX - (compR * 0.54f), compY - (compR * 0.42f), size * 0.011f, starPaint)
            starPaint.color = Color.parseColor("#7DD3FC")
            canvas.drawCircle(compX - (compR * 0.62f), compY + (compR * 0.16f), size * 0.009f, starPaint)
            starPaint.color = Color.parseColor("#FDE047")
            canvas.drawCircle(compX + (compR * 0.42f), compY + (compR * 0.42f), size * 0.009f, starPaint)

            // Extra Large Crescent Moon Body
            val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#FDE047")
            }
            val moonX = compX - (compR * 0.10f)
            canvas.drawCircle(moonX, moonCenterY, moonRadius, moonPaint)

            // Subtract circular shadow with aperture background to make sharp, prominent crescent
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = apertureBgColor
            }
            canvas.drawCircle(moonX + (moonRadius * 0.44f), moonCenterY - (moonRadius * 0.18f), moonRadius * 0.88f, shadowPaint)

            // Micro-label "NIGHT"
            val nightLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = Color.parseColor("#38BDF8")
                textSize = size * 0.040f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.20f
            }
            canvas.drawText("NIGHT", compX, compY + (compR * 0.76f), nightLabelPaint)
        }

        // 6. Draw Hands
        // Hour Hand
        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = size * 0.026f
            color = hourHandColor
        }
        val hourLength = radius * 0.52f
        val hourBackLength = radius * 0.10f

        canvas.save()
        canvas.rotate(timeInfo.hourAngle, cx, cy)
        // Hand extends from cy + hourBackLength to cy - hourLength
        canvas.drawLine(cx, cy + hourBackLength, cx, cy - hourLength, hourPaint)
        canvas.restore()

        // Minute Hand
        val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = size * 0.016f
            color = minuteHandColor
        }
        val minuteLength = radius * 0.78f
        val minuteBackLength = radius * 0.12f

        canvas.save()
        canvas.rotate(timeInfo.minuteAngle, cx, cy)
        canvas.drawLine(cx, cy + minuteBackLength, cx, cy - minuteLength, minutePaint)
        canvas.restore()

        // Optional Second Hand
        if (showSeconds) {
            val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = size * 0.007f
                color = secondHandColor
            }
            val secLength = radius * 0.84f
            val secBackLength = radius * 0.18f

            canvas.save()
            canvas.rotate(timeInfo.secondAngle, cx, cy)
            canvas.drawLine(cx, cy + secBackLength, cx, cy - secLength, secPaint)
            // Accent counterbalance dot
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = secondHandColor
            }
            canvas.drawCircle(cx, cy + (secBackLength * 0.7f), size * 0.012f, dotPaint)
            canvas.restore()
        }

        // 7. Center Pivot Assembly
        // Outer pivot ring
        val pivotOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = hourHandColor
        }
        canvas.drawCircle(cx, cy, size * 0.024f, pivotOuterPaint)

        // Center pivot core
        val pivotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (showSeconds) secondHandColor else dialBgColor
        }
        canvas.drawCircle(cx, cy, size * 0.010f, pivotCorePaint)

        return bitmap
    }
}
