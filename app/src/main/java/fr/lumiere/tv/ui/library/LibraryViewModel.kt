package fr.lumiere.tv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.data.api.LibraryItem
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LibraryContent(val movies: List<LibraryItem>, val episodes: List<LibraryItem>)

/** Fichiers téléchargés : films et épisodes présents sur le disque du serveur. */
class LibraryViewModel(private val api: LumiereApi) : ViewModel() {
    private val _state = MutableStateFlow<UiState<LibraryContent>>(UiState.Loading)
    val state: StateFlow<UiState<LibraryContent>> = _state

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            if (_state.value !is UiState.Ready) _state.value = UiState.Loading
            _state.value = try {
                val items = api.library().filter { it.exists }
                UiState.Ready(
                    LibraryContent(
                        movies = items.filter { it.kind == "movie" }.sortedBy { it.title.lowercase() },
                        episodes = items.filter { it.kind == "episode" }
                            .sortedWith(compareBy({ it.title.lowercase() }, { it.season ?: 0 }, { it.episode ?: 0 })),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.toUiError()
            }
        }
    }
}
