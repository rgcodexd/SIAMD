package com.ogpika.siama.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ogpika.siama.data.AttendanceSystemRepository
import com.ogpika.siama.model.AttendanceQrPayload
import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.DailyAttendance
import com.ogpika.siama.model.InternGroup
import com.ogpika.siama.model.InternProfile
import com.ogpika.siama.model.QrTargetAudience
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.AlertDialog
import com.ogpika.siama.model.Department
import com.ogpika.siama.ui.components.AttendanceCalendarView
import com.ogpika.siama.ui.theme.StatusAbsent
import com.ogpika.siama.ui.theme.StatusHoliday
import com.ogpika.siama.ui.theme.StatusLeave
import com.ogpika.siama.ui.theme.StatusPresent
import com.ogpika.siama.util.QrCodeGenerator
import com.ogpika.siama.util.ReportExportManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun AdminPortalScreen(
    modifier: Modifier = Modifier
) {
    val interns by AttendanceSystemRepository.interns.collectAsState()
    val groups by AttendanceSystemRepository.groups.collectAsState()
    val attendanceMap by AttendanceSystemRepository.attendanceMap.collectAsState()
    val departments by AttendanceSystemRepository.departments.collectAsState()

    var showAddDeptDialog by remember { mutableStateOf(false) }
    var deptToDelete by remember { mutableStateOf<Department?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Interns, 1 = Resource Books & Groups, 2 = Daily Marking
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Modals
    var showDynamicQrDialog by remember { mutableStateOf(false) }
    var showAddInternDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var inspectInternForCalendar by remember { mutableStateOf<InternProfile?>(null) }
    var internForAttendanceMarking by remember { mutableStateOf<Pair<InternProfile, LocalDate>?>(null) }
    var groupForQrDisplay by remember { mutableStateOf<InternGroup?>(null) }
    var internToDelete by remember { mutableStateOf<InternProfile?>(null) }

    // Categorical Navigation Filters
    var selectedCategoryType by remember { mutableStateOf("DEPARTMENTS") } // "DEPARTMENTS" or "BOOKS"
    var selectedDepartmentFilter by remember { mutableStateOf<String?>(null) }
    var selectedBookFilter by remember { mutableStateOf<String?>(null) }

    val filteredInterns = remember(interns, selectedDepartmentFilter, selectedBookFilter) {
        interns.filter { intern ->
            val matchDept = selectedDepartmentFilter == null || intern.department.equals(selectedDepartmentFilter, ignoreCase = true)
            val matchBook = selectedBookFilter == null || intern.groupId == selectedBookFilter
            matchDept && matchBook
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Fixed bottom-corner Floating Action Button to launch Dynamic QR Attendance Generator
            ExtendedFloatingActionButton(
                onClick = { showDynamicQrDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Dynamic Attendance QR",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Daily QR Generator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Admin Primary Tabs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Interns (${interns.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resource Books (${groups.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Quick Mark", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Departments (${departments.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            // Categorical Navigation Bar (Departments & Resource Books)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Categorical Navigation:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Category Mode Switch
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedCategoryType == "DEPARTMENTS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCategoryType = "DEPARTMENTS" }
                            ) {
                                Text(
                                    text = "Departments",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategoryType == "DEPARTMENTS") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedCategoryType == "BOOKS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCategoryType = "BOOKS" }
                            ) {
                                Text(
                                    text = "Resource Books",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategoryType == "BOOKS") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedCategoryType == "DEPARTMENTS") {
                        // Horizontal scrolling Department Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedDepartmentFilter == null,
                                    onClick = { selectedDepartmentFilter = null },
                                    label = { Text("All Departments (${interns.size})", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                            items(departments) { dept ->
                                val count = interns.count { it.department.equals(dept.name, ignoreCase = true) }
                                FilterChip(
                                    selected = selectedDepartmentFilter == dept.name,
                                    onClick = {
                                        selectedDepartmentFilter = if (selectedDepartmentFilter == dept.name) null else dept.name
                                    },
                                    label = { Text("${dept.name} ($count)", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    } else {
                        // Resource Books Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedBookFilter == null,
                                    onClick = { selectedBookFilter = null },
                                    label = { Text("All Books / Batches (${groups.size})", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                            items(groups) { group ->
                                val count = interns.count { it.groupId == group.id }
                                FilterChip(
                                    selected = selectedBookFilter == group.id,
                                    onClick = {
                                        selectedBookFilter = if (selectedBookFilter == group.id) null else group.id
                                    },
                                    label = { Text("${group.name.take(16)} ($count)", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Interns List & Reports
                    AdminInternsListSection(
                        interns = filteredInterns,
                        totalInternCount = interns.size,
                        groups = groups,
                        attendanceMap = attendanceMap,
                        activeDepartmentFilter = selectedDepartmentFilter,
                        onAddInternClick = { showAddInternDialog = true },
                        onInspectCalendar = { inspectInternForCalendar = it },
                        onMarkAttendance = { intern, date ->
                            internForAttendanceMarking = Pair(intern, date)
                        },
                        onExportPdf = { intern ->
                            val currentMonth = YearMonth.of(2025, 1)
                            val summary = AttendanceSystemRepository.getMonthlySummary(intern.id, currentMonth)
                            val records = AttendanceSystemRepository.getRecordsForMonth(intern.id, currentMonth)
                            val pdfFile = ReportExportManager.generatePdfReport(context, intern, summary, records)
                            if (pdfFile != null) {
                                ReportExportManager.shareReportFile(context, pdfFile, "application/pdf", "Attendance PDF - ${intern.fullName}")
                                scope.launch { snackbarHostState.showSnackbar("PDF Report ready: ${pdfFile.name}") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error generating PDF report.") }
                            }
                        },
                        onExportExcel = { intern ->
                            val currentMonth = YearMonth.of(2025, 1)
                            val summary = AttendanceSystemRepository.getMonthlySummary(intern.id, currentMonth)
                            val records = AttendanceSystemRepository.getRecordsForMonth(intern.id, currentMonth)
                            val csvFile = ReportExportManager.generateExcelReport(context, intern, summary, records)
                            if (csvFile != null) {
                                ReportExportManager.shareReportFile(context, csvFile, "text/csv", "Attendance Excel - ${intern.fullName}")
                                scope.launch { snackbarHostState.showSnackbar("Excel/CSV Report ready: ${csvFile.name}") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error generating Excel report.") }
                            }
                        },
                        onDeleteIntern = { internToDelete = it }
                    )
                }
                1 -> {
                    // TAB 1: Resource Books / Groups & Share
                    AdminGroupsSection(
                        groups = groups,
                        interns = interns,
                        onAddGroupClick = { showAddGroupDialog = true },
                        onShowQr = { groupForQrDisplay = it },
                        onShareWhatsApp = { group ->
                            ReportExportManager.shareGroupViaWhatsApp(
                                context = context,
                                groupName = group.name,
                                joiningDate = group.joiningDate,
                                qrPayload = group.qrPayload
                            )
                        }
                    )
                }
                2 -> {
                    // TAB 2: Quick Daily Attendance Batch Marker
                    AdminQuickDailyMarkSection(
                        interns = filteredInterns,
                        attendanceMap = attendanceMap,
                        onMarkStatus = { internId, date, status ->
                            AttendanceSystemRepository.markAttendance(internId, date, status)
                            scope.launch {
                                snackbarHostState.showSnackbar("Updated attendance for $internId")
                            }
                        }
                    )
                }
                3 -> {
                    // TAB 3: Dynamic Plant Departments Management
                    AdminDepartmentsSection(
                        departments = departments,
                        interns = interns,
                        onAddDepartmentClick = { showAddDeptDialog = true },
                        onDeleteDepartment = { deptToDelete = it }
                    )
                }
            }
        }
    }


    // Modal: Add Department
    if (showAddDeptDialog) {
        AddDepartmentModal(
            onDismiss = { showAddDeptDialog = false },
            onSave = { name, code, desc ->
                AttendanceSystemRepository.addDepartment(name, code, desc)
                showAddDeptDialog = false
                scope.launch { snackbarHostState.showSnackbar("Department '$name' created.") }
            }
        )
    }

    // Modal: Delete Department Confirmation
    if (deptToDelete != null) {
        val target = deptToDelete!!
        AlertDialog(
            onDismissRequest = { deptToDelete = null },
            title = { Text("Delete Department", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${target.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        AttendanceSystemRepository.deleteDepartment(target.id)
                        deptToDelete = null
                        scope.launch { snackbarHostState.showSnackbar("Department '${target.name}' deleted.") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deptToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 1: Dynamic QR Code Generator (Configurable Audience, Date & Expiry)
    if (showDynamicQrDialog) {
        DynamicDailyQrModal(
            groups = groups,
            departments = departments,
            onDismiss = { showDynamicQrDialog = false },
            onShareWhatsApp = { _, message ->
                ReportExportManager.shareGenericMessage(context, message)
            }
        )
    }

    // Modal 2: Inspect Intern Calendar
    if (inspectInternForCalendar != null) {
        val intern = inspectInternForCalendar!!
        var calMonth by remember { mutableStateOf(YearMonth.of(2025, 1)) }
        var clickedDate by remember { mutableStateOf<LocalDate?>(null) }
        val internRecords = attendanceMap[intern.id] ?: emptyMap()

        Dialog(onDismissRequest = { inspectInternForCalendar = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = intern.fullName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${intern.id} • ${intern.department}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { inspectInternForCalendar = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AttendanceCalendarView(
                        currentMonth = calMonth,
                        attendanceMap = internRecords,
                        selectedDate = clickedDate,
                        onMonthChange = { calMonth = it },
                        onDayClick = { date, _ ->
                            clickedDate = date
                            internForAttendanceMarking = Pair(intern, date)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Tip: Tap on any date above to change its status (Present/Absent/Leave/Holiday).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Modal 3: Mark Daily Attendance
    if (internForAttendanceMarking != null) {
        val (intern, date) = internForAttendanceMarking!!
        val currentRecord = (attendanceMap[intern.id] ?: emptyMap())[date]
        var selectedStatus by remember { mutableStateOf(currentRecord?.status ?: AttendanceStatus.PRESENT) }
        var remarks by remember { mutableStateOf(currentRecord?.remarks ?: "") }

        Dialog(onDismissRequest = { internForAttendanceMarking = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Mark Attendance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${intern.fullName} • ${date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Select Daily Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AttendanceStatus.PRESENT to StatusPresent,
                            AttendanceStatus.ABSENT to StatusAbsent,
                            AttendanceStatus.ON_LEAVE to StatusLeave,
                            AttendanceStatus.HOLIDAY to StatusHoliday
                        ).forEach { (status, color) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStatus = status },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedStatus == status) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selectedStatus == status) androidx.compose.foundation.BorderStroke(1.dp, color) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedStatus == status,
                                        onClick = { selectedStatus = status }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = status.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { internForAttendanceMarking = null }, shape = RoundedCornerShape(8.dp)) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                AttendanceSystemRepository.markAttendance(
                                    internId = intern.id,
                                    date = date,
                                    status = selectedStatus,
                                    remarks = remarks.ifBlank { "Marked by Admin" }
                                )
                                internForAttendanceMarking = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("Marked ${intern.fullName} as ${selectedStatus.label} on $date")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Status", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }

    // Modal 4: Add Intern
    if (showAddInternDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newCollege by remember { mutableStateOf("") }
        var newDept by remember { mutableStateOf(selectedDepartmentFilter ?: departments.firstOrNull()?.name ?: "") }

        Dialog(onDismissRequest = { showAddInternDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Add New Intern", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Register intern in plant database", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newCollege, onValueChange = { newCollege = it }, label = { Text("College *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newDept, onValueChange = { newDept = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showAddInternDialog = false }, shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank() && newEmail.isNotBlank()) {
                                    AttendanceSystemRepository.registerNewIntern(
                                        email = newEmail,
                                        fullName = newName,
                                        phone = newPhone,
                                        college = newCollege,
                                        joiningDate = LocalDate.now(),
                                        department = newDept
                                    )
                                    showAddInternDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Intern added successfully.") }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Intern", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }

    // Modal 5: Add Group / Book
    if (showAddGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDept by remember { mutableStateOf(selectedDepartmentFilter ?: departments.firstOrNull()?.name ?: "") }
        var groupDesc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddGroupDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Create Resource Book / Group", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Organize interns by joining batch and department", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Group Name * (e.g. Batch 15-Mar-2025)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = groupDept, onValueChange = { groupDept = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = groupDesc, onValueChange = { groupDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showAddGroupDialog = false }, shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (groupName.isNotBlank()) {
                                    AttendanceSystemRepository.addGroup(
                                        name = groupName,
                                        joiningDate = LocalDate.now(),
                                        department = groupDept,
                                        description = groupDesc
                                    )
                                    showAddGroupDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Resource book created.") }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Create Book", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }

    // Modal 6: View Group QR
    if (groupForQrDisplay != null) {
        val group = groupForQrDisplay!!
        val qrBitmap = remember(group.qrPayload) {
            QrCodeGenerator.generateQrBitmap(group.qrPayload, 400)
        }

        Dialog(onDismissRequest = { groupForQrDisplay = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resource Book / Group QR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { groupForQrDisplay = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        text = group.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Joining: ${group.joiningDate} • ${group.department}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Group QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            ReportExportManager.shareGroupViaWhatsApp(
                                context = context,
                                groupName = group.name,
                                joiningDate = group.joiningDate,
                                qrPayload = group.qrPayload
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share with Interns on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Modal 7: Confirm Delete Intern
    if (internToDelete != null) {
        val intern = internToDelete!!
        Dialog(onDismissRequest = { internToDelete = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Delete Intern Record", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to remove ${intern.fullName} (${intern.id})? All historical attendance records will be permanently deleted.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { internToDelete = null }, shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                AttendanceSystemRepository.deleteIntern(intern.id)
                                internToDelete = null
                                scope.launch { snackbarHostState.showSnackbar("Intern deleted.") }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dynamic Daily QR Code Generator Modal with configurable Audience (All, Department, Book Group) & Expiry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicDailyQrModal(
    groups: List<InternGroup>,
    departments: List<Department>,
    onDismiss: () -> Unit,
    onShareWhatsApp: (payload: AttendanceQrPayload, message: String) -> Unit
) {
    val context = LocalContext.current
    var selectedAudience by remember { mutableStateOf(QrTargetAudience.ALL) }
    var selectedDepartment by remember { mutableStateOf(departments.firstOrNull()?.name ?: "") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }

    var deptDropdownExpanded by remember { mutableStateOf(false) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    var qrPayload by remember {
        mutableStateOf(
            AttendanceSystemRepository.generateDailyAttendanceQr(
                audience = QrTargetAudience.ALL,
                targetValue = "ALL"
            )
        )
    }

    fun regenerateQr() {
        val targetVal = when (selectedAudience) {
            QrTargetAudience.ALL -> "ALL"
            QrTargetAudience.DEPARTMENT -> selectedDepartment
            QrTargetAudience.GROUP -> selectedGroupId
        }
        qrPayload = AttendanceSystemRepository.generateDailyAttendanceQr(
            audience = selectedAudience,
            targetValue = targetVal
        )
    }

    LaunchedEffect(selectedAudience, selectedDepartment, selectedGroupId) {
        regenerateQr()
    }

    val serialized = qrPayload.toSerializedString()
    val qrBitmap = remember(serialized) {
        QrCodeGenerator.generateQrBitmap(serialized, 420)
    }

    val audienceOptions = listOf(
        Pair(QrTargetAudience.ALL, "All Interns"),
        Pair(QrTargetAudience.DEPARTMENT, "Department"),
        Pair(QrTargetAudience.GROUP, "Book Group")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Attendance QR Generator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Generates encrypted daily token valid for today",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Configurable Options: Target Audience
                Text(
                    text = "Configure Target Audience:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    audienceOptions.forEach { pair ->
                        val aud = pair.first
                        val label = pair.second
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedAudience == aud) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedAudience == aud) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAudience = aud }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAudience == aud) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Sub-target picker if Department or Book Group
                if (selectedAudience == QrTargetAudience.DEPARTMENT) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = deptDropdownExpanded,
                        onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedDepartment,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target Department") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = deptDropdownExpanded,
                            onDismissRequest = { deptDropdownExpanded = false }
                        ) {
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept.name, fontSize = 12.sp) },
                                    onClick = {
                                        selectedDepartment = dept.name
                                        deptDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedAudience == QrTargetAudience.GROUP) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val selectedGroupName = groups.firstOrNull { it.id == selectedGroupId }?.name ?: "Select Book Group"
                    ExposedDropdownMenuBox(
                        expanded = groupDropdownExpanded,
                        onExpandedChange = { groupDropdownExpanded = !groupDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedGroupName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target Resource Book") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = groupDropdownExpanded,
                            onDismissRequest = { groupDropdownExpanded = false }
                        ) {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name, fontSize = 12.sp) },
                                    onClick = {
                                        selectedGroupId = group.id
                                        groupDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // QR Code Bitmap Box
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Dynamic Attendance QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Expiry and Token Details Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Date: ${qrPayload.date}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Target: ${qrPayload.audience.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Secret Token: ${qrPayload.token}...",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Validity: Today only • Cutoff at 18:00 IST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusAbsent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Share on WhatsApp, Copy Token, Regenerate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SAIL Daily Attendance QR Token", serialized)
                            clipboard.setPrimaryClip(clip)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { regenerateQr() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val targetDesc = when (selectedAudience) {
                                QrTargetAudience.ALL -> "All Plant Interns"
                                QrTargetAudience.DEPARTMENT -> "Department: $selectedDepartment"
                                QrTargetAudience.GROUP -> "Book Group: ${groups.firstOrNull { it.id == selectedGroupId }?.name ?: ""}"
                            }
                            val msg = "🏢 *STEEL AUTHORITY OF INDIA LIMITED (SAIL)*\n" +
                                    "📋 *DAILY ATTENDANCE QR CODE*\n\n" +
                                    "📅 *Date:* ${qrPayload.date}\n" +
                                    "🎯 *Audience:* $targetDesc\n" +
                                    "⏰ *Expiry:* Today at 18:00 IST\n\n" +
                                    "👉 *Secret QR Token for SIAMD App:*\n```${serialized}```\n\n" +
                                    "Open your SIAMD App -> Tap 'Scan Daily QR' -> paste/scan to mark attendance as PRESENT."
                            onShareWhatsApp(qrPayload, msg)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminInternsListSection(
    interns: List<InternProfile>,
    totalInternCount: Int,
    groups: List<InternGroup>,
    attendanceMap: Map<String, Map<LocalDate, DailyAttendance>>,
    activeDepartmentFilter: String?,
    onAddInternClick: () -> Unit,
    onInspectCalendar: (InternProfile) -> Unit,
    onMarkAttendance: (InternProfile, LocalDate) -> Unit,
    onExportPdf: (InternProfile) -> Unit,
    onExportExcel: (InternProfile) -> Unit,
    onDeleteIntern: (InternProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Header Action Strip: Showing filtered count and "+ Add Intern" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (activeDepartmentFilter != null) "$activeDepartmentFilter Roster" else "Enrolled Interns Roster",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Showing ${interns.size} of $totalInternCount registered interns",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddInternClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Intern", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        if (interns.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No interns match the current category filter.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Intern' above or select another department chip.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(interns.size) { idx ->
                val intern = interns[idx]
                val currentMonth = YearMonth.of(2025, 1)
                val summary = remember(intern.id, attendanceMap) {
                    AttendanceSystemRepository.getMonthlySummary(intern.id, currentMonth)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = intern.fullName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = intern.id,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${intern.department} • Joining: ${intern.joiningDate}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onDeleteIntern(intern) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Summary Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "P: ${summary.presentDays}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPresent
                                )
                                Text(
                                    text = "A: ${summary.absentDays}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusAbsent
                                )
                                Text(
                                    text = "L: ${summary.leaveDays}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusLeave
                                )
                                Text(
                                    text = "H: ${summary.holidayDays}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusHoliday
                                )
                            }

                            Text(
                                text = "${String.format("%.1f", summary.attendancePercentage)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.attendancePercentage >= 75.0) StatusPresent else StatusAbsent
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons Row: Calendar, Mark, Export PDF, Export Excel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onInspectCalendar(intern) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Calendar", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { onMarkAttendance(intern, LocalDate.now()) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { onExportPdf(intern) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }

                            Button(
                                onClick = { onExportExcel(intern) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Excel", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun AdminGroupsSection(
    groups: List<InternGroup>,
    interns: List<InternProfile>,
    onAddGroupClick: () -> Unit,
    onShowQr: (InternGroup) -> Unit,
    onShareWhatsApp: (InternGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Resource Books & Batches",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Organize interns by joining batches. Share book invite QR on WhatsApp.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onAddGroupClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Book", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No resource books created yet.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'New Book' above to create a batch book and generate a join QR code.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(groups.size) { idx ->
                val group = groups[idx]
                val members = interns.filter { it.groupId == group.id }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Joining: ${group.joiningDate} • ${group.department}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${members.size} Interns",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (group.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onShowQr(group) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View QR", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onShareWhatsApp(group) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share via WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun AdminQuickDailyMarkSection(
    interns: List<InternProfile>,
    attendanceMap: Map<String, Map<LocalDate, DailyAttendance>>,
    onMarkStatus: (internId: String, date: LocalDate, status: AttendanceStatus) -> Unit
) {
    var targetDate by remember { mutableStateOf(LocalDate.now()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Attendance Marker",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Date: ${targetDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy (EEEE)"))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            interns.forEach { intern ->
                                onMarkStatus(intern.id, targetDate, AttendanceStatus.PRESENT)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPresent)
                    ) {
                        Text("Mark All Present", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (interns.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No interns enrolled yet to mark attendance.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(interns.size) { idx ->
                val intern = interns[idx]
                val record = (attendanceMap[intern.id] ?: emptyMap())[targetDate]
                val status = record?.status ?: AttendanceStatus.ABSENT

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = intern.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${intern.id} • ${intern.department}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { onMarkStatus(intern.id, targetDate, AttendanceStatus.PRESENT) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (status == AttendanceStatus.PRESENT) StatusPresent else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    "Present",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == AttendanceStatus.PRESENT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onMarkStatus(intern.id, targetDate, AttendanceStatus.ABSENT) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (status == AttendanceStatus.ABSENT) StatusAbsent else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    "Absent",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == AttendanceStatus.ABSENT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onMarkStatus(intern.id, targetDate, AttendanceStatus.ON_LEAVE) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (status == AttendanceStatus.ON_LEAVE) StatusLeave else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    "Leave",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == AttendanceStatus.ON_LEAVE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}


@Composable
private fun AdminDepartmentsSection(
    departments: List<Department>,
    interns: List<InternProfile>,
    onAddDepartmentClick: () -> Unit,
    onDeleteDepartment: (Department) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Plant Departments Management",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All departments are created by the Admin and used dynamically in intern onboarding.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onAddDepartmentClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Dept", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        if (departments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No departments created yet.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dummy departments have been removed. Tap 'Add Dept' above to create plant departments.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(departments.size) { idx ->
                val dept = departments[idx]
                val enrolledCount = interns.count { it.department.equals(dept.name, ignoreCase = true) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dept.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (dept.code.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = dept.code,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (dept.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dept.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$enrolledCount Interns enrolled",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (enrolledCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { onDeleteDepartment(dept) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Department",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun AddDepartmentModal(
    onDismiss: () -> Unit,
    onSave: (name: String, code: String, description: String) -> Unit
) {
    var deptName by remember { mutableStateOf("") }
    var deptCode by remember { mutableStateOf("") }
    var deptDesc by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Create Plant Department",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Department will be immediately available in intern onboarding and reports.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = deptName,
                    onValueChange = { deptName = it; errorMsg = null },
                    label = { Text("Department Name *") },
                    placeholder = { Text("e.g. Blast Furnace Operations") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deptCode,
                    onValueChange = { deptCode = it },
                    label = { Text("Code / Abbreviation") },
                    placeholder = { Text("e.g. BF-OPS") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deptDesc,
                    onValueChange = { deptDesc = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Primary iron making division") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 2
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (deptName.isBlank()) {
                                errorMsg = "Department name is required."
                                return@Button
                            }
                            onSave(deptName.trim(), deptCode.trim(), deptDesc.trim())
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Create Department", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
