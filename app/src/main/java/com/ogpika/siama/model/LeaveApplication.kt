package com.ogpika.siama.model

import java.time.LocalDate

enum class LeaveStatus(val label: String) {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled")
}

data class LeaveApplication(
    val id: String,
    val internId: String,
    val internName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalDays: Double,
    val reason: String,
    val status: LeaveStatus = LeaveStatus.PENDING,
    val appliedDate: LocalDate = LocalDate.now(),
    val adminRemarks: String? = null
)
