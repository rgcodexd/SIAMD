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
    val department: String = "",
    val groupId: String? = null,
    val groupName: String? = null,
    val internIdNumber: String? = null,
    val mentorName: String = "DGM Plant Operations",
    val address: String = "",
    val emergencyContact: String = "",
    val authProvider: String = "GOOGLE" // "GOOGLE" or "EMAIL"
) {
    val isProfileComplete: Boolean
        get() = fullName.isNotBlank() &&
                phone.isNotBlank() &&
                college.isNotBlank() &&
                department.isNotBlank() &&
                !internIdNumber.isNullOrBlank()
}

/**
 * Plant Department model dynamically created and managed by the Admin
 */
data class Department(
    val id: String,
    val name: String,
    val code: String = "",
    val description: String = ""
)

enum class QrTargetAudience(val label: String) {
    ALL("All Interns"),
    DEPARTMENT("Specific Department"),
    GROUP("Specific Book Group")
}

data class AttendanceQrPayload(
    val date: LocalDate,
    val token: String,
    val audience: QrTargetAudience,
    val targetValue: String,
    val expiryTimestamp: Long
) {
    fun toSerializedString(): String {
        return "SAIL_ATTENDANCE_QR:${date}:${token}:${audience.name}:${targetValue}:${expiryTimestamp}"
    }

    companion object {
        fun parse(payload: String): AttendanceQrPayload? {
            if (!payload.startsWith("SAIL_ATTENDANCE_QR:")) return null
            val parts = payload.split(":")
            if (parts.size < 6) return null
            return try {
                val date = LocalDate.parse(parts[1])
                val token = parts[2]
                val audience = QrTargetAudience.valueOf(parts[3])
                val targetValue = parts[4]
                val expiryTimestamp = parts[5].toLong()
                AttendanceQrPayload(date, token, audience, targetValue, expiryTimestamp)
            } catch (e: Exception) {
                null
            }
        }
    }
}

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
