package com.ogpika.siama.data.remote

import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.DailyAttendance
import com.ogpika.siama.model.InternGroup
import com.ogpika.siama.model.InternProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(
        endpoint: String,
        method: String = "GET",
        bodyJson: String? = null,
        preferHeader: String = "return=representation"
    ): Request? {
        val baseUrl = SupabaseConfig.getSupabaseUrl()
        val anonKey = SupabaseConfig.getSupabaseAnonKey()

        if (baseUrl.isBlank() || anonKey.isBlank()) return null

        val fullUrl = "$baseUrl/rest/v1/$endpoint"
        val builder = Request.Builder()
            .url(fullUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", preferHeader)

        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((bodyJson ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch((bodyJson ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete()
        }

        return builder.build()
    }

    /**
     * Tests live connection to Supabase database by pinging the holiday_calendar table
     */
    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Pair(false, "Supabase URL and Anon Key are not configured.")
        }

        val request = buildRequest("holiday_calendar?select=count", "GET")
            ?: return@withContext Pair(false, "Invalid Supabase configuration.")

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Pair(true, "Successfully connected to Supabase database (HTTP ${response.code}).")
            } else {
                val errorBody = response.body?.string() ?: ""
                Pair(false, "Supabase returned HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Fetch intern profiles from public.profiles
     */
    suspend fun fetchInternProfiles(): List<InternProfile> = withContext(Dispatchers.IO) {
        val request = buildRequest("profiles?select=*&order=created_at.desc", "GET") ?: return@withContext emptyList()
        val list = mutableListOf<InternProfile>()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val email = obj.optString("email", "")
                    val fullName = obj.optString("full_name", "")
                    val phone = obj.optString("phone", "")
                    val college = obj.optString("college", "")
                    val dept = obj.optString("department", "HRD Department")
                    val groupId = obj.optString("group_id", "").ifBlank { null }
                    val groupName = obj.optString("group_name", "").ifBlank { null }
                    val internIdNum = obj.optString("intern_id_number", "").ifBlank { null }
                    val mentor = obj.optString("mentor_name", "DGM Plant Operations")
                    val addr = obj.optString("address", "")
                    val emergency = obj.optString("emergency_contact", "")
                    val authProv = obj.optString("auth_provider", "EMAIL")
                    val joiningStr = obj.optString("internship_start_date", "")

                    val joiningDate = try {
                        if (joiningStr.isNotBlank()) LocalDate.parse(joiningStr) else LocalDate.now()
                    } catch (e: Exception) {
                        LocalDate.now()
                    }

                    list.add(
                        InternProfile(
                            id = id,
                            email = email,
                            fullName = fullName,
                            phone = phone,
                            college = college,
                            joiningDate = joiningDate,
                            department = dept,
                            groupId = groupId,
                            groupName = groupName,
                            internIdNumber = internIdNum,
                            mentorName = mentor,
                            address = addr,
                            emergencyContact = emergency,
                            authProvider = authProv
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Insert new intern profile into Supabase
     */
    suspend fun insertInternProfile(profile: InternProfile): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", profile.id)
            put("email", profile.email)
            put("full_name", profile.fullName)
            put("phone", profile.phone)
            put("college", profile.college)
            put("department", profile.department)
            put("intern_id_number", profile.internIdNumber ?: profile.id)
            put("internship_start_date", profile.joiningDate.toString())
            put("internship_end_date", profile.joiningDate.plusMonths(6).toString())
            if (profile.groupId != null) put("group_id", profile.groupId)
            if (profile.groupName != null) put("group_name", profile.groupName)
            put("address", profile.address)
            put("emergency_contact", profile.emergencyContact)
            put("auth_provider", profile.authProvider)
        }

        val request = buildRequest("profiles", "POST", json.toString()) ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Complete mandatory profile onboarding
     */
    suspend fun updateMandatoryProfile(
        id: String,
        fullName: String,
        phone: String,
        college: String,
        department: String,
        internIdNumber: String,
        emergencyContact: String,
        address: String
    ): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("full_name", fullName)
            put("phone", phone)
            put("college", college)
            put("department", department)
            put("intern_id_number", internIdNumber)
            put("emergency_contact", emergencyContact)
            put("address", address)
        }
        val request = buildRequest("profiles?id=eq.$id", "PATCH", json.toString()) ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete an intern profile from Supabase
     */
    suspend fun deleteInternProfile(id: String): Boolean = withContext(Dispatchers.IO) {
        val request = buildRequest("profiles?id=eq.$id", "DELETE") ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update an intern profile in Supabase
     */
    suspend fun updateInternProfile(id: String, phone: String, college: String, address: String, emergency: String): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("phone", phone)
            put("college", college)
            put("address", address)
            put("emergency_contact", emergency)
        }
        val request = buildRequest("profiles?id=eq.$id", "PATCH", json.toString()) ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Upsert daily attendance record in public.attendance_logs
     */
    suspend fun upsertDailyAttendance(record: DailyAttendance): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("intern_id", record.internId)
            put("attendance_date", record.date.toString())
            put("status", record.status.name)
            put("location_verified", true)
        }

        val request = buildRequest("attendance_logs", "POST", json.toString(), "resolution=merge-duplicates") ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Submit leave application to Supabase
     */
    suspend fun submitLeaveApplication(
        applicationId: String,
        internId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        totalDays: Double,
        reason: String
    ): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("intern_id", internId)
            put("start_date", startDate.toString())
            put("end_date", endDate.toString())
            put("total_days", totalDays)
            put("reason", reason)
            put("status", "PENDING")
        }
        val request = buildRequest("leave_applications", "POST", json.toString()) ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch groups from public.intern_groups
     */
    suspend fun fetchInternGroups(): List<InternGroup> = withContext(Dispatchers.IO) {
        val request = buildRequest("intern_groups?select=*&order=created_at.desc", "GET") ?: return@withContext emptyList()
        val list = mutableListOf<InternGroup>()
        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val name = obj.optString("name", "")
                    val joiningStr = obj.optString("joining_date", "")
                    val dept = obj.optString("department", "General")
                    val desc = obj.optString("description", "")
                    val joiningDate = try {
                        if (joiningStr.isNotBlank()) LocalDate.parse(joiningStr) else LocalDate.now()
                    } catch (e: Exception) {
                        LocalDate.now()
                    }
                    list.add(InternGroup(id = id, name = name, joiningDate = joiningDate, department = dept, description = desc))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Insert new group into Supabase
     */
    suspend fun insertInternGroup(group: InternGroup): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", group.id)
            put("name", group.name)
            put("joining_date", group.joiningDate.toString())
            put("department", group.department)
            put("description", group.description)
        }
        val request = buildRequest("intern_groups", "POST", json.toString()) ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch attendance records for a specific intern from public.attendance_logs
     */
    suspend fun fetchAttendanceLogs(internId: String): Map<LocalDate, DailyAttendance> = withContext(Dispatchers.IO) {
        val request = buildRequest("attendance_logs?intern_id=eq.$internId&select=*", "GET") ?: return@withContext emptyMap()
        val map = mutableMapOf<LocalDate, DailyAttendance>()
        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val dateStr = obj.optString("attendance_date", "")
                    val statusStr = obj.optString("status", "ABSENT")
                    val date = try { LocalDate.parse(dateStr) } catch (e: Exception) { null }
                    val status = try { AttendanceStatus.valueOf(statusStr) } catch (e: Exception) { AttendanceStatus.ABSENT }
                    if (date != null) {
                        map[date] = DailyAttendance(
                            id = "$internId-$date",
                            internId = internId,
                            date = date,
                            status = status,
                            checkInTime = if (status == AttendanceStatus.PRESENT) "09:00 AM" else null,
                            markedBy = "ADMIN",
                            remarks = "Synced from Supabase"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        map
    }
}
