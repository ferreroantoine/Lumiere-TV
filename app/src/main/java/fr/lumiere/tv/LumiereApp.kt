package fr.lumiere.tv

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader

class LumiereApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Coil passe par le client HTTP de l'appli. */
    override fun newImageLoader(context: Context): ImageLoader = container.imageLoader
}
