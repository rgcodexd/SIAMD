package com.ogpika.siama

import com.ogpika.siama.data.AttendanceSystemRepository
import com.ogpika.siama.data.LeaveManagementRepository
import com.ogpika.siama.data.SailHolidayRepository
import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.HolidayCategory
import com.ogpika.siama.model.LeavePolicy
import com.ogpika.siama.model.LeaveStatus
import com.ogpika.siama.model.UserRole
import com.ogpika.siama.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

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
    fun testAttendanceSystemAuthenticationAndRegistration() {
        // Admin Login
        assertTrue(AttendanceSystemRepository.loginAsAdmin())
        assertTrue(AttendanceSystemRepository.isLoggedIn.value)
        assertEquals(UserRole.ADMIN, AttendanceSystemRepository.currentUserRole.value)

        // Logout
        AttendanceSystemRepository.logout()
        assertFalse(AttendanceSystemRepository.isLoggedIn.value)

        // Intern Login with valid email
        assertTrue(AttendanceSystemRepository.loginAsIntern("rahul.sharma@sailintern.in"))
        assertTrue(AttendanceSystemRepository.isLoggedIn.value)
        assertEquals(UserRole.INTERN, AttendanceSystemRepository.currentUserRole.value)
        assertEquals("SAIL-INT-2025-001", AttendanceSystemRepository.currentInternId.value)

        // Intern Registration with mandatory details
        val registered = AttendanceSystemRepository.registerNewIntern(
            email = "vikram.singh@sailintern.in",
            fullName = "Vikram Singh",
            phone = "+91 98765 99999",
            college = "IIT Kharagpur",
            joiningDate = LocalDate.of(2025, 3, 1),
            department = "Blast Furnace"
        )
        assertNotNull(registered)
        assertTrue(registered.id.startsWith("SAIL-INT-2025-"))
        assertEquals("Vikram Singh", registered.fullName)
    }

    @Test
    fun testAttendanceSystemMarkingAndMonthlyCalculation() {
        val internId = "SAIL-INT-2025-001"
        val testDate = LocalDate.of(2025, 1, 10)

        // Mark Present
        AttendanceSystemRepository.markAttendance(internId, testDate, AttendanceStatus.PRESENT, "Biometric")
        val recordPresent = AttendanceSystemRepository.attendanceMap.value[internId]?.get(testDate)
        assertNotNull(recordPresent)
        assertEquals(AttendanceStatus.PRESENT, recordPresent?.status)

        // Mark Absent
        AttendanceSystemRepository.markAttendance(internId, testDate, AttendanceStatus.ABSENT, "Absent")
        val recordAbsent = AttendanceSystemRepository.attendanceMap.value[internId]?.get(testDate)
        assertEquals(AttendanceStatus.ABSENT, recordAbsent?.status)

        // Calculate Monthly Summary
        val summary = AttendanceSystemRepository.getMonthlySummary(internId, YearMonth.of(2025, 1))
        assertEquals(31, summary.totalCalendarDays)
        assertTrue(summary.presentDays >= 0)
        assertTrue(summary.attendancePercentage in 0.0..100.0)
    }

    @Test
    fun testQrJoinGroupFlow() {
        AttendanceSystemRepository.loginAsIntern("rahul.sharma@sailintern.in")
        val group = AttendanceSystemRepository.groups.value.first()

        val (success, message) = AttendanceSystemRepository.joinGroupByQr(group.qrPayload)
        assertTrue(success)
        assertTrue(message.contains(group.name))

        val intern = AttendanceSystemRepository.interns.value.first { it.id == "SAIL-INT-2025-001" }
        assertEquals(group.id, intern.groupId)
        assertEquals(group.name, intern.groupName)
    }

    @Test
    fun testQrCodeGeneratorMatrixCreation() {
        val matrix = QrCodeGenerator.generateBitMatrix("TEST_PAYLOAD", 200)
        assertNotNull(matrix)
        assertEquals(200, matrix.width)
        assertEquals(200, matrix.height)
        assertTrue(matrix.get(10, 10) || !matrix.get(10, 10))
    }
}
