package com.sipwell.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipwell.app.data.DIET_TIPS
import com.sipwell.app.data.Diet
import com.sipwell.app.domain.Days
import com.sipwell.app.domain.formatServings
import com.sipwell.app.store.AppStore

private fun dietLabel(d: Diet) = when (d) {
    Diet.VEG -> "Vegetarian"
    Diet.EGG -> "Eggetarian"
    Diet.NONVEG -> "Non-vegetarian"
}

@Composable
fun PlanScreen(store: AppStore, isPro: Boolean, onUpgrade: () -> Unit, onOpenArticles: () -> Unit, onEditProfile: () -> Unit) {
    val p = store.profile
    val todayKey = today
    var day by remember { mutableStateOf(todayKey) }
    val days = (0L until 7L).map { Days.shift(todayKey, it) }
    val plan = store.plan(day)
    val tip = DIET_TIPS[Days.parse(day).dayOfYear % DIET_TIPS.size]

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding) {
        item {
            ScreenHeader("DIET PLAN") {
                RoundButton(onEditProfile) { Text("⚙️", fontSize = 18.sp) }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                items(days) { d ->
                    val active = d == day
                    val locked = !isPro && d != todayKey
                    Column(
                        Modifier.width(58.dp).height(66.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (active) Palette.brand else Color.White)
                            .clickable { if (locked) onUpgrade() else day = d },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (d == todayKey) "Today" else if (locked) "🔒" else Days.weekdayShort(d),
                            style = Type.small.copy(color = if (active) Color.White else Palette.muted),
                        )
                        Text("${Days.parse(d).dayOfMonth}", style = Type.h2.copy(color = if (active) Color.White else Palette.ink))
                    }
                }
            }
        }
        item {
            AppCard(color = Palette.saffronSoft) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Planned for the day", style = Type.small.copy(color = Palette.inkSoft))
                        Text("${plan.sumOf { it.kcal }} / ${p.calorieGoal} kcal", style = Type.h2)
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tag(dietLabel(p.diet))
                            Tag(p.region.label + if (p.region.name == "ALL") "" else " Indian")
                        }
                    }
                    Text("🍱", fontSize = 44.sp)
                }
            }
        }
        items(plan, key = { it.slot.name }) { meal ->
            val logged = store.isPlanLogged(day, meal.slot)
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.slot.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${meal.slot.label.uppercase()} · ${meal.slot.time}", style = Type.tiny)
                        Text(meal.name, style = Type.title)
                    }
                    Text("${meal.kcal} kcal", style = Type.title)
                }
                Spacer(Modifier.height(8.dp))
                meal.items.forEach { i ->
                    Divider()
                    Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        DietMark(i.food.diet, 11.dp)
                        Text(
                            "  ${i.food.emoji} ${i.food.name}", style = Type.body,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${formatServings(i.servings)} × ${i.food.serving.removePrefix("1 ").replace(Regex("\\s*\\(.*\\)$"), "")}",
                            style = Type.small.copy(color = Palette.inkSoft), maxLines = 1,
                        )
                    }
                }
                Text("P ${meal.protein} g · C ${meal.carbs} g · F ${meal.fat} g", style = Type.small, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        if (isPro) "⇄  Swap" else "🔒 Swap", { if (isPro) store.swapMeal(day, meal.slot) else onUpgrade() }, Modifier.weight(1f),
                        color = Palette.chip, textColor = Palette.brand, enabled = !logged, height = 42.dp,
                    )
                    if (day == todayKey) {
                        PillButton(
                            if (logged) "✓ Logged" else "I ate this", { store.logPlannedMeal(day, meal) }, Modifier.weight(1f),
                            color = if (logged) Palette.leafSoft else Palette.leaf,
                            textColor = if (logged) Palette.leaf else Color.White,
                            enabled = !logged, height = 42.dp,
                        )
                    }
                }
            }
        }
        item {
            AppCard(color = Palette.leafSoft) {
                Text("💡 Tip of the day", style = Type.title.copy(color = Palette.leaf))
                Text(tip, style = Type.body.copy(color = Palette.inkSoft, lineHeight = 21.sp), modifier = Modifier.padding(top = 6.dp))
            }
            AppCard(onClick = onOpenArticles) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📚", fontSize = 26.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Health articles", style = Type.title)
                        Text("Indian nutrition, hydration and habits", style = Type.small)
                    }
                    Text("›", fontSize = 26.sp, color = Palette.muted)
                }
            }
            Text(
                "Plans are general guidance built from home-style Indian meals. If you have diabetes, thyroid, " +
                    "kidney or heart conditions, or are pregnant, check with a doctor or dietitian first.",
                style = Type.small.copy(lineHeight = 18.sp),
            )
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text.uppercase(), style = Type.tiny.copy(color = Palette.saffron))
    }
}
