package fr.lumiere.tv

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.prefs.ServerPrefs
import fr.lumiere.tv.data.server.ServerAddress
import fr.lumiere.tv.data.server.ServerCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private val Context.serverDataStore by preferencesDataStore(name = "serveur")

/** Racine de composition : crée et câble les services de l'appli. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Client HTTP unique : API, images, flux vidéo et sous-titres. */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val serverPrefs = ServerPrefs(context.applicationContext.serverDataStore)

    val serverCheck = ServerCheck(httpClient)

    val currentAddress: StateFlow<ServerAddress?> =
        serverPrefs.address.stateIn(appScope, SharingStarted.Eagerly, null)

    val api = LumiereApi(httpClient) {
        currentAddress.value ?: error("Aucun serveur Lumière configuré")
    }

    val imageLoader: ImageLoader = ImageLoader.Builder(context.applicationContext)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { httpClient })) }
        .crossfade(true)
        .build()
}
