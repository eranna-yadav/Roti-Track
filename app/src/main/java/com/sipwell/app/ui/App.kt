package com.sipwell.app.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sipwell.app.data.MealSlot
import com.sipwell.app.domain.Days
import com.sipwell.app.store.AppStore

/** What the Android layer provides to the UI. Keeps the screens platform-neutral. */
interface Platform {
    /** Asks for notification permission if needed, then reports whether reminders can be shown. */
    fun requestNotifications(onResult: (Boolean) -> Unit)
    fun scheduleReminders()
}

enum class Tab(val label: String, val icon: ImageVector) {
    WATER("Water", TabIcons.Water),
    FOOD("Food", TabIcons.Food),
    PLAN("Plan", TabIcons.Plan),
    HISTORY("History", TabIcons.History),
    ME("Me", TabIcons.Me),
}

sealed interface Route {
    data class Tabs(val tab: Tab) : Route
    data class AddFood(val slot: MealSlot, val day: String) : Route
    data object Articles : Route
    data class Article(val id: String) : Route
}

/** A tiny back stack; the Android activity wires the system back button to [pop]. */
class Navigator {
    val stack = mutableStateListOf<Route>(Route.Tabs(Tab.WATER))
    val current: Route get() = stack.last()
    val canPop: Boolean get() = stack.size > 1 || (current as? Route.Tabs)?.tab != Tab.WATER

    fun push(r: Route) { stack.add(r) }
    fun tab(t: Tab) {
        stack.clear()
        stack.add(Route.Tabs(t))
    }
    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else tab(Tab.WATER)
    }
}

@Composable
fun App(store: AppStore, platform: Platform, nav: Navigator = remember { Navigator() }) {
    SipwellTheme {
        if (!store.profile.onboarded) {
            OnboardingScreen(store, platform)
            return@SipwellTheme
        }
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
                        Tab.WATER -> WaterScreen(store)
                        Tab.FOOD -> FoodScreen(store, onAdd = { slot, day -> nav.push(Route.AddFood(slot, day)) })
                        Tab.PLAN -> PlanScreen(store, onOpenArticles = { nav.push(Route.Articles) }, onEditProfile = { nav.tab(Tab.ME) })
                        Tab.HISTORY -> HistoryScreen(store)
                        Tab.ME -> MeScreen(store, platform, onOpenArticles = { nav.push(Route.Articles) })
                    }
                    is Route.AddFood -> AddFoodScreen(store, route.slot, route.day, onBack = nav::pop)
                    Route.Articles -> ArticlesScreen(onOpen = { nav.push(Route.Article(it)) }, onBack = nav::pop)
                    is Route.Article -> ArticleScreen(route.id, onBack = nav::pop)
                }
            }
        }
    }
}

/** Standard content padding for scrolling screens. */
val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

val today: String get() = Days.today()
