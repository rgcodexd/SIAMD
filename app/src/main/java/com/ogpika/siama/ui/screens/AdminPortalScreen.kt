package com.ogpika.siama.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.DailyAttendance
import com.ogpika.siama.model.InternGroup
import com.ogpika.siama.model.InternProfile
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

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Interns, 1 = Groups & QR, 2 = Daily Marking
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Modals
    var showAddInternDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var inspectInternForCalendar by remember { mutableStateOf<InternProfile?>(null) }
    var internForAttendanceMarking by remember { mutableStateOf<Pair<InternProfile, LocalDate>?>(null) }
    var groupForQrDisplay by remember { mutableStateOf<InternGroup?>(null) }
    var internToDelete by remember { mutableStateOf<InternProfile?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddInternDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Intern")
                }
            } else if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Admin Tabs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
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
                        text = { Text("Interns (${interns.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Groups & QR (${groups.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Quick Mark", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Interns List & Reports
                    AdminInternsListSection(
                        interns = interns,
                        groups = groups,
                        attendanceMap = attendanceMap,
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
                    // TAB 1: Groups & QR Generator (WhatsApp Sharing)
                    AdminGroupsSection(
                        groups = groups,
                        interns = interns,
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
                        interns = interns,
                        attendanceMap = attendanceMap,
                        onMarkStatus = { internId, date, status ->
                            AttendanceSystemRepository.markAttendance(internId, date, status)
                            scope.launch {
                                snackbarHostState.showSnackbar("Updated attendance for $internId")
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal: Inspect Intern Calendar
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

    // Modal: Mark Daily Attendance
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

    // Modal: Group QR Code Dialog
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
                            text = "Group Invitation QR",
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
                        text = "Joining Date: ${group.joiningDate}",
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share with Interns on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Modal: Add Intern
    if (showAddInternDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newCollege by remember { mutableStateOf("") }
        var newDept by remember { mutableStateOf("") }

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

    // Modal: Add Group
    if (showAddGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDept by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddGroupDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Create Group / Book", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Organize interns by joining date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                                    scope.launch { snackbarHostState.showSnackbar("Group created with join QR code.") }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Create Group", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }

    // Modal: Confirm Delete Intern
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

@Composable
private fun AdminInternsListSection(
    interns: List<InternProfile>,
    groups: List<InternGroup>,
    attendanceMap: Map<String, Map<LocalDate, DailyAttendance>>,
    onInspectCalendar: (InternProfile) -> Unit,
    onMarkAttendance: (InternProfile, LocalDate) -> Unit,
    onExportPdf: (InternProfile) -> Unit,
    onExportExcel: (InternProfile) -> Unit,
    onDeleteIntern: (InternProfile) -> Unit
) {
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    val filteredInterns = remember(interns, selectedGroupFilter) {
        if (selectedGroupFilter == null) interns else interns.filter { it.groupId == selectedGroupFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Group Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedGroupFilter == null,
                    onClick = { selectedGroupFilter = null },
                    label = { Text("All Interns (${interns.size})", fontSize = 11.sp) }
                )
                groups.forEach { group ->
                    FilterChip(
                        selected = selectedGroupFilter == group.id,
                        onClick = { selectedGroupFilter = group.id },
                        label = { Text(group.name.take(15) + "...", fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredInterns.isEmpty()) {
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
                                text = if (interns.isEmpty()) "No interns enrolled yet." else "No interns in this group.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button below to add an intern or have them register.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredInterns.size) { idx ->
            val intern = filteredInterns[idx]
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
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun AdminGroupsSection(
    groups: List<InternGroup>,
    interns: List<InternProfile>,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Group & Book Management",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Groups organize interns by their joining dates. Generate group QR codes and share them with trainees on WhatsApp so they can join.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                text = "No groups created yet.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button below to create your first group and generate a WhatsApp join QR code.",
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
            Spacer(modifier = Modifier.height(72.dp))
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
                            // Quick mark all present for today
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
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
