package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.data.Recipe
import com.rotitrack.app.data.RecipeCategory
import com.rotitrack.app.data.Recipes
import com.rotitrack.app.store.AppStore

/** Pro: the weight loss recipe book. Everyone can browse the names; opening a recipe needs Pro. */
@Composable
fun RecipesScreen(isPro: Boolean, onOpen: (String) -> Unit, onUpgrade: () -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<RecipeCategory?>(null) }
    val list = Recipes.search(query, category)

    Column(Modifier.fillMaxSize().background(Palette.background).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Palette.chip).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, color = Palette.ink) }
            Spacer(Modifier.width(12.dp))
            Text(t("Weight loss recipes"), style = Type.h2, modifier = Modifier.weight(1f))
        }
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(t("Search paneer, dal, chicken…")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = t("Clear")) } },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Palette.chip, unfocusedContainerColor = Palette.chip,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(t("All"), category == null, { category = null })
            RecipeCategory.entries.filter { c -> Recipes.all.any { it.category == c } }.forEach { c ->
                Chip("${c.emoji} ${c.label}", category == c, { category = c })
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isPro) item {
                AppCard(color = Palette.saffronSoft, onClick = onUpgrade) {
                    Text(t("👑 {0} weight loss recipes with Pro", Recipes.all.size), style = Type.title)
                    Text(t("Indian recipes rebuilt for weight loss, with calories, protein and step-by-step instructions. Log any recipe in one tap."), style = Type.small, modifier = Modifier.padding(top = 4.dp))
                    PillButton(t("Go Pro"), onUpgrade, Modifier.fillMaxWidth().padding(top = 12.dp), height = 44.dp)
                }
            }
            item { Text(t("{0} recipes", list.size), style = Type.small) }
            items(list, key = { it.id }) { r -> RecipeRow(r, locked = !isPro) { if (isPro) onOpen(r.id) else onUpgrade() } }
            if (list.isEmpty()) item {
                Text(t("No recipes match. Try another word."), style = Type.body.copy(color = Palette.muted), modifier = Modifier.padding(top = 24.dp))
            }
        }
    }
}

@Composable
private fun RecipeRow(r: Recipe, locked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.card).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Palette.chip), contentAlignment = Alignment.Center) {
            Text(r.emoji, fontSize = 30.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(r.title, style = Type.title, maxLines = 2)
            Text(t("{0} kcal · {1} g protein · {2} min", r.kcal, r.protein, r.minutes), style = Type.small, modifier = Modifier.padding(top = 2.dp))
        }
        Text(if (locked) "🔒" else "›", fontSize = if (locked) 18.sp else 24.sp, color = Palette.muted)
    }
}

/** One recipe: what it is, ingredients, steps, why it helps, and logging it as a meal. */
@Composable
fun RecipeScreen(store: AppStore, id: String, onBack: () -> Unit) {
    val r = Recipes.byId(id) ?: return onBack()
    var slot by remember { mutableStateOf<MealSlot?>(null) }
    var logged by remember { mutableStateOf<String?>(null) }

    SubScreen(r.category.label, onBack) {
        AppCard {
            Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp)).background(Palette.saffronSoft), contentAlignment = Alignment.Center) {
                Text(r.emoji, fontSize = 64.sp)
            }
            Text(r.title, style = Type.h2, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Fact("🔥", t("{0} kcal", r.kcal), Modifier.weight(1f))
                Fact("💪", t("{0} g protein", r.protein), Modifier.weight(1f))
                Fact("⏱", t("{0} min", r.minutes), Modifier.weight(1f))
            }
            Text(r.intro, style = Type.body, modifier = Modifier.padding(top = 14.dp))
            Text(t("Per serving. Values are approximate."), style = Type.tiny, modifier = Modifier.padding(top = 8.dp))
        }

        SectionLabel(t("Ingredients"))
        AppCard {
            r.ingredients.forEach { line ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text("•", style = Type.body.copy(color = Palette.saffron, fontWeight = FontWeight.ExtraBold), modifier = Modifier.width(18.dp))
                    Text(line, style = Type.body)
                }
            }
        }

        SectionLabel(t("Method"))
        AppCard {
            r.steps.forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(Palette.brand), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = Type.tiny.copy(color = Color.White, fontWeight = FontWeight.ExtraBold))
                    }
                    Text(step, style = Type.body, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                }
            }
        }

        SectionLabel(t("Why it helps you lose weight"))
        AppCard(color = Palette.leafSoft) {
            r.benefits.forEach { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("✓", style = Type.body.copy(color = Palette.leaf, fontWeight = FontWeight.ExtraBold), modifier = Modifier.width(22.dp))
                    Text(line, style = Type.body)
                }
            }
        }

        SectionLabel(t("Ate this?"))
        AppCard {
            Text(t("Log one serving to your food diary for today."), style = Type.small)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealSlot.entries.forEach { s -> Chip("${s.emoji} ${s.short}", slot == s, { slot = s; logged = null }, Modifier.weight(1f), hPadding = 4.dp) }
            }
            PillButton(
                t("Log this meal"),
                {
                    val s = slot ?: return@PillButton
                    store.logFood(r.toFood(), 1.0, s, today)
                    logged = t("Added to {0}: {1} kcal", s.label, r.kcal)
                    slot = null
                },
                Modifier.fillMaxWidth().padding(top = 12.dp), height = 46.dp, enabled = slot != null,
            )
            logged?.let { Text("✅ $it", style = Type.body.copy(color = Palette.leaf, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 10.dp)) }
        }
    }
}

@Composable
private fun Fact(emoji: String, text: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Palette.chip).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(text, style = Type.small.copy(color = Palette.ink, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}
