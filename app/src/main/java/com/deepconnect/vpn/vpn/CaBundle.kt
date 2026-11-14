package com.deepconnect.vpn.vpn

import android.content.Context
import java.io.File

/**
 * Android keeps its trusted CA roots as individual PEM files. OpenConnect's OpenSSL
 * backend knows nothing about that layout, so we concatenate them into one bundle and
 * point the library at it with [LibOpenConnect.setCAFile].
 */
object CaBundle {

    private val SYSTEM_CA_DIRS = listOf(
        "/apex/com.android.conscrypt/cacerts",
        "/system/etc/security/cacerts",
        "/system/etc/security/cacerts_google",
    )

    fun build(context: Context, extraPem: String?): File? {
        val bundle = StringBuilder()
        var certificates = 0

        for (dir in SYSTEM_CA_DIRS) {
            val files = File(dir).listFiles() ?: continue
            for (file in files.sortedBy { it.name }) {
                if (!file.isFile) continue
                val pem = runCatching { file.readText() }.getOrNull() ?: continue
                if (!pem.contains("BEGIN CERTIFICATE")) continue
                bundle.append(pem.trimEnd()).append('\n')
                certificates++
            }
        }

        if (!extraPem.isNullOrBlank()) {
            bundle.append(extraPem.trimEnd()).append('\n')
            certificates++
        }

        if (certificates == 0) return null

        val out = File(context.cacheDir, "ca-bundle.pem")
        out.writeText(bundle.toString())
        return out
    }
}
