package com.ogpika.siama.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ogpika.siama.model.LeavePolicy

@Entity(tableName = "leave_balances")
data class LeaveBalanceEntity(
    @PrimaryKey
    val internId: String,
    val totalSanctionedQuota: Double = LeavePolicy.SANCTIONED_LEAVES_ENTITLEMENT, // 12.0 days
    val usedLeaves: Double = 0.0,
    val pendingLeaves: Double = 0.0,
    val academicYear: Int = 2025
)

@Entity(tableName = "leave_applications")
data class LeaveApplicationEntity(
    @PrimaryKey
    val id: String,
    val internId: String,
    val internName: String,
    val startDate: String, // YYYY-MM-DD
    val endDate: String,   // YYYY-MM-DD
    val totalDays: Double,
    val reason: String,
    val status: String,    // PENDING, APPROVED, REJECTED, CANCELLED
    val appliedDate: String,
    val adminRemarks: String? = null
)

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey
    val id: String,
    val internId: String,
    val attendanceDate: String,
    val status: String, // PRESENT, ABSENT, ON_LEAVE, HOLIDAY
    val scanTime: String? = null,
    val qrSessionCode: String? = null
)
