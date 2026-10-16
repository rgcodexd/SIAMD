package com.ogpika.siama

import com.ogpika.siama.data.LeaveManagementRepository
import com.ogpika.siama.data.SailHolidayRepository
import com.ogpika.siama.model.HolidayCategory
import com.ogpika.siama.model.LeavePolicy
import com.ogpika.siama.model.LeaveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LeaveAndHolidayPolicyTest {

    @Test
    fun testSailPolicyQuotaConstants() {
        assertEquals(12.0, LeavePolicy.SANCTIONED_LEAVES_ENTITLEMENT, 0.0)
        assertEquals(3, LeavePolicy.NATIONAL_HOLIDAYS_COUNT)
        assertEquals(1, LeavePolicy.SAIL_FOUNDATION_DAY_COUNT)
        assertEquals(6, LeavePolicy.FESTIVAL_HOLIDAYS_COUNT)
        assertEquals(1, LeavePolicy.AMBEDKAR_JAYANTI_COUNT)
        assertEquals(11, LeavePolicy.TOTAL_ADMISSIBLE_HOLIDAYS)
        assertEquals(23.0, LeavePolicy.TOTAL_ADMISSIBLE_LEAVE_AND_HOLIDAYS, 0.0)
    }

    @Test
    fun testOfficialHolidaysListCountsAndCategories() {
        val holidays = SailHolidayRepository.officialHolidays2025
        assertEquals(11, holidays.size)

        val nationalHolidays = holidays.filter { it.category == HolidayCategory.NATIONAL_HOLIDAY }
        assertEquals(3, nationalHolidays.size)
        assertTrue(nationalHolidays.any { it.title.contains("Republic") })
        assertTrue(nationalHolidays.any { it.title.contains("Independence") })
        assertTrue(nationalHolidays.any { it.title.contains("Gandhi") })

        val sailDay = holidays.filter { it.category == HolidayCategory.SAIL_FOUNDATION_DAY }
        assertEquals(1, sailDay.size)
        assertEquals(LocalDate.of(2025, 1, 24), sailDay.first().date)

        val festivalHolidays = holidays.filter { it.category == HolidayCategory.FESTIVAL_HOLIDAY }
        assertEquals(6, festivalHolidays.size)
        assertTrue(festivalHolidays.any { it.title.contains("Holi") })
        assertTrue(festivalHolidays.any { it.title.contains("Eid") })
        assertTrue(festivalHolidays.any { it.title.contains("Dussehra") })
        assertTrue(festivalHolidays.any { it.title.contains("Diwali") })
        assertTrue(festivalHolidays.any { it.title.contains("Guru Nanak") })

        val ambedkarDay = holidays.filter { it.category == HolidayCategory.AMBEDKAR_JAYANTI }
        assertEquals(1, ambedkarDay.size)
        assertEquals(LocalDate.of(2025, 4, 14), ambedkarDay.first().date)
    }

    @Test
    fun testLossOfPayCalculation() {
        // Within 12 days quota -> 0 LOP
        assertEquals(0.0, LeavePolicy.calculateLossOfPayDays(0.0), 0.0)
        assertEquals(0.0, LeavePolicy.calculateLossOfPayDays(8.0), 0.0)
        assertEquals(0.0, LeavePolicy.calculateLossOfPayDays(12.0), 0.0)

        // Exceeding 12 days quota -> LOP deduction
        assertEquals(1.0, LeavePolicy.calculateLossOfPayDays(13.0), 0.0)
        assertEquals(3.5, LeavePolicy.calculateLossOfPayDays(15.5), 0.0)
    }

    @Test
    fun testWorkingDaysExcludesOfficialHolidaysAndSundays() {
        // Republic day (2025-01-26 is a Sunday)
        // SAIL Foundation Day: 2025-01-24 is a Friday (Closed Holiday)
        val holiday = SailHolidayRepository.isHoliday(LocalDate.of(2025, 1, 24))
        assertNotNull(holiday)

        // Span from Jan 23 (Thu) to Jan 25 (Sat) -> Jan 24 is holiday, so 2 working days
        val workingDays = LeaveManagementRepository.calculateWorkingLeaveDays(
            LocalDate.of(2025, 1, 23),
            LocalDate.of(2025, 1, 25)
        )
        assertEquals(2.0, workingDays, 0.0)
    }

    @Test
    fun testLeaveSubmissionAndApprovalFlow() {
        val initialBalance = LeaveManagementRepository.leaveBalance.value
        val initialUsed = initialBalance.usedLeaves
        val initialPending = initialBalance.pendingLeaves

        val submitResult = LeaveManagementRepository.submitApplication(
            startDate = LocalDate.of(2025, 6, 2),
            endDate = LocalDate.of(2025, 6, 3),
            reason = "Test unit application"
        )
        assertTrue(submitResult.isSuccess)
        val app = submitResult.getOrThrow()
        assertEquals(LeaveStatus.PENDING, app.status)

        val balanceAfterSubmit = LeaveManagementRepository.leaveBalance.value
        assertEquals(initialPending + app.totalDays, balanceAfterSubmit.pendingLeaves, 0.0)

        // Approve
        LeaveManagementRepository.approveApplication(app.id, "Approved in test")
        val balanceAfterApprove = LeaveManagementRepository.leaveBalance.value
        assertEquals(initialUsed + app.totalDays, balanceAfterApprove.usedLeaves, 0.0)
    }

    @Test
    fun testStipendCalculationWithinQuota() {
        val stipend = LeaveManagementRepository.computeMonthlyStipend("May 2025")
        assertTrue(stipend.netPayableStipend > 0.0)
        assertEquals(15000.0, stipend.baseMonthlyStipend, 0.0)
    }
}
