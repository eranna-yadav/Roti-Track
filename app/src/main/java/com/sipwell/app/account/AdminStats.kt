package com.sipwell.app.account

import kotlin.math.roundToInt

private const val DAY_MS = 86_400_000L

/** Business KPIs, derived from the user list. */
data class AdminStats(
    val total: Int,
    val pro: Int,
    val monthly: Int,
    val yearly: Int,
    val comp: Int,
    val mrr: Int,
    val new7d: Int,
    val active24h: Int,
    val blocked: Int,
) {
    companion object {
        fun of(users: List<UserSummary>, now: Long = System.currentTimeMillis()): AdminStats {
            val monthly = users.count { it.plan == Plan.MONTHLY }
            val yearly = users.count { it.plan == Plan.YEARLY }
            return AdminStats(
                total = users.size,
                pro = users.count { it.isPro },
                monthly = monthly,
                yearly = yearly,
                comp = users.count { it.compPro && it.plan == null },
                // Yearly revenue spread over twelve months.
                mrr = monthly * Plan.MONTHLY.rupees + yearly * Plan.YEARLY.rupees / 12,
                new7d = users.count { now - it.createdAt < 7 * DAY_MS },
                active24h = users.count { now - it.lastActive < DAY_MS },
                blocked = users.count { it.blocked },
            )
        }
    }
}

/** Savings of the yearly plan against twelve months of monthly, e.g. 68. */
val yearlySavingPercent: Int = ((1 - Plan.YEARLY.rupees / (Plan.MONTHLY.rupees * 12.0)) * 100).roundToInt()
