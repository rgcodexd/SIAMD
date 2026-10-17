package com.ogpika.siama.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.DailyAttendance
import com.ogpika.siama.model.InternProfile
import com.ogpika.siama.model.MonthlyAttendanceSummary
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReportExportManager {

    /**
     * Generates a beautifully styled, high-contrast, easy-to-read PDF attendance report.
     */
    fun generatePdfReport(
        context: Context,
        profile: InternProfile,
        summary: MonthlyAttendanceSummary,
        records: List<DailyAttendance>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (points)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // 1. Top Header Banner (Deep Graphite / Emerald)
        paint.color = Color.parseColor("#18181B")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Accent strip
        paint.color = Color.parseColor("#10B981") // Emerald
        canvas.drawRect(0f, 86f, 595f, 90f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("SAIL • INTERN ATTENDANCE REPORT", 30f, 42f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#A1A1AA")
        canvas.drawText("Steel Authority of India Limited • Monthly Performance & Verification", 30f, 60f, paint)
        canvas.drawText("Generated on: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}", 30f, 74f, paint)

        // 2. Intern Metadata Box
        paint.color = Color.parseColor("#F4F4F5")
        val metaRect = RectF(30f, 105f, 565f, 185f)
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)

        paint.color = Color.parseColor("#27272A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Intern Profile:", 44f, 126f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 10f
        paint.color = Color.parseColor("#3F3F46")
        canvas.drawText("Name: ${profile.fullName}", 44f, 144f, paint)
        canvas.drawText("Intern ID: ${profile.id}", 44f, 160f, paint)
        canvas.drawText("Department: ${profile.department}", 44f, 174f, paint)

        canvas.drawText("Group: ${profile.groupName ?: "General Roster"}", 280f, 144f, paint)
        canvas.drawText("College: ${profile.college}", 280f, 160f, paint)
        canvas.drawText("Joining Date: ${profile.joiningDate}", 280f, 174f, paint)

        // 3. KPI Summary Strip (Present, Absent, Leave, Holidays, Attendance %)
        val kpiY = 200f
        val kpiWidth = 100f
        val kpiHeight = 50f

        // Present Box
        drawKpiBox(canvas, 30f, kpiY, kpiWidth, kpiHeight, "Present", "${summary.presentDays} Days", "#16A34A", "#DCFCE7")
        // Absent Box
        drawKpiBox(canvas, 137f, kpiY, kpiWidth, kpiHeight, "Absent", "${summary.absentDays} Days", "#DC2626", "#FEE2E2")
        // Leave Box
        drawKpiBox(canvas, 244f, kpiY, kpiWidth, kpiHeight, "On Leave", "${summary.leaveDays} Days", "#D97706", "#FEF3C7")
        // Holidays Box
        drawKpiBox(canvas, 351f, kpiY, kpiWidth, kpiHeight, "Holidays", "${summary.holidayDays} Days", "#4F46E5", "#E0E7FF")
        // Attendance %
        drawKpiBox(canvas, 458f, kpiY, 107f, kpiHeight, "Attendance %", String.format("%.1f%%", summary.attendancePercentage), "#0F766E", "#CCFBF1")

        // 4. Daily Attendance Table
        val tableTopY = 270f
        paint.color = Color.parseColor("#27272A")
        canvas.drawRect(30f, tableTopY, 565f, tableTopY + 24f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Date", 42f, tableTopY + 16f, paint)
        canvas.drawText("Day", 130f, tableTopY + 16f, paint)
        canvas.drawText("Attendance Status", 220f, tableTopY + 16f, paint)
        canvas.drawText("Check-In Time", 350f, tableTopY + 16f, paint)
        canvas.drawText("Remarks / Mode", 450f, tableTopY + 16f, paint)

        var currentY = tableTopY + 24f
        paint.isFakeBoldText = false
        val rowHeight = 16f

        records.take(28).forEachIndexed { index, record ->
            // Zebra row background
            paint.color = if (index % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
            canvas.drawRect(30f, currentY, 565f, currentY + rowHeight, paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 8.5f
            canvas.drawText(record.date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")), 42f, currentY + 11f, paint)
            canvas.drawText(record.date.dayOfWeek.name.take(3), 130f, currentY + 11f, paint)

            // Status chip text and color
            val (statusText, textColor) = when (record.status) {
                AttendanceStatus.PRESENT -> "PRESENT" to "#16A34A"
                AttendanceStatus.ABSENT -> "ABSENT" to "#DC2626"
                AttendanceStatus.ON_LEAVE -> "ON LEAVE" to "#D97706"
                AttendanceStatus.HOLIDAY -> "HOLIDAY" to "#4F46E5"
            }
            paint.color = Color.parseColor(textColor)
            paint.isFakeBoldText = true
            canvas.drawText(statusText, 220f, currentY + 11f, paint)

            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#64748B")
            canvas.drawText(record.checkInTime ?: "--:--", 350f, currentY + 11f, paint)
            canvas.drawText(record.remarks ?: "Verified by Plant Supervisor", 450f, currentY + 11f, paint)

            currentY += rowHeight
        }

        // Table Bottom Border
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(30f, currentY, 565f, currentY, paint)

        // Signatures area at bottom
        val sigY = 790f
        paint.color = Color.parseColor("#475569")
        paint.textSize = 9f
        canvas.drawLine(40f, sigY, 200f, sigY, paint)
        canvas.drawText("Intern Signature", 75f, sigY + 14f, paint)

        canvas.drawLine(390f, sigY, 550f, sigY, paint)
        canvas.drawText("Plant HR / Mentor Signature", 405f, sigY + 14f, paint)

        pdfDocument.finishPage(page)

        // Save to file
        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "SAIL_Attendance_${profile.id}_${summary.monthYear.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawKpiBox(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        value: String,
        accentHex: String,
        bgHex: String
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = Color.parseColor(bgHex)
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        paint.color = Color.parseColor(accentHex)
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText(title.uppercase(), x + 8f, y + 16f, paint)

        paint.textSize = 14f
        canvas.drawText(value, x + 8f, y + 36f, paint)
    }

    /**
     * Generates a clean Excel-compatible CSV report with metadata, attendance KPIs, and daily rows.
     */
    fun generateExcelReport(
        context: Context,
        profile: InternProfile,
        summary: MonthlyAttendanceSummary,
        records: List<DailyAttendance>
    ): File? {
        val sb = StringBuilder()

        // Metadata Header
        sb.append("STEEL AUTHORITY OF INDIA LIMITED (SAIL)\n")
        sb.append("INTERN MONTHLY ATTENDANCE REGISTER - ${summary.monthYear}\n\n")

        sb.append("Intern Name,${profile.fullName},Intern ID,${profile.id}\n")
        sb.append("Department,${profile.department},Group / Book,${profile.groupName ?: "General"}\n")
        sb.append("College,${profile.college},Joining Date,${profile.joiningDate}\n\n")

        // Summary Statistics
        sb.append("ATTENDANCE SUMMARY METRICS\n")
        sb.append("Total Working Days,${summary.totalWorkingDays}\n")
        sb.append("Days Present,${summary.presentDays}\n")
        sb.append("Days Absent,${summary.absentDays}\n")
        sb.append("Admissible Leaves,${summary.leaveDays}\n")
        sb.append("Admissible Holidays,${summary.holidayDays}\n")
        sb.append("Effective Attendance %,${String.format("%.1f", summary.attendancePercentage)}%\n\n")

        // Daily Register Table
        sb.append("DAILY ATTENDANCE SHEET\n")
        sb.append("Date,Day,Status,Check-In Time,Marked By,Remarks\n")

        records.forEach { record ->
            sb.append("${record.date},")
            sb.append("${record.date.dayOfWeek},")
            sb.append("${record.status.label},")
            sb.append("${record.checkInTime ?: "--:--"},")
            sb.append("${record.markedBy},")
            sb.append("\"${record.remarks ?: "Official Record"}\"\n")
        }

        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "SAIL_Attendance_${profile.id}_${summary.monthYear.replace(" ", "_")}.csv")
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share file (PDF or Excel) via Android share sheet or WhatsApp
     */
    fun shareReportFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Official attendance report from SAIL Intern Attendance Management Application (SIAMD).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title"))
    }

    /**
     * Share group invite QR & link directly via WhatsApp
     */
    fun shareGroupViaWhatsApp(context: Context, groupName: String, joiningDate: LocalDate, qrPayload: String) {
        val message = """
            📢 *SAIL Intern Group Invitation*
            
            You have been assigned to:
            🏢 *Group:* $groupName
            📅 *Joining Date:* $joiningDate
            
            Use your registered email to create your Intern ID in the SIAMD app, then scan this QR code or use the join key:
            🔑 *Join Key:* `$qrPayload`
            
            _Steel Authority of India Limited (Plant Training Cell)_
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp is not installed, fallback to general share chooser
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallback, "Share Group Invite via"))
        }
    }
}
