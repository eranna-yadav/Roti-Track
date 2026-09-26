package com.sipwell.app.account

import com.sipwell.app.store.AppStore

/** Everything behind the UI that differs between Firebase, device-only and the preview. */
class Services(
    val auth: AuthService,
    val directory: UserDirectory,
    val billing: Billing,
    /** Each account keeps its own log on the phone. */
    val storeFor: (uid: String) -> AppStore,
)

/** The activity fields the admin dashboard shows, built from the user's local data. */
fun summarize(account: Account, store: AppStore, plan: Plan?): UserSummary {
    val s = store.state
    return UserSummary(
        uid = account.uid,
        name = account.name,
        email = account.email,
        createdAt = System.currentTimeMillis(),
        lastActive = System.currentTimeMillis(),
        planId = plan?.productId,
        calorieGoal = s.profile.calorieGoal,
        waterGoalMl = s.profile.waterGoalMl,
        streak = store.streak(),
        daysLogged = (s.water.map { it.day } + s.meals.map { it.day }).toSet().size,
        diet = s.profile.diet.name.lowercase(),
    )
}
