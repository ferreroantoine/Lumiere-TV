package fr.lumiere.tv.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient

/**
 * ExoPlayer derrière [PlayerPort]. Les flux passent par le client HTTP de l'appli ;
 * les pistes texte d'ExoPlayer sont coupées (les sous-titres sont rendus par l'appli).
 */
@OptIn(UnstableApi::class)
class ExoPlayerPort(context: Context, httpClient: OkHttpClient) : PlayerPort {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(httpClient)))
        .build()
        .apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage("fr")
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        }

    private var listener: PlayerPort.Listener? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) listener?.onEnded()
            }

            override fun onPlayerError(error: PlaybackException) {
                listener?.onError(PlayerFailure(error.frenchMessage(), network = error.errorCode / 1000 == 2, cause = error))
            }
        })
    }

    override fun load(url: String, startPositionSec: Double) {
        Log.i("Lumiere", "Flux : $url (départ ${startPositionSec}s)")
        exoPlayer.setMediaItem(MediaItem.fromUri(url), (startPositionSec * 1000).toLong())
        exoPlayer.prepare()
    }

    override fun play() = exoPlayer.play()

    override fun pause() = exoPlayer.pause()

    override fun seekTo(positionSec: Double) = exoPlayer.seekTo((positionSec * 1000).toLong())

    override val positionSec: Double get() = exoPlayer.currentPosition / 1000.0

    override val isPlaying: Boolean get() = exoPlayer.isPlaying

    override val playWhenReady: Boolean get() = exoPlayer.playWhenReady

    /** Le flux se charge (démarrage, saut, coupure) : l'image attend des données. */
    val isBuffering: Boolean get() = exoPlayer.playbackState == Player.STATE_BUFFERING

    override fun setListener(listener: PlayerPort.Listener) {
        this.listener = listener
    }

    override fun release() = exoPlayer.release()
}

/** Familles de codes d'erreur de Media3 : 2xxx réseau, 3xxx lecture du fichier, 4xxx décodage. */
private fun PlaybackException.frenchMessage(): String = when (errorCode / 1000) {
    2 -> "le flux vidéo ne répond plus (réseau ou serveur)."
    3 -> "le fichier vidéo est illisible."
    4 -> "cet appareil ne sait pas décoder cette vidéo."
    5 -> "la sortie audio refuse ce format."
    else -> "erreur du lecteur ($errorCodeName)."
}
