package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.AdminStats
import com.rotitrack.app.account.Promoter
import com.rotitrack.app.account.ReferralReport
import com.rotitrack.app.account.Plan
import com.rotitrack.app.account.UserDirectory
import com.rotitrack.app.account.UserSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_MS = 86_400_000L

private enum class UserFilter(val label: String, val test: (UserSummary) -> Boolean) {
    ALL("All", { true }),
    PRO("Pro", { it.isPro }),
    FREE("Free", { !it.isPro }),
    BLOCKED("Blocked", { it.blocked }),
}

private enum class AdminSection(val label: String) { USERS("Users"), REFERRALS("Referrals") }

@Composable
fun AdminScreen(directory: UserDirectory, onOpen: (String) -> Unit, onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<UserSummary>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(UserFilter.ALL) }
    var section by remember { mutableStateOf(AdminSection.USERS) }
    var paying by remember { mutableStateOf<Promoter?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        error = null
        runCatching { directory.all() }
            .onSuccess { users = it }
            .onFailure { error = it.message ?: "Couldn't load users" }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 34.sp, color = Palette.ink, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 12.dp))
            Text("Admin dashboard", style = Type.h2, modifier = Modifier.weight(1f))
            IconButton(onClick = { reload++ }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Palette.brand) }
        }
        val list = users
        when {
            error != null -> Text(
                "⚠️ $error\n\nCheck that your account has role \"admin\" in Firestore and that the security rules are deployed.",
                style = Type.body.copy(color = Palette.danger), modifier = Modifier.padding(20.dp),
            )
            list == null -> Text("Loading users…", style = Type.body.copy(color = Palette.muted), modifier = Modifier.padding(20.dp))
            section == AdminSection.REFERRALS -> ReferralsPanel(
                ReferralReport.of(list), onOpen = onOpen, onPay = { paying = it },
                header = { ChipRow(AdminSection.entries.toList(), section, label = { it.label }, onSelect = { section = it }) },
            )
            else -> {
                val stats = AdminStats.of(list)
                val referrals = ReferralReport.of(list)
                val q = query.trim().lowercase()
                val shown = list.filter { filter.test(it) && (q.isEmpty() || it.name.lowercase().contains(q) || it.email.lowercase().contains(q)) }
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                    item {
                        Box(Modifier.padding(bottom = 12.dp)) {
                            ChipRow(AdminSection.entries.toList(), section, label = { it.label }, onSelect = { section = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Kpi("Total users", "${stats.total}", Modifier.weight(1f))
                            Kpi("Pro users", "${stats.pro}", Modifier.weight(1f), Palette.saffron)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Kpi("Est. monthly revenue", "₹${"%,d".format(stats.mrr)}", Modifier.weight(1f), Palette.leaf)
                            Kpi("Active (24 h)", "${stats.active24h}", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Kpi("Monthly subs", "${stats.monthly}", Modifier.weight(1f))
                            Kpi("Yearly subs", "${stats.yearly}", Modifier.weight(1f))
                            Kpi("New (7 d)", "${stats.new7d}", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        AppCard {
                            Text("Sign-ups, last 14 days", style = Type.title)
                            Spacer(Modifier.height(12.dp))
                            val now = System.currentTimeMillis()
                            val bars = (13 downTo 0).map { ago ->
                                val start = startOfDay(now) - ago * DAY_MS
                                Bar(
                                    if (ago % 2 == 0) SimpleDateFormat("d", Locale.getDefault()).format(Date(start)) else "",
                                    list.count { it.createdAt in start until start + DAY_MS }.toFloat(),
                                    ago == 0,
                                )
                            }
                            BarChart(bars, goal = bars.map { it.value }.average().toFloat(), color = Palette.brand, height = 110.dp)
                            Text("Dashed line is the daily average.", style = Type.small, modifier = Modifier.padding(top = 6.dp))
                        }
                        TextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text("Search name or email") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true, shape = RoundedCornerShape(50),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Palette.card, unfocusedContainerColor = Palette.card,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(Modifier.padding(vertical = 12.dp)) {
                            ChipRow(UserFilter.entries.toList(), filter, label = { it.label }, onSelect = { filter = it })
                        }
                        Text("${shown.size} users", style = Type.small, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(shown, key = { it.uid }) { u -> UserRow(u, referrals.promoterOf(u)?.name) { onOpen(u.uid) } }
                }
            }
        }
    }

    paying?.let { p ->
        PayDialog(p.user, onDismiss = { paying = null }) {
            paying = null
            scope.launch {
                runCatching { directory.markReferralPaid(p.user.uid) }
                    .onSuccess { reload++ }
                    .onFailure { error = it.message ?: "Couldn't mark as paid" }
            }
        }
    }
}

@Composable
private fun ReferralsPanel(report: ReferralReport, onOpen: (String) -> Unit, onPay: (Promoter) -> Unit, header: @Composable () -> Unit) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            Box(Modifier.padding(bottom = 12.dp)) { header() }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kpi("To pay now", rupees(report.due), Modifier.weight(1f), if (report.due > 0) Palette.danger else Palette.ink)
                Kpi("Paid so far", rupees(report.paid), Modifier.weight(1f), Palette.leaf)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kpi("Referred sign-ups", "${report.links.size}", Modifier.weight(1f))
                Kpi("Went Pro", "${report.wentPro}", Modifier.weight(1f), Palette.saffron)
                Kpi("Promoters", "${report.promoters.size}", Modifier.weight(1f))
            }
            Text("Payouts due", style = Type.h2, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            Text(
                "Send the amount to the promoter's UPI ID from your UPI app, then tap Mark as paid.",
                style = Type.small, modifier = Modifier.padding(bottom = 10.dp),
            )
            if (report.payoutsDue.isEmpty()) {
                Text("Nothing to pay right now. 🎉", style = Type.body.copy(color = Palette.muted), modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        items(report.payoutsDue, key = { "due-" + it.user.uid }) { p ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onOpen(p.user.uid) }) {
                        Text(p.user.name.ifBlank { "(no name)" }, style = Type.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.user.email, style = Type.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(rupees(p.user.referralDue), style = Type.h2.copy(color = Palette.danger))
                }
                Spacer(Modifier.height(8.dp))
                if (p.user.payoutUpi.isBlank()) {
                    Text("⚠️ No UPI ID added yet. Ask them to add it on the Refer & earn screen.", style = Type.small.copy(color = Palette.danger))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UPI: ${p.user.payoutUpi}", style = Type.body.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        Text(
                            "Copy", style = Type.body.copy(color = Palette.brand, fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { clipboard.setText(AnnotatedString(p.user.payoutUpi)) }.padding(8.dp),
                        )
                    }
                }
                Text(
                    "Earned ${rupees(p.user.referralEarnings)} · paid ${rupees(p.user.referralPaid)} · ${p.friends.size} friend${if (p.friends.size == 1) "" else "s"} referred",
                    style = Type.tiny, modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(10.dp))
                PillButton("Mark ${rupees(p.user.referralDue)} as paid", { onPay(p) }, Modifier.fillMaxWidth(), color = Palette.leaf, height = 44.dp)
            }
        }
        item { Text("Who referred whom", style = Type.h2, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) }
        if (report.links.isEmpty()) {
            item { Text("No one has signed up with a referral code yet.", style = Type.body.copy(color = Palette.muted)) }
        }
        items(report.links, key = { "link-" + it.friend.uid }) { link ->
            val f = link.friend
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(Palette.card)
                    .clickable { onOpen(f.uid) }.padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.name.ifBlank { "(no name)" }, style = Type.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    when {
                        f.plan != null -> Badge(f.plan!!.label.uppercase(), Palette.saffron)
                        f.compPro -> Badge("PRO (FREE)", Palette.leaf)
                        else -> Badge("FREE", Palette.muted)
                    }
                }
                Text(
                    "Referred by " + (link.promoter?.let { "${it.name.ifBlank { it.email }} (${link.code})" } ?: "${link.code} — no user has this code"),
                    style = Type.small, maxLines = 2,
                )
                Text(
                    "Joined ${date(f.createdAt)} · earned their promoter ${rupees(f.referralCreditedAmount)}",
                    style = Type.tiny, maxLines = 1,
                )
            }
        }
        if (report.promoters.isNotEmpty()) {
            item { Text("All promoters", style = Type.h2, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) }
            items(report.promoters, key = { "promoter-" + it.user.uid }) { p ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(Palette.card)
                        .clickable { onOpen(p.user.uid) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.user.name.ifBlank { "(no name)" }, style = Type.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${p.user.referralCode} · ${p.friends.size} friend${if (p.friends.size == 1) "" else "s"} · ${p.user.referralCount} went Pro", style = Type.tiny, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(rupees(p.user.referralEarnings), style = Type.title)
                        Text(if (p.user.referralDue > 0) "${rupees(p.user.referralDue)} due" else "all paid", style = Type.tiny)
                    }
                }
            }
        }
    }
}

