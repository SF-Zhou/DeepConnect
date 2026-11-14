package com.deepconnect.vpn.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.ServiceCompat
import com.deepconnect.vpn.MainActivity
import com.deepconnect.vpn.data.AppSettings
import com.deepconnect.vpn.data.Profile
import com.deepconnect.vpn.data.ProfileStore
import org.infradead.libopenconnect.LibOpenConnect
import java.io.IOException

/**
 * Foreground [VpnService] that owns the tunnel.
 *
 * The handshake and packet pump run on a dedicated thread because the native library
 * blocks in [LibOpenConnect.mainloop]; the tun file descriptor comes from this service
 * and is handed to the library with `setupTunFD()`, so no root or helper binary is needed.
 */
class DeepConnectVpnService : VpnService() {

    private val workerLock = Any()
    private val tunLock = Any()

    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var session: OpenConnectSession? = null

    @Volatile
    private var stopping = false

    private var tun: ParcelFileDescriptor? = null
    private var activeProfile: Profile? = null

    private lateinit var notifier: VpnNotifier
    private lateinit var settings: AppSettings
    private lateinit var store: ProfileStore
    private val handler = Handler(Looper.getMainLooper())

    private val statsTicker = object : Runnable {
        override fun run() {
            session?.requestStats()
            handler.postDelayed(this, STATS_INTERVAL_MS)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runCatching { setUnderlyingNetworks(arrayOf(network)) }
        }

        override fun onLost(network: Network) {
            runCatching { setUnderlyingNetworks(null) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notifier = VpnNotifier(this)
        notifier.ensureChannel()
        settings = AppSettings(this)
        store = ProfileStore.get(this)
        runCatching {
            getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(networkCallback)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 0L)
                val profile = store.get(profileId)
                if (profile == null) {
                    Log.w(TAG, "Connect requested for a missing profile ($profileId)")
                    stopSelf()
                    return START_NOT_STICKY
                }
                val connecting = VpnStatus.Connecting(profile.id, profile.displayName, "Starting")
                startForegroundCompat(notifier.build(connecting))
                ConnectionController.setStatus(connecting)
                startTunnel(profile)
            }
            ACTION_DISCONNECT -> stopTunnel(userInitiated = true)
            else -> {
                if (ConnectionController.status.value == VpnStatus.Disconnected) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        log(0, "VPN permission revoked; closing the tunnel")
        stopTunnel(userInitiated = false)
        super.onRevoke()
    }

    override fun onDestroy() {
        stopping = true
        ConnectionController.cancelPendingPrompts()
        session?.let { runCatching { it.cancel() } }
        closeTun()
        handler.removeCallbacksAndMessages(null)
        runCatching {
            getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(networkCallback)
        }
        super.onDestroy()
    }

    /* ------------------------------------------------------------------ */
    /* tunnel lifecycle                                                   */
    /* ------------------------------------------------------------------ */

    private fun startTunnel(profile: Profile) {
        synchronized(workerLock) {
            if (worker?.isAlive == true) {
                log(0, "A tunnel thread is already running; ignoring connect request")
                return
            }
            stopping = false
            activeProfile = profile
            worker = Thread({ runTunnel(profile) }, "deepconnect-tunnel").apply {
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
        }
    }

    private fun stopTunnel(userInitiated: Boolean) {
        val profile = activeProfile
        synchronized(workerLock) {
            stopping = true
            ConnectionController.cancelPendingPrompts()
            session?.let { runCatching { it.cancel() } }
            val thread = worker
            if (thread == null || !thread.isAlive) {
                ConnectionController.setStatus(VpnStatus.Disconnected)
                shutdownService()
                return
            }
        }
        if (profile != null) {
            ConnectionController.setStatus(VpnStatus.Disconnecting(profile.displayName))
            notifier.update(VpnStatus.Disconnecting(profile.displayName))
        }
        if (userInitiated) log(1, "Disconnecting…")
    }

    private fun runTunnel(profile: Profile) {
        var current: OpenConnectSession? = null
        var failure: String? = null
        var cancelled = false
        var dtlsUnavailable = false

        try {
            OpenConnectNative.ensureLoaded()

            val caBundle = CaBundle.build(this, profile.extraCaPem)
            if (caBundle == null) {
                log(0, "No CA certificates found on this device; TLS may fail")
            }

            current = OpenConnectSession(
                profile = profile,
                protectSocket = ::protectSocket,
                onStats = ::onTrafficStats,
                onDtlsState = ::onDtlsState,
            )
            session = current
            current.applyProfile(caBundle?.absolutePath, settings.logLevel)

            updateStage(profile, "Contacting ${profile.serverHost}")
            val parsed = current.parseURL(profile.server)
            if (parsed != 0) throw TunnelException("Could not parse server address (error $parsed)")

            updateStage(profile, "Authenticating")
            val cookie = current.obtainCookie()
            if (cookie != 0) {
                cancelled = current.isCanceled
                throw TunnelException(
                    if (cookie > 0) "Authentication was cancelled" else "Authentication failed (error $cookie)"
                )
            }

            updateStage(profile, "Negotiating tunnel")
            val cstp = current.makeCSTPConnection()
            if (cstp != 0) throw TunnelException("Gateway refused the tunnel connection (error $cstp)")

            val ipInfo = current.getIPInfo() ?: throw TunnelException("Gateway sent no IP configuration")

            updateStage(profile, "Configuring interface")
            val fd = establishTun(profile, ipInfo)
            synchronized(tunLock) { tun = fd }

            val tunResult = current.setupTunFD(fd.fd)
            if (tunResult != 0) throw TunnelException("Could not attach the tunnel interface (error $tunResult)")

            if (!profile.disableDtls) {
                updateStage(profile, "Starting DTLS")
                val dtls = current.setupDTLS(DTLS_ATTEMPT_PERIOD)
                if (dtls != 0) {
                    log(1, "DTLS could not be started (error $dtls); continuing over TLS only")
                    dtlsUnavailable = true
                }
            }

            val connected = VpnStatus.Connected(
                profileId = profile.id,
                profileName = profile.displayName,
                server = profile.serverHost,
                since = System.currentTimeMillis(),
                ipv4 = ipInfo.addr?.takeIf { it.isNotBlank() },
                ipv6 = ipInfo.addr6?.takeIf { it.isNotBlank() },
                mtu = ipInfo.MTU,
                dns = ipInfo.DNS?.filter { it.isNotBlank() }.orEmpty(),
                cstpCipher = runCatching { current.getCSTPCipher() }.getOrNull(),
                dtlsCipher = runCatching { current.getDTLSCipher() }.getOrNull(),
                dtlsState = when {
                    profile.disableDtls -> DtlsState.DISABLED
                    dtlsUnavailable -> DtlsState.UNAVAILABLE
                    else -> DtlsState.NEGOTIATING
                },
                protocol = runCatching { current.getProtocol() }.getOrNull().orEmpty(),
            )
            ConnectionController.setStatus(connected)
            notifier.update(connected)
            settings.lastConnectedProfileId = profile.id
            log(1, "Tunnel established: ${connected.ipv4 ?: "-"} ${connected.ipv6 ?: ""} mtu=${connected.mtu}")
            startStatsTicker()

            val result = current.mainloop(RECONNECT_TIMEOUT_SECONDS, LibOpenConnect.RECONNECT_INTERVAL_MIN)
            stopStatsTicker()
            cancelled = current.isCanceled
            if (!cancelled && result != 0) {
                failure = "Tunnel closed (error $result)"
            }
        } catch (t: TunnelException) {
            failure = t.message
        } catch (t: IOException) {
            failure = t.message ?: "I/O error"
        } catch (t: UnsatisfiedLinkError) {
            failure = "Native library could not be loaded: ${t.message}"
        } catch (t: Throwable) {
            Log.e(TAG, "Tunnel thread failed", t)
            failure = t.message ?: t.javaClass.simpleName
        } finally {
            stopStatsTicker()
            ConnectionController.cancelPendingPrompts()
            runCatching { current?.destroy() }
            session = null
            closeTun()

            if (failure != null && !stopping) {
                log(0, failure)
                ConnectionController.setStatus(
                    VpnStatus.Failed(profile.id, profile.displayName, failure)
                )
            } else {
                if (cancelled) log(1, "Disconnected")
                ConnectionController.setStatus(VpnStatus.Disconnected)
            }
            activeProfile = null
            shutdownService()
        }
    }

    /* ------------------------------------------------------------------ */
    /* tun device                                                         */
    /* ------------------------------------------------------------------ */

    private fun establishTun(profile: Profile, ip: LibOpenConnect.IPInfo): ParcelFileDescriptor {
        val builder = Builder()
        builder.setSession("DeepConnect: ${profile.displayName}".take(64))
        builder.setMtu(if (ip.MTU > 0) ip.MTU else DEFAULT_MTU)

        val ipv4 = ip.addr?.takeIf { it.isNotBlank() && IpUtils.isIpv4(it) }
        val ipv6 = ip.addr6?.takeIf { it.isNotBlank() }

        if (ipv4 != null) {
            builder.addAddress(ipv4, IpUtils.ipv4PrefixLength(ip.netmask))
        }
        if (ipv6 != null) {
            builder.addAddress(ipv6, IpUtils.ipv6PrefixLength(ip.netmask6))
        }
        if (ipv4 == null && ipv6 == null) {
            throw TunnelException("Gateway supplied neither an IPv4 nor an IPv6 address")
        }

        for (dns in ip.DNS.orEmpty()) {
            if (dns.isBlank()) continue
            runCatching { builder.addDnsServer(dns) }
                .onFailure { log(2, "Ignoring DNS server '$dns': ${it.message}") }
        }
        // Cisco sometimes routes DNS through the tunnel address; make sure it is reachable.
        ip.gatewayAddr?.takeIf { it.isNotBlank() && ipv4 != null }?.let {
            runCatching { builder.addDnsServer(it) }
        }

        val includes = ip.splitIncludes.orEmpty().filter { it.isNotBlank() }
        if (includes.isEmpty()) {
            if (ipv4 != null) builder.addRoute("0.0.0.0", 0)
            if (ipv6 != null) builder.addRoute("::", 0)
        } else {
            log(1, "Split tunnel: installing ${includes.size} route(s)")
            for (cidr in includes) {
                val parsed = IpUtils.parseCidr(cidr)
                if (parsed == null) {
                    log(1, "Ignoring malformed split route '$cidr'")
                    continue
                }
                runCatching { builder.addRoute(parsed.first, parsed.second) }
            }
            if (ipv4 != null && includes.none { it.startsWith("0.0.0.0") }) {
                // Reach DNS servers that live outside the split tunnel.
                for (dns in ip.DNS.orEmpty()) {
                    runCatching { builder.addRoute(dns, 32) }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (cidr in ip.splitExcludes.orEmpty().filter { it.isNotBlank() }) {
                val parts = IpUtils.parseCidr(cidr) ?: continue
                val prefix = runCatching {
                    android.net.IpPrefix(java.net.InetAddress.getByName(parts.first), parts.second)
                }.getOrNull() ?: continue
                runCatching { builder.excludeRoute(prefix) }
                    .onFailure { log(1, "Ignoring exclude route '$cidr': ${it.message}") }
            }
        } else if (!ip.splitExcludes.isNullOrEmpty()) {
            log(1, "Split-exclude routes need Android 13+; those destinations stay inside the tunnel")
        }

        try {
            return builder.establish() ?: throw TunnelException("The system refused to create the tunnel interface")
        } catch (e: IllegalStateException) {
            throw TunnelException("The system refused to create the tunnel interface: ${e.message}")
        }
    }

    private fun closeTun() {
        synchronized(tunLock) {
            val fd = tun
            tun = null
            runCatching { fd?.close() }
        }
    }

    /* ------------------------------------------------------------------ */
    /* callbacks                                                          */
    /* ------------------------------------------------------------------ */

    private fun protectSocket(fd: Int): Boolean {
        val ok = runCatching { protect(fd) }.getOrElse {
            log(0, "VpnService.protect($fd) failed: ${it.message}")
            return false
        }
        // Debug builds mirror this to logcat; it proves the DTLS/TLS sockets bypass the tun.
        log(2, "protect(fd=$fd) -> $ok")
        if (!ok) log(0, "VpnService.protect($fd) returned false")
        return ok
    }

    private fun onDtlsState(state: DtlsState, cipher: String?) {
        ConnectionController.updateDtls(state, cipher)
        if (state == DtlsState.ACTIVE) {
            log(1, "DTLS is active${if (!cipher.isNullOrBlank()) " ($cipher)" else ""}")
        }
        val status = ConnectionController.status.value
        if (status is VpnStatus.Connected) notifier.update(status)
    }

    private fun onTrafficStats(stats: TrafficStats) {
        ConnectionController.updateStats(stats)
        val status = ConnectionController.status.value
        if (status is VpnStatus.Connected) {
            notifier.update(status)
        }
    }

    private fun startStatsTicker() {
        handler.removeCallbacks(statsTicker)
        handler.postDelayed(statsTicker, STATS_INTERVAL_MS)
    }

    private fun stopStatsTicker() {
        handler.removeCallbacks(statsTicker)
    }

    /* ------------------------------------------------------------------ */
    /* misc                                                               */
    /* ------------------------------------------------------------------ */

    private fun updateStage(profile: Profile, stage: String) {
        if (stopping) return
        val status = VpnStatus.Connecting(profile.id, profile.displayName, stage)
        ConnectionController.setStatus(status)
        notifier.update(status)
        log(2, stage)
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, VpnNotifier.NOTIFICATION_ID, notification, type)
        }.onFailure {
            Log.e(TAG, "startForeground failed", it)
            log(0, "Could not enter the foreground: ${it.message}")
        }
    }

    private fun shutdownService() {
        handler.post {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun log(level: Int, message: String) {
        ConnectionController.log(level, message)
    }

    private class TunnelException(message: String) : IOException(message)

    companion object {
        private const val TAG = "DeepConnectVpnService"

        const val ACTION_CONNECT = "com.deepconnect.vpn.action.CONNECT"
        const val ACTION_DISCONNECT = "com.deepconnect.vpn.action.DISCONNECT"
        const val EXTRA_PROFILE_ID = "com.deepconnect.vpn.extra.PROFILE_ID"

        private const val DEFAULT_MTU = 1400
        private const val STATS_INTERVAL_MS = 1500L
        private const val DTLS_ATTEMPT_PERIOD = 60
        private const val RECONNECT_TIMEOUT_SECONDS = 300
    }
}
