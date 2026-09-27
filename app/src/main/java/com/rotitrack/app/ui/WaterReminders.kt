package com.rotitrack.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.Prefs
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.ReminderTime
import com.rotitrack.app.data.WaterReminderMode
import com.rotitrack.app.data.WaterSound
import com.rotitrack.app.domain.WaterSchedule
import com.rotitrack.app.store.AppStore

private const val MAX_CUSTOM_TIMES = 24

/** Water reminders: on/off, Standard / Interval / Custom mode, weekend mode, and sounds. */
@Composable
fun WaterRemindersScreen(store: AppStore, platform: Platform, onBack: () -> Unit) {
    val p = store.profile
    val prefs = store.prefs
    var editStandard by remember { mutableStateOf<Int?>(null) }
    var addingCustom by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ReminderTime?>(null) }
    var help by remember { mutableStateOf(false) }
    var sounds by remember { mutableStateOf(false) }

    fun reschedule() = platform.scheduleReminders(store.profile)
    fun prefs(f: (Prefs) -> Prefs) {
        store.updatePrefs(f)
        reschedule()
    }
    fun profile(f: (Profile) -> Profile) {
        store.updateProfile(f)
        reschedule()
    }
    fun setOn(on: Boolean) {
        if (on) {
            platform.requestNotifications { granted -> profile { it.copy(remindersOn = granted) } }
        } else {
            profile { it.copy(remindersOn = false) }
        }
    }

    Column(Modifier.fillMaxSize().background(Palette.brandDeep)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Text("‹", fontSize = 34.sp, color = Color.White)
            }
            Text("Reminder", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f))
            Switch(
                p.remindersOn, ::setOn,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = Palette.aqua,
                    uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.White.copy(alpha = 0.25f),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
        val modes = rememberScrollState()
        // Keep the chosen mode's card in view.
        LaunchedEffect(prefs.waterReminderMode, modes.maxValue) {
            modes.animateScrollTo(modes.maxValue * prefs.waterReminderMode.ordinal / (WaterReminderMode.entries.size - 1))
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(modes).padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WaterReminderMode.entries.forEach { m ->
                ModeCard(m.label, selected = prefs.waterReminderMode == m) { prefs { it.copy(waterReminderMode = m) } }
            }
        }

        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Palette.card)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            if (!p.remindersOn) {
                Banner("Water reminders are off. Turn them on with the switch at the top.", "Turn on", Palette.chip) { setOn(true) }
            } else if (!platform.notificationsEnabled()) {
                Banner("Notifications are turned off for Roti Track, so reminders can't appear.", "Open Settings", Palette.chip) {
                    platform.openNotificationSettings()
                }
            }
            val mode = prefs.waterReminderMode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mode.label, style = Type.h2)
                if (mode == WaterReminderMode.CUSTOM) {
                    Spacer(Modifier.width(6.dp))
                    Icon(TabIcons.Info, "How it works", tint = Palette.muted, modifier = Modifier.size(20.dp))
                }
            }
            Text(mode.blurb, style = Type.body.copy(color = Palette.muted))
            Spacer(Modifier.height(12.dp))

            Column(Modifier.alpha(if (p.remindersOn) 1f else 0.55f)) {
                when (mode) {
                    WaterReminderMode.STANDARD -> prefs.standardTimes.forEachIndexed { i, t ->
                        StandardRow(
                            t, last = i == prefs.standardTimes.lastIndex,
                            onEdit = { editStandard = i },
                            onToggle = { on -> prefs { pr -> pr.copy(standardTimes = pr.standardTimes.mapIndexed { j, x -> if (j == i) x.copy(enabled = on) else x }) } },
                        )
                    }
                    WaterReminderMode.INTERVAL -> IntervalSettings(p, ::profile)
                    WaterReminderMode.CUSTOM -> {
                        Text("Tap a time to turn it on or off. Press and hold to delete it.", style = Type.small, modifier = Modifier.padding(bottom = 12.dp))
                        TimeGrid(
                            prefs.customTimes,
                            onClick = { t -> prefs { pr -> pr.copy(customTimes = pr.customTimes.map { if (it == t) it.copy(enabled = !it.enabled) else it }) } },
                            onLongClick = { deleting = it },
                            trailing = if (prefs.customTimes.size < MAX_CUSTOM_TIMES) ({ addingCustom = true }) else null,
                        )
                    }
                }
            }

            Text(
                "Can't receive reminders?",
                style = Type.body.copy(color = Palette.muted, textDecoration = TextDecoration.Underline),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp, bottom = 16.dp).clickable { help = true },
            )
            Divider()
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Palette.chip).padding(horizontal = 18.dp, vertical = 8.dp)) {
                ToggleLine("Reminder Weekend Mode", prefs.weekendMode, "No water reminders on Saturday and Sunday") { on ->
                    prefs { it.copy(weekendMode = on) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Palette.chip).clickable { sounds = true }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(TabIcons.Speaker, null, tint = Palette.ink, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sounds & Effects", style = Type.title)
                    Text(
                        listOfNotNull(if (prefs.soundOn) prefs.sound.label else "Silent", "vibration".takeIf { prefs.vibration }).joinToString(" · "),
                        style = Type.small,
                    )
                }
                Text("›", fontSize = 24.sp, color = Palette.muted)
            }
            Spacer(Modifier.height(12.dp))
            val next = if (p.remindersOn) WaterSchedule.next(java.time.LocalDateTime.now(), p, prefs) else null
            Text(
                (next?.let { "Next reminder: ${if (it.toLocalDate() == java.time.LocalDate.now()) "today" else it.dayOfWeek.name.lowercase().replaceFirstChar { c -> c.uppercase() }} at ${clockTime(it.hour, it.minute)}. " } ?: "") +
                    "Reminders stop for the day once you reach your water goal.",
                style = Type.small,
            )
        }
    }

    editStandard?.let { i ->
        val t = prefs.standardTimes[i]
        TimeDialog(t.hour, t.minute, onDismiss = { editStandard = null }) { h, m ->
            prefs { pr -> pr.copy(standardTimes = pr.standardTimes.mapIndexed { j, x -> if (j == i) x.copy(hour = h, minute = m, enabled = true) else x }) }
            editStandard = null
        }
    }
    if (addingCustom) {
        TimeDialog(12, 0, onDismiss = { addingCustom = false }) { h, m ->
            prefs { pr ->
                val rest = pr.customTimes.filterNot { it.hour == h && it.minute == m }
                pr.copy(customTimes = (rest + ReminderTime(h, m)).sortedBy { it.minuteOfDay })
            }
            addingCustom = false
        }
    }
    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${clockTime(t.hour, t.minute)}?") },
            confirmButton = {
                TextButton(onClick = {
                    prefs { pr -> pr.copy(customTimes = pr.customTimes - t) }
                    deleting = null
                }) { Text("Delete", color = Palette.danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
    if (help) CantReceiveDialog(platform) { help = false }
    if (sounds) SoundsSheet(store, platform) { sounds = false }
}

@Composable
private fun ModeCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.width(196.dp).height(100.dp).clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$label\nMode", fontSize = 21.sp, lineHeight = 25.sp, maxLines = 2, softWrap = false, fontWeight = FontWeight.ExtraBold,
            color = if (selected) Palette.brandDeep else Color.White.copy(alpha = 0.45f),
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (selected) Palette.aqua else Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = if (selected) Color.White else Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun StandardRow(t: ReminderTime, last: Boolean, onEdit: () -> Unit, onToggle: (Boolean) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t.label, style = Type.title.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onEdit).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(clockTime(t.hour, t.minute), style = Type.title.copy(color = if (t.enabled) Palette.inkSoft else Palette.muted, fontWeight = FontWeight.Medium))
                Spacer(Modifier.width(4.dp))
                Icon(TabIcons.Pencil, "Change time", tint = Palette.muted, modifier = Modifier.size(16.dp))
            }
            Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(28.dp).background(Palette.divider))
            Switch(t.enabled, onToggle, colors = SwitchDefaults.colors(checkedTrackColor = Palette.brand))
        }
        if (!last) Divider()
    }
}