@Composable
private fun PayDialog(u: UserSummary, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark ${rupees(u.referralDue)} as paid?") },
        text = {
            Text(
                "Only do this after you've sent ${rupees(u.referralDue)} to ${u.name.ifBlank { u.email }}" +
                    (if (u.payoutUpi.isNotBlank()) " (${u.payoutUpi})" else "") + ". Their due amount will go to ₹0.",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yes, I've paid") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun rupees(n: Int): String = "₹" + "%,d".format(n)

@Composable
private fun Kpi(label: String, value: String, modifier: Modifier, color: Color = Palette.ink) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Palette.card).padding(14.dp)) {
        Text(label, style = Type.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = Type.h2.copy(color = color), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun UserRow(u: UserSummary, referredBy: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(Palette.card)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (u.blocked) Palette.muted else Palette.brand), contentAlignment = Alignment.Center) {
            Text(initials(u.name), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(u.name.ifBlank { "(no name)" }, style = Type.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(u.email, style = Type.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Active ${ago(u.lastActive)} · joined ${date(u.createdAt)}", style = Type.tiny, maxLines = 1)
            u.referredByCode?.let { code ->
                Text("Referred by ${referredBy?.ifBlank { null } ?: code}", style = Type.tiny.copy(color = Palette.brand), maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                u.plan != null -> Badge(u.plan!!.label.uppercase(), Palette.saffron)
                u.compPro -> Badge("PRO (FREE)", Palette.leaf)
                else -> Badge("FREE", Palette.muted)
            }
            if (u.isAdmin) Badge("ADMIN", Palette.brand)
            if (u.blocked) Badge("BLOCKED", Palette.danger)
        }
    }
}

@Composable
fun AdminUserScreen(directory: UserDirectory, uid: String, onBack: () -> Unit) {
    var user by remember { mutableStateOf<UserSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<ReferralReport?>(null) }
    var paying by remember { mutableStateOf(false) }
    LaunchedEffect(uid) { runCatching { directory.get(uid) }.onSuccess { user = it }.onFailure { error = it.message } }
    LaunchedEffect(uid, user) { runCatching { directory.all() }.onSuccess { report = ReferralReport.of(it) } }

    fun change(action: suspend () -> Unit) {
        scope.launch {
            runCatching { action(); user = directory.get(uid) }.onFailure { error = it.message }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 34.sp, color = Palette.ink, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 12.dp))
            Text("User", style = Type.h2)
        }
        val u = user
        Column(Modifier.padding(horizontal = 16.dp)) {
            error?.let { Text("⚠️ $it", style = Type.body.copy(color = Palette.danger)) }
            if (u == null) {
                if (error == null) Text("Loading…", style = Type.body.copy(color = Palette.muted))
            } else {
                UserDetail(u, uid, directory, report, onPay = { paying = true }, change = ::change)
            }
        }
    }
    val u = user
    if (paying && u != null) {
        PayDialog(u, onDismiss = { paying = false }) {
            paying = false
            change { directory.markReferralPaid(uid) }
        }
    }
}

@Composable
private fun UserDetail(
    u: UserSummary,
    uid: String,
    directory: UserDirectory,
    report: ReferralReport?,
    onPay: () -> Unit,
    change: (suspend () -> Unit) -> Unit,
) {
    Column {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Palette.brand), contentAlignment = Alignment.Center) {
                    Text(initials(u.name), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(u.name.ifBlank { "(no name)" }, style = Type.h2)
                    Text(u.email, style = Type.small)
                }
            }
        }
        AppCard {
            Info("Plan", u.plan?.let { "${it.label} · ₹${it.rupees}/${it.period}" } ?: if (u.compPro) "Pro (granted free)" else "Free")
            u.paidVia?.let { Info("Paid via", it) }
            u.razorpayStatus?.let { Info("Razorpay status", it) }
            Info("Joined", date(u.createdAt))
            Info("Last active", ago(u.lastActive))
            Info("Streak", "${u.streak} day${if (u.streak == 1) "" else "s"}")
            Info("Days logged", "${u.daysLogged}")
            Info("Calorie goal", "${u.calorieGoal} kcal")
            Info("Water goal", liters(u.waterGoalMl))
            Info("Diet", u.diet.ifBlank { "—" })
            Info("Role", if (u.isAdmin) "Admin" else "User")
        }
        Text("Referral", style = Type.title, modifier = Modifier.padding(vertical = 8.dp))
        AppCard {
            val link = report?.links?.firstOrNull { it.friend.uid == uid }
            Info(
                "Referred by",
                when {
                    u.referredByCode == null -> "No one"
                    link?.promoter != null -> "${link.promoter.name.ifBlank { link.promoter.email }} (${u.referredByCode})"
                    report == null -> u.referredByCode
                    else -> "${u.referredByCode} (no user has this code)"
                },
            )
            if (u.referredByCode != null) Info("Earned their promoter", rupees(u.referralCreditedAmount))
            Info("Own promo code", u.referralCode.ifBlank { "—" })
            val friends = report?.promoters?.firstOrNull { it.user.uid == uid }?.friends.orEmpty()
            Info("Friends signed up", "${friends.size}")
            Info("Friends went Pro", "${u.referralCount}")
            Info("Earned", rupees(u.referralEarnings))
            Info("Paid", rupees(u.referralPaid))
            Info("Due", rupees(u.referralDue))
            Info("UPI ID", u.payoutUpi.ifBlank { "Not added" })
            if (friends.isNotEmpty()) {
                Text("Friends: " + friends.joinToString { it.name.ifBlank { it.email } }, style = Type.small, modifier = Modifier.padding(top = 6.dp))
            }
            if (u.referralDue > 0) {
                Spacer(Modifier.height(10.dp))
                PillButton("Mark ${rupees(u.referralDue)} as paid", onPay, Modifier.fillMaxWidth(), color = Palette.leaf, height = 44.dp)
            }
        }
        Text("Actions", style = Type.title, modifier = Modifier.padding(vertical = 8.dp))
        AppCard {
            ToggleLine("Give Pro for free", u.compPro) { v -> change { directory.setCompPro(uid, v) } }
            Text("For testers, partners or support cases. Doesn't affect Play Store billing.", style = Type.small)
            Spacer(Modifier.height(12.dp))
            ToggleLine("Block account", u.blocked) { v -> if (!u.isAdmin) change { directory.setBlocked(uid, v) } }
            Text(
                if (u.isAdmin) "Admins can't be blocked here." else "Blocked users are signed out and can't use the app.",
                style = Type.small,
            )
        }
        }
    }

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = Type.body.copy(color = Palette.muted), modifier = Modifier.weight(1f))
        Text(value, style = Type.body.copy(fontWeight = FontWeight.Bold))
    }
}

private fun startOfDay(t: Long): Long {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = t }
    c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
    c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun date(t: Long): String = if (t <= 0) "—" else SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(t))

private fun ago(t: Long): String {
    if (t <= 0) return "never"
    val mins = (System.currentTimeMillis() - t) / 60_000
    return when {
        mins < 2 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 24 * 60 -> "${mins / 60} h ago"
        mins < 48 * 60 -> "yesterday"
        else -> "${mins / (24 * 60)} days ago"
    }
}
