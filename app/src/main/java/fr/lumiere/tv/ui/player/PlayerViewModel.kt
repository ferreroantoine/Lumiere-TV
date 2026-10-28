package fr.lumiere.tv.ui.player

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.AppContainer
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.player.ExoPlayerPort
import fr.lumiere.tv.player.PlaybackController
import fr.lumiere.tv.player.detectCapabilities
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Lecture d'un média : lecteur ExoPlayer, contrôleur et horloge d'une seconde. */
class PlayerViewModel(private val container: AppContainer, media: MediaRef) : ViewModel() {
    val port = ExoPlayerPort(container.appContext, container.httpClient)
    val controller = PlaybackController(
        api = container.api,
        player = port,
        capabilities = detectCapabilities(container.appContext),
        scope = viewModelScope,
        clock = SystemClock::elapsedRealtime,
    )
    val state = controller.state

    init {
        viewModelScope.launch { controller.start(media) }
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                controller.tick()
            }
        }
    }

    fun togglePause() {
        if (port.isPlaying) controller.pause() else controller.resume()
    }

    fun seekBy(deltaSec: Double) {
        viewModelScope.launch { controller.seekTo(controller.position + deltaSec) }
    }

    fun changeAudioTrack(index: Int) {
        viewModelScope.launch { controller.changeAudioTrack(index) }
    }

    /** L'écran se ferme : position envoyée et session fermée, même après la fin du ViewModel. */
    override fun onCleared() {
        container.appScope.launch { controller.stop() }
    }
}
