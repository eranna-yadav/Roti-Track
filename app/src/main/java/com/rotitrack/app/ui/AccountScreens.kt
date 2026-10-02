package com.rotitrack.app.ui

import com.rotitrack.app.i18n.I18n
import com.rotitrack.app.i18n.t
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rotitrack.app.data.Appearance
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.WeightGoal
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.Nutrition
import com.rotitrack.app.store.AppStore
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.WeightEntry
import kotlin.math.roundToInt

@Composable
fun PersonalDetailsScreen(store: AppStore, platform: Platform, onBack: () -> Unit) {
    val p = store.profile
    var name by remember { mutableStateOf(p.name) }
    var weightText by remember { mutableStateOf(fmt(p.weightKg)) }
    val weights = store.state.weights

    fun change(next: Profile) {
        store.updateProfile { next }
        platform.scheduleReminders(store.profile)
    }

    SubScreen(t("Personal details"), onBack) {
        OutlinedTextField(
            name, { name = it.take(40); store.updateProfile { pr -> pr.copy(name = name.trim()) } },
            label = { Text(t("Your name")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )

        FormLabel(t("Weight"))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    weightText, { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("kg") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.width(12.dp))
                PillButton(t("Log weight"), {
                    weightText.toDoubleOrNull()?.takeIf { it in 25.0..250.0 }?.let { store.logWeight((it * 10).roundToInt() / 10.0) }
                }, Modifier.width(132.dp), height = 52.dp)
            }
            if (weights.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                val first = weights.first()
                Text(
                    if (weights.size == 1) t("Logged {0} kg on {1}. Log again on another day to see your progress.", fmt(first.kg), Days.short(first.day))
                    else t("Change since {0}: {1} kg", Days.short(first.day), signed(p.weightKg - first.kg)),
                    style = Type.small.copy(color = Palette.inkSoft),
                )
                Spacer(Modifier.height(10.dp))
                WeightChart(weights.takeLast(7))
            } else {
                Text(t("Log your weight regularly to see your progress here and in your PDF report."), style = Type.small, modifier = Modifier.padding(top = 8.dp))
            }
        }

        ProfileForm(p, ::change, showWeight = false)
    }
}

private fun signed(v: Double): String = (if (v > 0) "+" else "") + fmt((v * 10).roundToInt() / 10.0)

