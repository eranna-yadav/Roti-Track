package com.rotitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.Billing
import com.rotitrack.app.account.DemoBilling
import com.rotitrack.app.account.Plan
import com.rotitrack.app.account.yearlySavingPercent

private val FEATURES = listOf(
    Triple("Water tracking & reminders", true, true),
    Triple("Calorie & macro tracking", true, true),
    Triple("142 Indian foods", true, true),
    Triple("Today's meal plan", true, true),
    Triple("Full 7-day meal plan", false, true),
    Triple("Swap any meal", false, true),
    Triple("30-day trends", false, true),
    Triple("Your own recipes", false, true),
)

@Composable
fun ProScreen(billing: Billing, compPro: Boolean, platform: Platform, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(Plan.YEARLY) }
    val active = billing.activePlan

    Column(Modifier.fillMaxSize().background(Palette.ink)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✕", fontSize = 22.sp, color = Color.White, modifier = Modifier.clickable(onClick = onBack).padding(14.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("👑", fontSize = 48.sp)
            Text("Roti Track Pro", style = Type.screenTitle.copy(color = Color.White))
            Text(
                "Plan every meal of the week and see the bigger picture.",
                style = Type.body.copy(color = Color.White.copy(alpha = 0.75f)), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.08f)).padding(16.dp)) {
                Row {
                    Spacer(Modifier.weight(1f))
                    Text("FREE", style = Type.tiny.copy(color = Color.White.copy(alpha = 0.6f)), modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    Text("PRO", style = Type.tiny.copy(color = Palette.saffron), modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                }
                FEATURES.forEach { (name, free, pro) ->
                    Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = Type.body.copy(color = Color.White), modifier = Modifier.weight(1f))
                        Text(if (free) "✓" else "—", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                        Text(if (pro) "✓" else "—", color = Palette.saffron, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            when {
                active != null -> Status(
                    "You're Pro 🎉",
                    "${active.label} plan · ${billing.price(active)}/${active.period}. Renews automatically until you cancel.",
                )
                compPro -> Status("You're Pro 🎉", "Pro access has been granted to your account by the Roti Track team.")
                else -> {
                    PlanCard(
                        Plan.YEARLY, billing.price(Plan.YEARLY), selected == Plan.YEARLY,
                        note = "Just ₹${Plan.YEARLY.rupees / 12}/month · save $yearlySavingPercent%",
                        badge = "BEST VALUE",
                    ) { selected = Plan.YEARLY }
                    Spacer(Modifier.height(10.dp))
                    PlanCard(Plan.MONTHLY, billing.price(Plan.MONTHLY), selected == Plan.MONTHLY, note = "Billed every month") { selected = Plan.MONTHLY }
                }
            }
            billing.unavailableReason?.let {
                Text(it, style = Type.small.copy(color = Palette.carbs), modifier = Modifier.padding(top = 12.dp))
            }
            if (billing is DemoBilling) {
                Text(
                    "Demo mode: Google Play Billing isn't connected, so no money is charged.",
                    style = Type.small.copy(color = Palette.carbs), modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                "Subscriptions renew automatically through Google Play until cancelled. Cancel any time in " +
                    "Play Store → Payments & subscriptions, at least 24 hours before renewal.",
                style = Type.small.copy(color = Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            when {
                active != null && billing is DemoBilling -> PillButton("Cancel subscription (demo)", { billing.cancel() }, Modifier.fillMaxWidth(), color = Color.White, textColor = Palette.ink)
                active != null -> PillButton(
                    "Manage subscription",
                    { platform.openUrl("https://play.google.com/store/account/subscriptions?sku=${active.productId}&package=com.rotitrack.app") },
                    Modifier.fillMaxWidth(), color = Color.White, textColor = Palette.ink,
                )
                compPro -> PillButton("Done", onBack, Modifier.fillMaxWidth(), color = Color.White, textColor = Palette.ink)
                else -> PillButton(
                    "Subscribe · ${billing.price(selected)}/${selected.period}", { billing.purchase(selected) },
                    Modifier.fillMaxWidth(), color = Palette.saffron, enabled = billing.unavailableReason == null, height = 58.dp,
                )
            }
            if (active == null && !compPro) {
                Text(
                    "Restore purchases", style = Type.small.copy(color = Color.White.copy(alpha = 0.7f)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { billing.restore() }.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, price: String, selected: Boolean, note: String, badge: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.08f))
            .border(2.dp, if (selected) Palette.saffron else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(plan.label, style = Type.title.copy(color = if (selected) Palette.ink else Color.White))
                badge?.let { Badge(it, Palette.leaf) }
            }
            Text(note, style = Type.small.copy(color = if (selected) Palette.inkSoft else Color.White.copy(alpha = 0.6f)))
        }
        Text(
            "$price\n/${plan.period}", style = Type.title.copy(color = if (selected) Palette.ink else Color.White),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun Status(title: String, body: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.leaf).padding(18.dp)) {
        Column {
            Text(title, style = Type.h2.copy(color = Color.White))
            Text(body, style = Type.body.copy(color = Color.White.copy(alpha = 0.9f)), modifier = Modifier.padding(top = 4.dp))
        }
    }
}
