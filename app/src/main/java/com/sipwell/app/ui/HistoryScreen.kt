package com.sipwell.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sipwell.app.domain.Days
import com.sipwell.app.store.AppStore

@Composable
fun HistoryScreen(store: AppStore) {
    val p = store.profile
    var range by remember { mutableIntStateOf(7) }
    val days = Days.lastDays(today, range)
    val water = days.map { store.waterTotal(it) }
    val kcal = days.map { store.totals(it).kcal }
    val loggedWater = water.filter { it > 0 }
    val loggedKcal = kcal.filter { it > 0 }
    val label = { d: String -> if (range == 7) Days.weekdayShort(d).take(2) else Days.parse(d).dayOfMonth.let { if (it % 5 == 0) "$it" else "" } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding) {
        item {
            ScreenHeader("HISTORY")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                Chip("7 days", range == 7, { range = 7 })
                Chip("30 days", range == 30, { range = 30 })
            }
        }
        item {
            AppCard {
                Text("💧 Water", style = Type.title)
                Spacer(Modifier.height(12.dp))
                BarChart(days.mapIndexed { i, d -> Bar(label(d), water[i].toFloat()) }, p.waterGoalMl.toFloat(), Palette.aqua, Palette.waterDeep)
                Row(Modifier.padding(top = 12.dp)) {
                    Summary("Daily average", liters(if (loggedWater.isEmpty()) 0 else loggedWater.average().toInt()), Modifier.weight(1f))
                    Summary("Goal met", "${water.count { it >= p.waterGoalMl }} / $range days", Modifier.weight(1f))
                    Summary("Streak", "${store.streak()} days", Modifier.weight(1f))
                }
            }
        }
        item {
            AppCard {
                Text("🍛 Calories", style = Type.title)
                Spacer(Modifier.height(12.dp))
                BarChart(days.mapIndexed { i, d -> Bar(label(d), kcal[i].toFloat()) }, p.calorieGoal.toFloat(), Palette.leaf, Palette.saffron)
                Row(Modifier.padding(top = 12.dp)) {
                    Summary("Daily average", "${if (loggedKcal.isEmpty()) 0 else loggedKcal.average().toInt()} kcal", Modifier.weight(1f))
                    Summary("Within goal", "${kcal.count { it in 1..p.calorieGoal }} / $range days", Modifier.weight(1f))
                    Summary("Days logged", "${loggedKcal.size}", Modifier.weight(1f))
                }
            }
        }
        item {
            Text("Averages only count days where something was logged.", style = Type.small)
        }
    }
}

@Composable
private fun Summary(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = Type.small)
        Text(value, style = Type.title)
    }
}
