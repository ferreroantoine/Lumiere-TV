package fr.lumiere.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import fr.lumiere.tv.data.api.TitleSummary
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Grille d'affiches ; [onNearEnd] est appelé quand les dernières lignes approchent
 * (chargement de la page suivante).
 */
@Composable
fun PosterGrid(
    titles: List<TitleSummary>,
    imageUrl: (String) -> String,
    onOpen: (TitleSummary) -> Unit,
    modifier: Modifier = Modifier,
    onFocus: (TitleSummary) -> Unit = {},
    onNearEnd: () -> Unit = {},
    firstItemFocus: FocusRequester? = null,
    columns: Int = 5,
) {
    val state = rememberLazyGridState()
    LaunchedEffect(state, titles.size) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> if (titles.isNotEmpty() && last >= titles.size - columns * 2) onNearEnd() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        // Marge pour l'affiche agrandie au focus et son halo.
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp, start = 12.dp, end = 12.dp),
        // En bout de ligne, Droite ne doit pas faire sauter le focus hors de la grille ;
        // Gauche, depuis la première colonne, ouvre le menu.
        modifier = modifier
            .leftExitsTo(LocalSidebarFocus.current) { it == FocusDirection.Right }
            .focusGroup(),
    ) {
        itemsIndexed(titles, key = { _, t -> t.id }) { index, title ->
            PosterCard(
                title = title.title,
                imageUrl = title.posterUrl?.let(imageUrl),
                onClick = { onOpen(title) },
                onFocus = { onFocus(title) },
                modifier = if (index == 0 && firstItemFocus != null) Modifier.focusRequester(firstItemFocus) else Modifier,
            )
        }
    }
}
