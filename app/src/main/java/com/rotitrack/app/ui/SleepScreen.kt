package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.SleepEntry
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.Sleep
import com.rotitrack.app.store.AppStore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

/** "7 h 20 min". */
fun sleepDuration(minutes: Int): String =
    if (minutes % 60 == 0) t("{0} h", minutes / 60) else t("{0} h {1} min", minutes / 60, minutes % 60)

private val QUALITY = listOf(1 to "😫", 2 to "🙂", 3 to "😴")
private fun qualityLabel(q: Int): String = when (q) {
    1 -> t("Poor")
    2 -> t("Okay")
    3 -> t("Great")
    else -> ""
}

@Composable
fun SleepScreen(store: AppStore, onBack: () -> Unit) {
    val log = store.state.sleep
    val goal = store.prefs.sleepGoalMin
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(log.activeStart) {
        while (log.activeStart != null) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }
    val time = SimpleDateFormat("h:mm a", com.rotitrack.app.i18n.I18n.locale)
    val dayTime = SimpleDateFormat("EEE d MMM", com.rotitrack.app.i18n.I18n.locale)
    var manual by remember { mutableStateOf(false) }

    SubScreen(t("Sleep"), onBack) {
        AppCard(color = Palette.night) {
            val start = log.activeStart
            if (start != null) {
                Text("🌙 " + t("Sleeping"), style = Type.small.copy(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)))
                Text(sleepDuration(((now - start) / 60_000).toInt().coerceAtLeast(0)), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White)
                Text(t("Since {0}", time.format(Date(start))), style = Type.small.copy(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)))
                Spacer(Modifier.height(14.dp))
                PillButton(t("☀️ I'm awake"), { store.endSleep() }, Modifier.fillMaxWidth(), color = Palette.saffron)
            } else {
                val last = store.lastSleep
                Text(t("Last night"), style = Type.small.copy(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)))
                Text(
                    last?.let { sleepDuration(Sleep.minutes(it)) } ?: "—",
                    fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White,
                )
                last?.let {
                    Text(
                        "${time.format(Date(it.start))} → ${time.format(Date(it.end))}" + if (it.quality > 0) " · ${QUALITY[it.quality - 1].second} ${qualityLabel(it.quality)}" else "",
                        style = Type.small.copy(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)),
                    )
                }
                Spacer(Modifier.height(14.dp))
                PillButton(t("🌙 Going to sleep"), { store.startSleep() }, Modifier.fillMaxWidth(), color = Palette.brand)
                Text(
                    t("Log sleep manually"),
                    style = Type.body.copy(color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally).clickable { manual = !manual }.padding(top = 12.dp),
                )
            }
        }

        // Ask how the last night went, until it's rated.
        store.lastSleep?.takeIf { it.quality == 0 && log.activeStart == null && now - it.end < 18 * 3_600_000L }?.let { e ->
            AppCard {
                Text(t("How did you sleep?"), style = Type.title)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QUALITY.forEach { (q, emoji) ->
                        Chip("$emoji ${qualityLabel(q)}", false, { store.rateSleep(e.id, q) }, Modifier.weight(1f), hPadding = 4.dp)
                    }
                }
            }
        }

        if (manual) ManualSleep(store) { manual = false }

        val days = Days.lastDays(today, 7)
        val minutes = days.map { store.sleepMinutes(it) }
        val logged = minutes.filter { it > 0 }
        SectionLabel(t("Last 7 nights"))
        AppCard {
            BarChart(
                days.mapIndexed { i, d -> Bar(Days.weekdayShort(d).take(2), minutes[i] / 60f, d == today) },
                goal / 60f, Palette.waterDeep, Palette.leaf,
            )
            Row(Modifier.padding(top = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(t("Average"), style = Type.small)
                    Text(if (logged.isEmpty()) "—" else sleepDuration(logged.average().toInt()), style = Type.title)
                }
                Column(Modifier.weight(1f)) {
                    Text(t("Goal met"), style = Type.small)
                    Text(t("{0} / {1} nights", minutes.count { it >= goal }, 7), style = Type.title)
                }
            }
        }

        SectionLabel(t("Sleep goal"))
        AppCard {
            Stepper(
                sleepDuration(goal),
                onMinus = { store.updatePrefs { it.copy(sleepGoalMin = (it.sleepGoalMin - 30).coerceAtLeast(300)) } },
                onPlus = { store.updatePrefs { it.copy(sleepGoalMin = (it.sleepGoalMin + 30).coerceAtMost(720)) } },
            )
            Text(t("Most adults need 7–9 hours of sleep. Good sleep helps control hunger and weight."), style = Type.small, modifier = Modifier.padding(top = 8.dp))
        }

        val recent = log.entries.sortedByDescending { it.end }.take(10)
        if (recent.isNotEmpty()) {
            SectionLabel(t("Recent nights"))
            recent.forEach { e -> SleepRow(e, dayTime.format(Date(e.end)), "${time.format(Date(e.start))} → ${time.format(Date(e.end))}") { store.removeSleep(e.id) } }
        }
    }
}

