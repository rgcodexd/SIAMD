package com.ogpika.siama.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.ogpika.siama.model.LeaveApplication
import com.ogpika.siama.model.LeavePolicy
import com.ogpika.siama.model.LeaveStatus
import com.ogpika.siama.model.MonthlyAttendanceSummary
import com.ogpika.siama.ui.components.AttendanceCalendarView
import com.ogpika.siama.ui.theme.StatusAbsent
import com.ogpika.siama.ui.theme.StatusHoliday
import com.ogpika.siama.ui.theme.StatusLeave
import com.ogpika.siama.ui.theme.StatusPresent
import com.ogpika.siama.util.ReportExportManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun InternAttendanceScreen(
    modifier: Modifier = Modifier
) {
    val currentInternId by AttendanceSystemRepository.currentInternId.collectAsState()
    val interns by AttendanceSystemRepository.interns.collectAsState()
    val attendanceMap by AttendanceSystemRepository.attendanceMap.collectAsState()
    val groups by AttendanceSystemRepository.groups.collectAsState()
    val leaveApplications by AttendanceSystemRepository.leaveApplications.collectAsState()

    val profile = interns.firstOrNull { it.id == currentInternId } ?: interns.firstOrNull()

    if (profile == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No intern profile found.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Please register your Intern ID from the login screen.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { AttendanceSystemRepository.logout() }) {
                    Text("Go to Login Screen")
                }
            }
        }
        return
    }

    val internAttendance = attendanceMap[profile.id] ?: emptyMap()
    val today = LocalDate.now()
    val todayRecord = internAttendance[today]
    val isPresentToday = todayRecord?.status == AttendanceStatus.PRESENT

    var selectedMonth by remember { mutableStateOf(YearMonth.of(2025, 1)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedRecord by remember { mutableStateOf<DailyAttendance?>(null) }

    var showScanQrDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showQrJoinDialog by remember { mutableStateOf(false) }
    var showApplyLeaveDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val summary = remember(profile.id, selectedMonth, attendanceMap) {
        AttendanceSystemRepository.getMonthlySummary(profile.id, selectedMonth)
    }

    // Leave Entitlement calculations
    val usedLeaveDays = internAttendance.values.count { it.status == AttendanceStatus.ON_LEAVE }
    val maxSanctionedLeaves = LeavePolicy.SANCTIONED_LEAVES_ENTITLEMENT.toInt()
    val remainingLeaveDays = (maxSanctionedLeaves - usedLeaveDays).coerceAtLeast(0)

    val myLeaveApplications: List<LeaveApplication> = remember(leaveApplications, profile.id) {
        leaveApplications.filter { it.internId == profile.id }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Fixed Bottom-Corner FAB to Scan Today's QR Code
            ExtendedFloatingActionButton(
                onClick = { showScanQrDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Daily QR",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = if (isPresentToday) "Re-Scan Daily QR" else "Scan Today's QR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                InternProfileBadgeCard(
                    profile = profile,
                    onEditProfile = { showEditProfileDialog = true },
                    onJoinGroup = { showQrJoinDialog = true }
                )
            }

            // 1. Today's Live Attendance Status Banner & Action
            item {
                TodayAttendanceStatusBanner(
                    today = today,
                    todayRecord = todayRecord,
                    isPresent = isPresentToday,
                    onScanClick = { showScanQrDialog = true }
                )
            }

            // 2. Monthly Attendance KPI Visualizer
            item {
                MonthlyKpiSummaryCard(summary = summary)
            }

            // 3. Interactive Monthly Calendar
            item {
                AttendanceCalendarView(
                    currentMonth = selectedMonth,
                    attendanceMap = internAttendance,
                    selectedDate = selectedDate,
                    onMonthChange = { newMonth ->
                        selectedMonth = newMonth
                        selectedDate = null
                        selectedRecord = null
                    },
                    onDayClick = { date, record ->
                        selectedDate = date
                        selectedRecord = record
                    }
                )
            }

            // Day Detail Card if date clicked
            if (selectedDate != null) {
                item {
                    DayDetailCard(
                        date = selectedDate!!,
                        record = selectedRecord,
                        onDismiss = {
                            selectedDate = null
                            selectedRecord = null
                        }
                    )
                }
            }

            // 4. Data Exporting Module (Download PDF & Excel .xlsx)
            item {
                PersonalDataExportCard(
                    onExportPdf = {
                        val pdfFile = ReportExportManager.generatePdfReport(context, profile, summary, AttendanceSystemRepository.getRecordsForMonth(profile.id, selectedMonth))
                        if (pdfFile != null) {
                            ReportExportManager.shareReportFile(context, pdfFile, "application/pdf", "Attendance PDF - ${profile.fullName}")
                            scope.launch { snackbarHostState.showSnackbar("PDF Report ready: ${pdfFile.name}") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Error generating PDF report.") }
                        }
                    },
                    onExportExcel = {
                        val csvFile = ReportExportManager.generateExcelReport(context, profile, summary, AttendanceSystemRepository.getRecordsForMonth(profile.id, selectedMonth))
                        if (csvFile != null) {
                            ReportExportManager.shareReportFile(context, csvFile, "text/csv", "Attendance Excel - ${profile.fullName}")
                            scope.launch { snackbarHostState.showSnackbar("Excel/CSV Report ready: ${csvFile.name}") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Error generating Excel report.") }
                        }
                    }
                )
            }

            // 5. Leave Application Module & Entitlement Quota
            item {
                LeaveQuotaAndApplicationCard(
                    usedDays = usedLeaveDays,
                    remainingDays = remainingLeaveDays,
                    maxDays = maxSanctionedLeaves,
                    onApplyClick = { showApplyLeaveDialog = true }
                )
            }

            // 6. Leave Applications History (if any)
            if (myLeaveApplications.isNotEmpty()) {
                item {
                    Text(
                        text = "Leave Applications History",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(myLeaveApplications.size) { idx ->
                    val app = myLeaveApplications[idx]
                    LeaveApplicationHistoryItem(app = app)
                }
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }

    // Modal 1: Scan Admin's Secret Daily QR Code
    if (showScanQrDialog) {
        ScanDailyAttendanceQrDialog(
            profile = profile,
            onDismiss = { showScanQrDialog = false },
            onScanSuccess = { msg ->
                showScanQrDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    // Modal 2: Edit Personal Profile
    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { phone, college, address, emergency ->
                AttendanceSystemRepository.updateInternProfile(phone, college, address, emergency)
                showEditProfileDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Personal profile updated successfully.")
                }
            }
        )
    }

    // Modal 3: Join Group / Book via QR
    if (showQrJoinDialog) {
        QrGroupJoinDialog(
            groups = groups,
            onDismiss = { showQrJoinDialog = false },
            onJoin = { payload ->
                val result = AttendanceSystemRepository.joinGroupByQr(payload)
                showQrJoinDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(result.second)
                }
            }
        )
    }

    // Modal 4: Comprehensive Leave Application Form
    if (showApplyLeaveDialog) {
        ComprehensiveApplyLeaveModal(
            remainingQuota = remainingLeaveDays,
            onDismiss = { showApplyLeaveDialog = false },
            onSubmit = { startDate, endDate, reason ->
                val result = AttendanceSystemRepository.applyForLeave(startDate, endDate, reason)
                showApplyLeaveDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(result.second)
                }
            }
        )
    }
}

/**
 * Live Daily Attendance Status Banner (Prompt 4A: QR Scanning & Marking)
 */
@Composable
private fun TodayAttendanceStatusBanner(
    today: LocalDate,
    todayRecord: DailyAttendance?,
    isPresent: Boolean,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent) StatusPresent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPresent) StatusPresent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        )
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
                    Icon(
                        imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isPresent) StatusPresent else StatusAbsent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPresent) "Marked PRESENT for Today" else "Attendance Pending Today",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPresent) StatusPresent else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isPresent) {
                        "Check-in: ${todayRecord?.checkInTime ?: "09:00 AM"} • Verified by Admin QR"
                    } else {
                        "Scan Admin's Secret Daily QR before cutoff (18:00 IST) to avoid being marked absent."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onScanClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPresent) StatusPresent else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPresent) "Re-Scan" else "Scan QR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Scan Daily Attendance QR Dialog (In-app scanner simulation + secret token verification)
 */
