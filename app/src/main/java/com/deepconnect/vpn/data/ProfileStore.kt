package com.deepconnect.vpn.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Single source of truth for saved profiles. Profiles are persisted as JSON in the app's
 * private files dir; the secret fields are encrypted through [SecretVault].
 */
class ProfileStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)
    private val vault = SecretVault()
    private val lock = Any()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    /**
     * Id of the profile used for "auto-connect on launch". The very first profile added
     * becomes the default; deleting it promotes the next profile in the list.
     */
    private val _defaultProfileId = MutableStateFlow<Long?>(null)
    val defaultProfileId: StateFlow<Long?> = _defaultProfileId.asStateFlow()

    init {
        synchronized(lock) {
            val (list, defaultId) = readFromDisk()
            _profiles.value = list
            _defaultProfileId.value = defaultId
        }
    }

    fun get(id: Long): Profile? = _profiles.value.firstOrNull { it.id == id }

    fun defaultProfile(): Profile? = _defaultProfileId.value?.let { get(it) }

    fun upsert(profile: Profile): Profile {
        val stored = if (profile.id == 0L) profile.copy(id = System.currentTimeMillis()) else profile
        synchronized(lock) {
            val list = _profiles.value.toMutableList()
            val idx = list.indexOfFirst { it.id == stored.id }
            if (idx >= 0) list[idx] = stored else list.add(stored)
            // First profile added automatically becomes the default; recover if the stored
            // default no longer exists for any reason.
            val defaultId = when {
                _defaultProfileId.value == null -> stored.id
                list.none { it.id == _defaultProfileId.value } -> stored.id
                else -> _defaultProfileId.value
            }
            writeToDisk(list, defaultId)
            _profiles.value = list
            _defaultProfileId.value = defaultId
        }
        return stored
    }

    fun delete(id: Long) {
        synchronized(lock) {
            val list = _profiles.value.filterNot { it.id == id }
            val defaultId = if (_defaultProfileId.value == id) {
                list.firstOrNull()?.id
            } else {
                _defaultProfileId.value?.takeIf { candidate -> list.any { it.id == candidate } }
                    ?: list.firstOrNull()?.id
            }
            writeToDisk(list, defaultId)
            _profiles.value = list
            _defaultProfileId.value = defaultId
        }
    }

    fun setDefaultProfile(id: Long) {
        synchronized(lock) {
            if (_profiles.value.none { it.id == id }) return
            writeToDisk(_profiles.value, id)
            _defaultProfileId.value = id
        }
    }

    fun updatePinnedHash(id: Long, hash: String) {
        val profile = get(id) ?: return
        upsert(profile.copy(pinnedCertHash = hash))
    }

    /* ---------------------------------------------------------------- */

    private fun readFromDisk(): Pair<List<Profile>, Long?> {
        if (!file.exists()) return emptyList<Profile>() to null
        return try {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("profiles") ?: return emptyList<Profile>() to null
            val list = buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(fromJson(o))
                }
            }
            // Version 1 stores have no default; honour the "first profile is the default"
            // rule when migrating, and repair stale ids.
            val storedDefault = root.optLong("defaultProfileId", 0L)
            val defaultId = storedDefault.takeIf { candidate -> list.any { it.id == candidate } }
                ?: list.firstOrNull()?.id
            list to defaultId
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read profile store", e)
            emptyList<Profile>() to null
        }
    }

    private fun writeToDisk(list: List<Profile>, defaultProfileId: Long?) {
        try {
            val array = JSONArray()
            list.forEach { array.put(toJson(it)) }
            val root = JSONObject()
                .put("version", 2)
                .put("defaultProfileId", defaultProfileId ?: 0L)
                .put("profiles", array)
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(root.toString(2))
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                file.writeText(root.toString(2))
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to persist profile store", e)
        }
    }

    private fun toJson(p: Profile): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("server", p.server)
        put("username", p.username)
        put("password", vault.encrypt(p.password))
        put("totpEnabled", p.totpEnabled)
        put("totpSecret", vault.encrypt(p.totpSecret))
        put("protocol", p.protocol)
        put("reportedOs", p.reportedOs)
        put("disableDtls", p.disableDtls)
        put("mtu", p.mtu)
        put("userAgent", p.userAgent)
        put("extraCaPem", vault.encrypt(p.extraCaPem))
        put("pinnedCertHash", p.pinnedCertHash)
        put("authGroup", p.authGroup)
    }

    private fun fromJson(o: JSONObject): Profile = Profile(
        id = o.optLong("id"),
        name = o.optString("name"),
        server = o.optString("server"),
        username = o.optString("username"),
        password = vault.decrypt(o.optString("password")),
        totpEnabled = o.optBoolean("totpEnabled"),
        totpSecret = vault.decrypt(o.optString("totpSecret")),
        protocol = o.optString("protocol", "anyconnect"),
        reportedOs = o.optString("reportedOs", "linux"),
        disableDtls = o.optBoolean("disableDtls"),
        mtu = o.optInt("mtu"),
        userAgent = o.optString("userAgent"),
        extraCaPem = vault.decrypt(o.optString("extraCaPem")),
        pinnedCertHash = o.optString("pinnedCertHash"),
        authGroup = o.optString("authGroup"),
    )

    companion object {
        private const val TAG = "ProfileStore"
        private const val FILE_NAME = "profiles.json"

        @Volatile
        private var instance: ProfileStore? = null

        fun get(context: Context): ProfileStore {
            return instance ?: synchronized(this) {
                instance ?: ProfileStore(context).also { instance = it }
            }
        }
    }
}
