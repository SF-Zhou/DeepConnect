package com.deepconnect.vpn.vpn

import org.infradead.libopenconnect.LibOpenConnect

/** Loads the bundled native library exactly once, before any JNI class is touched. */
object OpenConnectNative {

    @Volatile
    private var loaded = false

    val isLoaded: Boolean get() = loaded

    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            System.loadLibrary("openconnect")
            loaded = true
        }
    }

    val version: String
        get() = if (loaded) runCatching { LibOpenConnect.getVersion() }.getOrDefault("unknown") else "not loaded"

    data class Capabilities(
        val stoken: Boolean,
        val oath: Boolean,
        val pkcs11: Boolean,
        val tss: Boolean,
        val yubiOath: Boolean,
    )

    fun capabilities(): Capabilities? {
        if (!loaded) return null
        return runCatching {
            Capabilities(
                stoken = LibOpenConnect.hasStokenSupport(),
                oath = LibOpenConnect.hasOATHSupport(),
                pkcs11 = LibOpenConnect.hasPKCS11Support(),
                tss = LibOpenConnect.hasTSSBlobSupport(),
                yubiOath = LibOpenConnect.hasYubiOATHSupport(),
            )
        }.getOrNull()
    }
}
