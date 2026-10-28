package fr.lumiere.tv.ui.catalog

import fr.lumiere.tv.data.api.Page
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Chargement page par page (défilement infini) ; une seule page demandée à la fois. */
class PagedLoader<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend (page: Int) -> Page<T>,
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items

    private val _state = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val state: StateFlow<UiState<Unit>> = _state

    private var loaded = 0
    private var hasMore = true
    private var job: Job? = null

    fun loadMore() {
        if (job?.isActive == true || !hasMore) return
        val next = loaded + 1
        _state.value = UiState.Loading
        job = scope.launch {
            try {
                val page = fetch(next)
                _items.value = _items.value + page.items
                loaded = next
                hasMore = page.hasMore
                _state.value = UiState.Ready(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = e.toUiError()
            }
        }
    }

    fun reset() {
        job?.cancel()
        job = null
        loaded = 0
        hasMore = true
        _items.value = emptyList()
        loadMore()
    }
}
