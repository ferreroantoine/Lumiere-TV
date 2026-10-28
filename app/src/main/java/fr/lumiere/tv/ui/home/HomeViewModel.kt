package fr.lumiere.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.data.api.ContinueWatchingItem
import fr.lumiere.tv.data.api.LibraryItem
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.api.TitleSummary
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeContent(
    val featuredCandidates: List<TitleSummary>,
    val resume: List<ContinueWatchingItem>,
    val movies: List<TitleSummary>,
    val series: List<TitleSummary>,
    val library: List<LibraryItem>,
) {
    val featured: TitleSummary? get() = featuredCandidates.firstOrNull()
}

class HomeViewModel(private val api: LumiereApi) : ViewModel() {
    private val _state = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val state: StateFlow<UiState<HomeContent>> = _state

    init {
        reload()
    }

    fun reload() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = try {
                // coroutineScope : la première erreur remonte telle quelle (et annule les autres appels).
                coroutineScope {
                    val home = async { api.home() }
                    val resume = async { api.continueWatching() }
                    val library = async { api.library() }
                    val feed = home.await()
                    UiState.Ready(
                        HomeContent(
                            featuredCandidates = feed.featured.ifEmpty { feed.popularMovies },
                            resume = resume.await(),
                            movies = feed.popularMovies,
                            series = feed.popularSeries,
                            library = library.await().filter { it.exists },
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.toUiError()
            }
        }
    }
}
