package com.rotitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.Account
import com.rotitrack.app.account.Plan
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.Nutrition
import com.rotitrack.app.store.AppStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/** The user dashboard: today at a glance, what to eat next, and trends. */
@Composable
fun DashboardScreen(
    store: AppStore,
    account: Account,
    isPro: Boolean,
    plan: Plan?,
    onUpgrade: () -> Unit,
    onOpen: (Tab) -> Unit,
) {
    val p = store.profile
    val day = today
    val totals = store.totals(day)
    val water = store.waterTotal(day)
    val macros = store.macroTargets()
    val hour = LocalTime.now().hour
    val nextSlot = when {
        hour < 10 -> MealSlot.BREAKFAST
        hour < 15 -> MealSlot.LUNCH
        hour < 18 -> MealSlot.SNACK
        else -> MealSlot.DINNER
    }
    val next = store.plan(day).first { it.slot == nextSlot }
    val firstName = (p.name.ifBlank { account.name }).substringBefore(' ').ifBlank { "there" }
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Palette.brand), contentAlignment = Alignment.Center) {
                    Text(initials(p.name.ifBlank { account.name }), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        "$greeting · " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
                        style = Type.small, maxLines = 1,
                    )
                    Text(firstName, style = Type.h2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isPro) {
                    Badge("PRO", Palette.saffron)
                } else {
                    PillButton("Go Pro", onUpgrade, color = Palette.saffron, height = 36.dp, modifier = Modifier.width(92.dp))
                }
            }
        }

        item {
            AppCard(onClick = { onOpen(Tab.FOOD) }) {
                Text("Today", style = Type.title)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalorieRing(totals.kcal, p.calorieGoal, size = 128.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Progress("Calories", "${totals.kcal} / ${p.calorieGoal}", totals.kcal.toFloat() / p.calorieGoal, Palette.leaf)
                        Progress("Water", "${liters(water)} / ${liters(p.waterGoalMl)}", water.toFloat() / p.waterGoalMl, Palette.aqua)
                        Progress("Protein", "${totals.protein.toInt()} / ${macros.protein} g", (totals.protein / macros.protein).toFloat(), Palette.protein)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("🔥", "${store.streak()}", "day\nstreak", Modifier.weight(1f))
                val bmi = Nutrition.bmi(p.weightKg, p.heightCm)
                StatTile("⚖️", "%.1f".format(bmi), "BMI\n${Nutrition.bmiLabel(bmi)}", Modifier.weight(1f))
                StatTile("🎯", fmt(p.weightKg), "kg now\n${p.goal.label}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            AppCard(color = Palette.saffronSoft, onClick = { onOpen(Tab.PLAN) }) {
                Text("UP NEXT · ${next.slot.label.uppercase()} · ${next.slot.time}", style = Type.tiny)
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(next.slot.emoji, fontSize = 30.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(next.name, style = Type.title)
                        Text(next.items.joinToString(" · ") { it.food.name }, style = Type.small, maxLines = 2)
                    }
                    Text("${next.kcal} kcal", style = Type.title)
                }
            }
        }

        item { Trends(store, isPro, onUpgrade) }

        if (!isPro) {
            item {
                AppCard(color = Palette.ink, onClick = onUpgrade) {
                    Text("👑 Roti Track Pro", style = Type.h2.copy(color = Color.White))
                    Text(
                        "7-day meal plans, unlimited swaps, 30-day trends and your own recipes. " +
                            "From ${Plan.MONTHLY.fallbackPrice}/month or ${Plan.YEARLY.fallbackPrice}/year.",
                        style = Type.body.copy(color = Color.White.copy(alpha = 0.8f)), modifier = Modifier.padding(vertical = 8.dp),
                    )
                    PillButton("See plans", onUpgrade, color = Palette.saffron, height = 44.dp, modifier = Modifier.fillMaxWidth())
                }
            }
        } else if (plan != null) {
            item { Text("Pro · ${plan.label} plan", style = Type.small, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}

@Composable
private fun Trends(store: AppStore, isPro: Boolean, onUpgrade: () -> Unit) {
    val p = store.profile
    var range by remember { mutableIntStateOf(7) }
    val days = Days.lastDays(today, range)
    val water = days.map { store.waterTotal(it) }
    val kcal = days.map { store.totals(it).kcal }
    val loggedWater = water.filter { it > 0 }
    val loggedKcal = kcal.filter { it > 0 }
    val label = { d: String ->
        if (range == 7) Days.weekdayShort(d).take(2) else Days.parse(d).dayOfMonth.let { if (it % 5 == 0) "$it" else "" }
    }

    Row(Modifier.padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Trends", style = Type.h2, modifier = Modifier.weight(1f))
        Chip("7 days", range == 7, { range = 7 })
        Spacer(Modifier.width(8.dp))
        Chip(if (isPro) "30 days" else "30 days 🔒", range == 30, { if (isPro) range = 30 else onUpgrade() })
    }
    AppCard {
        Text("💧 Water", style = Type.title)
        Spacer(Modifier.height(12.dp))
        BarChart(days.mapIndexed { i, d -> Bar(label(d), water[i].toFloat(), d == today) }, p.waterGoalMl.toFloat(), Palette.aqua, Palette.waterDeep)
        Row(Modifier.padding(top = 12.dp)) {
            Summary("Daily average", liters(if (loggedWater.isEmpty()) 0 else loggedWater.average().toInt()), Modifier.weight(1f))
            Summary("Goal met", "${water.count { it >= p.waterGoalMl }} / $range days", Modifier.weight(1f))
        }
    }
    AppCard {
        Text("🍛 Calories", style = Type.title)
        Spacer(Modifier.height(12.dp))
        BarChart(days.mapIndexed { i, d -> Bar(label(d), kcal[i].toFloat(), d == today) }, p.calorieGoal.toFloat(), Palette.leaf, Palette.saffron)
        Row(Modifier.padding(top = 12.dp)) {
            Summary("Daily average", "${if (loggedKcal.isEmpty()) 0 else loggedKcal.average().toInt()} kcal", Modifier.weight(1f))
            Summary("Within goal", "${kcal.count { it in 1..p.calorieGoal }} / $range days", Modifier.weight(1f))
        }
    }
    Text("Averages only count days where something was logged.", style = Type.small, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun Progress(label: String, value: String, fraction: Float, color: Color) {
    Column {
        Row {
            Text(label, style = Type.small, modifier = Modifier.weight(1f))
            Text(value, style = Type.small.copy(color = Palette.ink))
        }
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.track)) {
            Box(Modifier.fillMaxWidth(min(1f, fraction.coerceAtLeast(0f))).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(12.dp)) {
        Text(emoji, fontSize = 20.sp)
        Text(value, style = Type.h2.copy(fontSize = 20.sp), modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        Text(label, style = Type.tiny, maxLines = 2)
    }
}

@Composable
private fun Summary(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = Type.small)
        Text(value, style = Type.title)
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, style = Type.tiny.copy(color = Color.White))
    }
}

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "🙂" }
