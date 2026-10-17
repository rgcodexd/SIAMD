package com.ogpika.siama.model

import java.time.LocalDate

enum class UserRole(val displayName: String) {
    ADMIN("Admin / Maintainer"),
    INTERN("Intern / Trainee")
}

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    ON_LEAVE("On Leave"),
    HOLIDAY("Holiday")
}

data class InternProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val college: String,
    val joiningDate: LocalDate,
    val department: String = "Plant Automation & IT",
    val groupId: String? = null,
    val groupName: String? = null,
    val mentorName: String = "DGM Plant Operations",
    val address: String = "SAIL Intern Hostel, Sector 4",
    val emergencyContact: String = "+91 98765 43210"
)

data class InternGroup(
    val id: String,
    val name: String,
    val joiningDate: LocalDate,
    val department: String,
    val description: String = ""
) {
    val qrPayload: String
        get() = "SIAMD_JOIN_GROUP:$id:$name:${joiningDate}"
}

data class DailyAttendance(
    val id: String,
    val internId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
    val checkInTime: String? = null,
    val markedBy: String = "ADMIN",
    val remarks: String? = null
)

data class MonthlyAttendanceSummary(
    val internId: String,
    val internName: String,
    val monthYear: String,
    val totalCalendarDays: Int,
    val totalWorkingDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val leaveDays: Int,
    val holidayDays: Int
) {
    val attendancePercentage: Double
        get() = if (totalWorkingDays > 0) {
            ((presentDays.toDouble() + leaveDays.toDouble() + holidayDays.toDouble()) / totalWorkingDays.toDouble() * 100.0).coerceAtMost(100.0)
        } else 0.0
}
