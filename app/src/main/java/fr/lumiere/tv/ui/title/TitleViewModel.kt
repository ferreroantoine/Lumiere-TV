package fr.lumiere.tv.ui.title

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.api.RemoteFile
import fr.lumiere.tv.data.api.TitleDetails
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Versions demandées pour un film ou un épisode (panneau « Versions »). */
data class VersionsRequest(val label: String, val season: Int?, val episode: Int?)

data class VersionsPanel(val request: VersionsRequest, val state: UiState<List<RemoteFile>>, val notice: String? = null)

class TitleViewModel(private val api: LumiereApi, private val titleId: Int) : ViewModel() {
    private val _state = MutableStateFlow<UiState<TitleDetails>>(UiState.Loading)
    val state: StateFlow<UiState<TitleDetails>> = _state

    private val _season = MutableStateFlow<Int?>(null)
    val season: StateFlow<Int?> = _season

    private val _versions = MutableStateFlow<VersionsPanel?>(null)
    val versions: StateFlow<VersionsPanel?> = _versions
    private var versionsJob: Job? = null

    init {
        reload()
    }

    /** Recharge la fiche (au retour du lecteur : positions de visionnage à jour). */
    fun reload() {
        viewModelScope.launch {
            if (_state.value !is UiState.Ready) _state.value = UiState.Loading
            _state.value = try {
                val details = api.title(titleId)
                if (_season.value == null) {
                    _season.value = details.seasons.firstOrNull { (it.number ?: 0) > 0 }?.number
                        ?: details.seasons.firstOrNull()?.number
                }
                UiState.Ready(details)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.toUiError()
            }
        }
    }

    fun selectSeason(number: Int?) {
        _season.value = number
    }

    fun openVersions(request: VersionsRequest) {
        versionsJob?.cancel()
        val details = (_state.value as? UiState.Ready)?.value
        if (request.season == null || request.episode == null) {
            _versions.value = VersionsPanel(request, UiState.Ready(details?.files.orEmpty()), details?.sourceNotice)
            return
        }
        _versions.value = VersionsPanel(request, UiState.Loading)
        versionsJob = viewModelScope.launch {
            _versions.value = try {
                val files = api.episodeFiles(titleId, request.season, request.episode)
                VersionsPanel(request, UiState.Ready(files.files), files.notice)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                VersionsPanel(request, e.toUiError())
            }
        }
    }

    fun closeVersions() {
        versionsJob?.cancel()
        _versions.value = null
    }
}
