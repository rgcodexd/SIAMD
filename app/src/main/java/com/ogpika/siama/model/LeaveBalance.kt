package com.ogpika.siama.model

data class LeaveBalance(
    val internId: String,
    val totalSanctionedQuota: Double = LeavePolicy.SANCTIONED_LEAVES_ENTITLEMENT, // 12.0
    val usedLeaves: Double = 0.0,
    val pendingLeaves: Double = 0.0
) {
    val remainingLeaves: Double
        get() = (totalSanctionedQuota - usedLeaves).coerceAtLeast(0.0)

    val isQuotaExceeded: Boolean
        get() = usedLeaves > totalSanctionedQuota

    val excessLeaves: Double
        get() = if (usedLeaves > totalSanctionedQuota) usedLeaves - totalSanctionedQuota else 0.0
}