@Composable
private fun IntervalSettings(p: Profile, change: ((Profile) -> Profile) -> Unit) {
    Text("Remind me every", style = Type.small, modifier = Modifier.padding(top = 4.dp))
    Stepper(
        if (p.reminderEveryMin % 60 == 0) "${p.reminderEveryMin / 60} h" else "${p.reminderEveryMin / 60} h ${p.reminderEveryMin % 60} min",
        onMinus = { change { it.copy(reminderEveryMin = (it.reminderEveryMin - 30).coerceAtLeast(30)) } },
        onPlus = { change { it.copy(reminderEveryMin = (it.reminderEveryMin + 30).coerceAtMost(240)) } },
    )
    Text("Wake-up time", style = Type.small, modifier = Modifier.padding(top = 12.dp))
    Stepper(
        hour(p.wakeHour),
        onMinus = { change { it.copy(wakeHour = (it.wakeHour - 1).coerceAtLeast(4)) } },
        onPlus = { change { it.copy(wakeHour = (it.wakeHour + 1).coerceAtMost(it.sleepHour - 2)) } },
    )
    Text("Bedtime", style = Type.small, modifier = Modifier.padding(top = 12.dp))
    Stepper(
        hour(p.sleepHour),
        onMinus = { change { it.copy(sleepHour = (it.sleepHour - 1).coerceAtLeast(it.wakeHour + 2)) } },
        onPlus = { change { it.copy(sleepHour = (it.sleepHour + 1).coerceAtMost(23)) } },
    )
    val times = WaterSchedule.intervalTimes(p.wakeHour, p.sleepHour, p.reminderEveryMin)
    Text("${times.size} reminders a day", style = Type.title, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
    TimeGrid(times.map { ReminderTime(it / 60, it % 60) }, onClick = null, onLongClick = null, trailing = null)
}

/** Three pill-shaped times per row; blue when the reminder is on. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeGrid(
    times: List<ReminderTime>,
    onClick: ((ReminderTime) -> Unit)?,
    onLongClick: ((ReminderTime) -> Unit)?,
    trailing: (() -> Unit)?,
) {
    val cells: List<ReminderTime?> = times + if (trailing != null) listOf(null) else emptyList()
    cells.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { t ->
                val base = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                if (t == null) {
                    Box(base.background(Palette.chip).clickable { trailing?.invoke() }, contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 26.sp, color = Palette.muted)
                    }
                } else {
                    val click = if (onClick != null) {
                        Modifier.combinedClickable(onClick = { onClick(t) }, onLongClick = onLongClick?.let { { it(t) } })
                    } else Modifier
                    Box(
                        base.background(if (t.enabled) Palette.brand else Palette.chip).then(click),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            clockTime(t.hour, t.minute), maxLines = 1, softWrap = false,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (t.enabled) Color.White else Palette.muted,
                        )
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun CantReceiveDialog(platform: Platform, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Can't receive reminders?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Allow notifications for Roti Track.", style = Type.body)
                Text(
                    "Open notification settings ›", style = Type.title.copy(color = Palette.brand, fontSize = 15.sp),
                    modifier = Modifier.clickable { platform.openNotificationSettings() },
                )
                Text("2. Let Roti Track run in the background: set battery use to \"Unrestricted\" or \"Not optimised\".", style = Type.body)
                Text(
                    "Open battery settings ›", style = Type.title.copy(color = Palette.brand, fontSize = 15.sp),
                    modifier = Modifier.clickable { platform.openBatterySettings() },
                )
                Text("3. On Xiaomi, Vivo, Oppo, Realme and OnePlus phones, also turn on Autostart for Roti Track, and lock it in Recent apps.", style = Type.body)
                Text("4. Check that Do Not Disturb is off and the phone isn't on silent.", style = Type.body)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}

/** The speaker button's sheet: pick the water sound, its volume, and vibration. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundsSheet(store: AppStore, platform: Platform, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val prefs = store.prefs
    var volume by remember { mutableStateOf(prefs.soundVolume) }
    val close = {
        platform.stopSound()
        onDismiss()
    }
    ModalBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Palette.card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Sounds & Effects", style = Type.screenTitle.copy(fontSize = 28.sp))
            Spacer(Modifier.height(12.dp))
            SheetToggle(TabIcons.Speaker, "Sound effect", "Plays with each reminder and when you log a drink", prefs.soundOn) { on ->
                store.updatePrefs { it.copy(soundOn = on) }
                if (on) platform.playSound(store.prefs.sound, volume) else platform.stopSound()
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(Palette.chip)
                    .alpha(if (prefs.soundOn) 1f else 0.45f),
            ) {
                WaterSound.entries.forEachIndexed { i, s ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = prefs.soundOn) {
                            store.updatePrefs { it.copy(sound = s) }
                            platform.playSound(s, volume)
                        }.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = prefs.sound == s, enabled = prefs.soundOn,
                            onClick = {
                                store.updatePrefs { it.copy(sound = s) }
                                platform.playSound(s, volume)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Palette.brand),
                        )
                        Text(s.label, style = Type.title, modifier = Modifier.weight(1f).padding(start = 6.dp))
                        Text("${s.seconds}s", style = Type.body.copy(color = Palette.muted), modifier = Modifier.padding(end = 8.dp))
                    }
                    if (i < WaterSound.entries.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.card))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (prefs.soundOn) 1f else 0.45f)) {
                fun nudge(by: Float) {
                    volume = (volume + by).coerceIn(0f, 1f)
                    store.updatePrefs { it.copy(soundVolume = volume) }
                    platform.playSound(store.prefs.sound, volume)
                }
                TextButton(onClick = { nudge(-0.1f) }, enabled = prefs.soundOn) { Text("−", fontSize = 26.sp, color = Palette.ink) }
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = {
                        store.updatePrefs { it.copy(soundVolume = volume) }
                        platform.playSound(store.prefs.sound, volume)
                    },
                    enabled = prefs.soundOn,
                    colors = SliderDefaults.colors(thumbColor = Palette.brand, activeTrackColor = Palette.brand, inactiveTrackColor = Palette.track),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { nudge(0.1f) }, enabled = prefs.soundOn) { Text("+", fontSize = 26.sp, color = Palette.ink) }
            }
            SheetToggle(TabIcons.Vibrate, "Vibration", "Buzz with each reminder", prefs.vibration) { on ->
                store.updatePrefs { it.copy(vibration = on) }
                if (on) platform.vibrate()
            }
            Spacer(Modifier.height(18.dp))
            PillButton("Done", close, Modifier.fillMaxWidth(), height = 58.dp)
        }
    }
}

@Composable
private fun SheetToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Palette.ink, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.title.copy(fontSize = 20.sp))
            Text(subtitle, style = Type.small)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Palette.brand))
    }
}

/** A round icon button for the Water screen's header. */
@Composable
fun HeaderIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(Palette.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp)) }
}
