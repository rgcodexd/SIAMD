package com.ogpika.siama.model

/**
 * Encapsulates the official SAIL policy for intern leave and holiday entitlements:
 *
 * "The following leave and holidays hereinunder shall be admissible to the selected
 * interns during their internship training period:-
 * a. Sanctioned leaves :- 12 (Twelve) days
 * b. National Holiday Leaves :- 03 (Three) days
 * c. SAIL Foundation Day Closed Holiday :- 01 (one)
 * d. Festival Holidays (FHs) :- 06 (Six)
 *    [(Calendar year 2025 :- Holi, Eid-ul-Fitar, Dussehra, Diwali, Gurunank Jayanti) and
 * e. Ambedkar Jayanti Holiday: - 01 (One) following the declaration by the government"
 */
object LeavePolicy {
    const val SANCTIONED_LEAVES_ENTITLEMENT: Double = 12.0
    const val NATIONAL_HOLIDAYS_COUNT: Int = 3
    const val SAIL_FOUNDATION_DAY_COUNT: Int = 1
    const val FESTIVAL_HOLIDAYS_COUNT: Int = 6
    const val AMBEDKAR_JAYANTI_COUNT: Int = 1

    const val TOTAL_ADMISSIBLE_HOLIDAYS: Int =
        NATIONAL_HOLIDAYS_COUNT +
        SAIL_FOUNDATION_DAY_COUNT +
        FESTIVAL_HOLIDAYS_COUNT +
        AMBEDKAR_JAYANTI_COUNT // 11 days

    const val TOTAL_ADMISSIBLE_LEAVE_AND_HOLIDAYS: Double =
        SANCTIONED_LEAVES_ENTITLEMENT + TOTAL_ADMISSIBLE_HOLIDAYS // 23 days

    /**
     * Computes loss-of-pay (LOP) days if sanctioned leaves exceed quota
     */
    fun calculateLossOfPayDays(totalApprovedLeavesTaken: Double): Double {
        return if (totalApprovedLeavesTaken > SANCTIONED_LEAVES_ENTITLEMENT) {
            totalApprovedLeavesTaken - SANCTIONED_LEAVES_ENTITLEMENT
        } else {
            0.0
        }
    }
}
