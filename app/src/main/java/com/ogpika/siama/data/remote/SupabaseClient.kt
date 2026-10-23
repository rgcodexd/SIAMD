package com.ogpika.siama.data.remote

import com.ogpika.siama.model.AttendanceStatus
import com.ogpika.siama.model.DailyAttendance
import com.ogpika.siama.model.Department
import com.ogpika.siama.model.InternGroup
import com.ogpika.siama.model.InternProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Resilient interceptor to handle transient Supabase API Gateway issues (502, 503, 504, 408, network timeouts)
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        var tryCount = 0
        val maxAttempts = 3

        while (tryCount < maxAttempts) {
            tryCount++
            try {
                response?.close()
                response = chain.proceed(request)
                // If the response is not a transient gateway or server failure, return it immediately
                if (response.code !in listOf(408, 502, 503, 504)) {
                    return@Interceptor response
                }
            } catch (e: IOException) {
                lastException = e
            }

            if (tryCount < maxAttempts) {
                try {
                    Thread.sleep(tryCount * 350L) // Exponential backoff: 350ms, 700ms
                } catch (ignored: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        response ?: throw (lastException ?: IOException("Request to Supabase failed after $maxAttempts attempts"))
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(retryInterceptor)
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
            val gwVersion = response.header("sb-gateway-version")?.let { " (Gateway: $it)" } ?: ""
            if (response.isSuccessful) {
                Pair(true, "Successfully connected to Supabase database (HTTP ${response.code}$gwVersion).")
            } else {
                val errorBody = response.body?.string() ?: ""
                Pair(false, "Supabase returned HTTP ${response.code}$gwVersion: $errorBody")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Fetch intern profiles from public.profiles
     */
    private fun parseInternProfile(obj: JSONObject): InternProfile {
        val id = obj.optString("id", "")
        val email = obj.optString("email", "")
        val fullName = obj.optString("full_name", "")
        val phone = obj.optString("phone", "")
        val college = obj.optString("college", "")
        val dept = obj.optString("department", "")
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

        return InternProfile(
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
    }

    /**
     * Check if a profile already exists in Supabase by email
     */
    suspend fun fetchProfileByEmail(email: String): InternProfile? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val request = buildRequest("profiles?email=eq.$cleanEmail&select=*", "GET") ?: return@withContext null
        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    return@withContext parseInternProfile(obj)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Fetch intern profiles from public.profiles (excludes admin records)
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
                    val role = obj.optString("role", "INTERN")
                    // Exclude ADMIN account from intern roster
                    if (role.equals("ADMIN", ignoreCase = true)) continue
                    list.add(parseInternProfile(obj))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Insert or Upsert intern profile into Supabase.
     * Uses ?on_conflict=email to prevent duplicate key constraint crashes.
     */
    suspend fun insertInternProfile(profile: InternProfile): Boolean = withContext(Dispatchers.IO) {
        val dept = if (profile.department.isNotBlank()) profile.department else "Plant Engineering"
        val fullJson = JSONObject().apply {
            put("id", profile.id)
            put("email", profile.email)
            put("full_name", profile.fullName)
            put("role", "INTERN")
            if (profile.phone.isNotBlank()) put("phone", profile.phone)
            if (profile.college.isNotBlank()) put("college", profile.college)
            put("department", dept)
            put("intern_id_number", profile.internIdNumber ?: profile.id)
            put("internship_start_date", profile.joiningDate.toString())
            put("internship_end_date", profile.joiningDate.plusMonths(6).toString())
            if (profile.groupId != null) put("group_id", profile.groupId)
            if (profile.groupName != null) put("group_name", profile.groupName)
            if (profile.address.isNotBlank()) put("address", profile.address)
            if (profile.emergencyContact.isNotBlank()) put("emergency_contact", profile.emergencyContact)
            put("auth_provider", profile.authProvider)
        }

        // Try upserting with on_conflict=email
        val primaryReq = buildRequest("profiles?on_conflict=email", "POST", fullJson.toString(), preferHeader = "resolution=merge-duplicates,return=representation")
            ?: return@withContext false
        try {
            val response = httpClient.newCall(primaryReq).execute()
            if (response.isSuccessful) return@withContext true

            // Fallback to standard upsert by primary key id
            val fbReq = buildRequest("profiles", "POST", fullJson.toString(), preferHeader = "resolution=merge-duplicates,return=representation")
            if (fbReq != null) {
                val fbRes = httpClient.newCall(fbReq).execute()
                if (fbRes.isSuccessful) return@withContext true
            }

            // Fallback for minimalist schema
            val errBody = response.body?.string() ?: ""
            if (response.code == 400 || errBody.contains("PGRST204")) {
                val minJson = JSONObject().apply {
                    put("id", profile.id)
                    put("email", profile.email)
                    put("full_name", profile.fullName)
                    put("role", "INTERN")
                    put("department", dept)
                    put("intern_id_number", profile.internIdNumber ?: profile.id)
                    put("internship_start_date", profile.joiningDate.toString())
                    put("internship_end_date", profile.joiningDate.plusMonths(6).toString())
                    put("auth_provider", profile.authProvider)
                }
                val minReq = buildRequest("profiles?on_conflict=email", "POST", minJson.toString(), preferHeader = "resolution=merge-duplicates,return=representation")
                if (minReq != null) {
                    val fallbackRes = httpClient.newCall(minReq).execute()
                    return@withContext fallbackRes.isSuccessful
                }
            }
            false
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
        email: String,
        fullName: String,
        phone: String,
        college: String,
        department: String,
        internIdNumber: String,
        emergencyContact: String,
        address: String
    ): Boolean = withContext(Dispatchers.IO) {
        val fullJson = JSONObject().apply {
            put("full_name", fullName)
            put("phone", phone)
            put("college", college)
            put("department", department.ifBlank { "Plant Engineering" })
            put("intern_id_number", internIdNumber)
            put("emergency_contact", emergencyContact)
            put("address", address)
        }
        val request = buildRequest("profiles?id=eq.$id", "PATCH", fullJson.toString()) ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) return@withContext true

            // Try patching by email if id was not found
            if (email.isNotBlank()) {
                val emailReq = buildRequest("profiles?email=eq.$email", "PATCH", fullJson.toString())
                if (emailReq != null) {
                    val emailRes = httpClient.newCall(emailReq).execute()
                    if (emailRes.isSuccessful) return@withContext true
                }
            }

            val errBody = response.body?.string() ?: ""
            if (response.code == 400 || errBody.contains("PGRST204")) {
                val minJson = JSONObject().apply {
                    put("full_name", fullName)
                    put("department", department.ifBlank { "Plant Engineering" })
                    put("intern_id_number", internIdNumber)
                }
                val minReq = buildRequest("profiles?id=eq.$id", "PATCH", minJson.toString())
                if (minReq != null) {
                    val fallbackRes = httpClient.newCall(minReq).execute()
                    return@withContext fallbackRes.isSuccessful
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch all departments created dynamically by the Admin
     */
    suspend fun fetchDepartments(): List<Department> = withContext(Dispatchers.IO) {
        val request = buildRequest("departments?select=*&order=created_at.asc", "GET") ?: return@withContext emptyList()
        val list = mutableListOf<Department>()
        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Department(
                            id = obj.optString("id", ""),
                            name = obj.optString("name", ""),
                            code = obj.optString("code", ""),
                            description = obj.optString("description", "")
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
     * Insert a new department created by the Admin
     */
    suspend fun insertDepartment(dept: Department): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", dept.id)
            put("name", dept.name)
            put("code", dept.code)
            put("description", dept.description)
        }
        val request = buildRequest("departments", "POST", json.toString(), preferHeader = "resolution=merge-duplicates,return=representation")
            ?: return@withContext false
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a department from Supabase
     */
    suspend fun deleteDepartment(id: String): Boolean = withContext(Dispatchers.IO) {
        val request = buildRequest("departments?id=eq.$id", "DELETE") ?: return@withContext false
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
