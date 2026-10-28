package fr.lumiere.tv

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.prefs.ServerPrefs
import fr.lumiere.tv.data.prefs.ServerStore
import fr.lumiere.tv.data.server.ServerAddress
import fr.lumiere.tv.data.server.ServerCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private val Context.serverDataStore by preferencesDataStore(name = "serveur")

/** Racine de composition : crée et câble les services de l'appli. */
class AppContainer(context: Context) {
    /** Client HTTP unique : API, images, flux vidéo et sous-titres. */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val serverPrefs = ServerPrefs(context.applicationContext.serverDataStore)

    val serverCheck = ServerCheck(httpClient)

    private val _currentAddress = MutableStateFlow<ServerAddress?>(null)

    /** Adresse utilisée par l'API ; mise à jour dès l'enregistrement, sans attendre DataStore. */
    val currentAddress: StateFlow<ServerAddress?> = _currentAddress

    /** Mémorisation de l'adresse : écrit dans DataStore et met à jour [currentAddress]. */
    val serverStore: ServerStore = object : ServerStore {
        override val address = serverPrefs.address
        override suspend fun save(address: ServerAddress) {
            serverPrefs.save(address)
            _currentAddress.value = address
        }
        override suspend fun clear() {
            serverPrefs.clear()
            _currentAddress.value = null
        }
    }

    /** Lit l'adresse retenue au démarrage. */
    suspend fun loadAddress(): ServerAddress? =
        serverPrefs.address.first().also { _currentAddress.value = it }

    val api = LumiereApi(httpClient) {
        currentAddress.value ?: error("Aucun serveur Lumière configuré")
    }

    val imageLoader: ImageLoader = ImageLoader.Builder(context.applicationContext)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { httpClient })) }
        .crossfade(true)
        .build()
}
