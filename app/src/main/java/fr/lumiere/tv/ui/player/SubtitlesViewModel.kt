package fr.lumiere.tv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.api.PlaybackSession
import fr.lumiere.tv.data.api.SubtitleTrackInfo
import fr.lumiere.tv.player.subtitles.Cue
import fr.lumiere.tv.player.subtitles.SubtitleTimeline
import fr.lumiere.tv.player.subtitles.WebVtt
import fr.lumiere.tv.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

data class SubtitlesState(
    val tracks: List<SubtitleTrackInfo> = emptyList(),
    val selected: String? = null,
    /** Piste choisie chargée et prête à l'affichage. */
    val loaded: Boolean = false,
    val offset: Double = 0.0,
    val rate: Double = 1.0,
    val unavailable: Set<String> = emptySet(),
    val message: String? = null,
    val searching: Boolean = false,
    val onlineAvailable: Boolean = false,
)

/**
 * Sous-titres d'une session : choix de la piste, WebVTT du serveur, recalage (enregistré par
 * Lumière), recherche en ligne. Le texte est affiché par l'appli, pas par ExoPlayer.
 */
class SubtitlesViewModel(private val api: LumiereApi) : ViewModel() {
    private val _state = MutableStateFlow(SubtitlesState())
    val state: StateFlow<SubtitlesState> = _state

    private var sessionId: String? = null
    private var timeline = SubtitleTimeline(emptyList())
    private var loadJob: Job? = null
    private val saveLock = Mutex()

    /** Nouvelle session (début de lecture, changement de piste audio, épisode suivant). */
    fun attach(session: PlaybackSession) {
        if (session.sessionId == sessionId) return
        val previous = _state.value.selected
        sessionId = session.sessionId
        _state.value = SubtitlesState(tracks = session.subtitleTracks, onlineAvailable = session.onlineSubtitles.available)
        timeline = SubtitleTimeline(emptyList())
        val keep = previous?.takeIf { id -> session.subtitleTracks.any { it.id == id } }
        val initial = keep ?: session.defaultSubtitleTrack
        if (initial != null) select(initial)
        session.onlineSubtitles.autoLanguage?.let { language -> if (initial == null) searchOnline(autoLanguage = language) }
    }

    fun select(trackId: String?) {
        loadJob?.cancel()
        timeline = SubtitleTimeline(emptyList())
        val id = sessionId
        if (trackId == null || id == null) {
            _state.update { it.copy(selected = null, loaded = false, offset = 0.0, rate = 1.0) }
            return
        }
        _state.update { it.copy(selected = trackId, loaded = false, message = null) }
        loadJob = viewModelScope.launch {
            try {
                val cues = WebVtt.parse(api.subtitles(id, trackId))
                if (cues.isEmpty()) {
                    _state.update {
                        it.copy(selected = null, unavailable = it.unavailable + trackId, message = "Ces sous-titres sont illisibles.")
                    }
                    return@launch
                }
                val sync = runCatching { api.subtitleSync(id, trackId).sync }.getOrNull()
                timeline = SubtitleTimeline(cues)
                _state.update { it.copy(loaded = true, offset = sync?.offset ?: 0.0, rate = sync?.rate ?: 1.0) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(selected = null, message = e.toUiError().message) }
            }
        }
    }

    /** Décale les sous-titres (pas de 0,25 s) et enregistre le réglage dans Lumière. */
    fun nudge(deltaSec: Double) {
        val s = _state.value
        val trackId = s.selected ?: return
        val id = sessionId ?: return
        val offset = ((s.offset + deltaSec) * 100).roundToInt() / 100.0
        _state.update { it.copy(offset = offset, message = null) }
        viewModelScope.launch {
            saveLock.withLock { runCatching { api.saveSubtitleSync(id, trackId, offset, s.rate) } }
        }
    }

    fun autoSync() {
        val trackId = _state.value.selected ?: return
        val id = sessionId ?: return
        _state.update { it.copy(message = "Recalage en cours…") }
        viewModelScope.launch {
            try {
                val result = api.autoSubtitleSync(id, trackId)
                _state.update {
                    it.copy(
                        message = result.message,
                        offset = result.sync?.offset ?: it.offset,
                        rate = result.sync?.rate ?: it.rate,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = e.toUiError().message) }
            }
        }
    }

    /** Cherche des sous-titres en ligne ; avec `autoLanguage`, choisit le premier dans cette langue. */
    fun searchOnline(autoLanguage: String? = null) {
        val id = sessionId ?: return
        _state.update { it.copy(searching = true, message = "Recherche de sous-titres en ligne…") }
        viewModelScope.launch {
            try {
                val result = api.onlineSubtitles(id)
                val known = _state.value.tracks.map { it.id }.toSet()
                _state.update { s ->
                    s.copy(
                        tracks = s.tracks + result.tracks.filter { it.id !in known },
                        searching = false,
                        message = when {
                            result.tracks.isEmpty() -> result.notices.firstOrNull() ?: "Aucun sous-titre trouvé en ligne."
                            else -> null
                        },
                    )
                }
                if (autoLanguage != null) {
                    result.tracks.firstOrNull { it.language == autoLanguage }?.let { select(it.id) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(searching = false, message = e.toUiError().message) }
            }
        }
    }

    fun cuesAt(position: Double): List<Cue> {
        val s = _state.value
        if (s.selected == null || !s.loaded) return emptyList()
        return timeline.activeAt(position, s.offset, s.rate)
    }
}
