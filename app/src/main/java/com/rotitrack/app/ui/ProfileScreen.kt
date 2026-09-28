package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.Account
import com.rotitrack.app.account.REFERRAL_REWARD_RUPEES
import com.rotitrack.app.store.AppStore

const val APP_VERSION = "1.4.0"
/** Where "Support Email" and "Request a Feature" send mail. Change to your own address. */
const val SUPPORT_EMAIL = "support@rotitrack.app"

enum class ProfilePage { PERSONAL, PREFERENCES, LANGUAGE, GOALS, FASTING, REMINDERS, WATER_REMINDERS, REFERRAL, REFERRAL_RULES, BADGES, REPORT, TERMS, PRIVACY, ARTICLES, ADMIN, PRO }

@Composable
fun ProfileScreen(
    store: AppStore,
    account: Account,
    isPro: Boolean,
    isAdmin: Boolean,
    platform: Platform,
    onOpen: (ProfilePage) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    val p = store.profile
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding)) {
        ScreenHeader(t("Profile"))

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.card)
                .clickable { onOpen(ProfilePage.PERSONAL) }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(Palette.brand), contentAlignment = Alignment.Center) {
                Text(initials(p.name.ifBlank { account.name }), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(p.name.ifBlank { account.name }, style = Type.h2)
                Text(t("{0} years old", p.age), style = Type.body.copy(color = Palette.inkSoft))
            }
            if (isPro) Badge("PRO", Palette.saffron)
        }

        SectionLabel(t("Invite friends"))
        SettingsGroup {
            SettingsRow(
                "🎁", t("Refer a friend and earn up to ₹{0}", REFERRAL_REWARD_RUPEES),
                subtitle = t("For every friend who buys Pro with your promo code."),
                onClick = { onOpen(ProfilePage.REFERRAL) },
            )
            SettingsRow(
                "📜", t("Referral rules"),
                subtitle = t("How and when rewards are paid"),
                last = true, onClick = { onOpen(ProfilePage.REFERRAL_RULES) },
            )
        }

        Spacer(Modifier.height(12.dp))
        PillButton(
            if (isPro) t("👑 Roti Track Pro · manage") else t("👑 Upgrade to Pro"),
            { onOpen(ProfilePage.PRO) }, Modifier.fillMaxWidth(),
            color = if (isPro) Palette.chip else Palette.saffron, textColor = if (isPro) Palette.brand else Color.White, height = 50.dp,
        )

        SectionLabel(t("Account"))
        SettingsGroup {
            SettingsRow("🪪", t("Personal details"), onClick = { onOpen(ProfilePage.PERSONAL) })
            SettingsRow("⚙️", t("Preferences"), onClick = { onOpen(ProfilePage.PREFERENCES) })
            SettingsRow("🌐", t("Language"), value = languageName(com.rotitrack.app.i18n.I18n.lang), last = true, onClick = { onOpen(ProfilePage.LANGUAGE) })
        }

        SectionLabel(t("Goals & Tracking"))
        SettingsGroup {
            SettingsRow("🎯", t("Edit Nutrition Goals"), value = t("{0} kcal", p.calorieGoal), onClick = { onOpen(ProfilePage.GOALS) })
            SettingsRow(
                "⏳", t("Intermittent Fasting"),
                value = if (store.state.fasting.activeStart != null) t("Fasting") else "${store.state.fasting.targetHours}:${24 - store.state.fasting.targetHours}",
                onClick = { onOpen(ProfilePage.FASTING) },
            )
            SettingsRow("🔔", t("Tracking Reminders"), onClick = { onOpen(ProfilePage.REMINDERS) })
            SettingsRow("🏅", t("Badges"), value = "${store.state.badges.size}", last = true, onClick = { onOpen(ProfilePage.BADGES) })
        }

        if (isAdmin) {
            SectionLabel(t("Admin"))
            SettingsGroup {
                SettingsRow("🛠️", t("Admin dashboard"), subtitle = t("Users, subscriptions, revenue and referrals"), last = true, onClick = { onOpen(ProfilePage.ADMIN) })
            }
        }

        SectionLabel(t("Support & Legal"))
        SettingsGroup {
            SettingsRow("📣", t("Request a Feature"), onClick = {
                platform.openUrl("mailto:$SUPPORT_EMAIL?subject=" + encode("Roti Track feature request"))
            })
            SettingsRow("✉️", t("Support Email"), onClick = {
                platform.openUrl("mailto:$SUPPORT_EMAIL?subject=" + encode("Roti Track support ($APP_VERSION)"))
            })
            SettingsRow("📄", t("Export PDF Summary Report"), onClick = { onOpen(ProfilePage.REPORT) })
            SettingsRow("📚", t("Health articles"), onClick = { onOpen(ProfilePage.ARTICLES) })
            SettingsRow("📃", t("Terms and Conditions"), onClick = { onOpen(ProfilePage.TERMS) })
            SettingsRow("🛡️", t("Privacy Policy"), last = true, onClick = { onOpen(ProfilePage.PRIVACY) })
        }

        SectionLabel(t("Account Actions"))
        SettingsGroup {
            SettingsRow("↪️", t("Logout"), onClick = { confirmLogout = true })
            SettingsRow("🗑️", t("Delete Account"), titleColor = Palette.danger, last = true, onClick = { confirmDelete = true })
        }

        Text(
            t("VERSION {0}", APP_VERSION), style = Type.small.copy(color = Palette.inkSoft),
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(t("Log out?")) },
            text = { Text(t("Your logs stay on this phone and come back when you sign in again.")) },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onSignOut() }) { Text(t("Log out")) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(t("Cancel")) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t("Delete your account?")) },
            text = {
                Text(
                    t("This permanently deletes your account, profile, meals, water, exercise and weight logs. It can't be undone. If you pay for Pro, cancel the subscription first (Google Play or the Pro screen).")
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteAccount() }) { Text(t("Delete"), color = Palette.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(t("Cancel")) } },
        )
    }
}

val LANGUAGES = listOf(
    "en" to "English", "hi" to "हिन्दी (Hindi)", "te" to "తెలుగు (Telugu)", "ta" to "தமிழ் (Tamil)",
    "kn" to "ಕನ್ನಡ (Kannada)", "mr" to "मराठी (Marathi)", "bn" to "বাংলা (Bengali)", "gu" to "ગુજરાતી (Gujarati)",
)

fun languageName(code: String) = LANGUAGES.firstOrNull { it.first == code }?.second?.substringBefore(" (") ?: "English"

private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

@Composable
fun ToggleLine(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Type.title)
            subtitle?.let { Text(it, style = Type.small) }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Palette.brand))
    }
}

fun hour(h: Int): String = when {
    h == 0 -> "12 AM"
    h < 12 -> "$h AM"
    h == 12 -> "12 PM"
    else -> "${h - 12} PM"
}

fun clockTime(h: Int, m: Int): String {
    val hh = if (h % 12 == 0) 12 else h % 12
    return "%02d:%02d %s".format(hh, m, if (h < 12) "AM" else "PM")
}
