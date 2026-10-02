package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
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
import com.rotitrack.app.account.normalizeReferralCode
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.Account
import com.rotitrack.app.account.Billing
import com.rotitrack.app.account.RazorpayGateway
import com.rotitrack.app.account.UserSummary
import com.rotitrack.app.account.DemoBilling
import com.rotitrack.app.account.Plan
import com.rotitrack.app.account.yearlySavingPercent
import kotlinx.coroutines.launch

private val FEATURES get() = listOf(
    Triple(t("Water tracking & reminders"), true, true),
    Triple(t("Calorie & macro tracking"), true, true),
    Triple(t("190+ Indian, Western & Chinese foods"), true, true),
    Triple(t("Today's meal plan"), true, true),
    Triple(t("Full 7-day meal plan"), false, true),
    Triple(t("Swap any meal"), false, true),
    Triple(t("30-day trends"), false, true),
    Triple(t("Your own recipes"), false, true),
)

@Composable
fun ProScreen(
    billing: Billing,
    razorpay: RazorpayGateway,
    account: Account,
    summary: UserSummary?,
    platform: Platform,
    /** Why a referral code can't be used, or null when it gives the first-year price. */
    checkReferralCode: suspend (String) -> String?,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf(Plan.YEARLY) }
    // Yearly opens a second page where the user may enter a friend's referral code.
    var codeStep by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val playPlan = billing.activePlan
    val rzpPlan = summary?.razorpayPlan
    val compPro = summary?.compPro == true

    // Installed from Play → Play's own flow (with its Razorpay choice screen when enrolled).
    // Otherwise Razorpay directly; with neither, the debug demo.
    val usePlay = billing !is DemoBilling && billing.unavailableReason == null
    val useRazorpay = !usePlay && razorpay.unavailableReason == null
    val demo = !usePlay && !useRazorpay && billing is DemoBilling

    if (codeStep && playPlan == null && rzpPlan == null && !compPro) {
        ReferralCodeStep(
            regularPrice = billing.price(Plan.YEARLY),
            referralPrice = if (usePlay) billing.referralPrice(Plan.YEARLY) else Plan.YEARLY.referralPrice,
            prefill = summary?.referredByCode.orEmpty(),
            busy = razorpay.busy,
            message = razorpay.message,
            enabled = usePlay || useRazorpay || demo,
            check = checkReferralCode,
            onPay = { code ->
                when {
                    usePlay || demo -> billing.purchase(Plan.YEARLY, referralCode = code)
                    useRazorpay -> scope.launch { runCatching { razorpay.subscribe(Plan.YEARLY, account, referralCode = code) } }
                }
            },
            onBack = { codeStep = false },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(Palette.night)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✕", fontSize = 22.sp, color = Color.White, modifier = Modifier.clickable(onClick = onBack).padding(14.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("👑", fontSize = 48.sp)
            Text(t("Roti Track Pro"), style = Type.screenTitle.copy(color = Color.White))
            Text(
                t("Plan every meal of the week and see the bigger picture."),
                style = Type.body.copy(color = Color.White.copy(alpha = 0.75f)), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.08f)).padding(16.dp)) {
                Row {
                    Spacer(Modifier.weight(1f))
                    Text(t("FREE"), style = Type.tiny.copy(color = Color.White.copy(alpha = 0.6f)), modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
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
                playPlan != null -> Status(
                    t("You're Pro 🎉"),
                    t("{0} plan via Google Play · {1}/{2}. Renews automatically until you cancel.", playPlan.label, billing.price(playPlan), playPlan.period),
                )
                rzpPlan != null -> Status(
                    t("You're Pro 🎉"),
                    if (summary?.razorpayStatus == "cancelled") {
                        t("Auto-renew is off. Pro stays on until {0}.", longDate(summary.razorpayUntil))
                    } else {
                        t("{0} plan via Razorpay · {1}/{2}. Next renewal {3}.", rzpPlan.label, rzpPlan.fallbackPrice, rzpPlan.period, longDate(summary!!.razorpayUntil))
                    },
                )
                compPro -> Status(t("You're Pro 🎉"), t("Pro access has been granted to your account by the Roti Track team."))
                else -> {
                    PlanCard(
                        Plan.YEARLY, billing.price(Plan.YEARLY), selected == Plan.YEARLY,
                        note = t("Just ₹{0}/month · save {1}%", Plan.YEARLY.rupees / 12, yearlySavingPercent),
                        badge = t("BEST VALUE"),
                    ) { selected = Plan.YEARLY; codeStep = true }
                    Spacer(Modifier.height(10.dp))
                    PlanCard(Plan.MONTHLY, billing.price(Plan.MONTHLY), selected == Plan.MONTHLY, note = t("Billed every month")) { selected = Plan.MONTHLY }
                    Text(
                        when {
                            usePlay && billing.offersAlternative -> t("Next, choose Google Play or Razorpay (UPI, cards, netbanking, wallets).")
                            usePlay -> t("Payment through Google Play.")
                            useRazorpay -> t("Pay securely with UPI, cards, netbanking or wallets via Razorpay.")
                            demo -> t("Demo mode: no payment service is connected, so no money is charged.")
                            else -> billing.unavailableReason ?: razorpay.unavailableReason.orEmpty()
                        },
                        style = Type.small.copy(color = if (demo) Palette.carbs else Color.White.copy(alpha = 0.75f)),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            razorpay.message?.let {
                Text(it, style = Type.small.copy(color = Palette.carbs), modifier = Modifier.padding(top = 12.dp))
            }
            Text(
                t("Subscriptions renew automatically until cancelled. Google Play subscriptions are managed in Play Store → Payments & subscriptions; Razorpay ones from this screen. Prices include GST."),
                style = Type.small.copy(color = Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            val light = Modifier.fillMaxWidth()
            when {
                playPlan != null && billing is DemoBilling -> PillButton(t("Cancel subscription (demo)"), { billing.cancel() }, light, color = Color.White, textColor = Palette.night)
                playPlan != null -> PillButton(
                    t("Manage subscription"),
                    { platform.openUrl("https://play.google.com/store/account/subscriptions?sku=${playPlan.productId}&package=com.rotitrack.app") },
                    light, color = Color.White, textColor = Palette.night,
                )
                rzpPlan != null && summary?.razorpayStatus != "cancelled" -> PillButton(
                    if (razorpay.busy) t("Please wait…") else t("Turn off auto-renew"),
                    { scope.launch { runCatching { razorpay.cancel() } } },
                    light, color = Color.White, textColor = Palette.night, enabled = !razorpay.busy,
                )
                rzpPlan != null || compPro -> PillButton(t("Done"), onBack, light, color = Color.White, textColor = Palette.night)
                else -> PillButton(
                    when {
                        razorpay.busy -> t("Please wait…")
                        selected == Plan.YEARLY -> t("Continue · {0}/{1}", billing.price(selected), selected.period)
                        else -> t("Subscribe · {0}/{1}", billing.price(selected), selected.period)
                    },
                    {
                        when {
                            selected == Plan.YEARLY -> codeStep = true
                            usePlay || demo -> billing.purchase(selected)
                            useRazorpay -> scope.launch { runCatching { razorpay.subscribe(selected, account) } }
                        }
                    },
                    light, color = Palette.saffron, enabled = (usePlay || useRazorpay || demo) && !razorpay.busy, height = 58.dp,
                )
            }
            if (playPlan == null && rzpPlan == null && !compPro && usePlay) {
                Text(
                    t("Restore purchases"), style = Type.small.copy(color = Color.White.copy(alpha = 0.7f)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { billing.restore() }.padding(top = 12.dp),
                )
            }
        }
    }
}

private fun longDate(t: Long): String = java.text.SimpleDateFormat("d MMM yyyy", com.rotitrack.app.i18n.I18n.locale).format(java.util.Date(t))

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
                Text(plan.label, style = Type.title.copy(color = if (selected) Palette.night else Color.White))
                badge?.let { Badge(it, Palette.leaf) }
            }
            Text(note, style = Type.small.copy(color = if (selected) Color(0xFF3A4762) else Color.White.copy(alpha = 0.6f)))
        }
        Text(
            "$price\n/${plan.period}", style = Type.title.copy(color = if (selected) Palette.night else Color.White),
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

/** The yearly plan's second page: an optional friend's referral code, then payment. */
@Composable
private fun ReferralCodeStep(
    regularPrice: String,
    referralPrice: String?,
    prefill: String,
    busy: Boolean,
    message: String?,
    enabled: Boolean,
    check: suspend (String) -> String?,
    onPay: (code: String?) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(prefill) }
    var applied by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val saving = Plan.YEARLY.referralRupees?.let { "₹${Plan.YEARLY.rupees - it}" }.orEmpty()
    val dim = Color.White.copy(alpha = 0.7f)

    Column(Modifier.fillMaxSize().background(Palette.night)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 26.sp, color = Color.White, modifier = Modifier.clickable(onClick = onBack).padding(14.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("👑", fontSize = 40.sp)
            Text(t("Pro · {0}", Plan.YEARLY.label), style = Type.screenTitle.copy(color = Color.White))
            Spacer(Modifier.height(16.dp))

            // The price, which drops once a valid code is applied.
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
                    .border(2.dp, Palette.saffron, RoundedCornerShape(20.dp)).padding(18.dp),
            ) {
                if (applied != null && referralPrice != null) {
                    Text(regularPrice, style = Type.body.copy(color = Color(0xFF7A869F), textDecoration = TextDecoration.LineThrough))
                    Text(referralPrice, style = Type.screenTitle.copy(color = Palette.night))
                    Text(t("{0} for the first year, then {1}/year", referralPrice, regularPrice), style = Type.small.copy(color = Color(0xFF3A4762)))
                } else {
                    Text("$regularPrice /${Plan.YEARLY.period}", style = Type.screenTitle.copy(color = Palette.night))
                    Text(t("Just ₹{0}/month · save {1}%", Plan.YEARLY.rupees / 12, yearlySavingPercent), style = Type.small.copy(color = Color(0xFF3A4762)))
                }
            }
            Spacer(Modifier.height(20.dp))

            Text(t("Have a referral code?"), style = Type.h2.copy(color = Color.White))
            Text(
                t("Enter a friend's code to get your first year for {0} instead of {1}.", referralPrice ?: Plan.YEARLY.referralPrice.orEmpty(), regularPrice),
                style = Type.body.copy(color = dim), modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    code, { code = it.uppercase().replace(" ", "").take(12); error = null; applied = null },
                    label = { Text(t("Referral code")) }, singleLine = true, enabled = !checking && !busy,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, disabledTextColor = dim,
                        focusedLabelColor = Palette.saffron, unfocusedLabelColor = dim,
                        focusedBorderColor = Palette.saffron, unfocusedBorderColor = dim, cursorColor = Palette.saffron,
                    ),
                )
                PillButton(
                    when {
                        checking -> t("Checking…")
                        applied != null -> t("Remove")
                        else -> t("Apply")
                    },
                    {
                        if (applied != null) { applied = null; code = "" } else scope.launch {
                            checking = true
                            error = check(code) ?: if (referralPrice == null) t("The referral price isn't available right now. You can still subscribe at the regular price.") else null
                            if (error == null) applied = normalizeReferralCode(code)
                            checking = false
                        }
                    },
                    Modifier.width(118.dp), color = Color.White, textColor = Palette.night,
                    enabled = !checking && !busy && (applied != null || code.isNotBlank()), height = 56.dp,
                )
            }
            error?.let { Text(it, style = Type.small.copy(color = Palette.carbs), modifier = Modifier.padding(top = 10.dp)) }
            if (applied != null) Text(t("✓ Code applied. You save {0} on your first year.", saving), style = Type.small.copy(color = Palette.leaf), modifier = Modifier.padding(top = 10.dp))
            message?.let { Text(it, style = Type.small.copy(color = Palette.carbs), modifier = Modifier.padding(top = 12.dp)) }
            Text(
                t("No code? Just continue at the regular price."),
                style = Type.small.copy(color = Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            PillButton(
                when {
                    busy -> t("Please wait…")
                    applied != null && referralPrice != null -> t("Pay {0} for the first year", referralPrice)
                    else -> t("Pay {0}/{1}", regularPrice, Plan.YEARLY.period)
                },
                { onPay(applied) },
                Modifier.fillMaxWidth(), color = Palette.saffron, enabled = enabled && !busy && !checking, height = 58.dp,
            )
        }
    }
}
