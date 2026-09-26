package com.rotitrack.app.ui

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
import com.rotitrack.app.domain.Days

/** What the Android layer provides to the UI. Keeps the screens platform-neutral. */
interface Platform {
    /** Asks for notification permission if needed, then reports whether reminders can be shown. */
    fun requestNotifications(onResult: (Boolean) -> Unit)
    fun scheduleReminders(profile: Profile)
    /** Called when someone signs in (uid) or out (null), so background work follows the right account. */
    fun sessionChanged(uid: String?)
    fun openUrl(url: String)
}

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", TabIcons.Home),
    WATER("Water", TabIcons.Water),
    FOOD("Food", TabIcons.Food),
    PLAN("Plan", TabIcons.Plan),
    ME("Me", TabIcons.Me),
}

sealed interface Route {
    data class Tabs(val tab: Tab) : Route
    data class AddFood(val slot: MealSlot, val day: String) : Route
    data object Articles : Route
    data class Article(val id: String) : Route
    data object Pro : Route
    data object Admin : Route
    data class AdminUser(val uid: String) : Route
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
    RotiTrackTheme {
        val account = services.auth.account
        if (account == null) {
            LoginScreen(services.auth)
            return@RotiTrackTheme
        }
        val store = remember(account.uid) { services.storeFor(account.uid) }
        var summary by remember(account.uid) { mutableStateOf<UserSummary?>(null) }
        LaunchedEffect(account.uid) {
            platform.sessionChanged(account.uid)
            summary = runCatching { services.directory.get(account.uid) }.getOrNull()
        }
        // Publish activity for the admin dashboard, debounced: any change restarts the wait.
        val plan = services.billing.activePlan
        LaunchedEffect(account.uid, store.state, plan) {
            delay(2_000)
            runCatching { services.directory.publish(summarize(account, store, plan)) }
        }

        if (summary?.blocked == true) {
            BlockedScreen(onSignOut = {
                platform.sessionChanged(null)
                services.auth.signOut()
            })
            return@RotiTrackTheme
        }
        if (!store.profile.onboarded) {
            LaunchedEffect(account.uid) { if (store.profile.name.isBlank()) store.updateProfile { it.copy(name = account.name) } }
            OnboardingScreen(store, platform)
            return@RotiTrackTheme
        }

        val isPro = plan != null || summary?.compPro == true
        val isAdmin = account.isAdmin || summary?.isAdmin == true
        val upgrade = { nav.push(Route.Pro) }
        val route = nav.current
        Scaffold(
            containerColor = Palette.background,
            bottomBar = {
                if (route is Route.Tabs) {
                    NavigationBar(containerColor = Color.White) {
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
                        Tab.WATER -> WaterScreen(store)
                        Tab.FOOD -> FoodScreen(store, onAdd = { slot, day -> nav.push(Route.AddFood(slot, day)) })
                        Tab.PLAN -> PlanScreen(store, isPro, onUpgrade = upgrade, onOpenArticles = { nav.push(Route.Articles) }, onEditProfile = { nav.tab(Tab.ME) })
                        Tab.ME -> MeScreen(
                            store, platform, account, isPro, isAdmin,
                            onOpenArticles = { nav.push(Route.Articles) },
                            onUpgrade = upgrade,
                            onAdmin = { nav.push(Route.Admin) },
                            onSignOut = {
                                platform.sessionChanged(null)
                                nav.tab(Tab.HOME)
                                services.auth.signOut()
                            },
                        )
                    }
                    is Route.AddFood -> AddFoodScreen(store, route.slot, route.day, isPro, onUpgrade = upgrade, onBack = nav::pop)
                    Route.Articles -> ArticlesScreen(onOpen = { nav.push(Route.Article(it)) }, onBack = nav::pop)
                    is Route.Article -> ArticleScreen(route.id, onBack = nav::pop)
                    Route.Pro -> ProScreen(services.billing, compPro = summary?.compPro == true, platform, onBack = nav::pop)
                    Route.Admin -> if (isAdmin) AdminScreen(services.directory, onOpen = { nav.push(Route.AdminUser(it)) }, onBack = nav::pop) else LaunchedEffect(Unit) { nav.pop() }
                    is Route.AdminUser -> if (isAdmin) AdminUserScreen(services.directory, route.uid, onBack = nav::pop) else LaunchedEffect(Unit) { nav.pop() }
                }
            }
        }
    }
}

/** Standard content padding for scrolling screens. */
val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

val today: String get() = Days.today()