@Composable
fun NutritionGoalsScreen(store: AppStore, onBack: () -> Unit) {
    val p = store.profile
    val suggested = Nutrition.recommendedCalories(p)
    val suggestedWater = Nutrition.recommendedWaterMl(p.weightKg, p.gender, p.activity)
    val macros = store.macroTargets()

    SubScreen(t("Nutrition goals"), onBack) {
        FormLabel(t("Goal"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WeightGoal.entries.forEach { g ->
                Chip("${g.emoji} ${g.label}", p.goal == g, { store.updateProfile { it.copy(goal = g) } }, Modifier.weight(1f), hPadding = 4.dp)
            }
        }

        FormLabel(t("Daily calories"))
        AppCard {
            Text(t("{0} kcal", p.calorieGoal), style = Type.screenTitle)
            Text(t("Protein {0} g · Carbs {1} g · Fat {2} g", macros.protein, macros.carbs, macros.fat), style = Type.small)
            Spacer(Modifier.height(12.dp))
            ToggleLine(t("Set my own target"), p.calorieGoalCustom) { on ->
                store.updateProfile { it.copy(calorieGoalCustom = on, calorieGoal = if (on) it.calorieGoal else suggested) }
            }
            if (p.calorieGoalCustom) {
                Stepper(
                    t("{0} kcal", p.calorieGoal),
                    onMinus = { store.updateProfile { it.copy(calorieGoal = (it.calorieGoal - 50).coerceAtLeast(1000)) } },
                    onPlus = { store.updateProfile { it.copy(calorieGoal = (it.calorieGoal + 50).coerceAtMost(4500)) } },
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(t("Suggested for you: {0} kcal", suggested), style = Type.small, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(t("Worked out from your body, activity and goal (Mifflin–St Jeor)."), style = Type.small)
            }
        }

        FormLabel(t("Water"))
        AppCard {
            Text(t("{0} a day", liters(p.waterGoalMl)), style = Type.h2)
            ToggleLine(t("Set my own water goal"), p.waterGoalCustom) { on ->
                store.updateProfile { it.copy(waterGoalCustom = on, waterGoalMl = if (on) it.waterGoalMl else suggestedWater) }
            }
            if (p.waterGoalCustom) {
                Stepper(
                    liters(p.waterGoalMl),
                    onMinus = { store.updateProfile { it.copy(waterGoalMl = (it.waterGoalMl - 100).coerceAtLeast(500)) } },
                    onPlus = { store.updateProfile { it.copy(waterGoalMl = (it.waterGoalMl + 100).coerceAtMost(5000)) } },
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(t("Usual glass"), style = Type.small)
            Stepper(
                t("{0} ml", p.cupMl),
                onMinus = { store.updateProfile { it.copy(cupMl = (it.cupMl - 50).coerceAtLeast(100)) } },
                onPlus = { store.updateProfile { it.copy(cupMl = (it.cupMl + 50).coerceAtMost(1000)) } },
            )
        }
    }
}

@Composable
fun PreferencesScreen(store: AppStore, onBack: () -> Unit) {
    val prefs = store.prefs
    var menu by remember { mutableStateOf(false) }
    SubScreen(t("Preferences"), onBack) {
        SettingsGroup {
            SettingsRow(
                "🎨", t("Appearance"), subtitle = t("Choose light, dark, or system appearance"), last = true,
                onClick = { menu = true },
                trailing = {
                    Column {
                        Text("${prefs.appearance.label} ⌄", style = Type.title.copy(color = Palette.inkSoft))
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            Appearance.entries.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text(a.label) },
                                    onClick = { store.updatePrefs { it.copy(appearance = a) }; menu = false },
                                )
                            }
                        }
                    }
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            Column(Modifier.padding(vertical = 8.dp)) {
                ToggleLine(t("Badge Celebrations"), prefs.badgeCelebrations, t("Show celebrations when you unlock new badges")) { on ->
                    store.updatePrefs { it.copy(badgeCelebrations = on) }
                }
                Divider()
                ToggleLine(t("Add Burned Calories"), prefs.addBurnedCalories, t("Add calories burned in exercise to your daily goal")) { on ->
                    store.updatePrefs { it.copy(addBurnedCalories = on) }
                }
                Divider()
                ToggleLine(t("Rollover calories"), prefs.rolloverCalories, t("Add up to 200 left-over calories from yesterday to today's goal")) { on ->
                    store.updatePrefs { it.copy(rolloverCalories = on) }
                }
            }
        }
    }
}

@Composable
fun LanguageScreen(store: AppStore, onBack: () -> Unit) {
    SubScreen(t("Language"), onBack) {
        Text(
            t("Choose the language for Roti Track. Food names and screens change straight away."),
            style = Type.body.copy(color = Palette.inkSoft), modifier = Modifier.padding(bottom = 12.dp),
        )
        SettingsGroup {
            LANGUAGES.forEachIndexed { i, (code, name) ->
                SettingsRow(
                    if (code == "en") "🇬🇧" else "🇮🇳", name,
                    last = i == LANGUAGES.lastIndex,
                    onClick = {
                        store.updatePrefs { it.copy(language = code) }
                        I18n.lang = code
                    },
                    trailing = { Radio(I18n.lang == code) },
                )
            }
        }
        Text(
            t("Health articles and the Terms and Privacy Policy are in English."),
            style = Type.small, modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** Weight over the last few logged days: a line with each value above its point and the date below. */
@Composable
fun WeightChart(entries: List<WeightEntry>, height: Dp = 130.dp) {
    val measurer = rememberTextMeasurer()
    val line = Palette.brand
    val dot = Palette.card
    val base = Palette.divider
    val valueStyle = Type.tiny.copy(color = Palette.ink, letterSpacing = 0.sp)
    val lo = entries.minOf { it.kg }
    val hi = entries.maxOf { it.kg }
    val mid = (lo + hi) / 2
    val span = maxOf(hi - lo, 2.0) * 0.75
    Column {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val slot = size.width / entries.size
            val top = 26.dp.toPx()
            val bottom = size.height - 10.dp.toPx()
            fun point(i: Int) = Offset(
                slot * (i + 0.5f),
                bottom - ((entries[i].kg - (mid - span)) / (2 * span)).toFloat() * (bottom - top),
            )
            drawLine(base, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
            if (entries.size > 1) {
                val path = Path()
                entries.indices.forEach { i -> point(i).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
                drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            entries.indices.forEach { i ->
                val c = point(i)
                drawCircle(line, 6.dp.toPx(), c)
                drawCircle(dot, 3.dp.toPx(), c)
                val label = measurer.measure(fmt(entries[i].kg) + if (entries.size <= 4) t(" kg") else "", valueStyle)
                drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - 10.dp.toPx() - label.size.height))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            entries.forEach { e ->
                Text(
                    Days.short(e.day),
                    style = Type.tiny.copy(color = if (e.day == Days.today()) Palette.ink else Palette.muted, letterSpacing = 0.sp),
                    textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
