package com.deepconnect.vpn.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.deepconnect.vpn.data.AppSettings
import com.deepconnect.vpn.data.Profile
import com.deepconnect.vpn.data.ProfileStore
import com.deepconnect.vpn.vpn.ConnectionController
import com.deepconnect.vpn.vpn.LogEntry
import com.deepconnect.vpn.vpn.UserPrompt
import com.deepconnect.vpn.vpn.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-facing state holder.
 *
 * The VPN engine and the profile store are process-wide singletons, so the ViewModel mostly
 * re-exposes their flows and translates UI intents into controller calls. The one piece of
 * real state it owns is the VPN consent handshake: [startConnect] asks the platform whether
 * the app already has VPN permission; when it does not, the consent [Intent] is published so
 * the hosting Activity can launch it and the profile is remembered until the result arrives.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store: ProfileStore = ProfileStore.get(app)
    private val settings: AppSettings = AppSettings(app)

    val profiles: StateFlow<List<Profile>> = store.profiles
    val defaultProfileId: StateFlow<Long?> = store.defaultProfileId
    val status: StateFlow<VpnStatus> = ConnectionController.status
    val logs: StateFlow<List<LogEntry>> = ConnectionController.logs
    val prompt: StateFlow<UserPrompt?> = ConnectionController.prompt

    /** Non-null when the Activity must launch the system VPN consent dialog. */
    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent: StateFlow<Intent?> = _consentIntent.asStateFlow()

    /** Alias for [consentIntent] with the terminology used by the consent flow. */
    val pendingConsent: StateFlow<Intent?> get() = consentIntent

    /** One-shot user-facing message (shown as a snackbar on the home screen). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _logLevel = MutableStateFlow(settings.logLevel)
    val logLevel: StateFlow<Int> = _logLevel.asStateFlow()

    private val _showTimestampsInLog = MutableStateFlow(settings.showTimestampsInLog)
    val showTimestampsInLog: StateFlow<Boolean> = _showTimestampsInLog.asStateFlow()

    private val _autoConnectOnLaunch = MutableStateFlow(settings.autoConnectOnLaunch)
    val autoConnectOnLaunch: StateFlow<Boolean> = _autoConnectOnLaunch.asStateFlow()

    private val _themeMode = MutableStateFlow(settings.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(settings.dynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private var pendingProfile: Profile? = null
    private var autoConnectAttempted = false

    /* ------------------------------------------------------------------ */
    /* profiles                                                           */
    /* ------------------------------------------------------------------ */

    fun profile(id: Long): Profile? = store.get(id)

    fun defaultProfile(): Profile? = store.defaultProfile()

    fun saveProfile(profile: Profile): Profile = store.upsert(profile)

    fun deleteProfile(id: Long) {
        store.delete(id)
    }

    fun setDefaultProfile(id: Long) {
        store.setDefaultProfile(id)
    }

    /* ------------------------------------------------------------------ */
    /* connection                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Called once when the UI is first created. When the user enabled "auto-connect on
     * launch" and the app is idle, connects to the default profile (asking for VPN consent
     * through the normal flow if the permission was revoked).
     */
    fun maybeAutoConnectOnLaunch(context: Context, fromLauncher: Boolean) {
        if (autoConnectAttempted) return
        autoConnectAttempted = true
        if (!fromLauncher) return
        if (!settings.autoConnectOnLaunch) return
        val current = status.value
        if (current is VpnStatus.Connecting ||
            current is VpnStatus.Connected ||
            current is VpnStatus.Disconnecting
        ) {
            return
        }
        val profile = store.defaultProfile() ?: return
        _message.value = "Connecting to ${profile.displayName}"
        startConnect(context, profile)
    }

    fun startConnect(context: Context, profile: Profile) {
        val current = status.value
        if (current is VpnStatus.Connecting ||
            current is VpnStatus.Connected ||
            current is VpnStatus.Disconnecting
        ) {
            return
        }
        val consent = ConnectionController.prepareIntent(context)
        if (consent == null) {
            ConnectionController.connect(context.applicationContext, profile)
        } else {
            pendingProfile = profile
            _consentIntent.value = consent
        }
    }

    /** Called by the Activity as soon as it has started the consent dialog. */
    fun onConsentLaunched() {
        _consentIntent.value = null
    }

    /** Called by the Activity with the result of the system VPN consent dialog. */
    fun onConsentResult(granted: Boolean) {
        val profile = pendingProfile
        pendingProfile = null
        if (granted && profile != null) {
            ConnectionController.connect(getApplication<Application>(), profile)
        } else if (!granted) {
            _message.value = "VPN permission is required to connect."
        }
    }

    fun disconnect(context: Context) {
        ConnectionController.disconnect(context.applicationContext)
    }

    /* ------------------------------------------------------------------ */
    /* logs & settings                                                    */
    /* ------------------------------------------------------------------ */

    fun clearLogs() {
        ConnectionController.clearLogs()
    }

    fun setLogLevel(level: Int) {
        val clamped = level.coerceIn(0, 3)
        settings.logLevel = clamped
        _logLevel.value = clamped
    }

    fun setShowTimestampsInLog(show: Boolean) {
        settings.showTimestampsInLog = show
        _showTimestampsInLog.value = show
    }

    fun setAutoConnectOnLaunch(enabled: Boolean) {
        settings.autoConnectOnLaunch = enabled
        _autoConnectOnLaunch.value = enabled
    }

    fun setThemeMode(mode: String) {
        val normalised = when (mode) {
            AppSettings.THEME_LIGHT, AppSettings.THEME_DARK -> mode
            else -> AppSettings.THEME_SYSTEM
        }
        settings.themeMode = normalised
        _themeMode.value = normalised
    }

    fun setDynamicColor(enabled: Boolean) {
        settings.dynamicColor = enabled
        _dynamicColor.value = enabled
    }

    fun consumeMessage() {
        _message.value = null
    }
}
