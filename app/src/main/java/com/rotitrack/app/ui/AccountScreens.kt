package com.rotitrack.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

    SubScreen("Personal details", onBack) {
        OutlinedTextField(
            name, { name = it.take(40); store.updateProfile { pr -> pr.copy(name = name.trim()) } },
            label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )

        FormLabel("Weight")
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    weightText, { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("kg") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.padding(6.dp))
                PillButton("Log weight", {
                    weightText.toDoubleOrNull()?.takeIf { it in 25.0..250.0 }?.let { store.logWeight((it * 10).roundToInt() / 10.0) }
                }, height = 48.dp)
            }
            if (weights.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val recent = weights.takeLast(10)
                val first = weights.first().kg
                Text(
                    "Since ${Days.label(weights.first().day)}: ${signed(p.weightKg - first)} kg",
                    style = Type.small.copy(color = Palette.inkSoft),
                )
                Spacer(Modifier.height(8.dp))
                BarChart(
                    recent.map { Bar(Days.parse(it.day).let { d -> "${d.dayOfMonth}/${d.monthValue}" }, it.kg.toFloat(), it.day == Days.today()) },
                    goal = 0f, color = Palette.brand, height = 100.dp,
                )
            } else {
                Text("Log your weight regularly to see your progress here and in your PDF report.", style = Type.small, modifier = Modifier.padding(top = 8.dp))
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
    val suggestedWater = Nutrition.recommendedWaterMl(p.weightKg, p.gender)
    val macros = store.macroTargets()

    SubScreen("Nutrition goals", onBack) {
        FormLabel("Goal")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WeightGoal.entries.forEach { g ->
                Chip("${g.emoji} ${g.label}", p.goal == g, { store.updateProfile { it.copy(goal = g) } }, Modifier.weight(1f), hPadding = 4.dp)
            }
        }

        FormLabel("Daily calories")
        AppCard {
            Text("${p.calorieGoal} kcal", style = Type.screenTitle)
            Text("Protein ${macros.protein} g · Carbs ${macros.carbs} g · Fat ${macros.fat} g", style = Type.small)
            Spacer(Modifier.height(12.dp))
            ToggleLine("Set my own target", p.calorieGoalCustom) { on ->
                store.updateProfile { it.copy(calorieGoalCustom = on, calorieGoal = if (on) it.calorieGoal else suggested) }
            }
            if (p.calorieGoalCustom) {
                Stepper(
                    "${p.calorieGoal} kcal",
                    onMinus = { store.updateProfile { it.copy(calorieGoal = (it.calorieGoal - 50).coerceAtLeast(1000)) } },
                    onPlus = { store.updateProfile { it.copy(calorieGoal = (it.calorieGoal + 50).coerceAtMost(4500)) } },
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text("Suggested for you: $suggested kcal", style = Type.small, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text("Worked out from your body, activity and goal (Mifflin–St Jeor).", style = Type.small)
            }
        }

        FormLabel("Water")
        AppCard {
            Text(liters(p.waterGoalMl) + " a day", style = Type.h2)
            ToggleLine("Set my own water goal", p.waterGoalCustom) { on ->
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
            Text("Usual glass", style = Type.small)
            Stepper(
                "${p.cupMl} ml",
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
    SubScreen("Preferences", onBack) {
        SettingsGroup {
            SettingsRow(
                "🎨", "Appearance", subtitle = "Choose light, dark, or system appearance", last = true,
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
                ToggleLine("Badge Celebrations", prefs.badgeCelebrations, "Show celebrations when you unlock new badges") { on ->
                    store.updatePrefs { it.copy(badgeCelebrations = on) }
                }
                Divider()
                ToggleLine("Add Burned Calories", prefs.addBurnedCalories, "Add calories burned in exercise to your daily goal") { on ->
                    store.updatePrefs { it.copy(addBurnedCalories = on) }
                }
                Divider()
                ToggleLine("Rollover calories", prefs.rolloverCalories, "Add up to 200 left-over calories from yesterday to today's goal") { on ->
                    store.updatePrefs { it.copy(rolloverCalories = on) }
                }
            }
        }
    }
}

@Composable
fun LanguageScreen(store: AppStore, onBack: () -> Unit) {
    SubScreen("Language", onBack) {
        Text(
            "Roti Track is in English today. Indian languages are being translated. Pick yours and we'll switch you over as soon as it's ready.",
            style = Type.body.copy(color = Palette.inkSoft), modifier = Modifier.padding(bottom = 12.dp),
        )
        SettingsGroup {
            LANGUAGES.forEachIndexed { i, (code, name) ->
                SettingsRow(
                    if (code == "en") "🇬🇧" else "🇮🇳", name,
                    subtitle = if (code == "en") null else "Coming soon",
                    last = i == LANGUAGES.lastIndex,
                    onClick = { store.updatePrefs { it.copy(language = code) } },
                    trailing = { Radio(store.prefs.language == code) },
                )
            }
        }
    }
}
