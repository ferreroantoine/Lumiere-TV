package fr.lumiere.tv.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.data.api.TitleSummary
import fr.lumiere.tv.ui.home.metaLine
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.EmptyMessage
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.PosterGrid
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.navigation.Navigator
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.lumiereChipColors

/** Catalogue des films ou des séries : tri, genre et grille d'affiches sans fin. */
@OptIn(ExperimentalTvMaterial3Api::class)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (kind == TitleKind.MOVIE) "Films" else "Séries", style = MaterialTheme.typography.headlineMedium)
            SortOption.entries.forEach { option ->
                FilterChip(selected = sort == option, onClick = { vm.setSort(option) }, colors = lumiereChipColors()) { Text(option.label) }
            }
        }
        if (genres.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                item(key = "tous") {
                    FilterChip(selected = genre == null, onClick = { vm.setGenre(null) }, colors = lumiereChipColors()) { Text("Tous les genres") }
                }
                items(genres, key = { it.name }) { g ->
                    FilterChip(selected = genre == g.name, onClick = { vm.setGenre(g.name) }, colors = lumiereChipColors()) { Text(g.name) }
                }
            }
        }
        Text(
            focused?.let { "${it.title}  ·  ${it.metaLine()}" } ?: " ",
            style = MaterialTheme.typography.titleMedium,
            color = LumiereColors.Brand200,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
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
