package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.data.burnedKcal
import com.rotitrack.app.data.ExerciseType
import com.rotitrack.app.data.ACTIVITIES
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.formatServings
import com.rotitrack.app.store.AppStore
import kotlin.math.roundToInt

@Composable
fun FoodScreen(store: AppStore, onAdd: (MealSlot, String) -> Unit) {
    val p = store.profile
    val todayKey = today
    var day by remember { mutableStateOf(todayKey) }
    val totals = store.totals(day)
    val macros = store.macroTargets()
    val meals = store.mealsForDay(day)
    val week = Days.lastDays(todayKey, 7)
    val goal = store.calorieGoalFor(day)
    val burned = store.burned(day)
    val exercises = store.exercisesForDay(day)
    var exerciseOpen by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding) {
        item {
            ScreenHeader(t("CALORIES"))
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹", fontSize = 32.sp, color = Palette.brand,
                    modifier = Modifier.clickable { day = Days.shift(day, -1) }.padding(horizontal = 16.dp),
                )
                Text(Days.label(day, todayKey), style = Type.title, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(
                    "›", fontSize = 32.sp, color = if (day < todayKey) Palette.brand else Palette.track,
                    modifier = Modifier.clickable(enabled = day < todayKey) { day = Days.shift(day, 1) }.padding(horizontal = 16.dp),
                )
            }
        }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalorieRing(totals.kcal, goal)
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat(t("Goal"), t("{0} kcal", goal), Palette.brand)
                        Stat(t("Eaten"), t("{0} kcal", totals.kcal), Palette.leaf)
                        Stat(t("Burned"), t("{0} kcal", burned), Palette.saffron)
                        Stat(t("Water"), "${liters(store.waterTotal(day))} / ${liters(p.waterGoalMl)}", Palette.aqua)
                    }
                }
                val extras = listOfNotNull(
                    burned.takeIf { store.prefs.addBurnedCalories && it > 0 }?.let { t("+{0} burned", it) },
                    store.rollover(day).takeIf { store.prefs.rolloverCalories && it > 0 }?.let { t("+{0} rolled over", it) },
                )
                if (extras.isNotEmpty()) {
                    Text(t("Goal includes {0}", extras.joinToString(" · ")), style = Type.small, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MacroBar(t("Protein"), totals.protein, macros.protein, Palette.protein)
                    MacroBar(t("Carbs"), totals.carbs, macros.carbs, Palette.carbs)
                    MacroBar(t("Fat"), totals.fat, macros.fat, Palette.fat)
                }
            }
        }
        item(key = "exercise") {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏃", fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("Exercise"), style = Type.title)
                        Text(if (burned > 0) t("{0} kcal burned", burned) else t("Log a walk, yoga, gym or sport"), style = Type.small)
                    }
                    RoundButton({ exerciseOpen = true }, size = 40.dp, color = Palette.saffron) {
                        Icon(Icons.Filled.Add, contentDescription = t("Add exercise"), tint = Color.White)
                    }
                }
                exercises.forEach { e ->
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(e.emoji, fontSize = 22.sp, modifier = Modifier.width(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t(e.name), style = Type.body)
                            Text(t("{0} min", e.minutes), style = Type.small)
                        }
                        Text("−${e.kcal}", style = Type.title.copy(color = Palette.saffron))
                        IconButton(onClick = { store.removeExercise(e.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = t("Remove"), tint = Palette.muted)
                        }
                    }
                }
            }
        }
        MealSlot.entries.forEach { slot ->
            item(key = slot.name) {
                val items = meals.filter { it.slot == slot }
                val kcal = items.sumOf { it.kcal }
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(slot.emoji, fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(slot.label, style = Type.title)
                            Text(
                                if (kcal > 0) t("{0} kcal", kcal) else t("Suggested {0} kcal", (p.calorieGoal * slot.share).roundToInt()),
                                style = Type.small,
                            )
                        }
                        RoundButton({ onAdd(slot, day) }, size = 40.dp, color = Palette.brand) {
                            Icon(Icons.Filled.Add, contentDescription = t("Add to {0}", slot.label), tint = Color.White)
                        }
                    }
                    items.forEach { m ->
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(m.emoji, fontSize = 22.sp, modifier = Modifier.width(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t(m.name), style = Type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatServings(m.servings)} × ${t(m.serving)}", style = Type.small, maxLines = 1)
                            }
                            Text("${m.kcal}", style = Type.title)
                            IconButton(onClick = { store.removeMeal(m.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = t("Remove"), tint = Palette.muted)
                            }
                        }
                    }
                }
            }
        }
        item {
            AppCard {
                Text(t("Last 7 days"), style = Type.title)
                Spacer(Modifier.height(12.dp))
                BarChart(
                    bars = week.map { Bar(Days.weekdayShort(it).take(2), store.totals(it).kcal.toFloat(), it == day) },
                    goal = p.calorieGoal.toFloat(),
                    color = Palette.leaf,
                    overColor = Palette.saffron,
                    onBarClick = { day = week[it] },
                )
                Text(t("Dashed line is your {0} kcal goal.", p.calorieGoal), style = Type.small, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    if (exerciseOpen) {
        ExerciseSheet(p.weightKg, onDismiss = { exerciseOpen = false }) { activity, minutes ->
            store.addExercise(activity, minutes, day)
            exerciseOpen = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSheet(weightKg: Double, onDismiss: () -> Unit, onLog: (ExerciseType, Int) -> Unit) {
    var activity by remember { mutableStateOf(ACTIVITIES.first()) }
    var minutes by remember { mutableStateOf(30) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.card,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(t("Log exercise"), style = Type.h2)
            Spacer(Modifier.height(12.dp))
            ACTIVITIES.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    row.forEach { a ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (a == activity) Palette.brand else Palette.chip)
                                .clickable { activity = a }.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(a.emoji, fontSize = 22.sp)
                            Text(
                                a.name, style = Type.tiny.copy(color = if (a == activity) Color.White else Palette.inkSoft),
                                maxLines = 1, textAlign = TextAlign.Center,
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Stepper(
                t("{0} min", minutes),
                onMinus = { minutes = (minutes - 5).coerceAtLeast(5) },
                onPlus = { minutes = (minutes + 5).coerceAtMost(300) },
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Text(
                t("≈ {0} kcal burned", burnedKcal(activity, weightKg, minutes)),
                style = Type.title.copy(color = Palette.saffron), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            PillButton(t("Add {0}", activity.name), { onLog(activity, minutes) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, style = Type.small)
            Text(value, style = Type.title.copy(fontSize = 15.sp))
        }
    }
}
