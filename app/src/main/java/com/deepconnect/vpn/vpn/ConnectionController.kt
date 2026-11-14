package com.deepconnect.vpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.core.content.ContextCompat
import com.deepconnect.vpn.BuildConfig
import com.deepconnect.vpn.data.Profile
import com.deepconnect.vpn.data.ProfileStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * In-process bridge between the UI and [DeepConnectVpnService].
 *
 * The VPN service runs in the same process, so a singleton with StateFlows is enough;
 * there is no Binder plumbing.
 */
object ConnectionController {

    private const val TAG = "ConnectionController"
    private const val PROMPT_TIMEOUT_MS = 180_000L
    private const val MAX_LOG_LINES = 600

    private val _status = MutableStateFlow<VpnStatus>(VpnStatus.Disconnected)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _prompt = MutableStateFlow<UserPrompt?>(null)
    val prompt: StateFlow<UserPrompt?> = _prompt.asStateFlow()

    @Volatile
    private var activeProfile: Profile? = null

    @Volatile
    private var appContext: Context? = null

    /** Called once from [com.deepconnect.vpn.DeepConnectApp] so the controller can persist state. */
    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    /* ------------------------------------------------------------------ */
    /* public API (UI thread)                                             */
    /* ------------------------------------------------------------------ */

    /** Returns the system consent intent that still needs to be shown, or null if granted. */
    fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)

    fun connect(context: Context, profile: Profile) {
        if (_status.value is VpnStatus.Connecting || _status.value is VpnStatus.Connected) {
            Log.i(TAG, "Ignoring connect(): a session is already active")
            return
        }
        activeProfile = profile
        _logs.value = emptyList()
        val intent = Intent(context, DeepConnectVpnService::class.java).apply {
            action = DeepConnectVpnService.ACTION_CONNECT
            putExtra(DeepConnectVpnService.EXTRA_PROFILE_ID, profile.id)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, DeepConnectVpnService::class.java).apply {
            action = DeepConnectVpnService.ACTION_DISCONNECT
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun answerCertificate(accept: Boolean, remember: Boolean) {
        val current = _prompt.value
        if (current is UserPrompt.Certificate) {
            _prompt.value = null
            current.answer.complete(CertificateAnswer(accept, remember))
        }
    }

    fun answerOneTimePassword(code: String?) {
        val current = _prompt.value
        if (current is UserPrompt.OneTimePassword) {
            _prompt.value = null
            current.answer.complete(code?.trim()?.takeIf { it.isNotEmpty() })
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /* ------------------------------------------------------------------ */
    /* service-side API                                                   */
    /* ------------------------------------------------------------------ */

    internal fun currentProfile(): Profile? = activeProfile

    internal fun rememberPinnedCertificate(profileId: Long, fingerprint: String) {
        val context = appContext ?: return
        ProfileStore.get(context).updatePinnedHash(profileId, fingerprint)
        log(1, "Pinned the server certificate fingerprint for this profile")
    }

    internal fun setStatus(status: VpnStatus) {
        _status.value = status
        if (status is VpnStatus.Disconnected || status is VpnStatus.Failed) {
            activeProfile = null
        }
    }

    internal fun updateDtls(state: DtlsState, cipher: String?) {
        val current = _status.value
        if (current is VpnStatus.Connected) {
            _status.value = current.copy(
                dtlsState = state,
                dtlsCipher = cipher ?: current.dtlsCipher,
            )
        }
    }

    internal fun updateStats(stats: TrafficStats) {
        val current = _status.value
        if (current is VpnStatus.Connected) {
            _status.value = current.copy(stats = stats)
        }
    }

    internal fun log(level: Int, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, message.trimEnd('\n'))
        val current = _logs.value
        val next = if (current.size >= MAX_LOG_LINES) {
            current.subList(current.size - MAX_LOG_LINES + 1, current.size) + entry
        } else {
            current + entry
        }
        _logs.value = next
        // The in-app log is user-facing and stays available; mirroring to logcat would let
        // any app with READ_LOGS (or adb) read handshake details in release builds.
        if (BuildConfig.DEBUG) {
            if (level == 0) Log.w(TAG, message) else Log.d(TAG, message)
        }
    }

    /**
     * Blocks the native handshake thread until the user decides whether to trust a
     * certificate. Times out (and rejects) so a stuck prompt can never wedge the tunnel.
     */
    internal fun askCertificate(
        profileName: String,
        message: String,
        fingerprint: String,
        details: String,
    ): CertificateAnswer {
        val prompt = UserPrompt.Certificate(profileName, message, fingerprint, details)
        _prompt.value = prompt
        val answer = runBlocking {
            withTimeoutOrNull(PROMPT_TIMEOUT_MS) { prompt.answer.await() }
        } ?: CertificateAnswer(accept = false, remember = false)
        if (_prompt.value === prompt) _prompt.value = null
        return answer
    }

    /** Blocks the native handshake thread until a one-time code is entered (or cancelled). */
    internal fun askOneTimePassword(profileName: String, label: String): String? {
        val prompt = UserPrompt.OneTimePassword(profileName, label)
        _prompt.value = prompt
        val answer = runBlocking {
            withTimeoutOrNull(PROMPT_TIMEOUT_MS) { prompt.answer.await() }
        }
        if (_prompt.value === prompt) _prompt.value = null
        return answer
    }

    /** Releases any UI prompt that is still waiting when the tunnel is torn down. */
    internal fun cancelPendingPrompts() {
        when (val current = _prompt.value) {
            is UserPrompt.Certificate -> current.answer.complete(CertificateAnswer(false, false))
            is UserPrompt.OneTimePassword -> current.answer.complete(null)
            null -> {}
        }
        _prompt.value = null
    }

    /** Used by the service to know whether the session it started is still wanted. */
    internal fun isActive(profileId: Long): Boolean {
        return activeProfile?.id == profileId
    }
}
