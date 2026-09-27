package com.rotitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.Activity
import com.rotitrack.app.data.Diet
import com.rotitrack.app.data.Gender
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.Region
import com.rotitrack.app.data.WeightGoal
import kotlin.math.roundToInt

@Composable
fun FormLabel(text: String) = Text(text, style = Type.title.copy(color = Palette.inkSoft), modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))

@Composable
private fun Tile(emoji: String, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.height(84.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) Palette.brand else Palette.card)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 24.sp)
        Text(label, style = Type.small.copy(color = if (selected) Color.White else Palette.inkSoft), textAlign = TextAlign.Center)
    }
}

/** Body data and diet preferences. Used by onboarding and the Me tab. */
@Composable
fun ProfileForm(p: Profile, onChange: (Profile) -> Unit, showWeight: Boolean = true) {
    FormLabel("Gender")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Triple(Gender.FEMALE, "👩", "Female"), Triple(Gender.MALE, "👨", "Male"), Triple(Gender.OTHER, "🧑", "Other"))
            .forEach { (g, e, l) -> Tile(e, l, p.gender == g, { onChange(p.copy(gender = g)) }, Modifier.weight(1f)) }
    }

    FormLabel("Body")
    AppCard {
        if (showWeight) {
            Text("Weight", style = Type.small)
            Stepper(
                "${fmt(p.weightKg)} kg",
                onMinus = { onChange(p.copy(weightKg = ((p.weightKg - 0.5).coerceAtLeast(25.0) * 2).roundToInt() / 2.0)) },
                onPlus = { onChange(p.copy(weightKg = ((p.weightKg + 0.5).coerceAtMost(250.0) * 2).roundToInt() / 2.0)) },
            )
            Spacer(Modifier.height(10.dp))
        }
        Text("Height", style = Type.small)
        Stepper(
            "${p.heightCm} cm",
            onMinus = { onChange(p.copy(heightCm = (p.heightCm - 1).coerceAtLeast(120))) },
            onPlus = { onChange(p.copy(heightCm = (p.heightCm + 1).coerceAtMost(220))) },
        )
        Spacer(Modifier.height(10.dp))
        Text("Age", style = Type.small)
        Stepper(
            "${p.age} years",
            onMinus = { onChange(p.copy(age = (p.age - 1).coerceAtLeast(12))) },
            onPlus = { onChange(p.copy(age = (p.age + 1).coerceAtMost(100))) },
        )
    }

    FormLabel("Goal")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WeightGoal.entries.forEach { g -> Tile(g.emoji, g.label, p.goal == g, { onChange(p.copy(goal = g)) }, Modifier.weight(1f)) }
    }

    FormLabel("Activity")
    AppCard {
        Activity.entries.forEachIndexed { i, a ->
            if (i > 0) Divider()
            Row(
                Modifier.fillMaxWidth().clickable { onChange(p.copy(activity = a)) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = Type.title)
                    Text(a.hint, style = Type.small)
                }
                Radio(p.activity == a)
            }
        }
    }

    FormLabel("Food preference")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Triple(Diet.VEG, "🥦", "Veg"), Triple(Diet.EGG, "🥚", "Egg"), Triple(Diet.NONVEG, "🍗", "Non-veg"))
            .forEach { (d, e, l) -> Tile(e, l, p.diet == d, { onChange(p.copy(diet = d)) }, Modifier.weight(1f)) }
    }

    FormLabel("Cuisine for meal plans")
    ChipRow(Region.entries.toList(), p.region, label = { it.label }, onSelect = { onChange(p.copy(region = it)) })
}

@Composable
fun Radio(selected: Boolean) {
    Box(
        Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) Palette.brand else Palette.track, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(Palette.brand))
    }
}
