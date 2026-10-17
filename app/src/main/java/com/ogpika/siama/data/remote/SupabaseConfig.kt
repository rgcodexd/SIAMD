package com.ogpika.siama.data.remote

import android.content.Context
import android.content.SharedPreferences

object SupabaseConfig {

    private const val PREFS_NAME = "siamd_supabase_config"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    // Default configuration (User can set directly here or via in-app Settings dialog)
    const val DEFAULT_URL = "https://ftnueczhuwoxlmllvkgt.supabase.co"
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ0bnVlY3podXdveGxtbGx2a2d0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIyMTE0MTksImV4cCI6MjEwNzc4NzQxOX0.gFlN4DzfeyHPKQswjWBa6lbkFwhbGi27co5uy0lYBYU"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSupabaseUrl(): String {
        val saved = prefs?.getString(KEY_URL, null)
        return if (!saved.isNullOrBlank()) saved else DEFAULT_URL
    }

    fun getSupabaseAnonKey(): String {
        val saved = prefs?.getString(KEY_ANON_KEY, null)
        return if (!saved.isNullOrBlank()) saved else DEFAULT_ANON_KEY
    }

    fun saveConfig(url: String, anonKey: String) {
        val cleanUrl = url.trim().trimEnd('/')
        prefs?.edit()
            ?.putString(KEY_URL, cleanUrl)
            ?.putString(KEY_ANON_KEY, anonKey.trim())
            ?.apply()
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()
        return url.isNotBlank() && key.isNotBlank() && url.startsWith("https://")
    }

    fun clearConfig() {
        prefs?.edit()?.clear()?.apply()
    }
}
