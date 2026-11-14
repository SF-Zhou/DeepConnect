package com.deepconnect.vpn.vpn

import kotlinx.coroutines.CompletableDeferred

/** How the optional DTLS (UDP) side channel is doing. */
enum class DtlsState {
    /** Handshake in progress / no information yet. */
    NEGOTIATING,
    /** DTLS is established; traffic uses UDP for lower latency. */
    ACTIVE,
    /** DTLS could not be established (UDP blocked by a firewall/NAT, or not offered). */
    UNAVAILABLE,
    /** The user switched DTLS off for this profile. */
    DISABLED,
}

/** Coarse connection state shown by the UI and the notification. */
sealed interface VpnStatus {
    data object Disconnected : VpnStatus

    data class Connecting(
        val profileId: Long,
        val profileName: String,
        val stage: String,
    ) : VpnStatus

    data class Connected(
        val profileId: Long,
        val profileName: String,
        val server: String,
        /** System.currentTimeMillis() when the tunnel came up. */
        val since: Long,
        val ipv4: String?,
        val ipv6: String?,
        val mtu: Int,
        val dns: List<String>,
        val cstpCipher: String?,
        val dtlsCipher: String?,
        val dtlsState: DtlsState = DtlsState.NEGOTIATING,
        val protocol: String,
        val stats: TrafficStats = TrafficStats(),
    ) : VpnStatus

    data class Disconnecting(val profileName: String) : VpnStatus

    data class Failed(
        val profileId: Long,
        val profileName: String,
        val message: String,
    ) : VpnStatus
}

data class TrafficStats(
    val txPackets: Long = 0,
    val txBytes: Long = 0,
    val rxPackets: Long = 0,
    val rxBytes: Long = 0,
)

data class LogEntry(
    val timestamp: Long,
    /** Mirrors LibOpenConnect.PRG_* (0=error, 1=info, 2=debug, 3=trace). */
    val level: Int,
    val message: String,
)

data class CertificateAnswer(val accept: Boolean, val remember: Boolean)

/** A question the native handshake is blocked on; the UI must answer it. */
sealed class UserPrompt {
    data class Certificate(
        val profileName: String,
        val message: String,
        val fingerprint: String,
        val details: String,
    ) : UserPrompt() {
        internal val answer = CompletableDeferred<CertificateAnswer>()
    }

    data class OneTimePassword(
        val profileName: String,
        val label: String,
    ) : UserPrompt() {
        internal val answer = CompletableDeferred<String?>()
    }
}
