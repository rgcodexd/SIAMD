package com.ogpika.siama.data

import com.ogpika.siama.model.LeaveApplication
import com.ogpika.siama.model.LeaveBalance
import com.ogpika.siama.model.LeavePolicy
import com.ogpika.siama.model.LeaveStatus
import com.ogpika.siama.model.StipendCalculation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

object LeaveManagementRepository {

    const val CURRENT_INTERN_ID = ""
    const val CURRENT_INTERN_NAME = ""
    const val CURRENT_INTERN_ROLL = ""
    const val CURRENT_INTERN_DEPT = ""

    private val _leaveBalance = MutableStateFlow(
        LeaveBalance(
            internId = "",
            totalSanctionedQuota = LeavePolicy.SANCTIONED_LEAVES_ENTITLEMENT, // 12 days
            usedLeaves = 0.0,
            pendingLeaves = 0.0
        )
    )
    val leaveBalance: StateFlow<LeaveBalance> = _leaveBalance.asStateFlow()

    private val _applications = MutableStateFlow<List<LeaveApplication>>(emptyList())
    val applications: StateFlow<List<LeaveApplication>> = _applications.asStateFlow()

    /**
     * Calculates working days between start and end date (excluding Sundays and official SAIL holidays)
     */
    fun calculateWorkingLeaveDays(start: LocalDate, end: LocalDate): Double {
        if (end.isBefore(start)) return 0.0
        var count = 0.0
        var current = start
        while (!current.isAfter(end)) {
            val isSunday = current.dayOfWeek == DayOfWeek.SUNDAY
            val isHoliday = SailHolidayRepository.isHoliday(current) != null
            if (!isSunday && !isHoliday) {
                count += 1.0
            }
            current = current.plusDays(1)
        }
        return count.coerceAtLeast(1.0) // At least 1 day if selected
    }

    /**
     * Submit a new leave application
     */
    fun submitApplication(
        startDate: LocalDate,
        endDate: LocalDate,
        reason: String
    ): Result<LeaveApplication> {
        val totalDays = calculateWorkingLeaveDays(startDate, endDate)
        val currentBalance = _leaveBalance.value

        val newApp = LeaveApplication(
            id = UUID.randomUUID().toString(),
            internId = CURRENT_INTERN_ID,
            internName = CURRENT_INTERN_NAME,
            startDate = startDate,
            endDate = endDate,
            totalDays = totalDays,
            reason = reason,
            status = LeaveStatus.PENDING,
            appliedDate = LocalDate.now(),
            adminRemarks = null
        )

        _applications.value = listOf(newApp) + _applications.value
        _leaveBalance.value = currentBalance.copy(
            pendingLeaves = currentBalance.pendingLeaves + totalDays
        )

        return Result.success(newApp)
    }

    /**
     * Admin approves leave application
     */
    fun approveApplication(applicationId: String, remarks: String) {
        val app = _applications.value.firstOrNull { it.id == applicationId } ?: return
        if (app.status != LeaveStatus.PENDING) return

        _applications.value = _applications.value.map {
            if (it.id == applicationId) {
                it.copy(status = LeaveStatus.APPROVED, adminRemarks = remarks.ifBlank { "Approved by Admin" })
            } else it
        }

        if (app.internId == CURRENT_INTERN_ID) {
            val balance = _leaveBalance.value
            _leaveBalance.value = balance.copy(
                usedLeaves = balance.usedLeaves + app.totalDays,
                pendingLeaves = (balance.pendingLeaves - app.totalDays).coerceAtLeast(0.0)
            )
        }
    }

    /**
     * Admin rejects leave application
     */
    fun rejectApplication(applicationId: String, remarks: String) {
        val app = _applications.value.firstOrNull { it.id == applicationId } ?: return
        if (app.status != LeaveStatus.PENDING) return

        _applications.value = _applications.value.map {
            if (it.id == applicationId) {
                it.copy(status = LeaveStatus.REJECTED, adminRemarks = remarks.ifBlank { "Rejected as per operational constraints" })
            } else it
        }

        if (app.internId == CURRENT_INTERN_ID) {
            val balance = _leaveBalance.value
            _leaveBalance.value = balance.copy(
                pendingLeaves = (balance.pendingLeaves - app.totalDays).coerceAtLeast(0.0)
            )
        }
    }

    /**
     * Computes current monthly stipend calculation considering leaves & holidays
     */
    fun computeMonthlyStipend(month: String = "May 2025"): StipendCalculation {
        val balance = _leaveBalance.value
        val lossOfPay = LeavePolicy.calculateLossOfPayDays(balance.usedLeaves)
        return StipendCalculation(
            internId = CURRENT_INTERN_ID,
            internName = CURRENT_INTERN_NAME,
            monthName = month,
            totalCalendarDays = 31,
            totalWorkingDays = 22,
            daysPresent = 18,
            approvedLeaveDays = balance.usedLeaves,
            holidaysInMonth = 2,
            lossOfPayDays = lossOfPay,
            baseMonthlyStipend = 15000.0
        )
    }
}
