package com.rotitrack.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.FoodScanner
import com.rotitrack.app.account.ScannedItem
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.domain.formatServings
import com.rotitrack.app.i18n.t
import com.rotitrack.app.store.AppStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface ScanPhase {
    data object Pick : ScanPhase
    data object Reading : ScanPhase
    data class Found(val items: List<ScannedItem>, val note: String) : ScanPhase
    data class Failed(val message: String) : ScanPhase
}

/** Pro: photograph a plate, check what the scanner found, then add it to [slot] on [day]. */
@Composable
fun ScanScreen(
    store: AppStore,
    scanner: FoodScanner,
    platform: Platform,
    slot: MealSlot,
    day: String,
    onBack: () -> Unit,
) {
    var phase by remember { mutableStateOf<ScanPhase>(ScanPhase.Pick) }
    val scope = rememberCoroutineScope()

    fun start(camera: Boolean) {
        scanner.unavailableReason?.let { phase = ScanPhase.Failed(it); return }
        platform.pickFoodPhoto(camera) { jpeg ->
            if (jpeg == null) return@pickFoodPhoto
            phase = ScanPhase.Reading
            scope.launch {
                phase = runCatching { scanner.scan(jpeg, slot) }.fold(
                    onSuccess = { ScanPhase.Found(it.items, it.note) },
                    onFailure = { ScanPhase.Failed(it.message ?: t("Something went wrong. Try again.")) },
                )
            }
        }
    }

    SubScreen(t("Scan your {0}", slot.label.lowercase()), onBack) {
        when (val p = phase) {
            ScanPhase.Pick -> PickPhoto(onCamera = { start(true) }, onGallery = { start(false) })
            ScanPhase.Reading -> Column(
                Modifier.fillMaxWidth().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Palette.brand, modifier = Modifier.size(56.dp))
                Text(t("Reading your plate…"), style = Type.h2, modifier = Modifier.padding(top = 20.dp))
                Text(t("This takes a few seconds."), style = Type.small, modifier = Modifier.padding(top = 6.dp))
            }
            is ScanPhase.Found -> Results(p, store, slot, day, onDone = onBack, onRetake = { phase = ScanPhase.Pick })
            is ScanPhase.Failed -> {
                AppCard {
                    Text("⚠️", fontSize = 32.sp)
                    Text(p.message, style = Type.body, modifier = Modifier.padding(vertical = 8.dp))
                    PillButton(t("Try again"), { phase = ScanPhase.Pick }, Modifier.fillMaxWidth(), height = 46.dp)
                }
            }
        }
    }
}

@Composable
private fun PickPhoto(onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(TabIcons.Scan, null, tint = Palette.brand, modifier = Modifier.size(96.dp))
        Text(
            t("Take a clear photo of your plate from above, in good light."),
            style = Type.title, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp),
        )
        PillButton(t("📷 Take photo"), onCamera, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        PillButton(t("🖼️ Choose from gallery"), onGallery, Modifier.fillMaxWidth(), color = Palette.chip, textColor = Palette.ink)
        Text(
            t("Portions and calories are estimates. Check them before adding."),
            style = Type.small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun Results(found: ScanPhase.Found, store: AppStore, slot: MealSlot, day: String, onDone: () -> Unit, onRetake: () -> Unit) {
    val foods = remember(found) { found.items.mapIndexed { i, it -> it.toFood("scan-${System.currentTimeMillis()}-$i") } }
    val checked = remember(found) { mutableStateListOf(*Array(found.items.size) { true }) }
    val servings = remember(found) { mutableStateListOf(*found.items.map { it.servings }.toTypedArray()) }

    if (found.items.isEmpty()) {
        AppCard {
            Text("🍽️", fontSize = 32.sp)
            Text(
                found.note.ifBlank { t("No food found in this photo. Try again with the plate in good light.") },
                style = Type.body, modifier = Modifier.padding(vertical = 8.dp),
            )
            PillButton(t("Scan again"), onRetake, Modifier.fillMaxWidth(), height = 46.dp)
        }
        return
    }

    Text(t("We found:"), style = Type.h2, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
    found.items.forEachIndexed { i, item ->
        val food = foods[i]
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked[i], { checked[i] = it },
                    colors = CheckboxDefaults.colors(checkedColor = Palette.brand),
                )
                Text(food.emoji, fontSize = 24.sp, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f).clickable { checked[i] = !checked[i] }) {
                    Text(t(food.name), style = Type.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        t("{0} kcal", (food.kcal * servings[i]).roundToInt()) + " · " + t(food.serving),
                        style = Type.small, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (item.confidence == "low") {
                        Text(t("Not sure — please check"), style = Type.tiny.copy(color = Palette.saffron, fontWeight = FontWeight.Bold))
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Servings"), style = Type.small, modifier = Modifier.weight(1f))
                SmallStepper(
                    formatServings(servings[i]),
                    onMinus = { servings[i] = (servings[i] - 0.5).coerceAtLeast(0.5) },
                    onPlus = { servings[i] = (servings[i] + 0.5).coerceAtMost(20.0) },
                )
            }
        }
    }
    if (found.note.isNotBlank()) Text(found.note, style = Type.small, modifier = Modifier.padding(vertical = 8.dp))

    val count = checked.count { it }
    val total = found.items.indices.filter { checked[it] }.sumOf { (foods[it].kcal * servings[it]).roundToInt() }
    Spacer(Modifier.height(8.dp))
    PillButton(
        if (count == 1) t("Add 1 item to {0} · {1} kcal", slot.label, total) else t("Add {0} items to {1} · {2} kcal", count, slot.label, total),
        {
            found.items.indices.filter { checked[it] }.forEach { store.logFood(foods[it], servings[it], slot, day) }
            onDone()
        },
        Modifier.fillMaxWidth(), enabled = count > 0,
    )
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
        Text(
            t("Scan again"), style = Type.body.copy(color = Palette.brand, fontWeight = FontWeight.Bold),
            modifier = Modifier.clickable(onClick = onRetake).padding(12.dp),
        )
    }
}

@Composable
private fun SmallStepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundButton(onMinus, size = 34.dp, color = Palette.chip) { Text("−", style = Type.title.copy(color = Palette.brand)) }
        Text(value, style = Type.title, textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
        RoundButton(onPlus, size = 34.dp, color = Palette.chip) { Text("+", style = Type.title.copy(color = Palette.brand)) }
    }
}
