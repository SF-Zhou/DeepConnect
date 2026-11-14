package com.deepconnect.vpn.data

import android.content.Context
import android.content.SharedPreferences

/** Small, non-secret app preferences. */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 0 = errors only … 3 = trace. Maps to LibOpenConnect.PRG_* levels. */
    var logLevel: Int
        get() = prefs.getInt(KEY_LOG_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_LOG_LEVEL, value).apply()

    var lastConnectedProfileId: Long
        get() = prefs.getLong(KEY_LAST_PROFILE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PROFILE, value).apply()

    var showTimestampsInLog: Boolean
        get() = prefs.getBoolean(KEY_LOG_TIMESTAMPS, true)
        set(value) = prefs.edit().putBoolean(KEY_LOG_TIMESTAMPS, value).apply()

    /** Connect to the default profile as soon as the app is opened. */
    var autoConnectOnLaunch: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT_ON_LAUNCH, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT_ON_LAUNCH, value).apply()

    companion object {
        private const val KEY_LOG_LEVEL = "log_level"
        private const val KEY_LAST_PROFILE = "last_profile"
        private const val KEY_LOG_TIMESTAMPS = "log_timestamps"
        private const val KEY_AUTO_CONNECT_ON_LAUNCH = "auto_connect_on_launch"
    }
}
