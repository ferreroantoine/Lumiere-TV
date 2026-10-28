package fr.lumiere.tv.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.data.api.GenreCount
import fr.lumiere.tv.data.api.LumiereApi
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.data.api.TitleSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Tri du catalogue : valeur de l'API et libellé. */
enum class SortOption(val apiValue: String, val label: String) {
    Popular("popular", "Popularité"),
    Recent("recent", "Sorties récentes"),
    Title("title", "Titre"),
    Rating("rating", "Note"),
}

/** Catalogue des films ou des séries, page par page, avec tri et genre. */
class CatalogViewModel(private val api: LumiereApi, private val kind: TitleKind) : ViewModel() {
    private val _sort = MutableStateFlow(SortOption.Popular)
    val sort: StateFlow<SortOption> = _sort

    private val _genre = MutableStateFlow<String?>(null)
    val genre: StateFlow<String?> = _genre

    private val _genres = MutableStateFlow<List<GenreCount>>(emptyList())
    val genres: StateFlow<List<GenreCount>> = _genres

    val loader = PagedLoader<TitleSummary>(viewModelScope) { page ->
        api.titles(kind = kind, q = null, sort = _sort.value.apiValue, genre = _genre.value, page = page)
    }

    init {
        loader.loadMore()
        viewModelScope.launch { _genres.value = runCatching { api.genres(kind) }.getOrDefault(emptyList()) }
    }

    fun setSort(sort: SortOption) {
        if (_sort.value == sort) return
        _sort.value = sort
        loader.reset()
    }

    fun setGenre(genre: String?) {
        if (_genre.value == genre) return
        _genre.value = genre
        loader.reset()
    }
}
