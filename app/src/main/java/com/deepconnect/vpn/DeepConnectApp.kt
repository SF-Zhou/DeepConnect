package com.deepconnect.vpn

import android.app.Application
import com.deepconnect.vpn.vpn.ConnectionController
import com.deepconnect.vpn.vpn.OpenConnectNative
import com.deepconnect.vpn.data.ProfileStore

class DeepConnectApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Touch the store early so the Keystore key is created and profiles are loaded.
        ProfileStore.get(this)
        ConnectionController.attach(this)
        try {
            OpenConnectNative.ensureLoaded()
        } catch (t: Throwable) {
            ConnectionController.log(0, "Failed to load the native OpenConnect library: ${t.message}")
        }
    }
}
