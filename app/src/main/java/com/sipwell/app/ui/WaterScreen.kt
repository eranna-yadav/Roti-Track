package com.sipwell.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipwell.app.data.DRINKS
import com.sipwell.app.data.Drink
import com.sipwell.app.data.drinkById
import com.sipwell.app.store.AppStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WaterScreen(store: AppStore) {
    val p = store.profile
    val day = today
    val total = store.waterTotal(day)
    val entries = store.waterForDay(day).asReversed()
    val progress = if (p.waterGoalMl > 0) min(1f, total.toFloat() / p.waterGoalMl) else 0f
    var drink by remember { mutableStateOf(DRINKS.first()) }
    var customOpen by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding) {
        item {
            ScreenHeader("WATER") {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔥 ${store.streak()}-day streak", style = Type.small.copy(color = Palette.ink))
                }
            }
        }
        item {
            AppCard {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WaterGlass(progress, Modifier.size(210.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(liters(total), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                        Text("of ${liters(p.waterGoalMl)} · ${(progress * 100).toInt()}%", style = Type.small.copy(color = Palette.inkSoft))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (progress >= 1f) "🏆 Goal reached — nicely done!"
                    else "${liters(p.waterGoalMl - total)} to go · about ${((p.waterGoalMl - total) / p.cupMl.toFloat()).let { kotlin.math.ceil(it).toInt() }} more ${if (p.cupMl >= 400) "bottles" else "glasses"}",
                    style = Type.body.copy(color = Palette.inkSoft),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Text("What are you drinking?", style = Type.title, modifier = Modifier.padding(vertical = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DRINKS) { d -> DrinkChip(d, d == drink) { drink = d } }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(p.cupMl, 150, 500).distinct().forEach { ml ->
                    PillButton(
                        "+ $ml ml", { store.addWater(ml, drink.id) },
                        modifier = Modifier.weight(1f), height = 56.dp,
                    )
                }
                PillButton("Other", { customOpen = true }, Modifier.weight(0.8f), color = Color.White, textColor = Palette.brand, height = 56.dp)
            }
            if (drink.hydration < 1.0) {
                Text(
                    "${drink.name} counts as ${(drink.hydration * 100).toInt()}% hydration.",
                    style = Type.small, modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("Today's log", style = Type.title, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (entries.isEmpty()) {
            item { Text("Nothing yet. Start with a glass of water 💧", style = Type.body.copy(color = Palette.muted)) }
        }
        items(entries, key = { it.id }) { e ->
            val d = drinkById(e.drinkId)
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${e.ml} ml ${d.name}", style = Type.title)
                        Text(clock(e.ts), style = Type.small)
                    }
                    IconButton(onClick = { store.removeWater(e.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Palette.muted)
                    }
                }
            }
        }
    }

    if (customOpen) {
        AmountDialog(
            title = "Add ${drink.name}",
            onDismiss = { customOpen = false },
            onConfirm = { ml ->
                store.addWater(ml, drink.id)
                customOpen = false
            },
        )
    }
}

@Composable
private fun DrinkChip(d: Drink, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(74.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) Palette.brand else Color.White)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(d.emoji, fontSize = 24.sp)
        Text(
            d.name, style = Type.tiny.copy(color = if (selected) Color.White else Palette.inkSoft),
            maxLines = 1, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AmountDialog(title: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val ml = text.toIntOrNull()?.takeIf { it in 1..3000 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() }.take(4) },
                label = { Text("Amount (ml)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { ml?.let(onConfirm) }, enabled = ml != null) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A circle that fills with gently moving water as the day's intake grows. */
@Composable
fun WaterGlass(progress: Float, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(progress, tween(900), label = "level")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2600), RepeatMode.Restart), label = "phase",
    )
    Canvas(modifier.clip(CircleShape).background(Color(0xFFE9F0FF))) {
        val w = size.width
        val h = size.height
        val surface = h * (1 - (0.06f + 0.94f * level))
        val amp = h * 0.025f
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                lineTo(x, surface + amp * sin(phase + x / w * 2 * PI.toFloat()))
                x += 4f
            }
            lineTo(w, h)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Palette.waterTop, Palette.waterDeep), startY = surface, endY = h))
        drawCircle(Color.White.copy(alpha = 0.25f), radius = w * 0.06f, center = Offset(w * 0.3f, surface + h * 0.18f))
    }
}

fun liters(ml: Int): String = if (ml >= 1000 || ml == 0) "%.1f L".format(ml / 1000f) else "$ml ml"

fun clock(ts: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
