package fr.lumiere.tv.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.lumiere.tv.data.server.ServerAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Adresse du serveur retenue entre deux lancements. */
interface ServerStore {
    val address: Flow<ServerAddress?>
    suspend fun save(address: ServerAddress)
    suspend fun clear()
}

class ServerPrefs(private val dataStore: DataStore<Preferences>) : ServerStore {
    private val key = stringPreferencesKey("server_base_url")

    override val address: Flow<ServerAddress?> =
        dataStore.data.map { prefs -> prefs[key]?.let(::ServerAddress) }

    override suspend fun save(address: ServerAddress) {
        dataStore.edit { it[key] = address.baseUrl }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
