package com.rotitrack.app.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.Diet
import com.rotitrack.app.data.Food
import com.rotitrack.app.data.FoodCategory
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.domain.formatServings
import com.rotitrack.app.store.AppStore
import kotlin.math.roundToInt

private sealed interface Filter {
    data object Recent : Filter
    data class Category(val c: FoodCategory) : Filter
}

@Composable
fun AddFoodScreen(store: AppStore, slot: MealSlot, day: String, isPro: Boolean, onUpgrade: () -> Unit, onBack: () -> Unit) {
    val pref = store.profile.diet
    val recent = store.recentFoods()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<Filter>(if (recent.isNotEmpty()) Filter.Recent else Filter.Category(FoodCategory.BREAKFAST)) }
    var showAll by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Food?>(null) }
    var added by remember { mutableIntStateOf(0) }
    var customOpen by remember { mutableStateOf(false) }

    val dietOk = { f: Food -> showAll || f.diet.ordinal <= pref.ordinal }
    val q = query.trim().lowercase()
    val results = when {
        q.isNotEmpty() -> store.allFoods.filter { dietOk(it) && it.name.lowercase().contains(q) }
        filter == Filter.Recent -> recent.filter(dietOk)
        (filter as Filter.Category).c == FoodCategory.CUSTOM -> store.state.customFoods
        else -> store.allFoods.filter { it.category == (filter as Filter.Category).c && dietOk(it) }
    }
    val filters = buildList {
        if (recent.isNotEmpty()) add(Filter.Recent)
        FoodCategory.entries.forEach { add(Filter.Category(it)) }
    }

    Column(Modifier.fillMaxSize().background(Palette.card).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 34.sp, color = Palette.ink, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 12.dp))
            Text("Add to ${slot.label}", style = Type.h2, modifier = Modifier.weight(1f))
            if (added > 0) {
                TextButton(onClick = onBack) { Text("Done ($added)", style = Type.title.copy(color = Palette.brand)) }
            }
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search dal, roti, idli, paneer…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Palette.chip,
                unfocusedContainerColor = Palette.chip,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        if (q.isEmpty()) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                ChipRow(filters, filter, label = {
                    when (it) {
                        Filter.Recent -> "Recent"
                        is Filter.Category -> it.c.label
                    }
                }, onSelect = { filter = it })
            }
        }

        if (pref != Diet.NONVEG) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
                Text(
                    if (showAll) "Showing all foods" else "Showing ${if (pref == Diet.VEG) "vegetarian" else "veg + egg"} foods",
                    style = Type.small, modifier = Modifier.weight(1f),
                )
                Text(
                    if (showAll) "Filter" else "Show all",
                    style = Type.small.copy(color = Palette.brand),
                    modifier = Modifier.clickable { showAll = !showAll },
                )
            }
        }

        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
            if (results.isEmpty()) {
                item {
                    Text(
                        if (filter == Filter.Category(FoodCategory.CUSTOM) && q.isEmpty()) "No custom foods yet. Add your own recipes below."
                        else "No match. Try another spelling, or add it as a custom food.",
                        style = Type.body.copy(color = Palette.muted),
                        modifier = Modifier.padding(top = 40.dp),
                    )
                }
            }
            items(results, key = { it.id }) { food ->
                FoodRow(
                    food,
                    onClick = { picked = food },
                    trailing = if (food.category == FoodCategory.CUSTOM && q.isEmpty() && filter != Filter.Recent) {
                        {
                            IconButton(onClick = { store.removeCustomFood(food.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Palette.muted)
                            }
                        }
                    } else null,
                )
                Divider()
            }
        }

        PillButton(
            if (isPro) "＋ Create custom food" else "🔒 Create custom food · Pro", { if (isPro) customOpen = true else onUpgrade() },
            Modifier.fillMaxWidth().padding(16.dp), color = Palette.night,
        )
    }

    picked?.let { food ->
        LogFoodSheet(food, slot, onDismiss = { picked = null }) { servings, s ->
            store.logFood(food, servings, s, day)
            added++
            picked = null
        }
    }

    if (customOpen) {
        CustomFoodDialog(
            initialName = query,
            onDismiss = { customOpen = false },
            onSave = { name, serving, kcal, p, c, f, diet ->
                val food = store.addCustomFood(name, serving, kcal, p, c, f, diet)
                customOpen = false
                picked = food
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodSheet(food: Food, initialSlot: MealSlot, onDismiss: () -> Unit, onLog: (Double, MealSlot) -> Unit) {
    var servings by remember(food.id) { mutableStateOf(1.0) }
    var slot by remember(food.id) { mutableStateOf(initialSlot) }
    val step = if (food.piece) 1.0 else 0.5

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.card,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(food.emoji, fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(food.name, style = Type.h2)
                    Text("${food.serving} per serving", style = Type.small)
                }
            }
            Stepper(
                "${formatServings(servings)} serving${if (servings == 1.0) "" else "s"}",
                onMinus = { servings = maxOf(step, servings - step) },
                onPlus = { servings = minOf(20.0, servings + step) },
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.chip).padding(vertical = 14.dp),
            ) {
                Nutri("Calories", "${(food.kcal * servings).roundToInt()}", Palette.leaf)
                Nutri("Protein", "${fmt(food.protein * servings)} g", Palette.protein)
                Nutri("Carbs", "${fmt(food.carbs * servings)} g", Palette.carbs)
                Nutri("Fat", "${fmt(food.fat * servings)} g", Palette.fat)
            }
            Row(Modifier.padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealSlot.entries.forEach { s ->
                    Chip(s.short, s == slot, { slot = s }, Modifier.weight(1f), hPadding = 4.dp)
                }
            }
            PillButton("Add to ${slot.short}", { onLog(servings, slot) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Nutri(label: String, value: String, color: Color) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(value, style = Type.title, modifier = Modifier.padding(top = 6.dp))
        Text(label.uppercase(), style = Type.tiny)
    }
}

@Composable
fun CustomFoodDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Double, Double, Double, Diet) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var serving by remember { mutableStateOf("1 bowl") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var nonveg by remember { mutableStateOf(false) }

    fun num(s: String) = s.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0
    // If only macros are filled in, derive calories (4 / 4 / 9 kcal per gram).
    val derived = (num(protein) * 4 + num(carbs) * 4 + num(fat) * 9).roundToInt()
    val calories = if (kcal.isNotBlank()) num(kcal).roundToInt() else derived
    val valid = name.isNotBlank() && calories > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(serving, { serving = it }, label = { Text("Serving size") }, singleLine = true)
                NumberField(kcal, { kcal = it }, if (derived > 0) "Calories ($derived from macros)" else "Calories (kcal)")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField(protein, { protein = it }, "Protein g", Modifier.weight(1f))
                    NumberField(carbs, { carbs = it }, "Carbs g", Modifier.weight(1f))
                    NumberField(fat, { fat = it }, "Fat g", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("🟢 Veg", !nonveg, { nonveg = false }, Modifier.weight(1f))
                    Chip("🔴 Non-veg", nonveg, { nonveg = true }, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        name.trim(), serving.trim().ifBlank { "1 serving" }, calories,
                        num(protein), num(carbs), num(fat), if (nonveg) Diet.NONVEG else Diet.VEG,
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value, { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
