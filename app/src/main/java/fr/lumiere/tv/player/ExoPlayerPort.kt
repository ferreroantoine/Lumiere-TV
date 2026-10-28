package fr.lumiere.tv.player

import android.content.Context
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
                listener?.onError(error)
            }
        })
    }

    override fun load(url: String, startPositionSec: Double) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url), (startPositionSec * 1000).toLong())
        exoPlayer.prepare()
    }

    override fun play() = exoPlayer.play()

    override fun pause() = exoPlayer.pause()

    override fun seekTo(positionSec: Double) = exoPlayer.seekTo((positionSec * 1000).toLong())

    override val positionSec: Double get() = exoPlayer.currentPosition / 1000.0

    override val isPlaying: Boolean get() = exoPlayer.isPlaying

    override fun setListener(listener: PlayerPort.Listener) {
        this.listener = listener
    }

    override fun release() = exoPlayer.release()
}
