package com.deepconnect.vpn.vpn

import com.deepconnect.vpn.data.Profile
import org.infradead.libopenconnect.LibOpenConnect

/**
 * [LibOpenConnect] subclass that drives one VPN connection.
 *
 * Instances are created by [DeepConnectVpnService] on a dedicated worker thread; the
 * native library calls the callbacks below from that same thread.
 */
class OpenConnectSession(
    private val profile: Profile,
    private val protectSocket: (Int) -> Boolean,
    private val onStats: (TrafficStats) -> Unit,
    private val onDtlsState: (DtlsState, String?) -> Unit,
) : LibOpenConnect(USER_AGENT) {

    @Volatile
    private var authGroupSent = false

    @Volatile
    private var authFormsHandled = 0

    @Volatile
    private var consecutiveAuthFailures = 0

    @Volatile
    private var lastDtlsState = DtlsState.NEGOTIATING

    /** Updated in-place when the user chooses "Trust & remember" mid-handshake. */
    @Volatile
    private var pinnedCertHash: String = profile.pinnedCertHash

    private val tokenModeActive: Boolean
        get() = profile.totpEnabled && profile.totpSecret.isNotBlank()

    /* ------------------------------------------------------------------ */
    /* configuration                                                      */
    /* ------------------------------------------------------------------ */

    fun applyProfile(caBundlePath: String?, logLevel: Int) {
        setLogLevel(logLevel)
        setReportedOS(profile.reportedOs)
        setCertExpiryWarning(300)

        if (profile.mtu > 0) {
            setReqMTU(profile.mtu)
        }
        if (!caBundlePath.isNullOrBlank()) {
            setCAFile(caBundlePath)
        }
        if (profile.disableDtls) {
            disableDTLS()
        }
        if (profile.protocol.isNotBlank() && profile.protocol != "anyconnect") {
            val ret = setProtocol(profile.protocol)
            if (ret != 0) {
                log(LibOpenConnect.PRG_ERR, "Server protocol '${profile.protocol}' was not accepted (${ret}); using AnyConnect")
            }
        }
        if (tokenModeActive) {
            val ret = setTokenMode(LibOpenConnect.OC_TOKEN_MODE_TOTP, normalizeTotpSecret(profile.totpSecret))
            if (ret != 0) {
                log(LibOpenConnect.PRG_ERR, "TOTP secret was rejected by OpenConnect ($ret); OTP will be requested manually")
            } else {
                log(LibOpenConnect.PRG_INFO, "TOTP mode enabled")
            }
        }
    }

    /**
     * OpenConnect treats a bare string as a *raw ASCII* HMAC key; base32 secrets (what
     * authenticator apps hand out) only get decoded when prefixed with "base32:" or
     * embedded in an otpauth:// URI. Normalise the user's input accordingly.
     */
    private fun normalizeTotpSecret(raw: String): String {
        val secret = raw.trim()
        if (secret.isEmpty()) return secret
        val lower = secret.lowercase()
        val alreadyQualified = lower.startsWith("otpauth://") ||
            lower.startsWith("base32:") ||
            lower.startsWith("sha1:") ||
            lower.startsWith("sha256:") ||
            lower.startsWith("sha512:") ||
            secret.startsWith("0x") ||
            secret.startsWith("<?xml")
        if (alreadyQualified) return secret
        return "base32:" + secret.filterNot { it.isWhitespace() }.uppercase()
    }

    /* ------------------------------------------------------------------ */
    /* callbacks from native                                              */
    /* ------------------------------------------------------------------ */

    override fun onProgress(level: Int, msg: String) {
        log(level, msg)

        // Track the optional DTLS side channel so the UI can show whether UDP is in use,
        // and explain the timeout (SSL_ERROR_WANT_READ = 2) instead of leaving users with
        // a bare "DTLS handshake failed: 2".
        when {
            msg.contains("Established DTLS connection") -> {
                val cipher = msg.substringAfter("Ciphersuite ", "")
                    .trim()
                    .trimEnd('.')
                    .ifBlank { runCatching { getDTLSCipher() }.getOrNull().orEmpty() }
                if (lastDtlsState != DtlsState.ACTIVE) {
                    lastDtlsState = DtlsState.ACTIVE
                    onDtlsState(DtlsState.ACTIVE, cipher)
                }
            }

            msg.contains("DTLS handshake failed") -> {
                if (lastDtlsState != DtlsState.UNAVAILABLE) {
                    lastDtlsState = DtlsState.UNAVAILABLE
                    onDtlsState(DtlsState.UNAVAILABLE, null)
                    log(
                        LibOpenConnect.PRG_INFO,
                        "DTLS (UDP) could not be established: the gateway did not answer its UDP " +
                            "port, usually because a firewall/NAT on this network filters UDP. " +
                            "The tunnel continues over TLS and works normally; DTLS is only a " +
                            "performance optimisation. It is retried periodically, and you can " +
                            "turn it off per profile (Advanced → Disable DTLS).",
                    )
                }
            }

            msg.contains("DTLS session resume failed") || msg.contains("possible MITM attack") -> {
                if (lastDtlsState != DtlsState.UNAVAILABLE) {
                    lastDtlsState = DtlsState.UNAVAILABLE
                    onDtlsState(DtlsState.UNAVAILABLE, null)
                }
            }

            msg.contains("No DTLS address") -> {
                if (lastDtlsState == DtlsState.NEGOTIATING) {
                    lastDtlsState = DtlsState.UNAVAILABLE
                    onDtlsState(DtlsState.UNAVAILABLE, null)
                }
            }
        }
    }

    override fun onProtectSocket(fd: Int) {
        if (fd >= 0) protectSocket(fd)
    }

    override fun onStatsUpdate(stats: LibOpenConnect.VPNStats) {
        onStats(
            TrafficStats(
                txPackets = stats.txPkts,
                txBytes = stats.txBytes,
                rxPackets = stats.rxPkts,
                rxBytes = stats.rxBytes,
            )
        )
    }

    override fun onWriteNewConfig(buf: ByteArray): Int = 0

    override fun onSetupTun() {
        // The tun device is created through VpnService and handed over with setupTunFD(),
        // so nothing to do here.
    }

    override fun onReconnected() {
        log(LibOpenConnect.PRG_INFO, "Reconnected to the VPN gateway")
    }

    override fun onValidatePeerCert(msg: String): Int {
        val fingerprint = runCatching { getPeerCertHash() }.getOrNull().orEmpty()

        if (fingerprint.isNotEmpty() && pinnedCertHash.isNotEmpty()) {
            if (checkPeerCertHash(pinnedCertHash) == 0) {
                log(LibOpenConnect.PRG_INFO, "Server certificate matches the pinned fingerprint")
                return 0
            }
        }

        val details = runCatching { getPeerCertDetails() }.getOrNull().orEmpty()
        log(LibOpenConnect.PRG_INFO, "Server certificate requires confirmation: $msg")

        val answer = ConnectionController.askCertificate(
            profileName = profile.displayName,
            message = msg,
            fingerprint = fingerprint,
            details = details,
        )
        return when {
            !answer.accept -> {
                log(LibOpenConnect.PRG_ERR, "Server certificate rejected by user")
                -1
            }
            answer.remember && fingerprint.isNotEmpty() -> {
                pinnedCertHash = fingerprint
                ConnectionController.rememberPinnedCertificate(profile.id, fingerprint)
                0
            }
            else -> 0
        }
    }

    override fun onProcessAuthForm(form: LibOpenConnect.AuthForm): Int {
        authFormsHandled++

        form.banner?.takeIf { it.isNotBlank() }?.let { log(LibOpenConnect.PRG_INFO, it.trim()) }
        form.message?.takeIf { it.isNotBlank() }?.let { log(LibOpenConnect.PRG_INFO, it.trim()) }
        form.error?.takeIf { it.isNotBlank() }?.let { log(LibOpenConnect.PRG_ERR, it.trim()) }
        log(LibOpenConnect.PRG_DEBUG, "Authentication form: authID=${form.authID} method=${form.method} opts=${form.opts.size}")

        if (authFormsHandled > MAX_AUTH_FORMS) {
            log(LibOpenConnect.PRG_ERR, "Too many authentication rounds; aborting")
            return LibOpenConnect.OC_FORM_RESULT_CANCELLED
        }

        // Those credentials would only ever be retried verbatim, which risks locking the
        // account out; with a TOTP secret the library rotates the code, so waiting helps.
        if (form.error.isNullOrBlank()) {
            consecutiveAuthFailures = 0
        } else {
            consecutiveAuthFailures++
            val retryLimit = if (tokenModeActive) MAX_TOKEN_RETRIES else MAX_PASSWORD_RETRIES
            if (consecutiveAuthFailures >= retryLimit) {
                log(
                    LibOpenConnect.PRG_ERR,
                    "${form.error.trim()} — aborting instead of retrying the same stored credentials"
                )
                return LibOpenConnect.OC_FORM_RESULT_CANCELLED
            }
        }

        // Auth group selection (Cisco "GROUP" list) — needs its own round trip.
        val groupOpt = form.authgroupOpt
        if (groupOpt != null && !authGroupSent && groupOpt.choices.isNotEmpty()) {
            val choices = groupOpt.choices
            val wanted = profile.authGroup
            val picked = choices.firstOrNull { it.name == wanted }
                ?: choices.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
                ?: choices.first()
            groupOpt.value = picked.name
            authGroupSent = true
            log(LibOpenConnect.PRG_INFO, "Selecting auth group '${picked.label ?: picked.name}'")
            return LibOpenConnect.OC_FORM_RESULT_NEWGROUP
        }

        var filled = 0
        for (opt in form.opts) {
            if (opt.flags and LibOpenConnect.OC_FORM_OPT_IGNORE.toLong() != 0L) continue
            if (opt === groupOpt) continue

            when (opt.type) {
                LibOpenConnect.OC_FORM_OPT_HIDDEN -> {
                    // Server-provided value; leave untouched.
                }
                LibOpenConnect.OC_FORM_OPT_SELECT -> {
                    if (opt.value.isNullOrBlank() && opt.choices.isNotEmpty()) {
                        opt.value = opt.choices.first().name
                        filled++
                    }
                }
                LibOpenConnect.OC_FORM_OPT_TEXT -> {
                    val value = textValueFor(opt)
                    if (value != null) {
                        opt.value = value
                        filled++
                    }
                }
                LibOpenConnect.OC_FORM_OPT_PASSWORD -> {
                    val name = opt.name.orEmpty().lowercase()
                    val label = opt.label.orEmpty().lowercase()
                    if (name.contains("secondary") || label.contains("secondary") ||
                        name.contains("otp") || name.contains("token") || name.contains("code")
                    ) {
                        val code = requestOneTimePassword(opt.label ?: opt.name ?: "one-time code")
                        if (code == null) {
                            log(LibOpenConnect.PRG_ERR, "One-time password was not provided")
                            return LibOpenConnect.OC_FORM_RESULT_CANCELLED
                        }
                        opt.value = code
                        filled++
                    } else {
                        opt.value = profile.password
                        filled++
                    }
                }
                LibOpenConnect.OC_FORM_OPT_TOKEN -> {
                    // With token mode enabled OpenConnect fills this itself after the
                    // callback returns; otherwise we have to ask the user.
                    if (!tokenModeActive) {
                        val code = requestOneTimePassword(opt.label ?: opt.name ?: "one-time code")
                        if (code == null) {
                            log(LibOpenConnect.PRG_ERR, "One-time password was not provided")
                            return LibOpenConnect.OC_FORM_RESULT_CANCELLED
                        }
                        opt.value = code
                        filled++
                    }
                }
                FORM_OPT_SSO_TOKEN -> {
                    log(LibOpenConnect.PRG_ERR, "This gateway requires SAML/SSO browser authentication, which is not supported yet")
                    return LibOpenConnect.OC_FORM_RESULT_ERR
                }
                else -> log(LibOpenConnect.PRG_DEBUG, "Ignoring unsupported form option ${opt.name} (type ${opt.type})")
            }
        }

        log(
            LibOpenConnect.PRG_DEBUG,
            "Filled $filled field(s): " + form.opts.joinToString { opt ->
                "${opt.name}[${if (opt.value.isNullOrEmpty()) "unset" else "set"}]"
            }
        )
        if (filled == 0 && form.error.isNullOrBlank() && form.opts.isNotEmpty()) {
            log(LibOpenConnect.PRG_DEBUG, "Form had no fields we could fill")
        }
        return LibOpenConnect.OC_FORM_RESULT_OK
    }

    /* ------------------------------------------------------------------ */
    /* helpers                                                            */
    /* ------------------------------------------------------------------ */

    private fun textValueFor(opt: LibOpenConnect.FormOpt): String? {
        if (!opt.value.isNullOrBlank()) return opt.value
        val name = opt.name.orEmpty().lowercase()
        val label = opt.label.orEmpty().lowercase()
        return when {
            name.contains("user") || name.contains("login") || name == "email" ||
                label.contains("username") || label.contains("user name") -> profile.username
            name.contains("domain") || label.contains("domain") -> ""
            name.contains("pass") || name.contains("pwd") || label.contains("password") -> profile.password
            name.contains("otp") || name.contains("code") || name.contains("token") ->
                requestOneTimePassword(opt.label ?: opt.name ?: "one-time code")
            else -> profile.username
        }
    }

    private fun requestOneTimePassword(label: String): String? {
        log(LibOpenConnect.PRG_INFO, "Requesting one-time password from user")
        return ConnectionController.askOneTimePassword(profile.displayName, label)
    }

    private fun log(level: Int, message: String) {
        if (message.isBlank()) return
        ConnectionController.log(level, message)
    }

    companion object {
        private const val USER_AGENT = "OpenConnect VPN Agent (DeepConnect)"
        private const val MAX_AUTH_FORMS = 12
        private const val MAX_PASSWORD_RETRIES = 2
        private const val MAX_TOKEN_RETRIES = 3

        /** OC_FORM_OPT_SSO_TOKEN; the Java wrapper does not expose this constant. */
        private const val FORM_OPT_SSO_TOKEN = 6
    }
}
