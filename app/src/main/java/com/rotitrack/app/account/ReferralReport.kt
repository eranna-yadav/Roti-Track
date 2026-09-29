package com.rotitrack.app.account

/** A user who signed up with someone's code. [promoter] is null if no user holds that code. */
data class ReferralLink(val friend: UserSummary, val code: String, val promoter: UserSummary?)

/** Someone who shared their code, with the friends who signed up with it. */
data class Promoter(val user: UserSummary, val friends: List<UserSummary>)

/** Who referred whom, and how much the admin owes each promoter. Built from the user list. */
data class ReferralReport(
    val links: List<ReferralLink>,
    val promoters: List<Promoter>,
    /** ₹ earned by all promoters, paid out so far, and still to pay. */
    val earned: Int,
    val paid: Int,
    val due: Int,
    /** Referred friends whose payments have earned their promoter something. */
    val wentPro: Int,
) {
    /** Promoters with money waiting, largest amount first. */
    val payoutsDue: List<Promoter> get() = promoters.filter { it.user.referralDue > 0 }

    fun promoterOf(friend: UserSummary): UserSummary? = links.firstOrNull { it.friend.uid == friend.uid }?.promoter

    companion object {
        fun of(users: List<UserSummary>): ReferralReport {
            val byCode = users.filter { it.referralCode.isNotEmpty() }.associateBy { it.referralCode }
            val links = users
                .mapNotNull { u -> u.referredByCode?.takeIf { it.isNotEmpty() }?.let { ReferralLink(u, it, byCode[it]) } }
                .sortedByDescending { it.friend.createdAt }
            val friendsOf = links.filter { it.promoter != null }.groupBy { it.promoter!!.uid }
            val promoters = users
                .filter { it.referralEarnings > 0 || it.referralPaid > 0 || friendsOf.containsKey(it.uid) }
                .map { p -> Promoter(p, friendsOf[p.uid].orEmpty().map { it.friend }) }
                .sortedWith(compareByDescending<Promoter> { it.user.referralDue }.thenByDescending { it.user.referralEarnings }.thenByDescending { it.friends.size })
            return ReferralReport(
                links = links,
                promoters = promoters,
                earned = users.sumOf { it.referralEarnings },
                paid = users.sumOf { it.referralPaid },
                due = users.sumOf { it.referralDue },
                wentPro = links.count { it.friend.referralCreditedAmount > 0 },
            )
        }
    }
}
