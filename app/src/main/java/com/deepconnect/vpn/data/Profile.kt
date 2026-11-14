package com.deepconnect.vpn.data

/**
 * A saved VPN connection profile.
 *
 * Secrets ([password], [totpSecret]) and [extraCaPem] are stored encrypted at rest
 * (see [com.deepconnect.vpn.data.SecretVault]); this model always carries plaintext.
 */
data class Profile(
    val id: Long = 0L,
    val name: String = "",
    /** Hostname, or a full https:// URL with an optional group/url-path. */
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val totpEnabled: Boolean = false,
    /** Base32 secret, or a complete otpauth:// URI (both are understood by OpenConnect). */
    val totpSecret: String = "",
    /** OpenConnect protocol id: anyconnect, nc, pulse, gp, fortinet, f5. */
    val protocol: String = "anyconnect",
    /** OS string reported to the server: linux, linux-64, win, mac-intel, android, apple-ios. */
    val reportedOs: String = "linux",
    val disableDtls: Boolean = false,
    /** Requested MTU; 0 lets the server decide. */
    val mtu: Int = 0,
    /** Optional user-agent override. */
    val userAgent: String = "",
    /** Extra CA certificate(s), PEM, trusted in addition to the Android system store. */
    val extraCaPem: String = "",
    /** SHA-256 fingerprint of a peer certificate the user chose to trust. */
    val pinnedCertHash: String = "",
    /** Pre-selected auth group (Cisco). */
    val authGroup: String = "",
) {
    val displayName: String
        get() = name.ifBlank { serverHost.ifBlank { "VPN profile" } }

    val serverHost: String
        get() = server
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore('?')

    fun isValid(): Boolean = server.isNotBlank() && username.isNotBlank()
}

object Protocols {
    data class Entry(val id: String, val label: String)

    val ALL = listOf(
        Entry("anyconnect", "Cisco AnyConnect / ocserv"),
        Entry("nc", "Juniper NC (legacy)"),
        Entry("pulse", "Pulse Secure"),
        Entry("gp", "Palo Alto GlobalProtect"),
        Entry("fortinet", "Fortinet"),
        Entry("f5", "F5 Big-IP"),
    )

    fun labelFor(id: String): String = ALL.firstOrNull { it.id == id }?.label ?: id
}

object ReportedOs {
    val ALL = listOf("linux", "linux-64", "win", "mac-intel", "android", "apple-ios")

    fun labelFor(id: String): String = when (id) {
        "linux" -> "Linux"
        "linux-64" -> "Linux (64-bit)"
        "win" -> "Windows"
        "mac-intel" -> "macOS"
        "android" -> "Android"
        "apple-ios" -> "iOS"
        else -> id
    }
}
