package com.ogpika.siama.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogpika.siama.data.LeaveManagementRepository
import com.ogpika.siama.model.LeaveApplication
import com.ogpika.siama.model.LeavePolicy
import com.ogpika.siama.model.LeaveStatus
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun AdminManagementScreen(
    modifier: Modifier = Modifier
) {
    val applications by LeaveManagementRepository.applications.collectAsState()
    val pendingApplications = remember(applications) {
        applications.filter { it.status == LeaveStatus.PENDING }
    }
    val leaveBalance by LeaveManagementRepository.leaveBalance.collectAsState()

    val remarksMap = remember { mutableStateMapOf<String, String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                AdminHeaderCard(pendingCount = pendingApplications.size)
            }

            item {
                SailPolicyRuleSummaryCard()
            }

            item {
                Text(
                    text = "Pending Leave Requests (${pendingApplications.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (pendingApplications.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All leave requests have been reviewed.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                items(pendingApplications.size) { index ->
                    val app = pendingApplications[index]
                    AdminLeaveRequestCard(
                        application = app,
                        remark = remarksMap[app.id] ?: "",
                        onRemarkChange = { remarksMap[app.id] = it },
                        onApprove = {
                            LeaveManagementRepository.approveApplication(app.id, remarksMap[app.id] ?: "Approved")
                            scope.launch {
                                snackbarHostState.showSnackbar("Approved leave for ${app.internName}")
                            }
                        },
                        onReject = {
                            LeaveManagementRepository.rejectApplication(app.id, remarksMap[app.id] ?: "Rejected")
                            scope.launch {
                                snackbarHostState.showSnackbar("Rejected leave for ${app.internName}")
                            }
                        }
                    )
                }
            }

            item {
                InternLeaveRosterSection(currentUsed = leaveBalance.usedLeaves)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AdminHeaderCard(pendingCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEA580C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Admin & Supervisor Portal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Bokaro Steel Plant • Plant HR & Training",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (pendingCount > 0) Color(0xFFEA580C).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "$pendingCount Pending",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingCount > 0) Color(0xFFFB923C) else Color(0xFF34D399),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SailPolicyRuleSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = "Policy",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAIL Internship Entitlements Policy Matrix",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "• a. Sanctioned Leaves: 12 (Twelve) days\n" +
                        "• b. National Holiday Leaves: 03 (Three) days (26 Jan, 15 Aug, 02 Oct)\n" +
                        "• c. SAIL Foundation Day Closed Holiday: 01 (One) day (24 Jan)\n" +
                        "• d. Festival Holidays (FHs): 06 (Six) days (Holi, Eid, Dussehra, Diwali, Guru Nanak Jayanti, Christmas)\n" +
                        "• e. Ambedkar Jayanti Holiday: 01 (One) day following govt declaration\n" +
                        "Total Admissible Non-Working Days = 23 Days (No stipend deduction)",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun AdminLeaveRequestCard(
    application: LeaveApplication,
    remark: String,
    onRemarkChange: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = application.internName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Applied on ${application.appliedDate.format(dateFormatter)}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2563EB).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${application.totalDays.toInt()} Day(s)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Period: ${application.startDate.format(dateFormatter)}  ➔  ${application.endDate.format(dateFormatter)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Reason: ${application.reason}",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Remarks field
            OutlinedTextField(
                value = remark,
                onValueChange = onRemarkChange,
                placeholder = { Text("Add optional admin remarks...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF87171)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve Leave", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun InternLeaveRosterSection(currentUsed: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Intern Leave Quota & LOP Audit",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Tracked against 12 Sanctioned Leaves quota",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                InternRosterRow("Rahul Sharma (BSL-089)", "${currentUsed.toInt()} / 12 used", if (currentUsed > 12) "Loss of Pay" else "In Quota", currentUsed > 12)
                Spacer(modifier = Modifier.height(8.dp))
                InternRosterRow("Priya Kumari (BSL-094)", "2 / 12 used", "In Quota", false)
                Spacer(modifier = Modifier.height(8.dp))
                InternRosterRow("Amit Patel (BSL-102)", "11 / 12 used", "1 Day Left", false)
                Spacer(modifier = Modifier.height(8.dp))
                InternRosterRow("Sunita Soren (BSL-108)", "13 / 12 used", "1 Day LOP", true)
            }
        }
    }
}

@Composable
private fun InternRosterRow(name: String, quotaText: String, statusText: String, isAlert: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = quotaText, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isAlert) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.15f)
        ) {
            Text(
                text = statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) Color(0xFFF87171) else Color(0xFF34D399),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