@Composable
private fun SleepRow(e: SleepEntry, day: String, times: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(Palette.card).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (e.quality > 0) QUALITY[e.quality - 1].second else "🌙", fontSize = 22.sp, modifier = Modifier.width(34.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(sleepDuration(Sleep.minutes(e)), style = Type.title)
            Text("$day · $times", style = Type.small)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = t("Remove"), tint = Palette.muted) }
    }
}

/** Bedtime and wake-up for last night (waking up today), with a rating. */
@Composable
private fun ManualSleep(store: AppStore, onDone: () -> Unit) {
    var bedH by remember { mutableIntStateOf(22) }
    var bedM by remember { mutableIntStateOf(30) }
    var wakeH by remember { mutableIntStateOf(6) }
    var wakeM by remember { mutableIntStateOf(30) }
    var quality by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val (start, end) = Sleep.night(today, bedH, bedM, wakeH, wakeM)

    AppCard {
        Text(t("Log last night"), style = Type.title)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeBox(t("Bedtime"), clockTime(bedH, bedM), Modifier.weight(1f)) { picking = "bed" }
            TimeBox(t("Woke up"), clockTime(wakeH, wakeM), Modifier.weight(1f)) { picking = "wake" }
        }
        Text(sleepDuration(((end - start) / 60_000).toInt()), style = Type.h2, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUALITY.forEach { (q, emoji) ->
                Chip("$emoji ${qualityLabel(q)}", quality == q, { quality = q }, Modifier.weight(1f), hPadding = 4.dp)
            }
        }
        error?.let { Text(it, style = Type.small.copy(color = Palette.danger), modifier = Modifier.padding(top = 8.dp)) }
        PillButton(
            t("Save"),
            {
                if (store.addSleep(start, end, quality)) onDone()
                else error = t("Sleep should be between 10 minutes and 16 hours.")
            },
            Modifier.fillMaxWidth().padding(top = 12.dp), height = 46.dp,
        )
    }
    when (picking) {
        "bed" -> TimeDialog(bedH, bedM, onDismiss = { picking = null }) { h, m -> bedH = h; bedM = m; picking = null }
        "wake" -> TimeDialog(wakeH, wakeM, onDismiss = { picking = null }) { h, m -> wakeH = h; wakeM = m; picking = null }
    }
}

@Composable
private fun TimeBox(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Palette.chip).clickable(onClick = onClick).padding(12.dp)) {
        Text(label, style = Type.small)
        Text(value, style = Type.title)
    }
}

/** Home: last night's sleep against the goal, or the live timer. */
@Composable
fun SleepCard(store: AppStore, onOpen: () -> Unit) {
    val log = store.state.sleep
    val last = store.lastSleep
    val goal = store.prefs.sleepGoalMin
    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (log.activeStart != null) "🌙" else "😴", fontSize = 28.sp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(t("Sleep"), style = Type.small)
                Text(
                    when {
                        log.activeStart != null -> t("Sleeping…")
                        last != null -> sleepDuration(Sleep.minutes(last))
                        else -> t("Log your sleep")
                    },
                    style = Type.h2,
                )
            }
            Box(Modifier.clip(RoundedCornerShape(50)).background(Palette.chip).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(t("Goal {0}", sleepDuration(goal)), style = Type.tiny)
            }
        }
    }
}
