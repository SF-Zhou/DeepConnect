package com.deepconnect.vpn.ui

import com.deepconnect.vpn.data.Protocols
import com.deepconnect.vpn.vpn.DtlsState
import com.deepconnect.vpn.vpn.VpnStatus

/** Coarse UI phase derived from [VpnStatus]; stable across traffic-stat updates. */
enum class ConnectionPhase { Disconnected, Connecting, Connected, Disconnecting, Failed }

internal fun VpnStatus.phase(): ConnectionPhase = when (this) {
    is VpnStatus.Disconnected -> ConnectionPhase.Disconnected
    is VpnStatus.Connecting -> ConnectionPhase.Connecting
    is VpnStatus.Connected -> ConnectionPhase.Connected
    is VpnStatus.Disconnecting -> ConnectionPhase.Disconnecting
    is VpnStatus.Failed -> ConnectionPhase.Failed
}

/** Big hero title. */
internal fun VpnStatus.headline(): String = when (this) {
    is VpnStatus.Disconnected -> "Not connected"
    is VpnStatus.Connecting -> "Connecting"
    is VpnStatus.Connected -> "Protected"
    is VpnStatus.Disconnecting -> "Disconnecting"
    is VpnStatus.Failed -> "Connection failed"
}

/** Small uppercase eyebrow shown above the hero title. */
internal fun VpnStatus.eyebrow(): String = when (this) {
    is VpnStatus.Disconnected -> "Tunnel offline"
    is VpnStatus.Connecting -> "Handshake"
    is VpnStatus.Connected -> "Secure tunnel"
    is VpnStatus.Disconnecting -> "Closing tunnel"
    is VpnStatus.Failed -> "Attention needed"
}

/** Secondary line under the hero title: profile / server / stage / error. */
internal fun VpnStatus.subtitle(): String = when (this) {
    is VpnStatus.Disconnected -> "Your traffic is not protected"
    is VpnStatus.Connecting -> listOf(profileName, stage).filter { it.isNotBlank() }.joinToString(" · ")
    is VpnStatus.Connected -> listOf(profileName, server).filter { it.isNotBlank() }.joinToString(" · ")
    is VpnStatus.Disconnecting -> profileName
    is VpnStatus.Failed -> profileName
}

/** Human readable summary of the optional DTLS side channel. */
internal fun VpnStatus.Connected.dtlsSummary(): String = when (dtlsState) {
    DtlsState.ACTIVE -> dtlsCipher?.takeIf { it.isNotBlank() } ?: "Active"
    DtlsState.NEGOTIATING -> "Negotiating…"
    DtlsState.UNAVAILABLE -> "Unavailable (TLS only)"
    DtlsState.DISABLED -> "Disabled"
}

/** Local tunnel addresses, or an em dash when the server assigned nothing. */
internal fun VpnStatus.Connected.localAddresses(): String =
    listOfNotNull(ipv4?.takeIf { it.isNotBlank() }, ipv6?.takeIf { it.isNotBlank() })
        .joinToString("  ·  ")
        .ifBlank { "—" }

/** Friendly protocol name for a stored protocol id. */
internal fun protocolLabel(protocolId: String): String = when (protocolId) {
    "anyconnect" -> "Cisco AnyConnect"
    "nc" -> "Juniper NC"
    "pulse" -> "Pulse Secure"
    "gp" -> "GlobalProtect"
    "fortinet" -> "Fortinet"
    "f5" -> "F5 BIG-IP"
    else -> Protocols.labelFor(protocolId)
}
