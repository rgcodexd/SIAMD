package com.ogpika.siama.data

import com.ogpika.siama.data.remote.SupabaseClient
import com.ogpika.siama.data.remote.SupabaseConfig
import com.ogpika.siama.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object AttendanceSystemRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // 1. Session State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole.asStateFlow()

    private val _currentInternId = MutableStateFlow<String?>(null)
    val currentInternId: StateFlow<String?> = _currentInternId.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _needsOnboarding = MutableStateFlow(false)
    val needsOnboarding: StateFlow<Boolean> = _needsOnboarding.asStateFlow()

    private val _currentActiveQrPayload = MutableStateFlow<AttendanceQrPayload?>(null)
    val currentActiveQrPayload: StateFlow<AttendanceQrPayload?> = _currentActiveQrPayload.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // 2b. Plant Departments (Created dynamically by Admin, no dummy data)
    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    fun addDepartment(name: String, code: String = "", description: String = "") {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val id = "DEPT-${System.currentTimeMillis() % 100000}"
        val dept = Department(id = id, name = cleanName, code = code.trim(), description = description.trim())
        _departments.value = _departments.value + dept
        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.insertDepartment(dept)
            }
        }
    }

    fun deleteDepartment(id: String) {
        _departments.value = _departments.value.filterNot { it.id == id }
        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.deleteDepartment(id)
            }
        }
    }

    // 2. Groups / Books (Clean state, no dummy records)
    private val _groups = MutableStateFlow<List<InternGroup>>(emptyList())
    val groups: StateFlow<List<InternGroup>> = _groups.asStateFlow()

    // 3. Intern Profiles (Clean state, no dummy records)
    private val _interns = MutableStateFlow<List<InternProfile>>(emptyList())
    val interns: StateFlow<List<InternProfile>> = _interns.asStateFlow()

    // 4. Daily Attendance Records (Clean state, no dummy records)
    private val _attendanceMap = MutableStateFlow<Map<String, Map<LocalDate, DailyAttendance>>>(emptyMap())
    val attendanceMap: StateFlow<Map<String, Map<LocalDate, DailyAttendance>>> = _attendanceMap.asStateFlow()

    // 5. Leave Applications
    private val _leaveApplications = MutableStateFlow<List<LeaveApplication>>(emptyList())
    val leaveApplications: StateFlow<List<LeaveApplication>> = _leaveApplications.asStateFlow()

    /**
     * Synchronize live data from Supabase if configured
     */
    fun syncWithSupabase() {
        if (!SupabaseConfig.isConfigured()) return

        repositoryScope.launch {
            val remoteDepts = SupabaseClient.fetchDepartments()
            _departments.value = remoteDepts

            val remoteInterns = SupabaseClient.fetchInternProfiles()
            _interns.value = remoteInterns

            val remoteGroups = SupabaseClient.fetchInternGroups()
            if (remoteGroups.isNotEmpty()) {
                _groups.value = remoteGroups
            }

            val currentMap = _attendanceMap.value.toMutableMap()
            for (intern in _interns.value) {
                val logs = SupabaseClient.fetchAttendanceLogs(intern.id)
                if (logs.isNotEmpty()) {
                    currentMap[intern.id] = logs
                }
            }
            _attendanceMap.value = currentMap
        }
    }

    // 6. Authentication Actions
    fun loginAsAdmin(email: String = "admin@sail.in", password: String = "admin123"): Boolean {
        val cleanEmail = email.trim()
        val isValid = cleanEmail.equals("admin@sail.in", ignoreCase = true) && password == "admin123"
        if (isValid) {
            _isLoggedIn.value = true
            _currentUserRole.value = UserRole.ADMIN
            _currentInternId.value = null
            _needsOnboarding.value = false
            syncWithSupabase()
            return true
        }
        return false
    }

    fun loginWithGoogle(email: String, displayName: String, onComplete: ((Boolean) -> Unit)? = null) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        repositoryScope.launch {
            // 1. Check if user profile already exists remotely in Supabase
            var existingProfile = if (SupabaseConfig.isConfigured()) {
                SupabaseClient.fetchProfileByEmail(cleanEmail)
            } else null

            // 2. Fallback to local memory cache
            if (existingProfile == null) {
                existingProfile = _interns.value.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
            }

            val profile: InternProfile
            val isNew = existingProfile == null

            if (existingProfile != null) {
                profile = existingProfile
                // Ensure local state contains this profile
                if (_interns.value.none { it.id == profile.id }) {
                    _interns.value = _interns.value + profile
                }
            } else {
                val count = _interns.value.size + 1
                val newId = "SAIL-INT-${LocalDate.now().year}-${String.format("%03d", count)}"
                val name = displayName.ifBlank { cleanEmail.substringBefore("@").replace(".", " ").capitalize() }
                profile = InternProfile(
                    id = newId,
                    email = cleanEmail,
                    fullName = name,
                    phone = "",
                    college = "",
                    joiningDate = LocalDate.now(),
                    department = "Plant Engineering",
                    internIdNumber = newId,
                    authProvider = "GOOGLE"
                )
                _interns.value = _interns.value + profile

                if (SupabaseConfig.isConfigured()) {
                    SupabaseClient.insertInternProfile(profile)
                }
            }

            _isLoggedIn.value = true
            _currentUserRole.value = UserRole.INTERN
            _currentInternId.value = profile.id

            // Check if mandatory onboarding is needed
            val complete = profile.isProfileComplete
            _needsOnboarding.value = !complete

            // Fetch attendance logs for this intern
            if (SupabaseConfig.isConfigured()) {
                val logs = SupabaseClient.fetchAttendanceLogs(profile.id)
                if (logs.isNotEmpty()) {
                    val current = _attendanceMap.value.toMutableMap()
                    current[profile.id] = logs
                    _attendanceMap.value = current
                }
            }

            onComplete?.invoke(true)
        }
    }

    fun loginAsIntern(email: String): Boolean {
        val cleanEmail = email.trim()
        val profile = _interns.value.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
        return if (profile != null) {
            _isLoggedIn.value = true
            _currentUserRole.value = UserRole.INTERN
            _currentInternId.value = profile.id
            _needsOnboarding.value = !profile.isProfileComplete

            if (SupabaseConfig.isConfigured()) {
                repositoryScope.launch {
                    val logs = SupabaseClient.fetchAttendanceLogs(profile.id)
                    if (logs.isNotEmpty()) {
                        val current = _attendanceMap.value.toMutableMap()
                        current[profile.id] = logs
                        _attendanceMap.value = current
                    }
                }
            }
            true
        } else {
            false
        }
    }

    fun completeOnboarding(
        fullName: String,
        department: String,
        internIdNumber: String,
        phone: String,
        college: String,
        emergencyContact: String,
        address: String
    ): Boolean {
        val currentId = _currentInternId.value ?: return false
        val currentProfile = _interns.value.firstOrNull { it.id == currentId }

        val targetDept = department.trim().ifBlank { "Plant Engineering" }

        val updated = currentProfile?.copy(
            fullName = fullName.trim(),
            department = targetDept,
            internIdNumber = internIdNumber.trim(),
            phone = phone.trim(),
            college = college.trim(),
            emergencyContact = emergencyContact.trim(),
            address = address.trim()
        ) ?: InternProfile(
            id = currentId,
            email = "",
            fullName = fullName.trim(),
            phone = phone.trim(),
            college = college.trim(),
            joiningDate = LocalDate.now(),
            department = targetDept,
            internIdNumber = internIdNumber.trim(),
            emergencyContact = emergencyContact.trim(),
            address = address.trim()
        )

        _interns.value = _interns.value.map {
            if (it.id == currentId) updated else it
        }

        _needsOnboarding.value = false

        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.insertInternProfile(updated)
                SupabaseClient.updateMandatoryProfile(
                    id = currentId,
                    email = updated.email,
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    college = college.trim(),
                    department = targetDept,
                    internIdNumber = internIdNumber.trim(),
                    emergencyContact = emergencyContact.trim(),
                    address = address.trim()
                )
                val refreshed = SupabaseClient.fetchInternProfiles()
                if (refreshed.isNotEmpty()) {
                    _interns.value = refreshed
                }
            }
        }

        return true
    }

    fun registerNewIntern(
        email: String,
        fullName: String,
        phone: String,
        college: String,
        joiningDate: LocalDate,
        department: String
    ): InternProfile {
        val count = _interns.value.size + 1
        val newId = "SAIL-INT-2025-${String.format("%03d", count)}"

        val newProfile = InternProfile(
            id = newId,
            email = email.trim(),
            fullName = fullName.trim(),
            phone = phone.trim(),
            college = college.trim(),
            joiningDate = joiningDate,
            department = department.trim(),
            internIdNumber = newId,
            groupId = null,
            groupName = null,
            authProvider = "EMAIL"
        )

        _interns.value = _interns.value + newProfile
        _isLoggedIn.value = true
        _currentUserRole.value = UserRole.INTERN
        _currentInternId.value = newId
        _needsOnboarding.value = false

        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.insertInternProfile(newProfile)
            }
        }

        return newProfile
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUserRole.value = null
        _currentInternId.value = null
        _needsOnboarding.value = false
    }

    // 7. Dynamic QR Generator & Verification Workflow
    fun generateDailyAttendanceQr(
        audience: QrTargetAudience = QrTargetAudience.ALL,
        targetValue: String = "ALL"
    ): AttendanceQrPayload {
        val token = UUID.randomUUID().toString().take(8).uppercase()
        val expiryTimestamp = System.currentTimeMillis() + (12 * 60 * 60 * 1000L) // 12 hours from now
        val payload = AttendanceQrPayload(
            date = LocalDate.now(),
            token = token,
            audience = audience,
            targetValue = targetValue,
            expiryTimestamp = expiryTimestamp
        )
        _currentActiveQrPayload.value = payload
        return payload
    }

    fun scanAndMarkAttendance(qrPayloadString: String): Pair<Boolean, String> {
        val internId = _currentInternId.value ?: return Pair(false, "No active intern logged in.")
        val intern = _interns.value.firstOrNull { it.id == internId }
            ?: return Pair(false, "Intern profile not found.")

        val payload = AttendanceQrPayload.parse(qrPayloadString)
            ?: return Pair(false, "Invalid QR Code format. Please scan the Admin's Official Daily Attendance QR.")

        // 1. Verify Date
        val today = LocalDate.now()
        if (payload.date != today) {
            return Pair(false, "This QR code is for ${payload.date}. Attendance can only be marked on the same date.")
        }

        // 2. Verify Target Audience
        when (payload.audience) {
            QrTargetAudience.ALL -> {
                // Eligible for all
            }
            QrTargetAudience.DEPARTMENT -> {
                if (!intern.department.equals(payload.targetValue, ignoreCase = true)) {
                    return Pair(false, "QR is restricted to ${payload.targetValue}. Your department is ${intern.department}.")
                }
            }
            QrTargetAudience.GROUP -> {
                if (!intern.groupId.equals(payload.targetValue, ignoreCase = true)) {
                    return Pair(false, "QR is restricted to Group ${payload.targetValue}.")
                }
            }
        }

        // 3. Check if already marked
        val existingToday = _attendanceMap.value[internId]?.get(today)
        if (existingToday?.status == AttendanceStatus.PRESENT) {
            return Pair(true, "You are already marked PRESENT today at ${existingToday.checkInTime ?: "09:00 AM"}.")
        }

        // 4. Mark Present
        val timeNow = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
        markAttendance(
            internId = internId,
            date = today,
            status = AttendanceStatus.PRESENT,
            remarks = "QR Verified • Check-in: $timeNow"
        )

        return Pair(true, "Attendance Confirmed! Marked PRESENT for ${intern.fullName} at $timeNow.")
    }

    // 6. Intern Actions (Self details edit only)
    fun updateInternProfile(
        phone: String,
        college: String,
        address: String,
        emergencyContact: String
    ) {
        val currentId = _currentInternId.value ?: return
        _interns.value = _interns.value.map {
            if (it.id == currentId) {
                it.copy(
                    phone = phone.trim(),
                    college = college.trim(),
                    address = address.trim(),
                    emergencyContact = emergencyContact.trim()
                )
            } else it
        }

        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.updateInternProfile(currentId, phone, college, address, emergencyContact)
            }
        }
    }

    // 7. Join Group via QR Code Scanning
    fun joinGroupByQr(qrPayload: String): Pair<Boolean, String> {
        val internId = _currentInternId.value ?: return Pair(false, "No active intern logged in.")
        // Expected format: SIAMD_JOIN_GROUP:groupId:groupName:joiningDate
        val parts = qrPayload.split(":")
        val groupId = if (parts.size >= 2) parts[1] else qrPayload.trim()

        val group = _groups.value.firstOrNull { it.id.equals(groupId, ignoreCase = true) }
            ?: return Pair(false, "Invalid QR Code or Group not found.")

        _interns.value = _interns.value.map {
            if (it.id == internId) {
                it.copy(groupId = group.id, groupName = group.name)
            } else it
        }

        return Pair(true, "Successfully joined ${group.name}!")
    }

    fun addGroup(name: String, joiningDate: LocalDate, department: String, description: String): InternGroup {
        val newGroup = InternGroup(
            id = "GRP-${joiningDate.year}-${String.format("%02d", joiningDate.monthValue)}-${_groups.value.size + 1}",
            name = name.trim(),
            joiningDate = joiningDate,
            department = department.trim(),
            description = description.trim()
        )
        _groups.value = _groups.value + newGroup
        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.insertInternGroup(newGroup)
            }
        }
        return newGroup
    }

    fun deleteIntern(internId: String) {
        _interns.value = _interns.value.filterNot { it.id == internId }
        val newMap = _attendanceMap.value.toMutableMap()
        newMap.remove(internId)
        _attendanceMap.value = newMap

        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.deleteInternProfile(internId)
            }
        }
    }

    fun markAttendance(
        internId: String,
        date: LocalDate,
        status: AttendanceStatus,
        remarks: String? = null
    ) {
        val currentAll = _attendanceMap.value.toMutableMap()
        val internMap = (currentAll[internId] ?: emptyMap()).toMutableMap()

        val record = DailyAttendance(
            id = "$internId-$date",
            internId = internId,
            date = date,
            status = status,
            checkInTime = if (status == AttendanceStatus.PRESENT) "09:00 AM" else null,
            markedBy = "ADMIN",
            remarks = remarks ?: "Updated by Admin"
        )
        internMap[date] = record
        currentAll[internId] = internMap
        _attendanceMap.value = currentAll

        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.upsertDailyAttendance(record)
            }
        }
    }

    // 9. Monthly Summary Generator
    fun getMonthlySummary(internId: String, yearMonth: YearMonth): MonthlyAttendanceSummary {
        val intern = _interns.value.firstOrNull { it.id == internId }
        val internRecords = _attendanceMap.value[internId] ?: emptyMap()

        val daysInMonth = yearMonth.lengthOfMonth()
        var present = 0
        var absent = 0
        var leave = 0
        var holiday = 0
        var workingDays = 0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val record = internRecords[date]
            val status = record?.status ?: AttendanceStatus.ABSENT

            val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
            val isOfficialHoliday = SailHolidayRepository.isHoliday(date) != null

            if (!isSunday && !isOfficialHoliday) {
                workingDays++
            }

            when (status) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.ABSENT -> absent++
                AttendanceStatus.ON_LEAVE -> leave++
                AttendanceStatus.HOLIDAY -> holiday++
            }
        }

        return MonthlyAttendanceSummary(
            internId = internId,
            internName = intern?.fullName ?: "Intern",
            monthYear = yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
            totalCalendarDays = daysInMonth,
            totalWorkingDays = workingDays,
            presentDays = present,
            absentDays = absent,
            leaveDays = leave,
            holidayDays = holiday
        )
    }

    fun getRecordsForMonth(internId: String, yearMonth: YearMonth): List<DailyAttendance> {
        val internRecords = _attendanceMap.value[internId] ?: emptyMap()
        val daysInMonth = yearMonth.lengthOfMonth()
        val list = mutableListOf<DailyAttendance>()

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val record = internRecords[date] ?: DailyAttendance(
                id = "$internId-$date",
                internId = internId,
                date = date,
                status = AttendanceStatus.ABSENT
            )
            list.add(record)
        }
        return list
    }

    // 10. Leave Applications Module
    fun applyForLeave(startDate: LocalDate, endDate: LocalDate, reason: String): Pair<Boolean, String> {
        val internId = _currentInternId.value ?: return Pair(false, "No active intern logged in.")
        val intern = _interns.value.firstOrNull { it.id == internId } ?: return Pair(false, "Intern not found.")

        if (endDate.isBefore(startDate)) {
            return Pair(false, "End date cannot be before start date.")
        }

        var workingDays = 0.0
        var cur = startDate
        while (!cur.isAfter(endDate)) {
            val isSunday = cur.dayOfWeek == DayOfWeek.SUNDAY
            val isHoliday = SailHolidayRepository.isHoliday(cur) != null
            if (!isSunday && !isHoliday) {
                workingDays += 1.0
            }
            cur = cur.plusDays(1)
        }

        if (workingDays <= 0.0) {
            return Pair(false, "Selected dates contain only Sundays or official holidays.")
        }

        val internRecords = _attendanceMap.value[internId] ?: emptyMap()
        val usedDays = internRecords.values.count { it.status == AttendanceStatus.ON_LEAVE }.toDouble()
        val remainingQuota = (LeavePolicy.SANCTIONED_LEAVES_ENTITLEMENT - usedDays).coerceAtLeast(0.0)

        val newApp = LeaveApplication(
            id = "LEAVE-${System.currentTimeMillis()}",
            internId = internId,
            internName = intern.fullName,
            startDate = startDate,
            endDate = endDate,
            totalDays = workingDays,
            reason = reason.trim(),
            status = LeaveStatus.APPROVED
        )

        _leaveApplications.value = _leaveApplications.value + newApp

        cur = startDate
        while (!cur.isAfter(endDate)) {
            val isSunday = cur.dayOfWeek == DayOfWeek.SUNDAY
            val isHoliday = SailHolidayRepository.isHoliday(cur) != null
            if (!isSunday && !isHoliday) {
                markAttendance(internId, cur, AttendanceStatus.ON_LEAVE, "Leave: ${reason.take(20)}")
            }
            cur = cur.plusDays(1)
        }

        if (SupabaseConfig.isConfigured()) {
            repositoryScope.launch {
                SupabaseClient.submitLeaveApplication(
                    applicationId = newApp.id,
                    internId = internId,
                    startDate = startDate,
                    endDate = endDate,
                    totalDays = workingDays,
                    reason = reason
                )
            }
        }

        val quotaMsg = if (workingDays <= remainingQuota) {
            "Approved within 12-day quota (${remainingQuota - workingDays} days remaining)."
        } else {
            "Approved (Exceeds 12-day quota - Loss of Pay applicable)."
        }

        return Pair(true, "Leave granted! $quotaMsg")
    }

    // 11. Automated Absenteeism (Daily Cutoff Check)
    fun autoMarkAbsenteeism(upToDate: LocalDate = LocalDate.now().minusDays(1)) {
        val currentAll = _attendanceMap.value.toMutableMap()
        for (intern in _interns.value) {
            val internMap = (currentAll[intern.id] ?: emptyMap()).toMutableMap()
            var cur = intern.joiningDate
            while (!cur.isAfter(upToDate)) {
                val isSunday = cur.dayOfWeek == DayOfWeek.SUNDAY
                val isHoliday = SailHolidayRepository.isHoliday(cur) != null
                if (!isSunday && !isHoliday && !internMap.containsKey(cur)) {
                    val absentRecord = DailyAttendance(
                        id = "${intern.id}-$cur",
                        internId = intern.id,
                        date = cur,
                        status = AttendanceStatus.ABSENT,
                        checkInTime = null,
                        markedBy = "SYSTEM_SCHEDULER",
                        remarks = "Auto-marked Absent (Daily Cutoff)"
                    )
                    internMap[cur] = absentRecord
                    if (SupabaseConfig.isConfigured()) {
                        repositoryScope.launch {
                            SupabaseClient.upsertDailyAttendance(absentRecord)
                        }
                    }
                }
                cur = cur.plusDays(1)
            }
            currentAll[intern.id] = internMap
        }
        _attendanceMap.value = currentAll
    }
}
