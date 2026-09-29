package com.rotitrack.app.ui

import com.rotitrack.app.i18n.I18n
import com.rotitrack.app.i18n.t
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rotitrack.app.account.Services
import com.rotitrack.app.account.UserSummary
import com.rotitrack.app.account.summarize
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.WaterSound
import com.rotitrack.app.domain.Days
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.rotitrack.app.domain.Report
import com.rotitrack.app.store.AppStore
import com.rotitrack.app.account.Account

/** What the Android layer provides to the UI. Keeps the screens platform-neutral. */
interface Platform {
    /** Asks for notification permission if needed, then reports whether reminders can be shown. */
    fun requestNotifications(onResult: (Boolean) -> Unit)
    fun scheduleReminders(profile: Profile)
    /** Called when someone signs in (uid) or out (null), so background work follows the right account. */
    fun sessionChanged(uid: String?)
    fun openUrl(url: String)
    fun share(text: String)
    fun copyText(text: String)
    /** Whether the system lets the app post notifications at all. */
    fun notificationsEnabled(): Boolean
    fun openNotificationSettings()
    /** Where the user can stop Android from holding back background reminders. */
    fun openBatterySettings()
    /** Whether reminders can go off at the exact minute while the phone is locked. Compose state. */
    val exactAlarmsAllowed: Boolean get() = true
    /** Where the user allows "Alarms & reminders" for the app. */
    fun openExactAlarmSettings() {}
    /** Plays a water sound once (a preview, or when water is logged). */
    fun playSound(sound: WaterSound, volume: Float)
    fun stopSound()
    fun vibrate()
    /** Reads a sample meal reminder out loud, as the voice reminders will. */
    fun previewMealVoice() {}
    /** Renders the report as a PDF and opens the share sheet. */
    fun exportReport(report: Report)
    /** Light or dark status/navigation bar icons to match the theme. */
    fun setDarkTheme(dark: Boolean)
}

enum class Tab(private val labelEn: String, val icon: ImageVector) {
    HOME("Home", TabIcons.Home),
    WATER("Water", TabIcons.Water),
    FOOD("Food", TabIcons.Food),
    PLAN("Plan", TabIcons.Plan),
    PROFILE("Profile", TabIcons.Me);

    val label: String get() = t(labelEn)
}

sealed interface Route {
    data class Tabs(val tab: Tab) : Route
    data class AddFood(val slot: MealSlot, val day: String) : Route
    data object Articles : Route
    data class Article(val id: String) : Route
    data object Pro : Route
    data object Admin : Route
    data class AdminUser(val uid: String) : Route
    data class Page(val page: ProfilePage) : Route
}

/** A tiny back stack; the Android activity wires the system back button to [pop]. */
class Navigator {
    val stack = mutableStateListOf<Route>(Route.Tabs(Tab.HOME))
    val current: Route get() = stack.last()
    val canPop: Boolean get() = stack.size > 1 || (current as? Route.Tabs)?.tab != Tab.HOME

    fun push(r: Route) { stack.add(r) }
    fun tab(t: Tab) {
        stack.clear()
        stack.add(Route.Tabs(t))
    }
    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else tab(Tab.HOME)
    }
}

@Composable
fun App(services: Services, platform: Platform, nav: Navigator = remember { Navigator() }) {
    val account = services.auth.account
    if (account == null) {
        LaunchedEffect(Unit) { platform.setDarkTheme(false) }
        RotiTrackTheme { LoginScreen(services.auth) }
        return
    }
    val store = remember(account.uid) { services.storeFor(account.uid) }
    LaunchedEffect(store, store.prefs.language) { I18n.lang = I18n.resolve(store.prefs.language) }
    val dark = isDark(store.prefs.appearance)
    LaunchedEffect(dark) { platform.setDarkTheme(dark) }
    RotiTrackTheme(dark) { SignedIn(services, platform, nav, account, store) }
}

