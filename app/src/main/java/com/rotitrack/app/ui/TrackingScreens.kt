package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.MealReminder
import com.rotitrack.app.store.AppStore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------ reminders

@Composable
fun TrackingRemindersScreen(store: AppStore, platform: Platform, onWaterReminders: () -> Unit, onBack: () -> Unit) {
    val prefs = store.prefs
    val p = store.profile
    var editing by remember { mutableStateOf<MealReminder?>(null) }
    val meals = prefs.mealReminders.filter { it.key != "end_of_day" }
    val endOfDay = prefs.mealReminders.firstOrNull { it.key == "end_of_day" }

    fun save(r: MealReminder) {
        store.updatePrefs { pr -> pr.copy(mealReminders = pr.mealReminders.map { if (it.key == r.key) r else it }) }
        if (r.enabled) {
            platform.requestNotifications { platform.scheduleReminders(store.profile) }
        } else {
            platform.scheduleReminders(store.profile)
        }
    }

    SubScreen(t("Tracking Reminders"), onBack) {
        if (!platform.notificationsEnabled()) {
            Banner(
                t("Notifications are currently turned off for Roti Track.\nTo get reminders, allow notifications in system settings."),
                t("Open Settings"),
            ) { platform.openNotificationSettings() }
        }

        SettingsGroup {
            meals.forEachIndexed { i, r ->
                ReminderRow(r, last = i == meals.lastIndex, onTime = { editing = r }, onToggle = { save(r.copy(enabled = it)) })
            }
        }
        Spacer(Modifier.height(16.dp))
        endOfDay?.let { r ->
            SettingsGroup {
                ReminderRow(r, last = true, onTime = { editing = r }, onToggle = { save(r.copy(enabled = it)) })
                Text(t("Get one daily reminder and log all your meals at once."), style = Type.small, modifier = Modifier.padding(bottom = 16.dp))
            }
        }

        SectionLabel(t("Water reminders"))
        SettingsGroup {
            SettingsRow(
                "💧", t("Water reminders"),
                subtitle = if (p.remindersOn) t("{0} mode", prefs.waterReminderMode.label) else t("Off"),
                last = true, onClick = onWaterReminders,
            )
        }
    }

    editing?.let { r ->
        TimeDialog(r.hour, r.minute, onDismiss = { editing = null }) { h, m ->
            save(r.copy(hour = h, minute = m, enabled = true))
            editing = null
        }
    }
}

@Composable
private fun ReminderRow(r: MealReminder, last: Boolean, onTime: () -> Unit, onToggle: (Boolean) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t(r.label), style = Type.title, modifier = Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Palette.chip).clickable(onClick = onTime)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text(clockTime(r.hour, r.minute), style = Type.body) }
            Spacer(Modifier.width(10.dp))
            Switch(r.enabled, onToggle, colors = SwitchDefaults.colors(checkedTrackColor = Palette.night))
        }
        if (!last) Divider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text(t("OK")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel")) } },
        text = { TimePicker(state = state) },
    )
}

// -------------------------------------------------------------------- fasting

private val FAST_PLANS = listOf(12 to "12:12", 14 to "14:10", 16 to "16:8", 18 to "18:6", 20 to "20:4", 23 to "OMAD")

@Composable
fun FastingScreen(store: AppStore, onBack: () -> Unit) {
    val f = store.state.fasting
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(f.activeStart) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val targetMs = f.targetHours * 3_600_000L
    val elapsed = f.activeStart?.let { now - it } ?: 0L
    val fmt = SimpleDateFormat("EEE h:mm a", com.rotitrack.app.i18n.I18n.locale)

    SubScreen(t("Intermittent Fasting"), onBack) {
        Text(t("Plan"), style = Type.small, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FAST_PLANS.forEach { (h, label) ->
                Chip(label, f.targetHours == h, { if (f.activeStart == null) store.setFastTarget(h) }, Modifier.weight(1f), hPadding = 2.dp)
            }
        }
        Text(
            if (f.targetHours == 23) t("One meal a day: fast 23 hours, eat within 1 hour.")
            else t("Fast {0} hours, then eat within a {1}-hour window.", f.targetHours, 24 - f.targetHours),
            style = Type.small, modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(16.dp))
        AppCard {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val progress = if (f.activeStart == null) 0f else (elapsed.toFloat() / targetMs).coerceIn(0f, 1f)
                Canvas(Modifier.size(220.dp)) {
                    val stroke = 18.dp.toPx()
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(Palette.track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    drawArc(
                        if (progress >= 1f) Palette.leaf else Palette.saffron, -90f, 360f * progress, false, Offset(inset, inset), arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (f.activeStart == null) {
                        Text(t("Not fasting"), style = Type.h2)
                        Text(t("{0} h goal", f.targetHours), style = Type.small)
                    } else {
                        Text(if (elapsed >= targetMs) t("Goal reached! 🎉") else t("Fasting"), style = Type.small)
                        Text(duration(elapsed), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                        Text(
                            if (elapsed < targetMs) t("{0} to go", duration(targetMs - elapsed)) else t("+{0} extra", duration(elapsed - targetMs)),
                            style = Type.small,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            f.activeStart?.let {
                Text(t("Started {0} · ends {1}", fmt.format(Date(it)), fmt.format(Date(it + targetMs))), style = Type.small, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
            PillButton(
                if (f.activeStart == null) t("Start fast") else t("End fast"),
                { if (f.activeStart == null) store.startFast() else store.endFast() },
                Modifier.fillMaxWidth(),
                color = if (f.activeStart == null) Palette.brand else Palette.saffron,
            )
        }

        if (f.history.isNotEmpty()) {
            SectionLabel(t("Recent fasts"))
            SettingsGroup {
                val recent = f.history.takeLast(10).asReversed()
                recent.forEachIndexed { i, r ->
                    val done = r.end - r.start >= r.targetHours * 3_600_000L
                    SettingsRow(
                        if (done) "✅" else "⏱️", duration(r.end - r.start),
                        subtitle = t("{0} · goal {1} h", fmt.format(Date(r.start)), r.targetHours),
                        last = i == recent.lastIndex,
                    )
                }
            }
        }
        Text(
            t("Fasting isn't for everyone. Skip it if you're pregnant, diabetic, underweight or have had an eating disorder, and talk to a doctor first."),
            style = Type.small, modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private fun duration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}
