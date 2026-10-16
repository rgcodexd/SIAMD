package com.ogpika.siama.model

data class StipendCalculation(
    val internId: String,
    val internName: String,
    val monthName: String,
    val totalCalendarDays: Int = 30,
    val totalWorkingDays: Int = 22,
    val daysPresent: Int,
    val approvedLeaveDays: Double,
    val holidaysInMonth: Int,
    val lossOfPayDays: Double,
    val baseMonthlyStipend: Double = 15000.0,
    val dailyRate: Double = baseMonthlyStipend / totalWorkingDays.toDouble()
) {
    val deductionAmount: Double
        get() = lossOfPayDays * dailyRate

    val netPayableStipend: Double
        get() = (baseMonthlyStipend - deductionAmount).coerceAtLeast(0.0)

    val effectiveAttendancePercentage: Double
        get() {
            val creditedDays = daysPresent + approvedLeaveDays + holidaysInMonth
            return ((creditedDays / (totalWorkingDays + holidaysInMonth).toDouble()) * 100.0).coerceAtMost(100.0)
        }
}
