package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure background alarm ticker is active whenever app is opened
        UKClockWidgetProvider.scheduleNextAlarm(this)

        setContent {
            MyApplicationTheme {
                UKClockAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UKClockAppScreen() {
    val context = LocalContext.current
    val prefs = remember { ClockPreferences(context) }

    var currentTheme by remember { mutableStateOf(prefs.theme) }
    var currentStyle by remember { mutableStateOf(prefs.clockStyle) }
    var showSeconds by remember { mutableStateOf(prefs.showSeconds) }

    // Live ticking time state updated every second
    var londonTime by remember { mutableStateOf(ClockTimeHelper.getCurrentLondonTime()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            londonTime = ClockTimeHelper.getCurrentLondonTime()
            delay(1000L)
        }
    }

    // Render live bitmap preview for the current settings
    val clockBitmap = remember(londonTime.minute, londonTime.hour12, londonTime.second, currentTheme, currentStyle, showSeconds) {
        ClockBitmapRenderer.renderClockBitmap(
            size = 480,
            timeInfo = londonTime,
            theme = currentTheme,
            clockStyle = currentStyle,
            showSeconds = showSeconds
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Europe/London Timezone",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .widthIn(max = 640.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Clock Preview Card
            ClockPreviewCard(
                londonTime = londonTime,
                clockBitmap = clockBitmap,
                currentTheme = currentTheme
            )

            // Direct Pin / Add Widget Action Card
            AddWidgetCard(context = context)

            // Minimal Settings Card (Theme, Style, Seconds Hand)
            SettingsCard(
                currentTheme = currentTheme,
                onThemeSelected = { newTheme ->
                    currentTheme = newTheme
                    prefs.theme = newTheme
                },
                currentStyle = currentStyle,
                onStyleSelected = { newStyle ->
                    currentStyle = newStyle
                    prefs.clockStyle = newStyle
                },
                showSeconds = showSeconds,
                onToggleSeconds = { enabled ->
                    showSeconds = enabled
                    prefs.showSeconds = enabled
                }
            )

            // Timezone & DST Details Card
            TimezoneDetailsCard(londonTime = londonTime)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ClockPreviewCard(
    londonTime: ClockTimeHelper.LondonTimeInfo,
    clockBitmap: Bitmap,
    currentTheme: ClockPreferences.ThemeMode
) {
    val isLight = currentTheme == ClockPreferences.ThemeMode.LIGHT
    val cardBg = when (currentTheme) {
        ClockPreferences.ThemeMode.LIGHT -> MaterialTheme.colorScheme.surface
        ClockPreferences.ThemeMode.OLED -> Slate950
        else -> Slate900
    }
    val cardBorder = when (currentTheme) {
        ClockPreferences.ThemeMode.LIGHT -> Slate700.copy(alpha = 0.2f)
        ClockPreferences.ThemeMode.OLED -> Slate800
        else -> Slate800
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clock_preview_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: UK • LONDON and DST badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_uk_london),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Slate400
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (londonTime.isDayTime) Color(0xFFFEF3C7) else Color(0xFF1E293B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (londonTime.isDayTime) "☀️ Day" else "🌙 Night",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (londonTime.isDayTime) Color(0xFFD97706) else SkyBlue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isLight) Slate200 else Slate800)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${londonTime.tzCode} • ${londonTime.offsetString}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SkyBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Realistic Analog Clock Face
            Image(
                bitmap = clockBitmap.asImageBitmap(),
                contentDescription = londonTime.accessibilityDescription,
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .testTag("clock_preview_image")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Large AM / PM Indicator with Day / Night Sun or Moon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    painter = painterResource(if (londonTime.isDayTime) R.drawable.ic_sun else R.drawable.ic_moon),
                    contentDescription = if (londonTime.isDayTime) "Day (Sun)" else "Night (Moon)",
                    tint = if (londonTime.isDayTime) Color(0xFFF59E0B) else SkyBlue,
                    modifier = Modifier.size(80.dp)
                )
                Text(
                    text = londonTime.amPmString,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isLight) Slate900 else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("text_ampm_indicator")
                )
            }

            // Digital London Time
            Text(
                text = londonTime.digitalTime12WithSec,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = if (isLight) Slate900 else MaterialTheme.colorScheme.onSurface
            )

            // Full Date
            Text(
                text = londonTime.dateLong,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AddWidgetCard(context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_widget_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Add to Home Screen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(R.string.instruction_add_widget),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Pin widget button (for launchers that support requestPinAppWidget)
            Button(
                onClick = {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val myProvider = ComponentName(context, UKClockWidgetProvider::class.java)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                        val pinnedWidgetCallbackIntent = Intent(context, UKClockWidgetProvider::class.java).apply {
                            action = UKClockWidgetProvider.ACTION_UPDATE_UK_CLOCK
                        }
                        val successCallback = PendingIntent.getBroadcast(
                            context,
                            0,
                            pinnedWidgetCallbackIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                    } else {
                        Toast.makeText(
                            context,
                            "Long press your Home Screen and choose Widgets to add UK Clock Widget.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_add_widget"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Pin Widget to Home Screen", fontWeight = FontWeight.SemiBold)
            }

            // Visual Steps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StepItem(step = "1", text = "Long press any empty space on your Android Home Screen")
                StepItem(step = "2", text = "Tap \"Widgets\" from the pop-up menu")
                StepItem(step = "3", text = "Scroll to find \"UK Clock Widget\"")
                StepItem(step = "4", text = "Drag to place, then resize (Small, Medium, or Large)")
            }
        }
    }
}

@Composable
fun StepItem(step: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingsCard(
    currentTheme: ClockPreferences.ThemeMode,
    onThemeSelected: (ClockPreferences.ThemeMode) -> Unit,
    currentStyle: ClockPreferences.ClockStyle,
    onStyleSelected: (ClockPreferences.ClockStyle) -> Unit,
    showSeconds: Boolean,
    onToggleSeconds: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Widget Customization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Theme Options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme Appearance",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        label = "Dark",
                        isSelected = currentTheme == ClockPreferences.ThemeMode.DARK,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeSelected(ClockPreferences.ThemeMode.DARK) }
                    )
                    ThemeOptionButton(
                        label = "Light",
                        isSelected = currentTheme == ClockPreferences.ThemeMode.LIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeSelected(ClockPreferences.ThemeMode.LIGHT) }
                    )
                    ThemeOptionButton(
                        label = "OLED",
                        isSelected = currentTheme == ClockPreferences.ThemeMode.OLED,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeSelected(ClockPreferences.ThemeMode.OLED) }
                    )
                }
            }

            HorizontalDivider(color = Slate700.copy(alpha = 0.2f))

            // Clock Style Options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dial Style",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        label = "Minimal (Ticks)",
                        isSelected = currentStyle == ClockPreferences.ClockStyle.MINIMAL,
                        modifier = Modifier.weight(1f),
                        onClick = { onStyleSelected(ClockPreferences.ClockStyle.MINIMAL) }
                    )
                    ThemeOptionButton(
                        label = "Classic (12, 3, 6, 9)",
                        isSelected = currentStyle == ClockPreferences.ClockStyle.CLASSIC,
                        modifier = Modifier.weight(1f),
                        onClick = { onStyleSelected(ClockPreferences.ClockStyle.CLASSIC) }
                    )
                }
            }

            HorizontalDivider(color = Slate700.copy(alpha = 0.2f))

            // Seconds Hand Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Display Seconds Hand",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Real-time in app. (Widget updates every minute to conserve battery)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
                Switch(
                    checked = showSeconds,
                    onCheckedChange = onToggleSeconds,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("switch_show_seconds")
                )
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Slate700.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TimezoneDetailsCard(londonTime: ClockTimeHelper.LondonTimeInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timezone_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = SkyBlue
                )
                Text(
                    text = "Timezone & Daylight Saving",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TimezoneInfoRow(label = "Zone Identifier", value = "Europe/London")
            TimezoneInfoRow(label = "Active Standard", value = "${londonTime.tzCode} (${londonTime.tzFullName})")
            TimezoneInfoRow(
                label = "Day / Night Cycle",
                value = if (londonTime.isDayTime) "☀️ Day (Sun active: 06:00 - 18:00)" else "🌙 Night (Moon active: 18:00 - 06:00)"
            )
            TimezoneInfoRow(label = "UTC Offset", value = londonTime.offsetString)
            TimezoneInfoRow(
                label = "Daylight Saving Status",
                value = if (londonTime.isDst) "BST Active (Clocks +1 hour)" else "GMT Active (Standard Time)"
            )
            TimezoneInfoRow(label = "Network Dependency", value = "Offline (Uses Android ZoneRules)")
        }
    }
}

@Composable
fun TimezoneInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
