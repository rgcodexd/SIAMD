package com.ogpika.siama.data

import com.ogpika.siama.data.remote.SupabaseClient
import com.ogpika.siama.data.remote.SupabaseConfig
import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.DailyAttendance
import com.ogpika.siama.model.InternGroup
import com.ogpika.siama.model.InternProfile
import com.ogpika.siama.model.MonthlyAttendanceSummary
import com.ogpika.siama.model.UserRole
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

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
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

    /**
     * Synchronize live data from Supabase if configured
     */
    fun syncWithSupabase() {
        if (!SupabaseConfig.isConfigured()) return

        repositoryScope.launch {
            val remoteInterns = SupabaseClient.fetchInternProfiles()
            if (remoteInterns.isNotEmpty()) {
                _interns.value = remoteInterns
            }

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

    // 5. Authentication Actions
    fun loginAsAdmin(): Boolean {
        _isLoggedIn.value = true
        _currentUserRole.value = UserRole.ADMIN
        _currentInternId.value = null
        syncWithSupabase()
        return true
    }

    fun loginAsIntern(email: String): Boolean {
        val cleanEmail = email.trim()
        val profile = _interns.value.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
        return if (profile != null) {
            _isLoggedIn.value = true
            _currentUserRole.value = UserRole.INTERN
            _currentInternId.value = profile.id
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
            groupId = null,
            groupName = null
        )

        _interns.value = _interns.value + newProfile
        _isLoggedIn.value = true
        _currentUserRole.value = UserRole.INTERN
        _currentInternId.value = newId

        // Sync with Supabase asynchronously if configured
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
}
