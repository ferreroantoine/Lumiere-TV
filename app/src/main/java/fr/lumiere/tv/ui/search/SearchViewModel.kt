package fr.lumiere.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.api.TitleSummary
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Recherche au fil de la saisie : requête envoyée 400 ms après la dernière frappe, dès 2 caractères. */
@OptIn(FlowPreview::class)
class SearchViewModel(private val api: LumiereApi) : ViewModel() {
    private val query = MutableStateFlow("")

    private val _state = MutableStateFlow<UiState<List<TitleSummary>>>(UiState.Ready(emptyList()))
    val state: StateFlow<UiState<List<TitleSummary>>> = _state

    /** Requête réellement recherchée (vide tant qu'aucune recherche n'a été faite). */
    private val _searched = MutableStateFlow("")
    val searched: StateFlow<String> = _searched

    init {
        viewModelScope.launch {
            query.debounce(400).map { it.trim() }.distinctUntilChanged().collectLatest { search(it) }
        }
    }

    fun setQuery(q: String) {
        query.value = q
    }

    /** Nouvel essai après une erreur. */
    fun retry() {
        viewModelScope.launch { search(query.value.trim()) }
    }

    private suspend fun search(q: String) {
        if (q.length < 2) {
            _searched.value = ""
            _state.value = UiState.Ready(emptyList())
            return
        }
        _state.value = UiState.Loading
        _state.value = try {
            UiState.Ready(api.titles(kind = null, q = q, sort = null, genre = null, page = 1).items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toUiError()
        }
        _searched.value = q
    }
}