@Composable
private fun SignedIn(services: Services, platform: Platform, nav: Navigator, account: Account, store: AppStore) {
    var summary by remember(account.uid) { mutableStateOf<UserSummary?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(account.uid) { platform.sessionChanged(account.uid) }
    // Reload after sign-in, after every Razorpay payment, and when the Profile tab opens (referral earnings).
    LaunchedEffect(account.uid, services.razorpay.version, nav.current) {
        summary = runCatching { services.directory.get(account.uid) }.getOrNull()
    }
    // Publish activity for the admin dashboard, debounced: any change restarts the wait.
    val playPlan = services.billing.activePlan
    LaunchedEffect(account.uid, store.state, playPlan) {
        delay(2_000)
        runCatching { services.directory.publish(summarize(account, store, playPlan)) }
    }

    val signOut = {
        platform.sessionChanged(null)
        nav.tab(Tab.HOME)
        services.auth.signOut()
    }

    if (summary?.blocked == true) {
        BlockedScreen(onSignOut = signOut)
        return
    }
    if (!store.profile.onboarded) {
        LaunchedEffect(account.uid) { if (store.profile.name.isBlank()) store.updateProfile { it.copy(name = account.name) } }
        OnboardingScreen(store, platform)
        return
    }

    val plan = playPlan ?: summary?.razorpayPlan
    val isPro = plan != null || summary?.isPro == true
    val isAdmin = account.isAdmin || summary?.isAdmin == true
    val upgrade = { nav.push(Route.Pro) }
    val openPage = { page: ProfilePage ->
        when (page) {
            ProfilePage.ADMIN -> nav.push(Route.Admin)
            ProfilePage.PRO -> nav.push(Route.Pro)
            ProfilePage.ARTICLES -> nav.push(Route.Articles)
            else -> nav.push(Route.Page(page))
        }
    }
    val route = nav.current
    Scaffold(
        containerColor = Palette.background,
        bottomBar = {
            if (route is Route.Tabs) {
                NavigationBar(containerColor = Palette.card) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = route.tab == t,
                            onClick = { nav.tab(t) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.brand,
                                selectedTextColor = Palette.brand,
                                indicatorColor = Palette.chip,
                                unselectedIconColor = Palette.muted,
                                unselectedTextColor = Palette.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (route) {
                is Route.Tabs -> when (route.tab) {
                    Tab.HOME -> DashboardScreen(store, account, isPro, plan, onUpgrade = upgrade, onOpen = nav::tab)
                    Tab.WATER -> WaterScreen(store, platform, onReminders = { nav.push(Route.Page(ProfilePage.WATER_REMINDERS)) })
                    Tab.FOOD -> FoodScreen(store, onAdd = { slot, day -> nav.push(Route.AddFood(slot, day)) })
                    Tab.PLAN -> PlanScreen(store, isPro, onUpgrade = upgrade, onOpenArticles = { nav.push(Route.Articles) }, onEditProfile = { nav.push(Route.Page(ProfilePage.PERSONAL)) })
                    Tab.PROFILE -> ProfileScreen(
                        store, account, isPro, isAdmin, platform,
                        onOpen = openPage,
                        onSignOut = signOut,
                        onDeleteAccount = {
                            scope.launch {
                                try {
                                    platform.sessionChanged(null)
                                    services.auth.deleteAccount()
                                    store.resetAll()
                                    nav.tab(Tab.HOME)
                                } catch (e: Exception) {
                                    deleteError = e.message ?: t("Couldn't delete the account. Try again.")
                                }
                            }
                        },
                    )
                }
                is Route.AddFood -> AddFoodScreen(store, route.slot, route.day, isPro, onUpgrade = upgrade, onBack = nav::pop)
                Route.Articles -> ArticlesScreen(onOpen = { nav.push(Route.Article(it)) }, onBack = nav::pop)
                is Route.Article -> ArticleScreen(route.id, onBack = nav::pop)
                Route.Pro -> ProScreen(services.billing, services.razorpay, account, summary, platform, onBack = nav::pop)
                Route.Admin -> if (isAdmin) AdminScreen(services.directory, onOpen = { nav.push(Route.AdminUser(it)) }, onBack = nav::pop) else LaunchedEffect(Unit) { nav.pop() }
                is Route.AdminUser -> if (isAdmin) AdminUserScreen(services.directory, route.uid, onBack = nav::pop) else LaunchedEffect(Unit) { nav.pop() }
                is Route.Page -> when (route.page) {
                    ProfilePage.PERSONAL -> PersonalDetailsScreen(store, platform, onBack = nav::pop)
                    ProfilePage.PREFERENCES -> PreferencesScreen(store, onBack = nav::pop)
                    ProfilePage.LANGUAGE -> LanguageScreen(store, onBack = nav::pop)
                    ProfilePage.GOALS -> NutritionGoalsScreen(store, onBack = nav::pop)
                    ProfilePage.FASTING -> FastingScreen(store, onBack = nav::pop)
                    ProfilePage.REMINDERS -> TrackingRemindersScreen(store, platform, onWaterReminders = { nav.push(Route.Page(ProfilePage.WATER_REMINDERS)) }, onBack = nav::pop)
                    ProfilePage.WATER_REMINDERS -> WaterRemindersScreen(store, platform, onBack = nav::pop)
                    ProfilePage.REFERRAL -> ReferralScreen(store, account, summary, platform, onRules = { nav.push(Route.Page(ProfilePage.REFERRAL_RULES)) }, onBack = nav::pop)
                    ProfilePage.REFERRAL_RULES -> ReferralRulesScreen(onBack = nav::pop)
                    ProfilePage.BADGES -> BadgesScreen(store, onBack = nav::pop)
                    ProfilePage.REPORT -> ReportScreen(store, account, isPro, platform, onUpgrade = upgrade, onBack = nav::pop)
                    ProfilePage.TERMS -> LegalScreen(terms = true, onBack = nav::pop)
                    ProfilePage.PRIVACY -> LegalScreen(terms = false, onBack = nav::pop)
                    ProfilePage.ADMIN, ProfilePage.PRO, ProfilePage.ARTICLES -> LaunchedEffect(Unit) { nav.pop() }
                }
            }
        }
    }

    store.celebrations.firstOrNull()?.let { badge ->
        BadgeCelebration(badge) { store.celebrations.remove(badge) }
    }
    deleteError?.let { msg ->
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text(t("Account not deleted")) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { deleteError = null }) { Text(t("OK")) } },
        )
    }
}

/** Standard content padding for scrolling screens. */
val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

val today: String get() = Days.today()
