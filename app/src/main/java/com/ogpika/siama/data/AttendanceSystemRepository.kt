package com.ogpika.siama.data

import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.DailyAttendance
import com.ogpika.siama.model.InternGroup
import com.ogpika.siama.model.InternProfile
import com.ogpika.siama.model.MonthlyAttendanceSummary
import com.ogpika.siama.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object AttendanceSystemRepository {

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

    // 2. Groups / Books (Categorized by Joining Date)
    private val _groups = MutableStateFlow<List<InternGroup>>(
        listOf(
            InternGroup(
                id = "GRP-2025-JAN",
                name = "Batch 15-Jan-2025 (Blast Furnace)",
                joiningDate = LocalDate.of(2025, 1, 15),
                department = "Blast Furnace Operations",
                description = "Winter 2025 Mechanical & Metallurgy Interns"
            ),
            InternGroup(
                id = "GRP-2025-FEB",
                name = "Batch 01-Feb-2025 (IT & Automation)",
                joiningDate = LocalDate.of(2025, 2, 1),
                department = "Plant Automation & IT",
                description = "Systems & SCADA instrumentation trainees"
            ),
            InternGroup(
                id = "GRP-2025-MAR",
                name = "Batch 01-Mar-2025 (Steel Melting Shop)",
                joiningDate = LocalDate.of(2025, 3, 1),
                department = "Steel Melting Shop II",
                description = "Refractory & continuous casting project group"
            )
        )
    )
    val groups: StateFlow<List<InternGroup>> = _groups.asStateFlow()

    // 3. Intern Profiles
    private val _interns = MutableStateFlow<List<InternProfile>>(
        listOf(
            InternProfile(
                id = "SAIL-INT-2025-001",
                email = "rahul.sharma@sailintern.in",
                fullName = "Rahul Sharma",
                phone = "+91 98765 11001",
                college = "National Institute of Technology (NIT) Jamshedpur",
                joiningDate = LocalDate.of(2025, 1, 15),
                department = "Blast Furnace Operations",
                groupId = "GRP-2025-JAN",
                groupName = "Batch 15-Jan-2025 (Blast Furnace)",
                mentorName = "Shri R.K. Verma (DGM BF-1)",
                address = "SAIL Trainee Hostel Block B-12, Bokaro Steel City",
                emergencyContact = "+91 94311 02938"
            ),
            InternProfile(
                id = "SAIL-INT-2025-002",
                email = "priya.kumari@sailintern.in",
                fullName = "Priya Kumari",
                phone = "+91 98765 11002",
                college = "BIT Mesra, Ranchi",
                joiningDate = LocalDate.of(2025, 1, 15),
                department = "Blast Furnace Operations",
                groupId = "GRP-2025-JAN",
                groupName = "Batch 15-Jan-2025 (Blast Furnace)",
                mentorName = "Shri R.K. Verma (DGM BF-1)",
                address = "SAIL Trainee Hostel Block G-04, Bokaro Steel City",
                emergencyContact = "+91 94311 99281"
            ),
            InternProfile(
                id = "SAIL-INT-2025-003",
                email = "amit.patel@sailintern.in",
                fullName = "Amit Patel",
                phone = "+91 98765 11003",
                college = "IIT (ISM) Dhanbad",
                joiningDate = LocalDate.of(2025, 2, 1),
                department = "Plant Automation & IT",
                groupId = "GRP-2025-FEB",
                groupName = "Batch 01-Feb-2025 (IT & Automation)",
                mentorName = "Smt. S. Sengupta (Chief Manager IT)",
                address = "Hostel 3, Sector 4, Bokaro",
                emergencyContact = "+91 98350 44123"
            ),
            InternProfile(
                id = "SAIL-INT-2025-004",
                email = "sunita.soren@sailintern.in",
                fullName = "Sunita Soren",
                phone = "+91 98765 11004",
                college = "Birsa Institute of Technology Sindri",
                joiningDate = LocalDate.of(2025, 2, 1),
                department = "Plant Automation & IT",
                groupId = "GRP-2025-FEB",
                groupName = "Batch 01-Feb-2025 (IT & Automation)",
                mentorName = "Smt. S. Sengupta (Chief Manager IT)",
                address = "Sector 9D, Qtr 2034, Bokaro Steel City",
                emergencyContact = "+91 91234 56780"
            )
        )
    )
    val interns: StateFlow<List<InternProfile>> = _interns.asStateFlow()

    // 4. Daily Attendance Records: Map of internId to Map<LocalDate, DailyAttendance>
    private val _attendanceMap = MutableStateFlow<Map<String, Map<LocalDate, DailyAttendance>>>(
        generateInitialAttendanceRecords()
    )
    val attendanceMap: StateFlow<Map<String, Map<LocalDate, DailyAttendance>>> = _attendanceMap.asStateFlow()

    private fun generateInitialAttendanceRecords(): Map<String, Map<LocalDate, DailyAttendance>> {
        val result = mutableMapOf<String, MutableMap<LocalDate, DailyAttendance>>()
        val internIds = listOf("SAIL-INT-2025-001", "SAIL-INT-2025-002", "SAIL-INT-2025-003", "SAIL-INT-2025-004")

        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 5, 31)

        internIds.forEach { id ->
            val internMap = mutableMapOf<LocalDate, DailyAttendance>()
            var current = startDate
            var dayCount = 0

            while (!current.isAfter(endDate)) {
                val isHoliday = SailHolidayRepository.isHoliday(current) != null
                val isSunday = current.dayOfWeek == DayOfWeek.SUNDAY

                val status = when {
                    isHoliday -> AttendanceStatus.HOLIDAY
                    isSunday -> AttendanceStatus.HOLIDAY
                    // Seed realistic patterns: mostly Present, occasional Absent or Leave
                    (dayCount % 14 == 5) -> AttendanceStatus.ABSENT
                    (dayCount % 21 == 10) -> AttendanceStatus.ON_LEAVE
                    else -> AttendanceStatus.PRESENT
                }

                internMap[current] = DailyAttendance(
                    id = "$id-${current}",
                    internId = id,
                    date = current,
                    status = status,
                    checkInTime = if (status == AttendanceStatus.PRESENT) "09:0${(1..9).random()} AM" else null,
                    markedBy = if (status == AttendanceStatus.PRESENT) "QR_SCAN" else "ADMIN",
                    remarks = when (status) {
                        AttendanceStatus.HOLIDAY -> if (isHoliday) SailHolidayRepository.isHoliday(current)?.title else "Sunday Closed"
                        AttendanceStatus.ABSENT -> "Unexcused absence"
                        AttendanceStatus.ON_LEAVE -> "Sanctioned Leave approved by Mentor"
                        AttendanceStatus.PRESENT -> "Biometric / QR Verified"
                    }
                )

                current = current.plusDays(1)
                dayCount++
            }
            result[id] = internMap
        }
        return result
    }

    // 5. Authentication Actions
    fun loginAsAdmin(): Boolean {
        _isLoggedIn.value = true
        _currentUserRole.value = UserRole.ADMIN
        _currentInternId.value = null
        return true
    }

    fun loginAsIntern(email: String): Boolean {
        val profile = _interns.value.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
        return if (profile != null) {
            _isLoggedIn.value = true
            _currentUserRole.value = UserRole.INTERN
            _currentInternId.value = profile.id
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

        // Seed initial attendance map for this intern
        val newMap = _attendanceMap.value.toMutableMap()
        val internMap = mutableMapOf<LocalDate, DailyAttendance>()
        var cur = LocalDate.of(2025, 1, 1)
        while (!cur.isAfter(LocalDate.of(2025, 5, 31))) {
            val isHoliday = SailHolidayRepository.isHoliday(cur) != null
            val isSunday = cur.dayOfWeek == DayOfWeek.SUNDAY
            internMap[cur] = DailyAttendance(
                id = "$newId-$cur",
                internId = newId,
                date = cur,
                status = if (isHoliday || isSunday) AttendanceStatus.HOLIDAY else AttendanceStatus.PRESENT,
                checkInTime = "09:00 AM",
                markedBy = "ADMIN",
                remarks = "Registered Record"
            )
            cur = cur.plusDays(1)
        }
        newMap[newId] = internMap
        _attendanceMap.value = newMap

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

    // 8. Admin Management Actions
    fun addGroup(name: String, joiningDate: LocalDate, department: String, description: String): InternGroup {
        val newGroup = InternGroup(
            id = "GRP-${joiningDate.year}-${joiningDate.monthValue}-${_groups.value.size + 1}",
            name = name.trim(),
            joiningDate = joiningDate,
            department = department.trim(),
            description = description.trim()
        )
        _groups.value = _groups.value + newGroup
        return newGroup
    }

    fun deleteIntern(internId: String) {
        _interns.value = _interns.value.filterNot { it.id == internId }
        val newMap = _attendanceMap.value.toMutableMap()
        newMap.remove(internId)
        _attendanceMap.value = newMap
    }

    fun markAttendance(
        internId: String,
        date: LocalDate,
        status: AttendanceStatus,
        remarks: String? = null
    ) {
        val currentAll = _attendanceMap.value.toMutableMap()
        val internMap = (currentAll[internId] ?: emptyMap()).toMutableMap()

        internMap[date] = DailyAttendance(
            id = "$internId-$date",
            internId = internId,
            date = date,
            status = status,
            checkInTime = if (status == AttendanceStatus.PRESENT) "09:00 AM" else null,
            markedBy = "ADMIN",
            remarks = remarks ?: "Updated by Admin"
        )

        currentAll[internId] = internMap
        _attendanceMap.value = currentAll
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
