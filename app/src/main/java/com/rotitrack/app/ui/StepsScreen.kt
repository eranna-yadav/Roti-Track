package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.Steps
import com.rotitrack.app.store.AppStore
import kotlinx.coroutines.delay
import java.util.Locale

private fun count(n: Int): String = "%,d".format(Locale.ENGLISH, n)

private fun km(value: Double): String = t("{0} km", "%.1f".format(Locale.ENGLISH, value))

/** Pro: today's steps, the last 7 or 30 days, and the step settings. Opened from the Home card. */
@Composable
fun StepsScreen(store: AppStore, platform: Platform, onBack: () -> Unit) {
    val p = store.profile
    val goal = store.prefs.stepGoal
    val steps = store.stepsFor(today)
    var range by remember { mutableIntStateOf(7) }

    SubScreen(t("Steps"), onBack) {
        AppCard(color = Palette.leaf) {
            val soft = Color.White.copy(alpha = 0.75f)
            Text("👟 " + t("Today"), style = Type.small.copy(color = soft))
            Text(count(steps), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(t("of {0} steps", count(goal)), style = Type.small.copy(color = soft))
            Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth((steps.toFloat() / goal).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Color.White))
            }
            Row(Modifier.padding(top = 14.dp)) {
                HeroStat(t("≈ {0} kcal", store.stepKcal(today)), t("Burned"), Modifier.weight(1f))
                HeroStat(km(Steps.km(steps, p.heightCm)), t("Distance"), Modifier.weight(1f))
                HeroStat(if (steps >= goal) "✅" else count(goal - steps), if (steps >= goal) t("Goal met") else t("Steps to go"), Modifier.weight(1f))
            }
        }

        val days = Days.lastDays(today, range)
        val counts = days.map { store.stepsFor(it) }
        val walked = counts.filter { it > 0 }
        Row(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
            Text(t("History"), style = Type.h2, modifier = Modifier.weight(1f))
            Chip(t("7 days"), range == 7, { range = 7 })
            Spacer(Modifier.width(8.dp))
            Chip(t("30 days"), range == 30, { range = 30 })
        }
        AppCard {
            val label = { d: String ->
                if (range == 7) Days.weekdayShort(d).take(2) else Days.parse(d).dayOfMonth.let { if (it % 5 == 0) "$it" else "" }
            }
            BarChart(days.mapIndexed { i, d -> Bar(label(d), counts[i].toFloat(), d == today) }, goal.toFloat(), Palette.aqua, Palette.leaf)
            Row(Modifier.padding(top = 12.dp)) {
                Stat(t("Daily average"), if (walked.isEmpty()) "—" else count(walked.average().toInt()), Modifier.weight(1f))
                Stat(t("Goal met"), t("{0} / {1} days", counts.count { it >= goal }, range), Modifier.weight(1f))
            }
            Row(Modifier.padding(top = 10.dp)) {
                Stat(t("Best day"), counts.maxOrNull()?.takeIf { it > 0 }?.let { count(it) } ?: "—", Modifier.weight(1f))
                Stat(t("Total distance"), km(Steps.km(counts.sum(), p.heightCm)), Modifier.weight(1f))
            }
        }

        SectionLabel(t("Daily step goal"))
        AppCard {
            Stepper(
                count(goal),
                onMinus = { store.updatePrefs { it.copy(stepGoal = (it.stepGoal - 500).coerceAtLeast(1000)) } },
                onPlus = { store.updatePrefs { it.copy(stepGoal = (it.stepGoal + 500).coerceAtMost(30000)) } },
            )
            Text(t("8,000–10,000 steps a day is a good target for weight loss. Start lower and add 500 each week."), style = Type.small, modifier = Modifier.padding(top = 8.dp))
        }

        WatchCard(platform)

        SectionLabel(t("How it works"))
        AppCard {
            Text(
                t("Your phone counts steps in the background, even when Roti Track is closed. Keep the phone in your pocket or hand while you walk. Steps add calories burned on the Food tab, like exercise."),
                style = Type.small,
            )
            Spacer(Modifier.height(8.dp))
            ToggleLine(t("Count my steps"), true) { on ->
                store.updatePrefs { it.copy(stepsOn = on) }
                if (!on) onBack()
            }
        }
    }
}

/** Steps walked without the phone, from a watch or band through Health Connect. */
@Composable
private fun WatchCard(platform: Platform) {
    var state by remember { mutableStateOf<WatchSteps?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var check by remember { mutableIntStateOf(0) }
    // Checked again every few seconds, so it updates after installing Health Connect or allowing access.
    LaunchedEffect(check) {
        while (true) {
            state = platform.watchSteps()
            delay(3_000)
        }
    }
    val s = state ?: return
    if (s == WatchSteps.UNSUPPORTED) return

    SectionLabel(t("Watch or fitness band"))
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⌚", fontSize = 28.sp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(if (s == WatchSteps.ON) t("Connected ✓") else t("Count steps from your watch"), style = Type.title)
                Text(
                    if (s == WatchSteps.ON) t("Steps from your watch or band are added. Steps both devices counted are counted once.")
                    else t("Walking without your phone? Connect your watch or band through Health Connect and those steps count too."),
                    style = Type.small,
                )
            }
        }
        problem?.let { Text(it, style = Type.small.copy(color = Palette.danger), modifier = Modifier.padding(top = 8.dp)) }
        when (s) {
            WatchSteps.ON -> Text(
                t("Disconnect"),
                style = Type.body.copy(color = Palette.danger, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 10.dp).clickable {
                    platform.disconnectWatch()
                    check++
                },
            )
            else -> PillButton(
                if (s == WatchSteps.INSTALL) t("Install Health Connect") else t("Connect"),
                {
                    problem = null
                    platform.connectWatch { ok ->
                        if (!ok && s == WatchSteps.OFF) problem = t("Allow \"Steps\" in Health Connect to connect.")
                        check++
                    }
                },
                Modifier.fillMaxWidth().padding(top = 10.dp), height = 44.dp,
            )
        }
        Text(
            t("Your watch app needs to share steps with Health Connect. Fitbit, Samsung Health, Mi Fitness, Google Fit and most others can."),
            style = Type.tiny, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = Type.title.copy(color = Color.White), maxLines = 1)
        Text(label, style = Type.small.copy(color = Color.White.copy(alpha = 0.75f)), maxLines = 1)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = Type.small)
        Text(value, style = Type.title)
    }
}
