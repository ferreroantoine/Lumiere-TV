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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 * Pilote une lecture : session Lumière, flux, repositionnement, progression, épisode suivant,
 * reprise après coupure et repli en conversion. Sans dépendance à Android : le lecteur passe
 * par [PlayerPort]. Même logique que le lecteur web de Lumière (apps/web/src/features/player).
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

    /**
     * Instant réel de départ du flux en mode `restart` (conversion, remux). Provisoire (la
     * position demandée) jusqu'à la réponse du serveur, qui n'arrive qu'une fois ffmpeg lancé.
     */
    private var origin = 0.0

    /** Flux chargé ; une réponse d'origine pour un autre flux est ignorée. */
    private var currentStreamId: String? = null

    /** Appuis successifs en mode `restart` : seule la dernière position est chargée. */
    private var seekTarget: Double? = null
    private var seekFrom = 0.0
    private var seekJob: Job? = null

    /** Passage à l'épisode suivant en cours (création de la session) : rien ne doit le relancer. */
    private var switching = false

    private var fallbackTried = false
    private var hasPlayed = false
    private val recoveries = ArrayDeque<Long>()
    private var lastProgressAt = 0L
    private var nextCanceled = false

    init {
        player.setListener(this)
    }

    /** Position dans le média, en secondes (jamais négative : l'origine peut précéder zéro). */
    val position: Double
        get() = seekTarget
            ?: (if (session?.seek == "restart") origin + player.positionSec else player.positionSec).coerceAtLeast(0.0)

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
        hasPlayed = false
        recoveries.clear()
        cancelPendingSeek()
        lastProgressAt = clock()
        _state.update { it.copy(session = created, duration = created.duration, nextCountdown = null) }
        load(at ?: created.resumePosition)
        player.play()
        _state.update { it.copy(loading = false, position = position) }
    }

    /**
     * Repositionnement. Natif : immédiat. En `restart`, les appuis rapprochés sont regroupés
     * (350 ms) et le flux n'est relancé qu'une fois, à la dernière position demandée.
     */
    suspend fun seekTo(target: Double) {
        val s = session ?: return
        // Un flux converti relancé tout près de la fin serait vide : on s'arrête un peu avant.
        val max = s.duration?.let { (it - END_MARGIN_SEC).coerceAtLeast(0.0) } ?: Double.MAX_VALUE
        val clamped = target.coerceIn(0.0, max)
        if (s.seek == "native") {
            player.seekTo(clamped)
            _state.update { it.copy(position = position) }
            return
        }
        if (seekTarget == null) seekFrom = position
        seekTarget = clamped
        _state.update { it.copy(position = clamped) }
        seekJob?.cancel()
        seekJob = scope.launch {
            delay(SEEK_GROUPING_MS)
            val to = seekTarget ?: return@launch
            val from = seekFrom
            seekTarget = null
            load(to, forwardFrom = if (to > from) from else null)
        }
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

    /** En pause seulement si l'utilisateur l'a demandée : un chargement après un saut n'en est pas une. */
    val paused: Boolean get() = !player.playWhenReady

    /** OK de la télécommande : pause, ou reprise si la lecture est en pause. */
    fun togglePause() {
        if (paused) resume() else pause()
    }

    /** À appeler chaque seconde : progression toutes les 10 s, compte à rebours de l'épisode suivant. */
    fun tick() {
        val s = session ?: return
        if (switching || _state.value.loading) return
        if (player.isPlaying) hasPlayed = true
        val pos = position
        _state.update { it.copy(position = pos) }
        if (clock() - lastProgressAt >= PROGRESS_INTERVAL_MS) saveProgressAsync()

        val next = s.next ?: return
        val duration = s.duration ?: return
        val countdown = _state.value.nextCountdown
        if (nextCanceled || duration - pos > NEXT_WINDOW_SEC) {
            // Recul hors de la fenêtre de fin : le compte à rebours disparaît.
            if (countdown != null) _state.update { it.copy(nextCountdown = null) }
            return
        }
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
        cancelPendingSeek()
        session?.let { s ->
            val update = ProgressUpdate(position, s.duration)
            runQuietly { api.saveProgress(s.sessionId, update) }
            runQuietly { api.stopSession(s.sessionId) }
        }
        session = null
        player.release()
    }

    override fun onEnded() {
        if (switching) return
        val next = session?.next
        if (next != null && !nextCanceled) playNext(next.media) else {
            saveProgressAsync()
            _state.update { it.copy(ended = true) }
        }
    }

    override fun onError(error: Throwable) {
        if (switching) return
        val s = session
        val m = media
        // Fin d'un flux converti parfois signalée comme une erreur : c'est la fin du média.
        val duration = s?.duration
        if (duration != null && position >= duration - END_MARGIN_SEC) {
            onEnded()
            return
        }
        // Coupure d'un flux déjà lu (réseau, serveur) : reprise au même endroit, dans la même session.
        if (s != null && hasPlayed && error is PlayerFailure && error.network && recover()) return
        // Lecture directe ou remux (vidéo copiée) refusée par le décodeur : la conversion réencode la vidéo.
        if (s != null && m != null && s.mode != "transcode" && !fallbackTried) {
            fallbackTried = true
            val at = position
            scope.launch { start(m, mode = "transcode", at = at) }
        } else {
            _state.update { it.copy(error = "Lecture impossible : ${error.message ?: "erreur du lecteur"}") }
        }
    }

    /** Relance le flux là où il s'est interrompu ; false quand les reprises récentes sont épuisées. */
    private fun recover(): Boolean {
        val now = clock()
        while (recoveries.isNotEmpty() && now - recoveries.first() >= RECOVERY_WINDOW_MS) recoveries.removeFirst()
        if (recoveries.size >= MAX_RECOVERIES) return false
        recoveries.addLast(now)
        val s = session ?: return false
        val at = position
        _state.update { it.copy(loading = true) }
        scope.launch {
            delay(1_000L * recoveries.size)
            if (session !== s) return@launch
            load(at)
            player.play()
            _state.update { it.copy(loading = false) }
        }
        return true
    }

    private fun playNext(next: MediaRef) {
        if (switching) return
        switching = true
        val finished = session
        _state.update { it.copy(nextCountdown = null) }
        fallbackTried = false
        scope.launch {
            try {
                finished?.let { s -> runQuietly { api.saveProgress(s.sessionId, ProgressUpdate(s.duration ?: position, s.duration)) } }
                start(next)
            } finally {
                switching = false
            }
        }
    }

    /**
     * Charge le flux à `requested`. En `restart`, l'origine provisoire est posée tout de suite ;
     * `forwardFrom` : position quittée par un saut en avant ; si l'image clé atteinte la précède
     * (images clés espacées en remux), le flux est relancé plus loin.
     */
    private suspend fun load(requested: Double, forwardFrom: Double? = null) {
        val s = session ?: return
        val at = requested.coerceAtLeast(0.0)
        val url = api.absoluteUrl(s.streamUrl)
        if (s.seek == "native") {
            origin = 0.0
            currentStreamId = null
            player.load(url, at)
            return
        }
        val streamId = newStreamId()
        currentStreamId = streamId
        origin = at
        player.load("$url?start=${formatSeconds(at)}&stream=$streamId", 0.0)
        val start = try {
            api.streamOrigin(s.sessionId, streamId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        if (currentStreamId != streamId || session !== s) return
        val further = at + (at - start) + 1
        val duration = s.duration
        if (forwardFrom != null && start <= forwardFrom + 1 && (duration == null || further < duration - 1)) {
            load(further)
            return
        }
        origin = start
    }

    private fun cancelPendingSeek() {
        seekJob?.cancel()
        seekJob = null
        seekTarget = null
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
        const val END_MARGIN_SEC = 3.0
        const val SEEK_GROUPING_MS = 350L
        const val MAX_RECOVERIES = 3
        const val RECOVERY_WINDOW_MS = 60_000L

        /** Secondes sans décimales inutiles : 600.0 donne « 600 », 12.5 donne « 12.5 ». */
        fun formatSeconds(value: Double): String =
            if (value == Math.floor(value)) value.toLong().toString() else "%.3f".format(java.util.Locale.ROOT, value).trimEnd('0')
    }
}
