package fr.lumiere.tv.player

import fr.lumiere.tv.data.api.ApiException
import fr.lumiere.tv.data.api.ClientCapabilities
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.PlaybackRequest
import fr.lumiere.tv.data.api.PlaybackSession
import fr.lumiere.tv.data.api.ProgressUpdate
import fr.lumiere.tv.data.api.ServerUnreachableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerState(
    val session: PlaybackSession? = null,
    val position: Double = 0.0,
    val duration: Double? = null,
    val loading: Boolean = true,
    val error: String? = null,
    /** Secondes avant l'épisode suivant, `null` hors compte à rebours. */
    val nextCountdown: Int? = null,
    /** Fin du média sans suite : l'écran se ferme. */
    val ended: Boolean = false,
)

/**
 * Pilote une lecture : session Lumière, flux, repositionnement, progression, épisode suivant
 * et repli en conversion. Sans dépendance à Android : le lecteur passe par [PlayerPort].
 */
class PlaybackController(
    private val api: LumiereApi,
    private val player: PlayerPort,
    private val capabilities: ClientCapabilities,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) : PlayerPort.Listener {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private var media: MediaRef? = null
    private var session: PlaybackSession? = null

    /** Instant réel de départ du flux en mode `restart` (conversion, remux). */
    private var origin = 0.0
    private var fallbackTried = false
    private var lastProgressAt = 0L
    private var nextCanceled = false

    init {
        player.setListener(this)
    }

    /** Position dans le média, en secondes. */
    val position: Double
        get() = if (session?.seek == "restart") origin + player.positionSec else player.positionSec

    val currentSession: PlaybackSession? get() = session

    suspend fun start(media: MediaRef, audioTrack: Int? = null, mode: String = "auto", at: Double? = null) {
        val previous = session
        this.media = media
        _state.update { it.copy(loading = true, error = null, nextCountdown = null, ended = false) }
        val created = try {
            api.createSession(PlaybackRequest(media, capabilities, audioTrack, mode))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.playbackMessage()) }
            return
        }
        previous?.let { closeQuietly(it.sessionId) }
        session = created
        nextCanceled = false
        lastProgressAt = clock()
        load(at ?: created.resumePosition)
        player.play()
        _state.update { it.copy(session = created, duration = created.duration, loading = false, position = position) }
    }

    suspend fun seekTo(target: Double) {
        val s = session ?: return
        val clamped = target.coerceIn(0.0, s.duration ?: Double.MAX_VALUE)
        if (s.seek == "native") player.seekTo(clamped) else load(clamped)
        _state.update { it.copy(position = position) }
    }

    /** Change de piste audio hors lecture directe : nouvelle session à la position courante. */
    suspend fun changeAudioTrack(index: Int) {
        val s = session ?: return
        val m = media ?: return
        start(m, audioTrack = index, mode = if (s.mode == "transcode") "transcode" else "auto", at = position)
    }

    fun pause() {
        player.pause()
        saveProgressAsync()
    }

    fun resume() = player.play()

    /** À appeler chaque seconde : progression toutes les 10 s, compte à rebours de l'épisode suivant. */
    fun tick() {
        val s = session ?: return
        val pos = position
        _state.update { it.copy(position = pos) }
        if (clock() - lastProgressAt >= PROGRESS_INTERVAL_MS) saveProgressAsync()

        val next = s.next ?: return
        val duration = s.duration ?: return
        if (nextCanceled || duration - pos > NEXT_WINDOW_SEC) return
        val countdown = _state.value.nextCountdown
        when {
            countdown == null -> _state.update { it.copy(nextCountdown = NEXT_COUNTDOWN) }
            countdown > 1 -> _state.update { it.copy(nextCountdown = countdown - 1) }
            else -> playNext(next.media)
        }
    }

    fun cancelNext() {
        nextCanceled = true
        _state.update { it.copy(nextCountdown = null) }
    }

    fun playNextNow() {
        session?.next?.let { playNext(it.media) }
    }

    /** Envoie la position, ferme la session et libère le lecteur ; les erreurs réseau sont ignorées. */
    suspend fun stop() {
        session?.let { s ->
            runQuietly { api.saveProgress(s.sessionId, ProgressUpdate(position, s.duration)) }
            runQuietly { api.stopSession(s.sessionId) }
        }
        session = null
        player.release()
    }

    override fun onEnded() {
        val next = session?.next
        if (next != null && !nextCanceled) playNext(next.media) else {
            saveProgressAsync()
            _state.update { it.copy(ended = true) }
        }
    }

    override fun onError(error: Throwable) {
        val s = session
        val m = media
        if (s != null && m != null && s.mode == "direct" && !fallbackTried) {
            fallbackTried = true
            val at = position
            scope.launch { start(m, mode = "transcode", at = at) }
        } else {
            _state.update { it.copy(error = "Lecture impossible : ${error.message ?: "erreur du lecteur"}") }
        }
    }

    private fun playNext(next: MediaRef) {
        val finished = session
        _state.update { it.copy(nextCountdown = null) }
        fallbackTried = false
        scope.launch {
            finished?.let { s -> runQuietly { api.saveProgress(s.sessionId, ProgressUpdate(s.duration ?: position, s.duration)) } }
            start(next)
        }
    }

    private suspend fun load(at: Double) {
        val s = session ?: return
        val url = api.absoluteUrl(s.streamUrl)
        if (s.seek == "native") {
            origin = 0.0
            player.load(url, at)
        } else {
            val streamId = newStreamId()
            player.load("$url?start=${formatSeconds(at)}&stream=$streamId", 0.0)
            origin = try {
                api.streamOrigin(s.sessionId, streamId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                at
            }
        }
    }

    private fun saveProgressAsync() {
        val s = session ?: return
        lastProgressAt = clock()
        val update = ProgressUpdate(position, s.duration)
        scope.launch { runQuietly { api.saveProgress(s.sessionId, update) } }
    }

    private fun closeQuietly(sessionId: String) {
        scope.launch { runQuietly { api.stopSession(sessionId) } }
    }

    private suspend fun runQuietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun Exception.playbackMessage(): String = when (this) {
        is ServerUnreachableException -> "Lumière est injoignable : la lecture ne peut pas démarrer."
        is ApiException -> message
        else -> "Lecture impossible."
    }

    companion object {
        const val PROGRESS_INTERVAL_MS = 10_000L
        const val NEXT_WINDOW_SEC = 20.0
        const val NEXT_COUNTDOWN = 10

        /** Secondes sans décimales inutiles : 600.0 donne « 600 », 12.5 donne « 12.5 ». */
        fun formatSeconds(value: Double): String =
            if (value == Math.floor(value)) value.toLong().toString() else "%.3f".format(java.util.Locale.ROOT, value).trimEnd('0')
    }
}
