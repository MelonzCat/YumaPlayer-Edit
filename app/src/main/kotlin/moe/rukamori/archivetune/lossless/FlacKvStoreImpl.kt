package moe.rukamori.archivetune.lossless

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import moe.rukamori.archivetune.flaccore.FlacKvStore
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.getAsync

class FlacKvStoreImpl(private val context: Context) : FlacKvStore {
    override suspend fun get(key: String): String? {
        val prefKey = stringPreferencesKey("flac_kv_$key")
        return context.dataStore.getAsync(prefKey)
    }

    override suspend fun put(key: String, value: String?) {
        val prefKey = stringPreferencesKey("flac_kv_$key")
        context.dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(prefKey)
            } else {
                prefs[prefKey] = value
            }
        }
    }
}
