package com.sipwell.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipwell.app.data.Profile
import com.sipwell.app.domain.Nutrition
import com.sipwell.app.store.AppStore

@Composable
fun MeScreen(store: AppStore, platform: Platform, onOpenArticles: () -> Unit) {
    val p = store.profile
    var confirmReset by remember { mutableStateOf(false) }
    val suggestedKcal = Nutrition.recommendedCalories(p)
    val suggestedWater = Nutrition.recommendedWaterMl(p.weightKg, p.gender)
    val macros = store.macroTargets()
    val bmi = Nutrition.bmi(p.weightKg, p.heightCm)

    fun change(next: Profile) {
        store.updateProfile { next }
        if (next.remindersOn && (next.reminderEveryMin != p.reminderEveryMin || next.wakeHour != p.wakeHour || next.sleepHour != p.sleepHour)) {
            platform.scheduleReminders()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding)) {
        ScreenHeader("ME")

        AppCard(color = Palette.saffronSoft) {
            Text("Your daily targets", style = Type.small.copy(color = Palette.inkSoft))
            Row(Modifier.padding(top = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("${p.calorieGoal} kcal", style = Type.h2)
                    Text("P ${macros.protein} g · C ${macros.carbs} g · F ${macros.fat} g", style = Type.small)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(liters(p.waterGoalMl), style = Type.h2.copy(color = Palette.waterDeep))
                    Text("water", style = Type.small)
                }
            }
            Text(
                "BMI ${"%.1f".format(bmi)} · ${Nutrition.bmiLabel(bmi)} (Asian-Indian scale)",
                style = Type.small.copy(color = Palette.inkSoft), modifier = Modifier.padding(top = 10.dp),
            )
        }

        ProfileForm(p, ::change)

        FormLabel("Calorie target")
        AppCard {
            ToggleLine("Set my own target", p.calorieGoalCustom) {
                change(p.copy(calorieGoalCustom = it, calorieGoal = if (it) p.calorieGoal else suggestedKcal))
            }
            if (p.calorieGoalCustom) {
                Stepper(
                    "${p.calorieGoal} kcal",
                    onMinus = { change(p.copy(calorieGoal = (p.calorieGoal - 50).coerceAtLeast(1000))) },
                    onPlus = { change(p.copy(calorieGoal = (p.calorieGoal + 50).coerceAtMost(4500))) },
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text("Suggested for you: $suggestedKcal kcal", style = Type.small, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(
                    "Calculated from your body, activity and goal (Mifflin–St Jeor).",
                    style = Type.small, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        FormLabel("Water")
        AppCard {
            ToggleLine("Set my own water goal", p.waterGoalCustom) {
                change(p.copy(waterGoalCustom = it, waterGoalMl = if (it) p.waterGoalMl else suggestedWater))
            }
            if (p.waterGoalCustom) {
                Stepper(
                    liters(p.waterGoalMl),
                    onMinus = { change(p.copy(waterGoalMl = (p.waterGoalMl - 100).coerceAtLeast(500))) },
                    onPlus = { change(p.copy(waterGoalMl = (p.waterGoalMl + 100).coerceAtMost(5000))) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Usual glass", style = Type.small)
            Stepper(
                "${p.cupMl} ml",
                onMinus = { change(p.copy(cupMl = (p.cupMl - 50).coerceAtLeast(100))) },
                onPlus = { change(p.copy(cupMl = (p.cupMl + 50).coerceAtMost(1000))) },
            )
        }

        FormLabel("Water reminders")
        AppCard {
            ToggleLine("Remind me to drink", p.remindersOn) { on ->
                if (on) {
                    platform.requestNotifications { granted ->
                        store.updateProfile { it.copy(remindersOn = granted) }
                        platform.scheduleReminders()
                    }
                } else {
                    store.updateProfile { it.copy(remindersOn = false) }
                    platform.scheduleReminders()
                }
            }
            if (p.remindersOn) {
                Text("Every", style = Type.small, modifier = Modifier.padding(top = 10.dp))
                Stepper(
                    if (p.reminderEveryMin % 60 == 0) "${p.reminderEveryMin / 60} h" else "${p.reminderEveryMin / 60} h ${p.reminderEveryMin % 60} min",
                    onMinus = { change(p.copy(reminderEveryMin = (p.reminderEveryMin - 30).coerceAtLeast(30))) },
                    onPlus = { change(p.copy(reminderEveryMin = (p.reminderEveryMin + 30).coerceAtMost(240))) },
                )
                Text("From", style = Type.small, modifier = Modifier.padding(top = 10.dp))
                Stepper(
                    hour(p.wakeHour),
                    onMinus = { change(p.copy(wakeHour = (p.wakeHour - 1).coerceAtLeast(4))) },
                    onPlus = { change(p.copy(wakeHour = (p.wakeHour + 1).coerceAtMost(p.sleepHour - 2))) },
                )
                Text("Until", style = Type.small, modifier = Modifier.padding(top = 10.dp))
                Stepper(
                    hour(p.sleepHour),
                    onMinus = { change(p.copy(sleepHour = (p.sleepHour - 1).coerceAtLeast(p.wakeHour + 2))) },
                    onPlus = { change(p.copy(sleepHour = (p.sleepHour + 1).coerceAtMost(23))) },
                )
                Text(
                    "Reminders stop for the day once you reach your goal.",
                    style = Type.small, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        FormLabel("More")
        AppCard(onClick = onOpenArticles) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📚  Health articles", style = Type.title, modifier = Modifier.weight(1f))
                Text("›", fontSize = 24.sp, color = Palette.muted)
            }
        }
        AppCard(onClick = { confirmReset = true }) {
            Text("🗑️  Reset all data", style = Type.title.copy(color = Palette.danger))
        }
        Text(
            "Sipwell stores everything on this phone. No account, no server, no ads.\nVersion 1.0.0",
            style = Type.small, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all data?") },
            text = { Text("This deletes your profile, water log, meals and custom foods. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    store.resetAll()
                    platform.scheduleReminders()
                }) { Text("Reset", color = Palette.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.title, modifier = Modifier.weight(1f))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Palette.brand))
    }
}

fun hour(h: Int): String = when {
    h == 0 -> "12 AM"
    h < 12 -> "$h AM"
    h == 12 -> "12 PM"
    else -> "${h - 12} PM"
}
