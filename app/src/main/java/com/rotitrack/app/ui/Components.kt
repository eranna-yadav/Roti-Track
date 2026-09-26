package com.rotitrack.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.Diet
import com.rotitrack.app.data.Food
import kotlin.math.abs
import kotlin.math.min

object Type {
    val screenTitle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black, color = Palette.ink, letterSpacing = 0.5.sp)
    val h2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
    val title = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Palette.ink)
    val small = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.muted)
    val tiny = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Palette.muted, letterSpacing = 0.6.sp)
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = Palette.card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = shape,
        color = color,
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun ScreenHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Type.screenTitle, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun RoundButton(
    onClick: () -> Unit,
    size: Dp = 44.dp,
    color: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.brand,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, hPadding: Dp = 16.dp) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) Palette.brand else Palette.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = hPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.small.copy(color = if (selected) Color.White else Palette.inkSoft), maxLines = 1, softWrap = false)
    }
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { o -> Chip(label(o), o == selected, { onSelect(o) }) }
    }
}

@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundButton(onMinus, size = 38.dp, color = Palette.chip) { Text("−", style = Type.h2.copy(color = Palette.brand)) }
        Text(value, style = Type.title, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        RoundButton(onPlus, size = 38.dp, color = Palette.chip) { Text("+", style = Type.h2.copy(color = Palette.brand)) }
    }
}

/** Green dot for veg, orange for egg, red for non-veg — as printed on Indian food packs. */
@Composable
fun DietMark(diet: Diet, size: Dp = 13.dp) {
    val color = when (diet) {
        Diet.VEG -> Palette.leaf
        Diet.EGG -> Palette.saffron
        Diet.NONVEG -> Palette.nonveg
    }
    Box(
        Modifier.size(size).border(1.5.dp, color, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size * 0.45f).clip(CircleShape).background(color))
    }
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.divider))

@Composable
fun FoodRow(food: Food, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(food.emoji, fontSize = 26.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DietMark(food.diet, 12.dp)
                Spacer(Modifier.width(6.dp))
                Text(food.name, style = Type.title.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${food.serving} · P ${fmt(food.protein)} · C ${fmt(food.carbs)} · F ${fmt(food.fat)}",
                style = Type.small.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) trailing() else Text("${food.kcal} kcal", style = Type.title.copy(fontSize = 15.sp))
    }
}

fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

/** Ring of kcal eaten against the target; turns saffron when over. */
@Composable
fun CalorieRing(eaten: Int, goal: Int, size: Dp = 160.dp) {
    val over = eaten > goal
    val pct = if (goal > 0) min(1f, eaten.toFloat() / goal) else 0f
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(Palette.track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                if (over) Palette.saffron else Palette.leaf, -90f, 360f * pct, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${abs(goal - eaten)}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
            Text(if (over) "kcal over" else "kcal left", style = Type.small)
        }
    }
}

@Composable
fun RowScope.MacroBar(label: String, value: Double, target: Int, color: Color) {
    val pct = if (target > 0) min(1f, (value / target).toFloat()) else 0f
    Column(Modifier.weight(1f)) {
        Text(label, style = Type.small.copy(color = Palette.inkSoft))
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.track)) {
            Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
        Text(
            "${value.toInt()} / $target g",
            style = Type.small.copy(color = Palette.ink, fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

data class Bar(val label: String, val value: Float, val highlighted: Boolean = false)

/** Simple bar chart with a dashed goal line. Bars over the goal use [overColor]. */
@Composable
fun BarChart(
    bars: List<Bar>,
    goal: Float,
    color: Color,
    overColor: Color = color,
    height: Dp = 150.dp,
    onBarClick: ((Int) -> Unit)? = null,
) {
    val max = maxOf(goal * 1.2f, bars.maxOfOrNull { it.value } ?: 0f, 1f)
    Column {
        Box(Modifier.fillMaxWidth().height(height)) {
            Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
                val y = this.size.height * (1 - goal / max)
                drawLine(
                    Palette.muted, Offset(0f, y), Offset(this.size.width, y), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                )
            }
            Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.Bottom) {
                bars.forEachIndexed { i, b ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .then(if (onBarClick != null) Modifier.clickable { onBarClick(i) } else Modifier),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        val frac = (b.value / max).coerceIn(0.02f, 1f)
                        Box(
                            Modifier.fillMaxWidth(0.55f).fillMaxHeight(frac)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    (if (b.value > goal) overColor else color)
                                        .copy(alpha = if (b.value == 0f) 0.2f else if (b.highlighted) 1f else 0.7f)
                                ),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            bars.forEach { b ->
                Text(
                    b.label,
                    style = Type.tiny.copy(color = if (b.highlighted) Palette.ink else Palette.muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
    }
}
