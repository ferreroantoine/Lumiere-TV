package fr.lumiere.tv.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.data.api.TitleSummary
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.EmptyMessage
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.GlassChip
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.MetaRow
import fr.lumiere.tv.ui.components.PosterGrid
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.navigation.Navigator
import fr.lumiere.tv.ui.theme.LumiereColors

/** Catalogue des films ou des séries : tri, genre et grille d'affiches sans fin. */
@Composable
fun CatalogScreen(kind: TitleKind, navigator: Navigator) {
    val vm = containerViewModel(key = "catalog-${kind.apiValue}") { CatalogViewModel(it.api, kind) }
    val api = appContainer().api
    val items by vm.loader.items.collectAsStateWithLifecycle()
    val state by vm.loader.state.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val genre by vm.genre.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    val gridFocus = remember { FocusRequester() }
    var focused by remember { mutableStateOf<TitleSummary?>(null) }
    val hasItems = items.isNotEmpty()
    LaunchedEffect(hasItems) { if (hasItems) runCatching { gridFocus.requestFocus() } }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (kind == TitleKind.MOVIE) "Films" else "Séries",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(end = 12.dp),
            )
            SortOption.entries.forEach { option ->
                GlassChip(option.label, selected = sort == option, onClick = { vm.setSort(option) })
            }
        }
        if (genres.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
            ) {
                item(key = "tous") {
                    GlassChip("Tous les genres", selected = genre == null, onClick = { vm.setGenre(null) })
                }
                items(genres, key = { it.name }) { g ->
                    GlassChip(g.name, selected = genre == g.name, onClick = { vm.setGenre(g.name) })
                }
            }
        }
        // Titre qui a le focus : nom puis ligne d'informations de Lumière.
        Box(Modifier.height(28.dp).padding(start = 4.dp), contentAlignment = Alignment.CenterStart) {
            focused?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MetaRow(null, it.year, it.rating, it.genres.take(2))
                }
            }
        }
        when {
            hasItems -> PosterGrid(
                titles = items,
                imageUrl = api::absoluteUrl,
                onOpen = { navigator.openTitle(it.id) },
                onFocus = {
                    focused = it
                    navigator.setAmbient(it.posterUrl?.let(api::absoluteUrl))
                },
                onNearEnd = vm.loader::loadMore,
                firstItemFocus = gridFocus,
                modifier = Modifier.weight(1f),
            )
            state is UiState.Error -> {
                val error = state as UiState.Error
                ErrorScreen(error.message, onRetry = vm.loader::reset, onChangeServer = navigator::changeServer.takeIf { error.unreachable })
            }
            state is UiState.Loading -> LoadingScreen()
            else -> EmptyMessage("Aucun titre pour ces critères.")
        }
        if (hasItems && state is UiState.Error) {
            Text((state as UiState.Error).message, color = LumiereColors.Error, modifier = Modifier.padding(8.dp))
        }
    }
}
