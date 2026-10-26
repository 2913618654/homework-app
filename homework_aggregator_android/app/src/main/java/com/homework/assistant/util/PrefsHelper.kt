package com.homework.assistant.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.homework.assistant.model.HomeworkItem

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hw_assistant_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var chaoXingCookie: String
        get() = prefs.getString("cx_cookie", "") ?: ""
        set(value) = prefs.edit().putString("cx_cookie", value).apply()

    var chaoXingAccount: String
        get() = prefs.getString("cx_account", "") ?: ""
        set(value) = prefs.edit().putString("cx_account", value).apply()

    var mosoteachCookie: String
        get() = prefs.getString("ms_cookie", "") ?: ""
        set(value) = prefs.edit().putString("ms_cookie", value).apply()

    var mosoteachAccount: String
        get() = prefs.getString("ms_account", "") ?: ""
        set(value) = prefs.edit().putString("ms_account", value).apply()

    fun saveCachedHomeworks(list: List<HomeworkItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString("cached_homeworks", json).apply()
    }

    fun getCachedHomeworks(): List<HomeworkItem> {
        val json = prefs.getString("cached_homeworks", null) ?: return emptyList()
        val type = object : TypeToken<List<HomeworkItem>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
