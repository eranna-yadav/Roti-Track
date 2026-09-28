package com.rotitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.Account
import com.rotitrack.app.account.REFERRAL_MONTHLY_INSTALMENT
import com.rotitrack.app.account.REFERRAL_REWARD_RUPEES
import com.rotitrack.app.account.UserSummary
import com.rotitrack.app.domain.Reports
import com.rotitrack.app.store.AppStore
import com.rotitrack.app.store.Badge
import com.rotitrack.app.store.Badges
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------- referral

@Composable
fun ReferralScreen(store: AppStore, account: Account, summary: UserSummary?, platform: Platform, onRules: () -> Unit, onBack: () -> Unit) {
    // The code is assigned by the server at sign-up; show it once the account details have loaded.
    val code = summary?.referralCode?.takeIf { it.isNotBlank() }
    var upi by remember { mutableStateOf(store.prefs.payoutUpi) }
    val shareText = "I track my Indian diet, calories and water with Roti Track. Join with my code ${code.orEmpty()} " +
        "when you sign up and get healthier with me!"

    SubScreen("Refer & earn", onBack) {
        AppCard(color = Palette.saffronSoft) {
            Text("🎁", fontSize = 40.sp)
            Text("Earn up to ₹$REFERRAL_REWARD_RUPEES per friend", style = Type.h2)
            Text(
                "Share your code. When a friend signs up with it and buys Pro, you earn a reward.",
                style = Type.body.copy(color = Palette.inkSoft), modifier = Modifier.padding(top = 4.dp),
            )
        }
        AppCard {
            Text("Your promo code", style = Type.small)
            Text(code ?: "Loading…", fontSize = if (code == null) 22.sp else 32.sp, fontWeight = FontWeight.Black, color = Palette.brand, letterSpacing = 3.sp, modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Copy code", { code?.let(platform::copyText) }, Modifier.weight(1f), color = Palette.chip, textColor = Palette.brand, enabled = code != null, height = 46.dp)
                PillButton("Share", { platform.share(shareText) }, Modifier.weight(1f), enabled = code != null, height = 46.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Friends joined Pro", "${summary?.referralCount ?: 0}", Modifier.weight(1f))
            Stat("Earned", "₹${summary?.referralEarnings ?: 0}", Modifier.weight(1f))
            Stat("Paid to you", "₹${summary?.referralPaid ?: 0}", Modifier.weight(1f))
        }
        SectionLabel("Get paid")
        AppCard {
            OutlinedTextField(
                upi, { upi = it.trim().take(60) }, label = { Text("Your UPI ID (e.g. name@okaxis)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            PillButton("Save UPI ID", { store.updatePrefs { it.copy(payoutUpi = upi) } }, Modifier.fillMaxWidth(), height = 46.dp,
                enabled = upi != store.prefs.payoutUpi && (upi.isEmpty() || upi.contains('@')))
            Text("We send your earnings to this UPI ID after checking your friend's payment.", style = Type.small, modifier = Modifier.padding(top = 8.dp))
        }
        SectionLabel("How it works")
        SettingsGroup {
            SettingsRow("1️⃣", "Share your code", subtitle = "Send it on WhatsApp or anywhere you like")
            SettingsRow("2️⃣", "Your friend signs up", subtitle = "They enter ${code ?: "your code"} when creating an account")
            SettingsRow("3️⃣", "They buy Pro, you earn up to ₹$REFERRAL_REWARD_RUPEES", subtitle = "See the referral rules for details")
            SettingsRow("📜", "Referral rules", last = true, onClick = onRules)
        }
    }
}

/** The referral programme's rules, shown from Profile and from Refer & earn. */
val REFERRAL_RULES = listOf(
    "Share your promo code" to "Find it under Profile → Refer a friend. Your friend must enter it while creating their Roti Track account; it can't be added later.",
    "Yearly plan: ₹$REFERRAL_REWARD_RUPEES" to "When your friend buys the yearly plan (₹990), you earn ₹$REFERRAL_REWARD_RUPEES once their payment is confirmed.",
    "Monthly plan: ₹$REFERRAL_MONTHLY_INSTALMENT + ₹$REFERRAL_MONTHLY_INSTALMENT" to "When your friend buys the monthly plan (₹359), you earn ₹$REFERRAL_MONTHLY_INSTALMENT after their 1st month's payment and another ₹$REFERRAL_MONTHLY_INSTALMENT after their 2nd month's payment: ₹$REFERRAL_REWARD_RUPEES in total.",
    "If they stop after one month" to "If your friend cancels or their 2nd monthly payment doesn't go through, you keep the first ₹$REFERRAL_MONTHLY_INSTALMENT but don't earn the second.",
    "Switching to yearly" to "If your friend moves from monthly to yearly, you earn the rest of the ₹$REFERRAL_REWARD_RUPEES when the yearly payment is confirmed.",
    "Most ₹$REFERRAL_REWARD_RUPEES per friend" to "Each friend earns you at most ₹$REFERRAL_REWARD_RUPEES in total, however long they stay Pro. There's no limit on how many friends you invite.",
    "Which payments count" to "Only Pro plans your friend pays for in the Roti Track app with Razorpay (UPI, cards, netbanking or wallets). Free Pro given by Roti Track and refunded payments don't count.",
    "Getting paid" to "Add your UPI ID on the Refer & earn screen. The Roti Track team checks each payment and sends your earnings to that UPI ID.",
    "Fair use" to "Referring yourself, fake or duplicate accounts, and spam are not allowed. Rewards earned this way are cancelled and the account may be blocked.",
    "Changes" to "We may change or end the programme with notice in the app. Rewards you have already earned will still be paid.",
)

@Composable
fun ReferralRulesScreen(onBack: () -> Unit) {
    SubScreen("Referral rules", onBack) {
        AppCard(color = Palette.saffronSoft) {
            Text("Earn up to ₹$REFERRAL_REWARD_RUPEES per friend", style = Type.h2)
            Text(
                "Yearly plan: ₹$REFERRAL_REWARD_RUPEES at once.\nMonthly plan: ₹$REFERRAL_MONTHLY_INSTALMENT after the 1st payment + ₹$REFERRAL_MONTHLY_INSTALMENT after the 2nd.",
                style = Type.body.copy(color = Palette.inkSoft, lineHeight = 22.sp), modifier = Modifier.padding(top = 4.dp),
            )
        }
        REFERRAL_RULES.forEachIndexed { i, (h, body) ->
            Text("${i + 1}. $h", style = Type.title, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            Text(body, style = Type.body.copy(color = Palette.inkSoft, lineHeight = 22.sp))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Palette.card).padding(12.dp)) {
        Text(value, style = Type.h2)
        Text(label, style = Type.tiny, maxLines = 2)
    }
}

// --------------------------------------------------------------------- badges

@Composable
fun BadgesScreen(store: AppStore, onBack: () -> Unit) {
    val unlocked = store.state.badges
    val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    SubScreen("Badges", onBack) {
        Text("${unlocked.size} of ${Badges.ALL.size} unlocked", style = Type.body.copy(color = Palette.muted), modifier = Modifier.padding(bottom = 12.dp))
        Badges.ALL.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { b ->
                    val at = unlocked[b.id]
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Palette.card).padding(14.dp).alpha(if (at != null) 1f else 0.45f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (at != null) b.emoji else "🔒", fontSize = 34.sp)
                        Text(b.title, style = Type.title, textAlign = TextAlign.Center)
                        Text(b.description, style = Type.small, textAlign = TextAlign.Center)
                        at?.let { Text(date.format(Date(it)), style = Type.tiny, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BadgeCelebration(badge: Badge, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Awesome!") } },
        title = { Text("New badge unlocked!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(Palette.saffronSoft), contentAlignment = Alignment.Center) {
                    Text(badge.emoji, fontSize = 52.sp)
                }
                Text(badge.title, style = Type.h2, modifier = Modifier.padding(top = 12.dp))
                Text(badge.description, style = Type.body.copy(color = Palette.inkSoft), textAlign = TextAlign.Center)
            }
        },
    )
}

// --------------------------------------------------------------------- report

@Composable
fun ReportScreen(store: AppStore, account: Account, isPro: Boolean, platform: Platform, onUpgrade: () -> Unit, onBack: () -> Unit) {
    var days by remember { mutableIntStateOf(30) }
    var status by remember { mutableStateOf<String?>(null) }
    SubScreen("", onBack) {
        Box(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).height(180.dp).clip(RoundedCornerShape(28.dp)).background(Palette.card),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(40, 70, 55, 90, 65, 80, 50, 95, 75).forEachIndexed { i, h ->
                    Box(Modifier.width(12.dp).height(h.dp).clip(RoundedCornerShape(4.dp)).background(if (i % 3 == 0) Palette.saffron else Palette.brand))
                }
                Spacer(Modifier.width(12.dp))
                Text("📄", fontSize = 64.sp)
            }
        }
        Heading("Get your PDF Summary Report", "Here's what you'll get in your summary report:")
        SettingsGroup {
            SettingsRow("🍴", "Meal history", subtitle = "All logged meals and nutrition details")
            SettingsRow("🏃", "Exercise history", subtitle = "Logged workouts and activity sessions")
            SettingsRow("📈", "Weight progress", subtitle = "Weekly trend of recorded weight changes")
            SettingsRow("🥧", "Calorie & macros breakdown", subtitle = "Daily calories, protein, carbs, fat and water", last = true)
        }
        SectionLabel("Period")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30, 90).forEach { d ->
                val locked = d > 7 && !isPro
                Chip(if (locked) "$d days 🔒" else "$d days", days == d, { if (locked) onUpgrade() else days = d }, Modifier.weight(1f))
            }
        }
        LaunchedEffect(isPro) { if (!isPro) days = 7 }
        Spacer(Modifier.height(20.dp))
        PillButton("Next", {
            status = runCatching {
                platform.exportReport(Reports.build(store, store.profile.name.ifBlank { account.name }, days))
                null
            }.getOrElse { "Couldn't create the report: ${it.message}" }
        }, Modifier.fillMaxWidth(), color = Palette.night, height = 56.dp)
        status?.let { Text(it, style = Type.small.copy(color = Palette.danger), modifier = Modifier.padding(top = 8.dp)) }
        Text("The PDF opens in the share menu, so you can save it or send it to your doctor or dietitian.", style = Type.small, modifier = Modifier.padding(top = 12.dp))
    }
}

// ---------------------------------------------------------------------- legal

@Composable
fun LegalScreen(terms: Boolean, onBack: () -> Unit) {
    SubScreen(if (terms) "Terms and Conditions" else "Privacy Policy", onBack) {
        val paragraphs = if (terms) TERMS else PRIVACY
        paragraphs.forEach { (h, body) ->
            Text(h, style = Type.title, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            Text(body, style = Type.body.copy(color = Palette.inkSoft, lineHeight = 22.sp))
        }
        Text("Last updated: September 2026. Contact: $SUPPORT_EMAIL", style = Type.small, modifier = Modifier.padding(top = 20.dp))
    }
}

private val TERMS = listOf(
    "About Roti Track" to "Roti Track helps you plan Indian meals and track calories, macros, water, exercise, weight and fasting. It is a wellness tool, not a medical service, and does not replace advice from a doctor or dietitian.",
    "Your account" to "Keep your password safe. You are responsible for activity on your account. We may suspend accounts that misuse the app or the referral programme.",
    "Pro subscription" to "Pro costs ₹359 per month or ₹990 per year, including GST, and renews automatically until you cancel. Cancel Google Play subscriptions in the Play Store and Razorpay subscriptions from the Pro screen; you keep Pro until the end of the paid period. Payments are non-refundable except where the law or the store's policy requires.",
    "Referral programme" to "You earn up to ₹500 when a friend signs up with your code and pays for Pro: ₹500 once their yearly plan is paid, or ₹250 after their first monthly payment and ₹250 after their second. The full Referral rules in the app apply. Each friend counts once. Self-referrals, fake accounts and spam are not allowed and forfeit rewards. We may change or end the programme with notice; earned rewards will still be paid.",
    "Food and health information" to "Nutrition values are typical home-style estimates and vary with recipes and portions. Calorie, macro, water and fasting targets are general guidance. If you are pregnant, diabetic, have a medical condition or an eating disorder, consult a professional before changing your diet.",
    "Changes" to "We may update these terms. Continuing to use the app after an update means you accept the new terms.",
)

private val PRIVACY = listOf(
    "What we store" to "Your name, email and account details; your profile (age, height, weight, goals and preferences); and your food, water, exercise, weight and fasting logs.",
    "Where it lives" to "Your logs stay on your phone. When online accounts are set up, your account, a summary of your activity (goals, streak, days logged) and your subscription and referral status are stored securely in Google Firebase so you can sign in and so we can run Pro and referrals.",
    "Payments" to "Payments are handled by Razorpay or Google Play. We never see or store your card, UPI or bank details. We keep a record of your subscription status.",
    "Sharing" to "We don't sell your data or show ads. We share data only with the services that run the app (Firebase, Razorpay, Google Play) or when the law requires it.",
    "Your choices" to "You can edit your details any time, export a PDF of your data, and delete your account from Profile → Delete Account, which removes your account and your data.",
    "Contact" to "Questions about privacy? Email us at the address below.",
)