@Composable
private fun ScanDailyAttendanceQrDialog(
    profile: InternProfile,
    onDismiss: () -> Unit,
    onScanSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
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
                    Column {
                        Text(
                            text = "Daily Attendance Scanner",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Scan or paste Admin's Secret Daily QR Token",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Simulated Scanner Viewfinder Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner Viewfinder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Point camera at Admin's Daily QR Screen",
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Paste Button from Clipboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Or paste token string:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) tokenInput = text
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Token", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = {
                        tokenInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("SAIL_ATTENDANCE_QR:...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (tokenInput.isBlank()) {
                            errorMessage = "Please enter or scan a valid QR token."
                            return@Button
                        }
                        isValidating = true
                        val (success, message) = AttendanceSystemRepository.scanAndMarkAttendance(tokenInput.trim())
                        isValidating = false
                        if (success) {
                            onScanSuccess(message)
                        } else {
                            errorMessage = message
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isValidating) "Validating Token..." else "Validate & Mark Attendance",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Data Exporting Card (Prompt 5: PDF & Excel .xlsx Export)
 */
@Composable
private fun PersonalDataExportCard(
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
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
                Column {
                    Text(
                        text = "Export Personal Attendance",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Official certified statements for training verification",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export PDF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Button(
                    onClick = onExportExcel,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export Excel (.xlsx)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Leave Application Module Card & Quota Visualizer
 */
@Composable
private fun LeaveQuotaAndApplicationCard(
    usedDays: Int,
    remainingDays: Int,
    maxDays: Int,
    onApplyClick: () -> Unit
) {
    val progress = (usedDays.toFloat() / maxDays.toFloat()).coerceIn(0f, 1f)

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
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Sanctioned Leaves Entitlement",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Organizational Quota: 12 Days per Internship Period",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onApplyClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Apply Leave",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (remainingDays <= 2) StatusAbsent else StatusLeave,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Consumed: $usedDays of $maxDays days",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$remainingDays Days Available",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingDays > 0) StatusPresent else StatusAbsent
                )
            }
        }
    }
}

/**
 * Comprehensive Leave Application Modal (Date range calculation excluding Sundays & SAIL holidays)
 */
@Composable
private fun ComprehensiveApplyLeaveModal(
    remainingQuota: Int,
    onDismiss: () -> Unit,
    onSubmit: (startDate: LocalDate, endDate: LocalDate, reason: String) -> Unit
) {
    var startDateText by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var endDateText by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    var reasonText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Apply for Leave",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Leaves are subject to 12-day entitlement rules and exclude Sundays and SAIL holidays",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { startDateText = it; errorMsg = null },
                    label = { Text("From Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endDateText,
                    onValueChange = { endDateText = it; errorMsg = null },
                    label = { Text("To Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it; errorMsg = null },
                    label = { Text("Reason for Leave *") },
                    placeholder = { Text("e.g. University Exam / Health") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 2
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMsg!!,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Current Sanctioned Balance: $remainingQuota days remaining.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
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
                            try {
                                val s = LocalDate.parse(startDateText.trim())
                                val e = LocalDate.parse(endDateText.trim())
                                if (e.isBefore(s)) {
                                    errorMsg = "End date cannot precede start date."
                                    return@Button
                                }
                                if (reasonText.isBlank()) {
                                    errorMsg = "Please describe the reason for your leave."
                                    return@Button
                                }
                                onSubmit(s, e, reasonText.trim())
                            } catch (ex: Exception) {
                                errorMsg = "Invalid date format. Use YYYY-MM-DD (e.g. 2025-03-20)."
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Submit Application", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveApplicationHistoryItem(app: LeaveApplication) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
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
                Text(
                    text = "${app.startDate} to ${app.endDate} (${app.totalDays.toInt()} working days)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.reason,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val statusColor = when (app.status) {
                LeaveStatus.APPROVED -> StatusPresent
                LeaveStatus.PENDING -> StatusLeave
                LeaveStatus.REJECTED -> StatusAbsent
                LeaveStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = app.status.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InternProfileBadgeCard(
    profile: InternProfile,
    onEditProfile: () -> Unit,
    onJoinGroup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.fullName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = profile.id,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = profile.department,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onEditProfile, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Group / Book Status Pill & Joining Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (profile.groupId != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = if (profile.groupId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = profile.groupName ?: "No Book Group • Tap to Join",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profile.groupId != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onJoinGroup,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (profile.groupId == null) "Join via QR" else "Switch Book", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MonthlyKpiSummaryCard(summary: MonthlyAttendanceSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary.monthYear} Overview",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${String.format("%.1f", summary.attendancePercentage)}% Attendance",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (summary.attendancePercentage >= 75.0) StatusPresent else StatusAbsent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiPill("Present", "${summary.presentDays}d", StatusPresent, Modifier.weight(1f))
                KpiPill("Absent", "${summary.absentDays}d", StatusAbsent, Modifier.weight(1f))
                KpiPill("Leave", "${summary.leaveDays}d", StatusLeave, Modifier.weight(1f))
                KpiPill("Holidays", "${summary.holidayDays}d", StatusHoliday, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayDetailCard(
    date: LocalDate,
    record: DailyAttendance?,
    onDismiss: () -> Unit
) {
    val status = record?.status ?: AttendanceStatus.ABSENT
    val (statusLabel, statusColor) = when (status) {
        AttendanceStatus.PRESENT -> "PRESENT" to StatusPresent
        AttendanceStatus.ABSENT -> "ABSENT" to StatusAbsent
        AttendanceStatus.ON_LEAVE -> "ON LEAVE" to StatusLeave
        AttendanceStatus.HOLIDAY -> "HOLIDAY" to StatusHoliday
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy (EEEE)")),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record?.remarks ?: "Official Record",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: InternProfile,
    onDismiss: () -> Unit,
    onSave: (phone: String, college: String, address: String, emergency: String) -> Unit
) {
    var phone by remember { mutableStateOf(profile.phone) }
    var college by remember { mutableStateOf(profile.college) }
    var address by remember { mutableStateOf(profile.address) }
    var emergency by remember { mutableStateOf(profile.emergencyContact) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Personal Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Users can update contact and college details",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = college,
                    onValueChange = { college = it },
                    label = { Text("College / Institute") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emergency,
                    onValueChange = { emergency = it },
                    label = { Text("Emergency Contact") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Hostel / Local Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

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
                        onClick = { onSave(phone, college, address, emergency) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Details", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QrGroupJoinDialog(
    groups: List<InternGroup>,
    onDismiss: () -> Unit,
    onJoin: (payload: String) -> Unit
) {
    var manualKey by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Join Resource Book / Group",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "Scan QR shared on WhatsApp or select your batch group below",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Available Plant Groups (By Joining Date):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { group ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Joining: ${group.joiningDate} • ${group.department}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onJoin(group.qrPayload) },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Join", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = manualKey,
                    onValueChange = { manualKey = it },
                    label = { Text("Or Paste Join Key / QR Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (manualKey.isNotBlank()) onJoin(manualKey)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Join with Key", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
